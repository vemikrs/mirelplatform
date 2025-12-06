/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage.SenderType;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraConversationRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraMessageRepository;
import jp.vemi.mirel.apps.mira.domain.exception.MiraErrorCode;
import jp.vemi.mirel.apps.mira.domain.exception.MiraException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira ChatMemory アダプター.
 * <p>
 * Spring AI の {@link ChatMemory} インターフェースを実装し、
 * 既存の MiraConversation / MiraMessage エンティティと連携する。
 * </p>
 * 
 * <p><b>設計指針:</b></p>
 * <ul>
 *   <li>既存テーブル (mir_mira_conversation, mir_mira_message) をそのまま使用</li>
 *   <li>Spring AI の MessageChatMemoryAdvisor と互換</li>
 *   <li>tokenCount, usedModel 等の独自メタデータを維持</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiraChatMemoryAdapter implements ChatMemory {

    private final MiraConversationRepository conversationRepository;
    private final MiraMessageRepository messageRepository;

    /**
     * 会話にメッセージを追加.
     * 
     * @param conversationId 会話ID（UUID文字列）
     * @param messages 追加するメッセージリスト
     */
    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        if (log.isDebugEnabled()) {
            log.debug("[MiraChatMemoryAdapter] add: conversationId={}, messageCount={}",
                    conversationId, messages.size());
        }

        // 会話存在確認
        MiraConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    log.warn("[MiraChatMemoryAdapter] Conversation not found: {}", conversationId);
                    return new MiraException(MiraErrorCode.CONVERSATION_NOT_FOUND, conversationId);
                });

        // メッセージを保存
        for (Message message : messages) {
            MiraMessage entity = toMiraMessage(conversationId, message);
            messageRepository.save(entity);
            
            if (log.isTraceEnabled()) {
                log.trace("[MiraChatMemoryAdapter] Saved message: id={}, type={}, length={}",
                        entity.getId(), entity.getSenderType(), 
                        entity.getContent() != null ? entity.getContent().length() : 0);
            }
        }

        // 最終アクティビティ更新
        conversation.updateLastActivity();
        conversationRepository.save(conversation);
    }

    /**
     * 会話から直近N件のメッセージを取得.
     * 
     * @param conversationId 会話ID
     * @param lastN 取得件数
     * @return メッセージリスト（古い順）
     */
    @Override
    @Transactional(readOnly = true)
    public List<Message> get(String conversationId, int lastN) {
        if (log.isDebugEnabled()) {
            log.debug("[MiraChatMemoryAdapter] get: conversationId={}, lastN={}", 
                    conversationId, lastN);
        }

        // 全メッセージを取得して最後のN件を返す
        List<MiraMessage> allMessages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        if (allMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 直近N件を取得（末尾から）
        int startIndex = Math.max(0, allMessages.size() - lastN);
        List<MiraMessage> recentMessages = allMessages.subList(startIndex, allMessages.size());

        List<Message> result = new ArrayList<>(recentMessages.size());
        for (MiraMessage msg : recentMessages) {
            result.add(toSpringAiMessage(msg));
        }

        if (log.isDebugEnabled()) {
            log.debug("[MiraChatMemoryAdapter] Retrieved {} messages (total: {})", 
                    result.size(), allMessages.size());
        }

        return result;
    }

    /**
     * 会話のメッセージをクリア.
     * 
     * @param conversationId 会話ID
     */
    @Override
    @Transactional
    public void clear(String conversationId) {
        if (log.isDebugEnabled()) {
            log.debug("[MiraChatMemoryAdapter] clear: conversationId={}", conversationId);
        }

        long count = messageRepository.countByConversationId(conversationId);
        messageRepository.deleteByConversationId(conversationId);

        log.info("[MiraChatMemoryAdapter] Cleared {} messages from conversation: {}", 
                count, conversationId);
    }

    // ========================================
    // 変換メソッド
    // ========================================

    /**
     * Spring AI Message → MiraMessage エンティティ変換.
     */
    private MiraMessage toMiraMessage(String conversationId, Message message) {
        return MiraMessage.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .senderType(mapMessageType(message.getMessageType()))
                .content(message.getText())
                .contentType(MiraMessage.ContentType.PLAIN)
                .build();
    }

    /**
     * MiraMessage エンティティ → Spring AI Message 変換.
     */
    private Message toSpringAiMessage(MiraMessage entity) {
        return switch (entity.getSenderType()) {
            case USER -> new UserMessage(entity.getContent());
            case ASSISTANT -> new AssistantMessage(entity.getContent());
            case SYSTEM -> new SystemMessage(entity.getContent());
        };
    }

    /**
     * MessageType → SenderType マッピング.
     */
    private SenderType mapMessageType(MessageType type) {
        return switch (type) {
            case USER -> SenderType.USER;
            case ASSISTANT -> SenderType.ASSISTANT;
            case SYSTEM -> SenderType.SYSTEM;
            default -> {
                log.warn("[MiraChatMemoryAdapter] Unknown message type: {}, defaulting to SYSTEM", type);
                yield SenderType.SYSTEM;
            }
        };
    }
}
