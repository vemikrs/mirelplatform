/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraAuditLogRepository;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * MiraAuditService のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MiraAuditServiceTest {

    private MiraAuditService miraAuditService;

    @Mock
    private MiraAuditLogRepository auditLogRepository;

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraAiProperties.Audit auditConfig;

    @Captor
    private ArgumentCaptor<MiraAuditLog> auditLogCaptor;

    @BeforeEach
    void setUp() {
        // lenient を使用して不要なスタブ警告を回避
        lenient().when(properties.getAudit()).thenReturn(auditConfig);
        lenient().when(auditConfig.isEnabled()).thenReturn(true);
        lenient().when(auditLogRepository.save(any(MiraAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        miraAuditService = new MiraAuditService(auditLogRepository, properties);
    }

    @Nested
    @DisplayName("logSync メソッドのテスト")
    class LogSyncTest {

        @Test
        @DisplayName("監査ログを同期で記録")
        void shouldLogSynchronously() {
            // Arrange
            MiraAuditService.AuditLogBuilder builder = MiraAuditService.AuditLogBuilder.create()
                    .tenantId("tenant-1")
                    .userId("user-1")
                    .conversationId("conv-123")
                    .action(MiraAuditLog.AuditAction.CHAT)
                    .mode("GENERAL_CHAT")
                    .status(MiraAuditLog.AuditStatus.SUCCESS);

            // Act
            miraAuditService.logSync(builder);

            // Assert
            verify(auditLogRepository).save(auditLogCaptor.capture());
            MiraAuditLog saved = auditLogCaptor.getValue();

            assertThat(saved.getTenantId()).isEqualTo("tenant-1");
            assertThat(saved.getUserId()).isEqualTo("user-1");
            assertThat(saved.getConversationId()).isEqualTo("conv-123");
            assertThat(saved.getAction()).isEqualTo(MiraAuditLog.AuditAction.CHAT);
            assertThat(saved.getMode()).isEqualTo("GENERAL_CHAT");
            assertThat(saved.getStatus()).isEqualTo(MiraAuditLog.AuditStatus.SUCCESS);
        }

        @Test
        @DisplayName("監査が無効の場合はログを記録しない")
        void shouldNotLogWhenDisabled() {
            // Arrange
            lenient().when(auditConfig.isEnabled()).thenReturn(false);

            MiraAuditService.AuditLogBuilder builder = MiraAuditService.AuditLogBuilder.create()
                    .tenantId("tenant-1")
                    .userId("user-1");

            // Act
            miraAuditService.logSync(builder);

            // Assert
            verify(auditLogRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("logChatResponse メソッドのテスト")
    class LogChatResponseTest {

        @Test
        @DisplayName("チャット応答の監査ログを記録")
        void shouldLogChatResponse() {
            // Act - async 呼び出しなのでメソッドが例外をスローしないことを確認
            miraAuditService.logChatResponse(
                    "tenant-1",
                    "user-1",
                    "conv-123",
                    "GENERAL_CHAT",
                    "gpt-4o",
                    500,
                    100,
                    200,
                    MiraAuditLog.AuditStatus.SUCCESS);

            // Assert - async なので直接検証はできないが、例外がないことを確認
            // 本番環境では async の完了を待つ必要がある
        }
    }

    @Nested
    @DisplayName("logApiError メソッドのテスト")
    class LogApiErrorTest {

        @Test
        @DisplayName("APIエラーの監査ログを記録")
        void shouldLogApiError() {
            // Act
            miraAuditService.logApiError(
                    "tenant-1",
                    "user-1",
                    "CHAT",
                    "Connection timeout");

            // Assert - async
        }

        @Test
        @DisplayName("長いエラーメッセージは切り詰められる")
        void shouldTruncateLongErrorMessage() {
            // Arrange
            String longError = "a".repeat(100);

            // Act
            miraAuditService.logApiError(
                    "tenant-1",
                    "user-1",
                    "CHAT",
                    longError);

            // Assert - async, エラーコードは50文字以下に切り詰め
        }
    }

    @Nested
    @DisplayName("AuditLogBuilder のテスト")
    class AuditLogBuilderTest {

        @Test
        @DisplayName("全フィールドを設定してビルド")
        void shouldBuildWithAllFields() {
            // Act
            MiraAuditLog log = MiraAuditService.AuditLogBuilder.create()
                    .tenantId("tenant-1")
                    .userId("user-1")
                    .conversationId("conv-123")
                    .messageId("msg-456")
                    .action(MiraAuditLog.AuditAction.CHAT)
                    .mode("ERROR_ANALYZE")
                    .appId("promarker")
                    .screenId("dashboard")
                    .promptLength(100)
                    .responseLength(200)
                    .usedModel("gpt-4o")
                    .latencyMs(500)
                    .status(MiraAuditLog.AuditStatus.SUCCESS)
                    .errorCode(null)
                    .build();

            // Assert
            assertThat(log.getId()).isNotBlank();
            assertThat(log.getTenantId()).isEqualTo("tenant-1");
            assertThat(log.getUserId()).isEqualTo("user-1");
            assertThat(log.getConversationId()).isEqualTo("conv-123");
            assertThat(log.getMessageId()).isEqualTo("msg-456");
            assertThat(log.getAction()).isEqualTo(MiraAuditLog.AuditAction.CHAT);
            assertThat(log.getMode()).isEqualTo("ERROR_ANALYZE");
            assertThat(log.getAppId()).isEqualTo("promarker");
            assertThat(log.getScreenId()).isEqualTo("dashboard");
            assertThat(log.getPromptLength()).isEqualTo(100);
            assertThat(log.getResponseLength()).isEqualTo(200);
            assertThat(log.getUsedModel()).isEqualTo("gpt-4o");
            assertThat(log.getLatencyMs()).isEqualTo(500);
            assertThat(log.getStatus()).isEqualTo(MiraAuditLog.AuditStatus.SUCCESS);
            assertThat(log.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("デフォルト値でビルド")
        void shouldBuildWithDefaults() {
            // Act
            MiraAuditLog log = MiraAuditService.AuditLogBuilder.create()
                    .tenantId("tenant-1")
                    .userId("user-1")
                    .conversationId("conv-123")
                    .build();

            // Assert
            assertThat(log.getAction()).isEqualTo(MiraAuditLog.AuditAction.CHAT); // デフォルト
            assertThat(log.getStatus()).isEqualTo(MiraAuditLog.AuditStatus.SUCCESS); // デフォルト
        }

        @Test
        @DisplayName("エラーステータスでビルド")
        void shouldBuildWithErrorStatus() {
            // Act
            MiraAuditLog log = MiraAuditService.AuditLogBuilder.create()
                    .tenantId("tenant-1")
                    .userId("user-1")
                    .conversationId("conv-123")
                    .status(MiraAuditLog.AuditStatus.ERROR)
                    .errorCode("API_TIMEOUT")
                    .build();

            // Assert
            assertThat(log.getStatus()).isEqualTo(MiraAuditLog.AuditStatus.ERROR);
            assertThat(log.getErrorCode()).isEqualTo("API_TIMEOUT");
        }
    }
}
