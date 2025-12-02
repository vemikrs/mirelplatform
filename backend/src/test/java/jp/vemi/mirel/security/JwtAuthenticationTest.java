package jp.vemi.mirel.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "auth.method=jwt",
    "auth.jwt.enabled=true",
    "auth.jwt.secret=verylongsecretkeythatisatleast32byteslongforsecurityreasons",
    "auth.jwt.expiration=3600",
    "mipla2.security.api.csrf-enabled=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class JwtAuthenticationTest {

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
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie().exists("XSRF-TOKEN"));
    }
}
