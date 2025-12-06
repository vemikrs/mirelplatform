/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ContextSnapshotRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ErrorReportRequest;
import jp.vemi.mirel.apps.mira.domain.dto.response.ChatResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ContextSnapshotResponse;
import jp.vemi.mirel.apps.mira.domain.service.MiraAuditService;
import jp.vemi.mirel.apps.mira.domain.service.MiraChatService;
import jp.vemi.mirel.apps.mira.domain.service.MiraRbacAdapter;
import jp.vemi.mirel.apps.mira.domain.service.MiraTenantContextManager;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;

/**
 * MiraApiController のユニットテスト.
 * 
 * <p>Mockito を使用した純粋なユニットテスト。
 * Spring コンテキストを使用しないため高速に実行できます。</p>
 */
@ExtendWith(MockitoExtension.class)
class MiraApiControllerTest {

    @Mock
    private MiraChatService miraChatService;

    @Mock
    private MiraRbacAdapter rbacAdapter;

    @Mock
    private MiraAuditService auditService;

    @Mock
    private MiraTenantContextManager tenantContextManager;

    @InjectMocks
    private MiraApiController miraApiController;

    @BeforeEach
    void setUp() {
        // lenient を使用して不要なスタブ警告を回避
        lenient().when(tenantContextManager.getCurrentTenantId()).thenReturn("test-tenant");
        lenient().when(tenantContextManager.getCurrentUserId()).thenReturn("test-user");
        lenient().when(tenantContextManager.getCurrentSystemRole()).thenReturn("ADMIN");
        
        // RBAC: デフォルトで Mira 利用可能
        lenient().when(rbacAdapter.canUseMira(anyString(), anyString())).thenReturn(true);
    }

    @Nested
    @DisplayName("chat メソッドのテスト")
    class ChatTest {

        @Test
        @DisplayName("正常なチャットリクエストが成功")
        void shouldSucceedWithValidRequest() {
            // Arrange
            ChatResponse.AssistantMessage assistantMsg = ChatResponse.AssistantMessage.builder()
                    .content("テスト応答です")
                    .contentType("markdown")
                    .build();
            ChatResponse response = ChatResponse.builder()
                    .conversationId("conv-123")
                    .messageId("msg-456")
                    .mode("GENERAL_CHAT")
                    .assistantMessage(assistantMsg)
                    .build();

            given(miraChatService.chat(any(ChatRequest.class), anyString(), anyString()))
                    .willReturn(response);

            ChatRequest chatRequest = ChatRequest.builder()
                    .message(ChatRequest.Message.builder().content("テストメッセージ").build())
                    .mode("GENERAL_CHAT")
                    .build();
            ApiRequest<ChatRequest> request = new ApiRequest<>();
            request.setModel(chatRequest);

            // Act
            ResponseEntity<MiraApiController.MiraChatApiResponse> result = miraApiController.chat(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getConversationId()).isEqualTo("conv-123");
            assertThat(result.getBody().data().getAssistantMessage().getContent()).isEqualTo("テスト応答です");
            assertThat(result.getBody().errors()).isEmpty();
        }

        @Test
        @DisplayName("RBAC拒否で403エラー")
        void shouldReturn403WhenRbacDenied() {
            // Arrange
            given(rbacAdapter.canUseMira(anyString(), anyString())).willReturn(false);

            ChatRequest chatRequest = ChatRequest.builder()
                    .message(ChatRequest.Message.builder().content("テスト").build())
                    .build();
            ApiRequest<ChatRequest> request = new ApiRequest<>();
            request.setModel(chatRequest);

            // Act
            ResponseEntity<MiraApiController.MiraChatApiResponse> result = miraApiController.chat(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().errors()).contains("Mira の利用権限がありません");
        }

        @Test
        @DisplayName("サービス例外で500エラー")
        void shouldReturn500OnServiceException() {
            // Arrange
            given(miraChatService.chat(any(ChatRequest.class), anyString(), anyString()))
                    .willThrow(new RuntimeException("AI service unavailable"));

            ChatRequest chatRequest = ChatRequest.builder()
                    .message(ChatRequest.Message.builder().content("テスト").build())
                    .build();
            ApiRequest<ChatRequest> request = new ApiRequest<>();
            request.setModel(chatRequest);

            // Act
            ResponseEntity<MiraApiController.MiraChatApiResponse> result = miraApiController.chat(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().errors()).contains("チャット処理中にエラーが発生しました");
        }
    }

