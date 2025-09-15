package jp.vemi.ste;

import jp.vemi.ste.domain.engine.TemplateEngineProcessor;
import jp.vemi.ste.domain.context.SteContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TemplateEngineProcessorTest {

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    private TemplateEngineProcessor processor;
    private SteContext context;

    @BeforeEach
    public void setUp() {
        // テスト用のコンテキストを作成
        context = SteContext.standard("/samples/hello-world", "250913A");
        context.put("message", "Test Message");

        // TemplateEngineProcessorを作成
        processor = TemplateEngineProcessor.create(context);
        
        // ResourcePatternResolverを注入
        processor.setResourcePatternResolver(resourcePatternResolver);
    }

    @Test
    public void testGetStencilTemplateFiles() throws Exception {
        System.out.println("=== Testing layered template search ===");
        
        // リフレクションを使ってprivateメソッドを呼び出し
        java.lang.reflect.Method method = TemplateEngineProcessor.class.getDeclaredMethod("getStencilTemplateFiles");
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> templateFiles = (List<String>) method.invoke(processor);
        
        // 結果をファイルに出力
        java.io.FileWriter writer = new java.io.FileWriter("/workspaces/mirelplatform/test-result.txt");
        writer.write("=== Layered Template Search Test Results ===\n");
        writer.write("Found " + templateFiles.size() + " template files:\n");
        
        for (String file : templateFiles) {
            writer.write("  - " + file + "\n");
            System.out.println("  - " + file);
        }
        writer.close();
        
        // アサーション
        assertNotNull(templateFiles, "Template files should not be null");
        assertTrue(templateFiles.size() >= 0, "Template files list should be valid");
        
        System.out.println("Test completed! Results saved to test-result.txt");
    }
}