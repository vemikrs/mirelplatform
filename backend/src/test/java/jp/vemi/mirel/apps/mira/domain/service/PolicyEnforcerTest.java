/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.infrastructure.security.PiiMasker;
import jp.vemi.mirel.apps.mira.infrastructure.security.PromptInjectionDetector;

/**
 * PolicyEnforcer のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class PolicyEnforcerTest {

    @Mock
    private PiiMasker piiMasker;

    @Mock
    private PromptInjectionDetector promptInjectionDetector;

    @InjectMocks
    private PolicyEnforcer policyEnforcer;

    @BeforeEach
    void setUp() {
        // デフォルトのモック動作を設定（必要に応じて各テストで上書き）
        // lenient()を使って、不要なスタブ設定による例外を防ぐ
        org.mockito.Mockito.lenient().when(promptInjectionDetector.check(anyString()))
                .thenReturn(PromptInjectionDetector.InjectionCheckResult.safe());
        org.mockito.Mockito.lenient().when(piiMasker.mask(anyString())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Nested
    @DisplayName("validateRequest メソッドのテスト")
    class ValidateRequestTest {

        @Test
        @DisplayName("正常なリクエストは許可")
        void shouldAllowValidRequest() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("この機能の使い方を教えてください")
                            .build())
                    .build();

            // Act
            PolicyEnforcer.ValidationResult result = policyEnforcer.validateRequest(request);

            // Assert
            assertThat(result.valid()).isTrue();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("メッセージ長超過は拒否")
        void shouldRejectTooLongMessage() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("a".repeat(15000))
                            .build())
                    .build();

            // Act
            PolicyEnforcer.ValidationResult result = policyEnforcer.validateRequest(request);

            // Assert
            assertThat(result.valid()).isFalse();
            assertThat(result.errorMessage()).contains("長すぎます");
        }

        @Test
        @DisplayName("禁止キーワード(DROP TABLE)を含むメッセージは拒否")
        void shouldRejectDropTableKeyword() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("DROP TABLE users")
                            .build())
                    .build();

            // Act
            PolicyEnforcer.ValidationResult result = policyEnforcer.validateRequest(request);

            // Assert
            assertThat(result.valid()).isFalse();
            assertThat(result.errorMessage()).contains("許可されていません");
        }

        @Test
        @DisplayName("禁止キーワード(rm -rf)を含むメッセージは拒否")
        void shouldRejectRmRfKeyword() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("rm -rf /")
                            .build())
                    .build();

            // Act
            PolicyEnforcer.ValidationResult result = policyEnforcer.validateRequest(request);

            // Assert
            assertThat(result.valid()).isFalse();
        }

        @Test
        @DisplayName("空メッセージは拒否")
        void shouldRejectEmptyMessage() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(null)
                    .build();

            // Act
            PolicyEnforcer.ValidationResult result = policyEnforcer.validateRequest(request);

            // Assert
            assertThat(result.valid()).isFalse();
            assertThat(result.errorMessage()).contains("空");
        }
    }

    @Nested
    @DisplayName("canAccessFeature メソッドのテスト")
    class CanAccessFeatureTest {

        @Test
        @DisplayName("一般機能は誰でもアクセス可")
        void shouldAllowAccessToGeneralFeature() {
            // Act
            boolean result = policyEnforcer.canAccessFeature("general_feature", "USER");

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("管理者専用機能にADMINはアクセス可")
        void shouldAllowAdminToAccessAdminFeature() {
            // Act
            boolean result = policyEnforcer.canAccessFeature("user_management", "ADMIN");

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("管理者専用機能に一般ユーザーはアクセス不可")
        void shouldDenyUserFromAdminFeature() {
            // Act
            boolean result = policyEnforcer.canAccessFeature("user_management", "USER");

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("SYSTEM_ADMINは管理者専用機能にアクセス可")
        void shouldAllowSystemAdminToAccessAdminFeature() {
            // Act
            boolean result = policyEnforcer.canAccessFeature("audit_log_export", "SYSTEM_ADMIN");

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("canAccessMode メソッドのテスト")
    class CanAccessModeTest {

        @Test
        @DisplayName("GENERAL_CHATは誰でもアクセス可")
        void shouldAllowAccessToGeneralChat() {
            // Act
            boolean result = policyEnforcer.canAccessMode(MiraMode.GENERAL_CHAT, "USER", null);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("STUDIO_AGENTは開発者ロールのみ")
        void shouldAllowDeveloperToAccessStudioAgent() {
            // Act
            boolean resultDev = policyEnforcer.canAccessMode(MiraMode.STUDIO_AGENT, "DEVELOPER", null);
            boolean resultAdmin = policyEnforcer.canAccessMode(MiraMode.STUDIO_AGENT, "ADMIN", null);
            boolean resultUser = policyEnforcer.canAccessMode(MiraMode.STUDIO_AGENT, "USER", null);

            // Assert
            assertThat(resultDev).isTrue();
            assertThat(resultAdmin).isTrue();
            assertThat(resultUser).isFalse();
        }

        @Test
        @DisplayName("appRoleでも開発者権限が認められる")
        void shouldAllowAppRoleDeveloper() {
            // Act
            boolean result = policyEnforcer.canAccessMode(MiraMode.STUDIO_AGENT, "USER", "DEV");

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("filterResponse メソッドのテスト")
    class FilterResponseTest {

        @Test
        @DisplayName("通常テキストはそのまま返す")
        void shouldReturnNormalText() {
            when(piiMasker.mask(anyString())).thenReturn("通常の応答です");

            // Act
            String result = policyEnforcer.filterResponse("通常の応答です", "USER");

            // Assert
            assertThat(result).isEqualTo("通常の応答です");
        }

        @Test
        @DisplayName("パスワード情報をマスク")
        void shouldMaskPassword() {
            when(piiMasker.mask(anyString())).thenReturn("password=[REDACTED]");

            // Act
            String result = policyEnforcer.filterResponse("password=secret123", "USER");

            // Assert
            assertThat(result).contains("[REDACTED]");
            assertThat(result).doesNotContain("secret123");
        }

        @Test
        @DisplayName("APIキー情報をマスク")
        void shouldMaskApiKey() {
            when(piiMasker.mask(anyString())).thenReturn("api_key=[REDACTED]");

            // Act
            String result = policyEnforcer.filterResponse("api_key=sk-1234567890", "USER");

            // Assert
            assertThat(result).contains("[REDACTED]");
            assertThat(result).doesNotContain("sk-1234567890");
        }

        @Test
        @DisplayName("JDBCURLをマスク")
        void shouldMaskJdbcUrl() {
            when(piiMasker.mask(anyString())).thenReturn("接続先: [REDACTED]");

            // Act
            String result = policyEnforcer.filterResponse(
                    "接続先: jdbc://localhost:5432/db", "USER");

            // Assert
            assertThat(result).contains("[REDACTED]");
        }

        @Test
        @DisplayName("非管理者にはIPアドレスをマスク")
        void shouldMaskIpForNonAdmin() {
            // PiiMaskerはそのまま通すが、filterResponse内のmaskSystemDetailsでIPが隠されることを期待
            when(piiMasker.mask(anyString())).thenAnswer(i -> i.getArguments()[0]);

            // Act
            String result = policyEnforcer.filterResponse(
                    "サーバー: 192.168.1.100", "USER");

            // Assert
            assertThat(result).contains("[IP]");
            assertThat(result).doesNotContain("192.168.1.100");
        }

        @Test
        @DisplayName("管理者にはIPアドレスを表示")
        void shouldShowIpForAdmin() {
            when(piiMasker.mask(anyString())).thenAnswer(i -> i.getArguments()[0]);

            // Act
            String result = policyEnforcer.filterResponse(
                    "サーバー: 192.168.1.100", "ADMIN");

            // Assert
            assertThat(result).contains("192.168.1.100");
        }
    }

    @Nested
    @DisplayName("sanitizePayload メソッドのテスト")
    class SanitizePayloadTest {

        @Test
        @DisplayName("通常のペイロードはそのまま返す")
        void shouldReturnNormalPayload() {
            // Arrange
            Map<String, Object> payload = new HashMap<>();
            payload.put("screenId", "dashboard");
            payload.put("count", 10);

            // Act
            Map<String, Object> result = policyEnforcer.sanitizePayload(payload);

            // Assert
            assertThat(result).containsEntry("screenId", "dashboard");
            assertThat(result).containsEntry("count", 10);
        }

        @Test
        @DisplayName("機密フィールドを除去")
        void shouldRemoveSensitiveFields() {
            // Arrange
            Map<String, Object> payload = new HashMap<>();
            payload.put("screenId", "dashboard");
            payload.put("password", "secret");
            payload.put("apiKey", "sk-123");
            payload.put("token", "jwt-token");

            // Act
            Map<String, Object> result = policyEnforcer.sanitizePayload(payload);

            // Assert
            assertThat(result).containsKey("screenId");
            assertThat(result).doesNotContainKey("password");
            assertThat(result).doesNotContainKey("apiKey");
            assertThat(result).doesNotContainKey("token");
        }

        @Test
        @DisplayName("nullペイロードはnullを返す")
        void shouldReturnNullForNullPayload() {
            // Act
            Map<String, Object> result = policyEnforcer.sanitizePayload(null);

            // Assert
            assertThat(result).isNull();
        }
    }
}