    @Nested
    @DisplayName("saveContextSnapshot メソッドのテスト")
    class ContextSnapshotTest {

        @Test
        @DisplayName("コンテキストスナップショット保存が成功")
        void shouldSucceedSavingContextSnapshot() {
            // Arrange
            ContextSnapshotResponse response = ContextSnapshotResponse.builder()
                    .snapshotId("snapshot-123")
                    .createdAt(java.time.LocalDateTime.now())
                    .build();

            given(miraChatService.saveContextSnapshot(any(ContextSnapshotRequest.class), anyString(), anyString()))
                    .willReturn(response);

            ContextSnapshotRequest snapshotRequest = ContextSnapshotRequest.builder()
                    .appId("promarker")
                    .screenId("stencil-editor")
                    .build();
            ApiRequest<ContextSnapshotRequest> request = new ApiRequest<>();
            request.setModel(snapshotRequest);

            // Act
            ResponseEntity<MiraApiController.MiraSnapshotApiResponse> result = 
                    miraApiController.saveContextSnapshot(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getSnapshotId()).isEqualTo("snapshot-123");
        }
    }

    @Nested
    @DisplayName("analyzeError メソッドのテスト")
    class ErrorReportTest {

        @Test
        @DisplayName("エラー分析が成功")
        void shouldSucceedWithValidErrorReport() {
            // Arrange
            ChatResponse.AssistantMessage assistantMsg = ChatResponse.AssistantMessage.builder()
                    .content("## エラー原因\nテストエラーの分析結果")
                    .contentType("markdown")
                    .build();
            ChatResponse response = ChatResponse.builder()
                    .conversationId("conv-err-1")
                    .messageId("msg-err-1")
                    .mode("ERROR_ANALYZE")
                    .assistantMessage(assistantMsg)
                    .build();

            given(miraChatService.analyzeError(any(ErrorReportRequest.class), anyString(), anyString()))
                    .willReturn(response);

            ErrorReportRequest errorRequest = ErrorReportRequest.builder()
                    .code("ERR-001")
                    .message("テストエラー")
                    .detail("java.lang.Exception...")
                    .source("api")
                    .build();
            ApiRequest<ErrorReportRequest> request = new ApiRequest<>();
            request.setModel(errorRequest);

            // Act
            ResponseEntity<MiraApiController.MiraChatApiResponse> result = 
                    miraApiController.analyzeError(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().data().getMode()).isEqualTo("ERROR_ANALYZE");
            assertThat(result.getBody().errors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("clearConversation メソッドのテスト")
    class DeleteConversationTest {

        @Test
        @DisplayName("会話クリアが成功")
        void shouldClearConversation() {
            // Arrange
            doNothing().when(miraChatService).clearConversation("conv-123", "test-tenant", "test-user");

            // Act
            ResponseEntity<java.util.Map<String, Object>> result = 
                    miraApiController.clearConversation("conv-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().get("success")).isEqualTo(true);
            assertThat(result.getBody().get("message")).isEqualTo("会話をクリアしました");
        }
    }

    @Nested
    @DisplayName("getConversationStatus メソッドのテスト")
    class ConversationStatusTest {

        @Test
        @DisplayName("アクティブな会話ステータスを取得")
        void shouldReturnActiveStatus() {
            // Arrange
            given(miraChatService.isConversationActive("conv-123", "test-tenant", "test-user"))
                    .willReturn(true);

            // Act
            ResponseEntity<java.util.Map<String, Object>> result = 
                    miraApiController.getConversationStatus("conv-123");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().get("conversationId")).isEqualTo("conv-123");
            assertThat(result.getBody().get("active")).isEqualTo(true);
        }

        @Test
        @DisplayName("非アクティブな会話ステータスを取得")
        void shouldReturnInactiveStatus() {
            // Arrange
            given(miraChatService.isConversationActive("conv-closed", "test-tenant", "test-user"))
                    .willReturn(false);

            // Act
            ResponseEntity<java.util.Map<String, Object>> result = 
                    miraApiController.getConversationStatus("conv-closed");

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().get("active")).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("health メソッドのテスト")
    class HealthEndpointTest {

        @Test
        @DisplayName("ヘルスチェックが成功")
        void shouldReturnHealthStatus() {
            // Act
            ResponseEntity<java.util.Map<String, Object>> result = miraApiController.health();

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().get("status")).isEqualTo("UP");
            assertThat(result.getBody().get("service")).isEqualTo("mira-ai-assistant");
            assertThat(result.getBody().get("timestamp")).isNotNull();
        }
    }
}
