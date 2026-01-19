package jp.vemi.mirel.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.web.api.auth.dto.LoginRequest;

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
public class JwtLoginIntegrationTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String email = "user01@example.com";
    private String userId = "user-regular-001";
    private String password = "password123";

    @BeforeEach
    void setUp() {
        // 既存のテストユーザーがあれば削除
        userRepository.findById(userId).ifPresent(u -> userRepository.delete(u));
        systemUserRepository.findByEmail(email).ifPresent(su -> systemUserRepository.delete(su));

        // SystemUser 作成 (email_verified=true が重要)
        SystemUser systemUser = new SystemUser();
        systemUser.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"));
        systemUser.setUsername("user01");
        systemUser.setEmail(email);
        systemUser.setPasswordHash(passwordEncoder.encode(password));
        systemUser.setEmailVerified(true); // email_verified=true を明示的に設定
        systemUser.setIsActive(true);
        systemUser.setAccountLocked(false);
        systemUserRepository.save(systemUser);

        // Application User 作成
        User appUser = new User();
        appUser.setUserId(userId);
        appUser.setSystemUserId(systemUser.getId());
        appUser.setTenantId("default");
        appUser.setUsername("user01");
        appUser.setEmail(email);
        appUser.setDisplayName("Regular User 01");
        appUser.setFirstName("Test");
        appUser.setLastName("User01");
        appUser.setIsActive(true);
        appUser.setEmailVerified(true);
        appUser.setRoles("USER");
        appUser.setLastLoginAt(Instant.now());
        userRepository.save(appUser);
    }

    @Test
    public void loginAndAccessProtectedResources() throws Exception {
        // 1. Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("user01@example.com");
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
                .andExpect(jsonPath("$.email").value("user01@example.com"));

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
