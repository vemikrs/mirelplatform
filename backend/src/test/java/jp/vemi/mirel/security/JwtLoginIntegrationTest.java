package jp.vemi.mirel.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.foundation.web.api.auth.dto.LoginRequest;

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
public class JwtLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void loginAndAccessProtectedResources() throws Exception {
        // 1. Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("user@example.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").exists())
                .andReturn();

        String responseContent = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseContent).path("tokens").path("accessToken").asText();

        // 2. Access /users/me
        mockMvc.perform(get("/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-regular-001"))
                .andExpect(jsonPath("$.email").value("user@example.com"));

        // 3. Access /users/me/tenants
        mockMvc.perform(get("/users/me/tenants")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value("default"));

        // 4. Access /users/me/licenses
        mockMvc.perform(get("/users/me/licenses")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].applicationId").value("promarker"));

        // 5. Access /auth/me (Legacy/Auth context endpoint)
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userId").value("user-regular-001"));
    }
}
