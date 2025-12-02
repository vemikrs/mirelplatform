package jp.vemi.mirel.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "mipla2.security.api.enabled=false"
})
@AutoConfigureMockMvc
public class GuestModeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void protectedEndpointShouldBeAccessibleInGuestMode() throws Exception {
        // セキュリティ無効時は、Spring Securityの認証チェックをバイパスするため
        // 存在しないリソースへのアクセスは 404 Not Found になる（401/403ではない）
        mockMvc.perform(get("/api/some/protected/resource"))
                .andExpect(status().isNotFound());
    }
}
