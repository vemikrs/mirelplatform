package jp.vemi.mirel.apps.auth.api;

import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.foundation.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OTPログイン統合テスト: /auth/otp/request -> /auth/otp/verify -> /users/me
 * principal が User.userId になり、セッション認証後に /users/me が 200 を返すことを検証する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("OTPログイン統合テスト")
class OtpLoginIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SystemUserRepository systemUserRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestEmailService testEmailService;

    private String email;
    private String userId;

    @TestConfiguration
    static class Cfg {
        @Bean
        TestEmailService testEmailService() {
            return new TestEmailService();
        }

        /**
         * テスト用EmailServiceスタブ。OTPメール送信で埋め込まれる model の otpCode を捕捉。
         */
        static class TestEmailService implements EmailService {
            volatile String lastOtpCode;
            @Override
            public void sendTemplateEmail(String to, String subject, String template, Map<String, Object> model) {
                if ("otp-login".equals(template)) {
                    Object code = model.get("otpCode");
                    if (code != null) {
                        lastOtpCode = code.toString();
                    }
                }
            }
            @Override
            public void sendPlainTextEmail(String to, String subject, String body) {
                // not used
            }
        }
    }

    @BeforeEach
    void setup() {
        email = "otp-inttest@example.com";
        userId = "u-otp-inttest";

        // 既存残骸があれば削除
        userRepository.findById(userId).ifPresent(u -> userRepository.delete(u));
        systemUserRepository.findByEmail(email).ifPresent(su -> systemUserRepository.delete(su));

        // SystemUser 作成
        SystemUser su = new SystemUser();
        su.setId(UUID.randomUUID());
        su.setEmail(email);
        su.setPasswordHash("dummy");
        su.setEmailVerified(true);
        su.setIsActive(true);
        systemUserRepository.save(su);

        // Application User 作成 (principal で参照される userId)
        User appUser = new User();
        appUser.setUserId(userId);
        appUser.setEmail(email);
        appUser.setSystemUserId(su.getId());
        appUser.setDisplayName("OTP IntTest");
        appUser.setIsActive(true);
        appUser.setEmailVerified(true);
        appUser.setLastLoginAt(Instant.now());
        userRepository.save(appUser);
    }

    @Test
    @DisplayName("OTPログイン成功後 /users/me が 200")
    void otpLoginFlow() throws Exception {
        // 1) request OTP
        String requestPayload = String.format("{\"model\":{\"email\":\"%s\",\"purpose\":\"LOGIN\"}}", email);
        MvcResult requestResult = mockMvc.perform(post("/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestPayload))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(testEmailService.lastOtpCode).as("OTPコードがメールスタブで捕捉される").isNotBlank();

        // 2) verify OTP
        String verifyPayload = String.format("{\"model\":{\"email\":\"%s\",\"otpCode\":\"%s\",\"purpose\":\"LOGIN\"}}", email, testEmailService.lastOtpCode);
        mockMvc.perform(post("/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyPayload)
                .session(requestResult.getRequest().getSession()))
            .andExpect(status().isOk());

        // 3) /users/me should return 200 using same session (SecurityContext principal = userId)
        MvcResult meResult = mockMvc.perform(get("/users/me")
                .session(requestResult.getRequest().getSession()))
            .andExpect(status().isOk())
            .andReturn();

        String body = meResult.getResponse().getContentAsString();
        assertThat(body).contains(userId);
    }
}
