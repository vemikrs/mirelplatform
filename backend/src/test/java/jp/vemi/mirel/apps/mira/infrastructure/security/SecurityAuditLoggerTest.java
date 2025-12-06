/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraAuditLogRepository;
import jp.vemi.mirel.apps.mira.infrastructure.security.SecurityAuditLogger.SecurityEvent;
import jp.vemi.mirel.apps.mira.infrastructure.security.SecurityAuditLogger.Severity;

/**
 * SecurityAuditLogger のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class SecurityAuditLoggerTest {

    @Mock
    private MiraAuditLogRepository auditLogRepository;

    @Captor
    private ArgumentCaptor<MiraAuditLog> auditLogCaptor;

    private ObjectMapper objectMapper;

    private SecurityAuditLogger logger;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        logger = new SecurityAuditLogger(auditLogRepository, objectMapper);
    }

    @Nested
    @DisplayName("プロンプトインジェクション検知ログテスト")
    class PromptInjectionLogTest {

        @Test
        @DisplayName("プロンプトインジェクション検知をログ")
        void shouldLogPromptInjectionDetection() {
            // Arrange
            String conversationId = "conv-123";
            String userId = "user-456";
            String tenantId = "tenant-789";
            int score = 4;
            List<String> patterns = List.of("ignore_instructions", "system_prompt");

            // Act
            logger.logPromptInjectionEvent(tenantId, userId, conversationId, score, patterns, false);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog captured = auditLogCaptor.getValue();
            
            assertThat(captured.getAction()).isEqualTo(MiraAuditLog.AuditAction.PROMPT_INJECTION_DETECTED);
            assertThat(captured.getConversationId()).isEqualTo(conversationId);
            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getTenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("ブロック時は BLOCKED アクションでログ")
        void shouldLogBlockedWhenBlocked() {
            // Arrange
            String conversationId = "conv-123";
            String userId = "user-456";
            String tenantId = "tenant-789";
            int score = 6;
            List<String> patterns = List.of("jailbreak", "ignore_instructions");

            // Act
            logger.logPromptInjectionEvent(tenantId, userId, conversationId, score, patterns, true);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog captured = auditLogCaptor.getValue();
            
            assertThat(captured.getAction()).isEqualTo(MiraAuditLog.AuditAction.PROMPT_INJECTION_BLOCKED);
        }
    }

    @Nested
    @DisplayName("PII 検知ログテスト")
    class PiiDetectionLogTest {

        @Test
        @DisplayName("PII 検知をログ")
        void shouldLogPiiDetection() {
            // Arrange
            String conversationId = "conv-123";
            String userId = "user-456";
            String tenantId = "tenant-789";
            List<String> piiTypes = List.of("email", "phone");

            // Act
            logger.logPiiDetectedEvent(tenantId, userId, conversationId, piiTypes);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog captured = auditLogCaptor.getValue();
            
            assertThat(captured.getAction()).isEqualTo(MiraAuditLog.AuditAction.PII_DETECTED);
        }
    }

    @Nested
    @DisplayName("出力フィルタリングログテスト")
    class OutputFilterLogTest {

        @Test
        @DisplayName("出力フィルタリングをログ")
        void shouldLogOutputFiltered() {
            // Arrange
            String conversationId = "conv-123";
            String userId = "user-456";
            String tenantId = "tenant-789";
            List<String> patterns = List.of("system_prompt_reference");

            // Act
            logger.logOutputFiltered(tenantId, userId, conversationId, patterns);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog captured = auditLogCaptor.getValue();
            
            assertThat(captured.getAction()).isEqualTo(MiraAuditLog.AuditAction.OUTPUT_FILTERED);
        }
    }

    @Nested
    @DisplayName("レート制限ログテスト")
    class RateLimitLogTest {

        @Test
        @DisplayName("レート制限超過をログ")
        void shouldLogRateLimitExceeded() {
            // Arrange
            String userId = "user-456";
            String tenantId = "tenant-789";
            int limit = 100;
            int current = 105;

            // Act
            logger.logRateLimitExceeded(tenantId, userId, limit, current);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog captured = auditLogCaptor.getValue();
            
            assertThat(captured.getAction()).isEqualTo(MiraAuditLog.AuditAction.RATE_LIMIT_EXCEEDED);
            assertThat(captured.getTenantId()).isEqualTo(tenantId);
            assertThat(captured.getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("認可拒否ログテスト")
    class AuthorizationDeniedLogTest {

        @Test
        @DisplayName("認可拒否をログ")
        void shouldLogAuthorizationDenied() {
            // Arrange
            String userId = "user-456";
            String tenantId = "tenant-789";
            String resource = "conversation/other-tenant";
            String reason = "cross_tenant_access";

            // Act
            logger.logAuthorizationDenied(tenantId, userId, resource, reason);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog captured = auditLogCaptor.getValue();
            
            assertThat(captured.getAction()).isEqualTo(MiraAuditLog.AuditAction.AUTHORIZATION_DENIED);
        }
    }

    @Nested
    @DisplayName("汎用セキュリティイベントログテスト")
    class GenericSecurityEventLogTest {

        @Test
        @DisplayName("汎用セキュリティイベントをログ")
        void shouldLogGenericSecurityEvent() {
            // Arrange
            SecurityEvent event = SecurityEvent.builder()
                    .action(MiraAuditLog.AuditAction.CROSS_TENANT_ATTEMPT)
                    .severity(Severity.HIGH)
                    .tenantId("tenant-123")
                    .userId("user-456")
                    .conversationId("conv-789")
                    .build();

            // Act
            logger.logSecurityEvent(event);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog captured = auditLogCaptor.getValue();
            
            assertThat(captured.getAction()).isEqualTo(MiraAuditLog.AuditAction.CROSS_TENANT_ATTEMPT);
            assertThat(captured.getTenantId()).isEqualTo("tenant-123");
        }
    }
}
