package jp.vemi.mirel.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;

@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "auth.method=jwt",
        "auth.jwt.enabled=true",
        "auth.jwt.secret=verylongsecretkeythatisatleast32byteslongforsecurityreasons",
        "auth.jwt.expiration=3600",
        "mipla2.security.api.csrf-enabled=true",
        "mira.ai.provider=mock",
        "mira.ai.mock.enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
public class JwtAuthenticationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        ChatModel mockChatModel() {
            return mock(ChatModel.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheckShouldBePublic() throws Exception {
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk());
    }

    @Test
    void meShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void csrfTokenShouldBeGenerated() throws Exception {
        // CSRFトークンがCookieとして発行されることを確認
        mockMvc.perform(get("/auth/health"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .exists("XSRF-TOKEN"));
    }
}
