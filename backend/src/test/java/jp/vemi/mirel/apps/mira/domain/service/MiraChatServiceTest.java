/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraContextSnapshotRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraConversationRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraMessageRepository;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ContextSnapshotRequest;
import jp.vemi.mirel.apps.mira.domain.dto.response.ChatResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ContextSnapshotResponse;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderClient;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiResponse;

/**
 * MiraChatService のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MiraChatServiceTest {

    @Mock
    private AiProviderFactory aiProviderFactory;

    @Mock
    private AiProviderClient aiProviderClient;

    @Mock
    private ModeResolver modeResolver;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private PolicyEnforcer policyEnforcer;

    @Mock
    private ResponseFormatter responseFormatter;

    @Mock
    private MiraConversationRepository conversationRepository;

    @Mock
    private MiraMessageRepository messageRepository;

    @Mock
    private MiraContextSnapshotRepository contextSnapshotRepository;

    @InjectMocks
    private MiraChatService miraChatService;

    private static final String TENANT_ID = "tenant-001";
    private static final String USER_ID = "user-001";

    @Nested
    @DisplayName("chat メソッドのテスト")
    class ChatTest {

        private ChatRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("テストメッセージ")
                            .build())
                    .build();
        }

        @Test
        @DisplayName("正常なチャットリクエストで応答を返す")
        void shouldReturnResponseForValidRequest() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();
            String messageId = UUID.randomUUID().toString();

            when(policyEnforcer.validateRequest(any()))
                    .thenReturn(new PolicyEnforcer.ValidationResult(true, null));
            when(modeResolver.resolve(any())).thenReturn(MiraMode.GENERAL_CHAT);
            when(policyEnforcer.canAccessMode(any(), any(), any())).thenReturn(true);

            MiraConversation conversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId(USER_ID)
                    .mode(MiraConversation.ConversationMode.GENERAL_CHAT)
                    .status(MiraConversation.ConversationStatus.ACTIVE)
                    .build();
            when(conversationRepository.save(any())).thenReturn(conversation);

            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any()))
                    .thenReturn(List.of());

            AiRequest aiRequest = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.user("テストメッセージ")))
                    .build();
            when(promptBuilder.buildChatRequest(any(), any(), any())).thenReturn(aiRequest);

            when(aiProviderFactory.getProvider()).thenReturn(aiProviderClient);
            AiResponse aiResponse = AiResponse.success(
                    "テスト応答です",
                    AiResponse.Metadata.builder()
                            .model("gpt-4o")
                            .latencyMs(500L)
                            .build()
            );
            when(aiProviderClient.chat(any())).thenReturn(aiResponse);

            when(policyEnforcer.filterResponse(any(), any())).thenAnswer(i -> i.getArgument(0));
            when(responseFormatter.formatAsMarkdown(any())).thenAnswer(i -> i.getArgument(0));

            MiraMessage assistantMessage = MiraMessage.builder()
                    .id(messageId)
                    .conversationId(conversationId)
                    .senderType(MiraMessage.SenderType.ASSISTANT)
                    .content("テスト応答です")
                    .build();
            when(messageRepository.save(any())).thenReturn(assistantMessage);

            // Act
            ChatResponse response = miraChatService.chat(validRequest, TENANT_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getConversationId()).isEqualTo(conversationId);
            assertThat(response.getMode()).isEqualTo("GENERAL_CHAT");
            assertThat(response.getAssistantMessage()).isNotNull();
            assertThat(response.getAssistantMessage().getContent()).isEqualTo("テスト応答です");
        }

        @Test
        @DisplayName("ポリシー検証失敗時はエラーレスポンスを返す")
        void shouldReturnErrorWhenPolicyValidationFails() {
            // Arrange
            when(policyEnforcer.validateRequest(any()))
                    .thenReturn(new PolicyEnforcer.ValidationResult(false, "メッセージが空です"));

            // Act
            ChatResponse response = miraChatService.chat(validRequest, TENANT_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMode()).isEqualTo("ERROR");
            assertThat(response.getAssistantMessage().getContent()).contains("メッセージが空です");
        }

        @Test
        @DisplayName("モードアクセス権限がない場合はエラーレスポンスを返す")
        void shouldReturnErrorWhenModeAccessDenied() {
            // Arrange
            when(policyEnforcer.validateRequest(any()))
                    .thenReturn(new PolicyEnforcer.ValidationResult(true, null));
            when(modeResolver.resolve(any())).thenReturn(MiraMode.STUDIO_AGENT);
            when(policyEnforcer.canAccessMode(any(), any(), any())).thenReturn(false);

            // Act
            ChatResponse response = miraChatService.chat(validRequest, TENANT_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getMode()).isEqualTo("ERROR");
            assertThat(response.getAssistantMessage().getContent()).contains("アクセス権限がありません");
        }

        @Test
        @DisplayName("AI呼び出し失敗時はエラーレスポンスを返す")
        void shouldReturnErrorWhenAiCallFails() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();

            when(policyEnforcer.validateRequest(any()))
                    .thenReturn(new PolicyEnforcer.ValidationResult(true, null));
            when(modeResolver.resolve(any())).thenReturn(MiraMode.GENERAL_CHAT);
            when(policyEnforcer.canAccessMode(any(), any(), any())).thenReturn(true);

            MiraConversation conversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId(USER_ID)
                    .mode(MiraConversation.ConversationMode.GENERAL_CHAT)
                    .status(MiraConversation.ConversationStatus.ACTIVE)
                    .build();
            when(conversationRepository.save(any())).thenReturn(conversation);

            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(any()))
                    .thenReturn(List.of());

            AiRequest aiRequest = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.user("テストメッセージ")))
                    .build();
            when(promptBuilder.buildChatRequest(any(), any(), any())).thenReturn(aiRequest);

            when(aiProviderFactory.getProvider()).thenReturn(aiProviderClient);
            AiResponse errorResponse = AiResponse.error("API_ERROR", "APIエラーが発生しました");
            when(aiProviderClient.chat(any())).thenReturn(errorResponse);

            // Act
            ChatResponse response = miraChatService.chat(validRequest, TENANT_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAssistantMessage().getContent()).contains("AI応答の取得に失敗しました");
        }

        @Test
        @DisplayName("既存の会話を継続できる")
        void shouldContinueExistingConversation() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();
            ChatRequest requestWithConversation = ChatRequest.builder()
                    .conversationId(conversationId)
                    .message(ChatRequest.Message.builder()
                            .content("続きのメッセージ")
                            .build())
                    .build();

            when(policyEnforcer.validateRequest(any()))
                    .thenReturn(new PolicyEnforcer.ValidationResult(true, null));
            when(modeResolver.resolve(any())).thenReturn(MiraMode.GENERAL_CHAT);
            when(policyEnforcer.canAccessMode(any(), any(), any())).thenReturn(true);

            MiraConversation existingConversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId(USER_ID)
                    .mode(MiraConversation.ConversationMode.GENERAL_CHAT)
                    .status(MiraConversation.ConversationStatus.ACTIVE)
                    .build();
            when(conversationRepository.findById(conversationId))
                    .thenReturn(Optional.of(existingConversation));

            // 履歴あり
            MiraMessage prevUserMsg = MiraMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .conversationId(conversationId)
                    .senderType(MiraMessage.SenderType.USER)
                    .content("前のメッセージ")
                    .build();
            MiraMessage prevAssistantMsg = MiraMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .conversationId(conversationId)
                    .senderType(MiraMessage.SenderType.ASSISTANT)
                    .content("前の応答")
                    .build();
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                    .thenReturn(List.of(prevUserMsg, prevAssistantMsg));

            AiRequest aiRequest = AiRequest.builder()
                    .messages(List.of(
                            AiRequest.Message.user("前のメッセージ"),
                            AiRequest.Message.assistant("前の応答"),
                            AiRequest.Message.user("続きのメッセージ")
                    ))
                    .build();
            when(promptBuilder.buildChatRequest(any(), any(), any())).thenReturn(aiRequest);

            when(aiProviderFactory.getProvider()).thenReturn(aiProviderClient);
            AiResponse aiResponse = AiResponse.success(
                    "続きの応答です",
                    AiResponse.Metadata.builder().model("gpt-4o").build()
            );
            when(aiProviderClient.chat(any())).thenReturn(aiResponse);

            when(policyEnforcer.filterResponse(any(), any())).thenAnswer(i -> i.getArgument(0));
            when(responseFormatter.formatAsMarkdown(any())).thenAnswer(i -> i.getArgument(0));

            MiraMessage newAssistantMessage = MiraMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .conversationId(conversationId)
                    .senderType(MiraMessage.SenderType.ASSISTANT)
                    .content("続きの応答です")
                    .build();
            when(messageRepository.save(any())).thenReturn(newAssistantMessage);

            // Act
            ChatResponse response = miraChatService.chat(requestWithConversation, TENANT_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getConversationId()).isEqualTo(conversationId);

            // 新しい会話は作成されない（findById が呼ばれる）
            verify(conversationRepository).findById(conversationId);
            verify(conversationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("clearConversation メソッドのテスト")
    class ClearConversationTest {

        @Test
        @DisplayName("自分の会話をクリアできる")
        void shouldClearOwnConversation() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();
            MiraConversation conversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId(USER_ID)
                    .status(MiraConversation.ConversationStatus.ACTIVE)
                    .build();
            when(conversationRepository.findById(conversationId))
                    .thenReturn(Optional.of(conversation));

            // Act
            miraChatService.clearConversation(conversationId, TENANT_ID, USER_ID);

            // Assert
            ArgumentCaptor<MiraConversation> captor = ArgumentCaptor.forClass(MiraConversation.class);
            verify(conversationRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(MiraConversation.ConversationStatus.CLOSED);
        }

        @Test
        @DisplayName("他人の会話はクリアできない")
        void shouldNotClearOthersConversation() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();
            MiraConversation conversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId("other-user")
                    .status(MiraConversation.ConversationStatus.ACTIVE)
                    .build();
            when(conversationRepository.findById(conversationId))
                    .thenReturn(Optional.of(conversation));

            // Act
            miraChatService.clearConversation(conversationId, TENANT_ID, USER_ID);

            // Assert
            verify(conversationRepository, never()).save(any());
        }

        @Test
        @DisplayName("存在しない会話のクリアは何もしない")
        void shouldDoNothingForNonExistingConversation() {
            // Arrange
            when(conversationRepository.findById(any())).thenReturn(Optional.empty());

            // Act
            miraChatService.clearConversation("non-existing", TENANT_ID, USER_ID);

            // Assert
            verify(conversationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isConversationActive メソッドのテスト")
    class IsConversationActiveTest {

        @Test
        @DisplayName("アクティブな会話はtrueを返す")
        void shouldReturnTrueForActiveConversation() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();
            MiraConversation conversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId(USER_ID)
                    .status(MiraConversation.ConversationStatus.ACTIVE)
                    .build();
            when(conversationRepository.findById(conversationId))
                    .thenReturn(Optional.of(conversation));

            // Act
            boolean result = miraChatService.isConversationActive(conversationId, TENANT_ID, USER_ID);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("クローズされた会話はfalseを返す")
        void shouldReturnFalseForClosedConversation() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();
            MiraConversation conversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId(USER_ID)
                    .status(MiraConversation.ConversationStatus.CLOSED)
                    .build();
            when(conversationRepository.findById(conversationId))
                    .thenReturn(Optional.of(conversation));

            // Act
            boolean result = miraChatService.isConversationActive(conversationId, TENANT_ID, USER_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("他人の会話はfalseを返す")
        void shouldReturnFalseForOthersConversation() {
            // Arrange
            String conversationId = UUID.randomUUID().toString();
            MiraConversation conversation = MiraConversation.builder()
                    .id(conversationId)
                    .tenantId(TENANT_ID)
                    .userId("other-user")
                    .status(MiraConversation.ConversationStatus.ACTIVE)
                    .build();
            when(conversationRepository.findById(conversationId))
                    .thenReturn(Optional.of(conversation));

            // Act
            boolean result = miraChatService.isConversationActive(conversationId, TENANT_ID, USER_ID);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("存在しない会話はfalseを返す")
        void shouldReturnFalseForNonExistingConversation() {
            // Arrange
            when(conversationRepository.findById(any())).thenReturn(Optional.empty());

            // Act
            boolean result = miraChatService.isConversationActive("non-existing", TENANT_ID, USER_ID);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("saveContextSnapshot メソッドのテスト")
    class SaveContextSnapshotTest {

        @Test
        @DisplayName("コンテキストスナップショットを保存できる")
        void shouldSaveContextSnapshot() {
            // Arrange
            ContextSnapshotRequest request = ContextSnapshotRequest.builder()
                    .appId("studio")
                    .screenId("modeler")
                    .systemRole("ROLE_USER")
                    .appRole("Builder")
                    .build();

            when(contextSnapshotRepository.save(any())).thenAnswer(invocation -> {
                var snapshot = invocation.getArgument(0, jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextSnapshot.class);
                return snapshot;
            });

            // Act
            ContextSnapshotResponse response = miraChatService.saveContextSnapshot(request, TENANT_ID, USER_ID);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getSnapshotId()).isNotBlank();
        }
    }
}
