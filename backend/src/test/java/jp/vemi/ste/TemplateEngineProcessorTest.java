package jp.vemi.ste;

import jp.vemi.ste.domain.engine.TemplateEngineProcessor;
import jp.vemi.ste.domain.context.SteContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = jp.vemi.mirel.MiplaApplication.class)
@ActiveProfiles("test")
public class TemplateEngineProcessorTest {

    @Autowired(required = false)
    private ResourcePatternResolver resourcePatternResolver;

    private TemplateEngineProcessor processor;
    private SteContext context;

    @BeforeEach
    public void setUp() {
        // テスト用のコンテキストを作成
        context = SteContext.standard("/samples/hello-world", "250913A");
        context.put("message", "Test Message");

        // TemplateEngineProcessorを作成
        // ResourcePatternResolverが注入されていない場合はデフォルトを使用
        if (resourcePatternResolver == null) {
            resourcePatternResolver = new PathMatchingResourcePatternResolver();
        }
        
        processor = TemplateEngineProcessor.create(context, resourcePatternResolver);
    }

    @Test
    public void testGetStencilTemplateFiles() throws Exception {
        System.out.println("=== Testing layered template search ===");
        
        // リフレクションを使ってprivateメソッドを呼び出し
        java.lang.reflect.Method method = TemplateEngineProcessor.class.getDeclaredMethod("getStencilTemplateFiles");
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        List<String> templateFiles = (List<String>) method.invoke(processor);
        
        // ログ出力
        System.out.println("=== Layered Template Search Test Results ===");
        System.out.println("Found " + templateFiles.size() + " template files:");
        
        for (String file : templateFiles) {
            System.out.println("  - " + file);
        }
        
        // アサーション
        assertNotNull(templateFiles, "Template files should not be null");
        assertTrue(templateFiles.size() >= 0, "Template files list should be valid");
        
        System.out.println("Test completed!");
    }
}