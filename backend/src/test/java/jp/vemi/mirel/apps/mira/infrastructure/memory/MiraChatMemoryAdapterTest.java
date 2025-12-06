/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraConversationRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraMessageRepository;
import jp.vemi.mirel.apps.mira.domain.exception.MiraException;

/**
 * MiraChatMemoryAdapter のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MiraChatMemoryAdapterTest {

    @Mock
    private MiraConversationRepository conversationRepository;

    @Mock
    private MiraMessageRepository messageRepository;

    @Captor
    private ArgumentCaptor<MiraMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<MiraConversation> conversationCaptor;

    private MiraChatMemoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MiraChatMemoryAdapter(conversationRepository, messageRepository);
    }

    @Nested
    @DisplayName("メッセージ追加テスト")
    class AddMessagesTest {

        @Test
        @DisplayName("ユーザーメッセージを追加")
        void shouldAddUserMessage() {
            // Arrange
            String conversationId = "conv-123";
            MiraConversation conversation = createConversation(conversationId);
            
            when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
            
            UserMessage userMessage = new UserMessage("Hello, Mira!");

            // Act
            adapter.add(conversationId, List.of(userMessage));

            // Assert
            verify(messageRepository).save(messageCaptor.capture());
            MiraMessage saved = messageCaptor.getValue();
            
            assertThat(saved.getSenderType()).isEqualTo(MiraMessage.SenderType.USER);
            assertThat(saved.getContent()).isEqualTo("Hello, Mira!");
            assertThat(saved.getConversationId()).isEqualTo(conversationId);
        }

        @Test
        @DisplayName("アシスタントメッセージを追加")
        void shouldAddAssistantMessage() {
            // Arrange
            String conversationId = "conv-123";
            MiraConversation conversation = createConversation(conversationId);
            
            when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
            
            AssistantMessage assistantMessage = new AssistantMessage("こんにちは！何かお手伝いしましょうか？");

            // Act
            adapter.add(conversationId, List.of(assistantMessage));

            // Assert
            verify(messageRepository).save(messageCaptor.capture());
            MiraMessage saved = messageCaptor.getValue();
            
            assertThat(saved.getSenderType()).isEqualTo(MiraMessage.SenderType.ASSISTANT);
            assertThat(saved.getContent()).isEqualTo("こんにちは！何かお手伝いしましょうか？");
        }

        @Test
        @DisplayName("複数メッセージを追加")
        void shouldAddMultipleMessages() {
            // Arrange
            String conversationId = "conv-123";
            MiraConversation conversation = createConversation(conversationId);
            
            when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
            
            List<Message> messages = List.of(
                    new UserMessage("Question 1"),
                    new AssistantMessage("Answer 1"),
                    new UserMessage("Question 2")
            );

            // Act
            adapter.add(conversationId, messages);

            // Assert
            verify(messageRepository, org.mockito.Mockito.times(3)).save(any());
        }

        @Test
        @DisplayName("会話が存在しない場合は例外をスロー")
        void shouldThrowExceptionWhenConversationNotFound() {
            // Arrange
            String conversationId = "nonexistent";
            when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> adapter.add(conversationId, List.of(new UserMessage("test"))))
                    .isInstanceOf(MiraException.class);
        }

        @Test
        @DisplayName("メッセージ追加後に会話の最終活動日時を更新")
        void shouldUpdateConversationLastActivity() {
            // Arrange
            String conversationId = "conv-123";
            MiraConversation conversation = createConversation(conversationId);
            LocalDateTime originalTime = conversation.getLastActivityAt();
            
            when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
            
            UserMessage message = new UserMessage("Hello");

            // Act
            adapter.add(conversationId, List.of(message));

            // Assert
            verify(conversationRepository).save(conversationCaptor.capture());
            MiraConversation saved = conversationCaptor.getValue();
            // updateLastActivity() が呼ばれていることを確認
            assertThat(saved.getLastActivityAt()).isAfterOrEqualTo(originalTime);
        }
    }

    @Nested
    @DisplayName("メッセージ取得テスト")
    class GetMessagesTest {

        @Test
        @DisplayName("会話の全メッセージを取得")
        void shouldGetAllMessages() {
            // Arrange
            String conversationId = "conv-123";
            
            List<MiraMessage> miraMessages = List.of(
                    createMiraMessage("msg-1", MiraMessage.SenderType.USER, "Hello"),
                    createMiraMessage("msg-2", MiraMessage.SenderType.ASSISTANT, "Hi there!")
            );
            
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                    .thenReturn(miraMessages);

            // Act
            List<Message> result = adapter.get(conversationId, Integer.MAX_VALUE);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isInstanceOf(UserMessage.class);
            assertThat(result.get(0).getText()).isEqualTo("Hello");
            assertThat(result.get(1)).isInstanceOf(AssistantMessage.class);
            assertThat(result.get(1).getText()).isEqualTo("Hi there!");
        }

        @Test
        @DisplayName("指定した件数のみ取得")
        void shouldGetLimitedMessages() {
            // Arrange
            String conversationId = "conv-123";
            int lastN = 2;
            
            List<MiraMessage> miraMessages = List.of(
                    createMiraMessage("msg-1", MiraMessage.SenderType.USER, "First"),
                    createMiraMessage("msg-2", MiraMessage.SenderType.ASSISTANT, "Second"),
                    createMiraMessage("msg-3", MiraMessage.SenderType.USER, "Third"),
                    createMiraMessage("msg-4", MiraMessage.SenderType.ASSISTANT, "Fourth")
            );
            
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                    .thenReturn(miraMessages);

            // Act
            List<Message> result = adapter.get(conversationId, lastN);

            // Assert
            assertThat(result).hasSize(2);
            // 直近の2件（Third, Fourth）が取得される
            assertThat(result.get(0).getText()).isEqualTo("Third");
            assertThat(result.get(1).getText()).isEqualTo("Fourth");
        }

        @Test
        @DisplayName("メッセージが存在しない場合は空リストを返す")
        void shouldReturnEmptyListWhenNoMessages() {
            // Arrange
            String conversationId = "conv-empty";
            
            when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId))
                    .thenReturn(List.of());

            // Act
            List<Message> result = adapter.get(conversationId, Integer.MAX_VALUE);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("メッセージクリアテスト")
    class ClearMessagesTest {

        @Test
        @DisplayName("会話の全メッセージを削除")
        void shouldClearAllMessages() {
            // Arrange
            String conversationId = "conv-123";

            // Act
            adapter.clear(conversationId);

            // Assert
            verify(messageRepository).deleteByConversationId(conversationId);
        }
    }

    @Nested
    @DisplayName("ChatMemory インターフェース準拠テスト")
    class ChatMemoryInterfaceTest {

        @Test
        @DisplayName("ChatMemory インターフェースを実装している")
        void shouldImplementChatMemoryInterface() {
            // Assert
            assertThat(adapter).isInstanceOf(ChatMemory.class);
        }
    }

    /**
     * テスト用会話エンティティを作成.
     */
    private MiraConversation createConversation(String id) {
        return MiraConversation.builder()
                .id(id)
                .userId("user-123")
                .tenantId("tenant-456")
                .mode(MiraConversation.ConversationMode.GENERAL_CHAT)
                .title("Test Conversation")
                .status(MiraConversation.ConversationStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusHours(1))
                .lastActivityAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    /**
     * テスト用メッセージエンティティを作成.
     */
    private MiraMessage createMiraMessage(String id, MiraMessage.SenderType senderType, String content) {
        return MiraMessage.builder()
                .id(id)
                .conversationId("conv-123")
                .senderType(senderType)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
