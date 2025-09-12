package jp.vemi.mirel.config.properties;

import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Mipla2SecurityPropertiesTest {

    @Autowired
    private Mipla2SecurityProperties properties1;

    @Autowired
    private Mipla2SecurityProperties properties2;

    @Test
    void testSingleton() {
        // 同じインスタンスであることを確認
        assertSame(properties1, properties2);
    }
}
