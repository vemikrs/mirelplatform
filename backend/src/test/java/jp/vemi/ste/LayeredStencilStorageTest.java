package jp.vemi.ste;

import jp.vemi.framework.config.StorageConfig;
import jp.vemi.ste.domain.context.SteContext;
import jp.vemi.ste.domain.engine.TemplateEngineProcessor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * レイヤードストレージ構造でのステンシル読み込みテスト
 * 
 * テスト対象:
 * - ユーザーレイヤー (/user) からのステンシル読み込み
 * - シリアル番号の正しい検出
 * - TemplateEngineProcessor.getSerialNos() の動作確認
 */
@SpringBootTest(classes = jp.vemi.mirel.MiplaApplication.class)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LayeredStencilStorageTest {

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    private static final String TEST_STENCIL_CATEGORY = "/test-layer";
    private static final String TEST_STENCIL_ID = "/test-layer/test-stencil";
    private static final String TEST_SERIAL_NO = "250104A";

    private static Path testStencilPath;

    @BeforeEach
    public void setUpTestStencil() throws IOException {
        if (testStencilPath != null && Files.exists(testStencilPath)) {
            // 既に作成済み
            return;
        }

        System.out.println("=== Setting up test stencil in user layer ===");

        // ユーザーレイヤーにテストステンシルを作成
        String userLayerDir = StorageConfig.getUserStencilDir();
        testStencilPath = Paths.get(userLayerDir, "test-layer", "test-stencil", TEST_SERIAL_NO);

        // ディレクトリ作成
        Files.createDirectories(testStencilPath);
        System.out.println("Created test stencil directory: " + testStencilPath);

        // stencil-settings.yml を作成
        Path settingsFile = testStencilPath.resolve("stencil-settings.yml");
        try (FileWriter writer = new FileWriter(settingsFile.toFile())) {
            writer.write("stencil:\n");
            writer.write("  config:\n");
            writer.write("    categoryId: \"" + TEST_STENCIL_CATEGORY + "\"\n");
            writer.write("    categoryName: \"Test Layer Category\"\n");
            writer.write("    id: \"" + TEST_STENCIL_ID + "\"\n");
            writer.write("    name: \"Test Layer Stencil\"\n");
            writer.write("    serial: \"" + TEST_SERIAL_NO + "\"\n");
            writer.write("    lastUpdate: \"2025/01/04\"\n");
            writer.write("    lastUpdateUser: \"test\"\n");
            writer.write("    description: |\n");
            writer.write("      Test stencil for layered storage\n");
            writer.write("  dataElement:\n");
            writer.write("    - id: \"message\"\n");
            writer.write("  dataDomain:\n");
            writer.write("    - id: \"message\"\n");
            writer.write("      name: \"Message\"\n");
            writer.write("      value: \"Hello Test\"\n");
            writer.write("      type: \"text\"\n");
            writer.write("      placeholder: \"Enter message\"\n");
            writer.write("      note: \"Test parameter\"\n");
            writer.write("  codeInfo:\n");
            writer.write("    copyright: \"Test\"\n");
            writer.write("    versionNo: \"1.0\"\n");
            writer.write("    author: \"Test Author\"\n");
            writer.write("    vendor: \"Test Vendor\"\n");
        }
        System.out.println("Created stencil-settings.yml: " + settingsFile);

        // テンプレートファイルを作成
        Path templateFile = testStencilPath.resolve("template.ftl");
        try (FileWriter writer = new FileWriter(templateFile.toFile())) {
            writer.write("package test;\n\n");
            writer.write("/**\n");
            writer.write(" * Test generated class\n");
            writer.write(" * Message: ${message}\n");
            writer.write(" */\n");
            writer.write("public class TestClass {\n");
            writer.write("    private String message = \"${message}\";\n");
            writer.write("}\n");
        }
        System.out.println("Created template.ftl: " + templateFile);

        System.out.println("=== Test stencil setup completed ===");
    }

    @AfterAll
    public static void cleanUpTestStencil() throws IOException {
        System.out.println("=== Cleaning up test stencil ===");
        if (testStencilPath != null && Files.exists(testStencilPath)) {
            // ディレクトリとファイルを削除
            deleteRecursively(testStencilPath.toFile());
            System.out.println("Deleted test stencil directory: " + testStencilPath);
        }
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    @Test
    @Order(1)
    public void testUserLayerStencilExists() {
        System.out.println("=== Test 1: Verify test stencil exists in user layer ===");

        File settingsFile = testStencilPath.resolve("stencil-settings.yml").toFile();
        assertTrue(settingsFile.exists(), "stencil-settings.yml should exist in user layer");

        File templateFile = testStencilPath.resolve("template.ftl").toFile();
        assertTrue(templateFile.exists(), "template.ftl should exist in user layer");

        System.out.println("✓ Test stencil files exist");
    }

    @Test
    @Order(2)
    public void testGetSerialNosFindsUserLayerStencil() {
        System.out.println("=== Test 2: getSerialNos() should find serial number in user layer ===");

        // SteContextを作成 (serialNoは指定しない)
        SteContext context = SteContext.standard(TEST_STENCIL_ID, "");

        // TemplateEngineProcessorを作成
        TemplateEngineProcessor processor = TemplateEngineProcessor.create(context, resourcePatternResolver);

        // getSerialNos()を呼び出し
        List<String> serialNos = processor.getSerialNos();

        System.out.println("Found serial numbers: " + serialNos);

        // アサーション
        assertNotNull(serialNos, "Serial numbers list should not be null");
        assertFalse(serialNos.isEmpty(), "Serial numbers list should not be empty");
        assertTrue(serialNos.contains(TEST_SERIAL_NO),
                "Serial numbers should contain " + TEST_SERIAL_NO + ", but found: " + serialNos);

        System.out.println("✓ getSerialNos() correctly found: " + TEST_SERIAL_NO);
    }

    @Test
    @Order(3)
    public void testTemplateEngineProcessorCreationWithUserLayerStencil() {
        System.out.println("=== Test 3: TemplateEngineProcessor should successfully load user layer stencil ===");

        // SteContextを作成 (serialNoを明示的に指定)
        SteContext context = SteContext.standard(TEST_STENCIL_ID, TEST_SERIAL_NO);
        context.put("message", "Hello from test");

        // TemplateEngineProcessorを作成 - エラーが発生しないことを確認
        assertDoesNotThrow(() -> {
            TemplateEngineProcessor processor = TemplateEngineProcessor.create(context, resourcePatternResolver);
            assertNotNull(processor, "TemplateEngineProcessor should be created successfully");
            System.out.println("✓ TemplateEngineProcessor created successfully");
        });
    }

    @Test
    @Order(4)
    public void testGetStencilSettingsFromUserLayer() throws Exception {
        System.out.println("=== Test 4: getStencilSettings() should load settings from user layer ===");

        // SteContextを作成
        SteContext context = SteContext.standard(TEST_STENCIL_ID, TEST_SERIAL_NO);

        // TemplateEngineProcessorを作成
        TemplateEngineProcessor processor = TemplateEngineProcessor.create(context, resourcePatternResolver);

        // getStencilSettings()を呼び出し
        var settings = processor.getStencilSettings();

        // アサーション
        assertNotNull(settings, "Stencil settings should not be null");
        assertNotNull(settings.getStencil(), "Stencil should not be null");
        assertNotNull(settings.getStencil().getConfig(), "Stencil config should not be null");

        var config = settings.getStencil().getConfig();
        assertEquals(TEST_STENCIL_ID, config.getId(), "Stencil ID should match");
        assertEquals(TEST_SERIAL_NO, config.getSerial(), "Serial number should match");
        assertEquals("Test Layer Stencil", config.getName(), "Stencil name should match");

        System.out.println("✓ Stencil settings loaded correctly:");
        System.out.println("  ID: " + config.getId());
        System.out.println("  Serial: " + config.getSerial());
        System.out.println("  Name: " + config.getName());
    }

    @Test
    @Order(5)
    public void testExistingUserStencilSerialDetection() {
        System.out.println("=== Test 5: Test existing user stencil (springboot/service) ===");

        // 実際に存在するユーザーステンシルをテスト
        String existingStencilId = "/springboot/spring-boot-service";
        String expectedSerial = "250101A";

        // SteContextを作成
        SteContext context = SteContext.standard(existingStencilId, "");

        // TemplateEngineProcessorを作成
        TemplateEngineProcessor processor = TemplateEngineProcessor.create(context, resourcePatternResolver);

        // getSerialNos()を呼び出し
        List<String> serialNos = processor.getSerialNos();

        System.out.println("Found serial numbers for " + existingStencilId + ": " + serialNos);

        // アサーション
        assertNotNull(serialNos, "Serial numbers list should not be null");
        assertFalse(serialNos.isEmpty(),
                "Serial numbers list should not be empty for existing stencil " + existingStencilId);
        assertTrue(serialNos.contains(expectedSerial),
                "Serial numbers should contain " + expectedSerial + " for existing stencil, but found: " + serialNos);

        System.out.println("✓ Existing user stencil serial detected: " + expectedSerial);
    }

    @Test
    @Order(6)
    public void testSampleStencilSerialDetection() {
        System.out.println("=== Test 6: Test sample stencil (samples/hello-world) ===");

        // サンプルステンシルをテスト
        String sampleStencilId = "/samples/hello-world";
        String expectedSerial = "250913A";

        // SteContextを作成
        SteContext context = SteContext.standard(sampleStencilId, "");

        // TemplateEngineProcessorを作成
        TemplateEngineProcessor processor = TemplateEngineProcessor.create(context, resourcePatternResolver);

        // getSerialNos()を呼び出し
        List<String> serialNos = processor.getSerialNos();

        System.out.println("Found serial numbers for " + sampleStencilId + ": " + serialNos);

        // アサーション
        assertNotNull(serialNos, "Serial numbers list should not be null");
        assertFalse(serialNos.isEmpty(),
                "Serial numbers list should not be empty for sample stencil " + sampleStencilId);
        assertTrue(serialNos.contains(expectedSerial),
                "Serial numbers should contain " + expectedSerial + " for sample stencil, but found: " + serialNos);

        System.out.println("✓ Sample stencil serial detected: " + expectedSerial);
    }
}
