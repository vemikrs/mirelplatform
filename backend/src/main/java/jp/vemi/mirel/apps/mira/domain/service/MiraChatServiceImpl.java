/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.api.MiraChatService;
import jp.vemi.mirel.apps.mira.domain.dao.MiraContextSnapshot;
import jp.vemi.mirel.apps.mira.domain.dao.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.MiraMessage;
import jp.vemi.mirel.apps.mira.domain.dao.MiraContextSnapshotRepository;
import jp.vemi.mirel.apps.mira.domain.dao.MiraConversationRepository;
import jp.vemi.mirel.apps.mira.domain.dao.MiraMessageRepository;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ContextSnapshotRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ErrorReportRequest;
import jp.vemi.mirel.apps.mira.domain.dto.response.ChatResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ContextSnapshotResponse;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderClient;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira チャットサービス実装.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraChatServiceImpl implements MiraChatService {
    
    private final AiProviderFactory aiProviderFactory;
    private final ModeResolver modeResolver;
    private final PromptBuilder promptBuilder;
    private final PolicyEnforcer policyEnforcer;
    private final ResponseFormatter responseFormatter;
    private final MiraConversationRepository conversationRepository;
    private final MiraMessageRepository messageRepository;
    private final MiraContextSnapshotRepository contextSnapshotRepository;
    
    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request, String tenantId, String userId) {
        long startTime = System.currentTimeMillis();
        
        // 1. ポリシー検証
        PolicyEnforcer.ValidationResult validation = policyEnforcer.validateRequest(request);
        if (!validation.valid()) {
            return buildErrorResponse(request.getConversationId(), validation.errorMessage());
        }
        
        // 2. モード解決
        MiraMode mode = resolveMode(request);
        String systemRole = extractSystemRole(request);
        String appRole = extractAppRole(request);
        
        // モードアクセス権限チェック
        if (!policyEnforcer.canAccessMode(mode, systemRole, appRole)) {
            return buildErrorResponse(request.getConversationId(), 
                "このモードへのアクセス権限がありません");
        }
        
        // 3. 会話取得または作成
        MiraConversation conversation = getOrCreateConversation(
            request.getConversationId(), tenantId, userId, mode);
        
        // 4. ユーザーメッセージ保存
        saveUserMessage(conversation, request.getMessage().getContent());
        
        // 5. 会話履歴取得
        List<AiRequest.Message> history = loadConversationHistory(conversation.getId());
        
        // 6. プロンプト構築
        AiRequest aiRequest = promptBuilder.buildChatRequest(request, mode, history);
        
        // 7. AI 呼び出し
        AiProviderClient client = aiProviderFactory.getClient();
        AiResponse aiResponse = client.chat(aiRequest);
        
        long latency = System.currentTimeMillis() - startTime;
        
        // 8. 応答処理
        if (!aiResponse.isSuccess()) {
            log.error("AI呼び出し失敗: {}", aiResponse.getErrorMessage());
            return buildErrorResponse(conversation.getId().toString(), 
                "AI応答の取得に失敗しました: " + aiResponse.getErrorMessage());
        }
        
        // 9. 応答フィルタリング
        String filteredContent = policyEnforcer.filterResponse(
            aiResponse.getContent(), systemRole);
        String formattedContent = responseFormatter.formatAsMarkdown(filteredContent);
        
        // 10. アシスタントメッセージ保存
        MiraMessage assistantMessage = saveAssistantMessage(conversation, formattedContent);
        
        // 11. レスポンス構築
        return ChatResponse.builder()
            .conversationId(conversation.getId().toString())
            .messageId(assistantMessage.getId().toString())
            .mode(mode.name())
            .assistantMessage(ChatResponse.AssistantMessage.builder()
                .content(formattedContent)
                .contentType("markdown")
                .build())
            .metadata(ChatResponse.Metadata.builder()
                .provider(aiResponse.getProvider())
                .model(aiResponse.getModel())
                .latencyMs(latency)
                .promptTokens(aiResponse.getPromptTokens())
                .completionTokens(aiResponse.getCompletionTokens())
                .build())
            .build();
    }
    
    @Override
    @Transactional
    public ContextSnapshotResponse saveContextSnapshot(
            ContextSnapshotRequest request, 
            String tenantId, 
            String userId) {
        
        MiraContextSnapshot snapshot = MiraContextSnapshot.builder()
            .tenantId(tenantId)
            .userId(userId)
            .appId(request.getAppId())
            .screenId(request.getScreenId())
            .systemRole(request.getSystemRole())
            .appRole(request.getAppRole())
            .payload(convertPayloadToJson(request.getPayload()))
            .build();
        
        MiraContextSnapshot saved = contextSnapshotRepository.save(snapshot);
        
        return ContextSnapshotResponse.builder()
            .snapshotId(saved.getId().toString())
            .createdAt(saved.getCreatedAt())
            .build();
    }
    
    @Override
    @Transactional
    public ChatResponse analyzeError(ErrorReportRequest request, String tenantId, String userId) {
        long startTime = System.currentTimeMillis();
        
        // 新規会話作成
        MiraConversation conversation = createConversation(
            tenantId, userId, MiraMode.ERROR_ANALYZE);
        
        // エラー分析用プロンプト構築
        AiRequest aiRequest = promptBuilder.buildErrorAnalyzeRequest(request);
        
        // AI 呼び出し
        AiProviderClient client = aiProviderFactory.getClient();
        AiResponse aiResponse = client.chat(aiRequest);
        
        long latency = System.currentTimeMillis() - startTime;
        
        if (!aiResponse.isSuccess()) {
            return buildErrorResponse(conversation.getId().toString(), 
                "エラー分析に失敗しました");
        }
        
        String formattedContent = responseFormatter.formatAsMarkdown(aiResponse.getContent());
        MiraMessage assistantMessage = saveAssistantMessage(conversation, formattedContent);
        
        return ChatResponse.builder()
            .conversationId(conversation.getId().toString())
            .messageId(assistantMessage.getId().toString())
            .mode(MiraMode.ERROR_ANALYZE.name())
            .assistantMessage(ChatResponse.AssistantMessage.builder()
                .content(formattedContent)
                .contentType("markdown")
                .build())
            .metadata(ChatResponse.Metadata.builder()
                .provider(aiResponse.getProvider())
                .model(aiResponse.getModel())
                .latencyMs(latency)
                .promptTokens(aiResponse.getPromptTokens())
                .completionTokens(aiResponse.getCompletionTokens())
                .build())
            .build();
    }
    
    @Override
    @Transactional
    public void clearConversation(String conversationId, String tenantId, String userId) {
        UUID convId = UUID.fromString(conversationId);
        
        Optional<MiraConversation> conversation = conversationRepository.findById(convId);
        if (conversation.isPresent() 
                && conversation.get().getTenantId().equals(tenantId)
                && conversation.get().getUserId().equals(userId)) {
            
            MiraConversation conv = conversation.get();
            conv.setStatus("CLEARED");
            conv.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conv);
            
            log.info("会話をクリア: conversationId={}", conversationId);
        }
    }
    
    @Override
    public boolean isConversationActive(String conversationId, String tenantId, String userId) {
        try {
            UUID convId = UUID.fromString(conversationId);
            Optional<MiraConversation> conversation = conversationRepository.findById(convId);
            
            return conversation
                .filter(c -> c.getTenantId().equals(tenantId))
                .filter(c -> c.getUserId().equals(userId))
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .isPresent();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    // ========================================
    // Private Methods
    // ========================================
    
    private MiraMode resolveMode(ChatRequest request) {
        // リクエストで明示されていればそれを使用
        if (request.getMode() != null && !request.getMode().isEmpty()) {
            try {
                return MiraMode.valueOf(request.getMode());
            } catch (IllegalArgumentException e) {
                log.warn("不明なモード指定: {}", request.getMode());
            }
        }
        
        // コンテキストから推定
        ChatRequest.Context ctx = request.getContext();
        String appId = ctx != null ? ctx.getAppId() : null;
        String screenId = ctx != null ? ctx.getScreenId() : null;
        String content = request.getMessage() != null ? request.getMessage().getContent() : "";
        
        return modeResolver.resolve(appId, screenId, content);
    }
    
    private MiraConversation getOrCreateConversation(
            String conversationId, String tenantId, String userId, MiraMode mode) {
        
        if (conversationId != null && !conversationId.isEmpty()) {
            try {
                UUID convId = UUID.fromString(conversationId);
                Optional<MiraConversation> existing = conversationRepository.findById(convId);
                
                if (existing.isPresent() 
                        && existing.get().getTenantId().equals(tenantId)
                        && existing.get().getUserId().equals(userId)
                        && "ACTIVE".equals(existing.get().getStatus())) {
                    return existing.get();
                }
            } catch (IllegalArgumentException e) {
                log.warn("無効な会話ID: {}", conversationId);
            }
        }
        
        return createConversation(tenantId, userId, mode);
    }
    
    private MiraConversation createConversation(String tenantId, String userId, MiraMode mode) {
        MiraConversation conversation = MiraConversation.builder()
            .tenantId(tenantId)
            .userId(userId)
            .mode(mode.name())
            .status("ACTIVE")
            .build();
        
        return conversationRepository.save(conversation);
    }
    
    private void saveUserMessage(MiraConversation conversation, String content) {
        MiraMessage message = MiraMessage.builder()
            .conversationId(conversation.getId())
            .senderType("USER")
            .content(content)
            .contentType("text")
            .build();
        
        messageRepository.save(message);
    }
    
    private MiraMessage saveAssistantMessage(MiraConversation conversation, String content) {
        MiraMessage message = MiraMessage.builder()
            .conversationId(conversation.getId())
            .senderType("ASSISTANT")
            .content(content)
            .contentType("markdown")
            .build();
        
        return messageRepository.save(message);
    }
    
    private List<AiRequest.Message> loadConversationHistory(UUID conversationId) {
        List<MiraMessage> messages = messageRepository
            .findByConversationIdOrderByCreatedAtAsc(conversationId);
        
        List<AiRequest.Message> history = new ArrayList<>();
        for (MiraMessage msg : messages) {
            if ("USER".equals(msg.getSenderType())) {
                history.add(AiRequest.Message.user(msg.getContent()));
            } else if ("ASSISTANT".equals(msg.getSenderType())) {
                history.add(AiRequest.Message.assistant(msg.getContent()));
            }
        }
        
        return history;
    }
    
    private String extractSystemRole(ChatRequest request) {
        return request.getContext() != null ? request.getContext().getSystemRole() : null;
    }
    
    private String extractAppRole(ChatRequest request) {
        return request.getContext() != null ? request.getContext().getAppRole() : null;
    }
    
    private String convertPayloadToJson(java.util.Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "{}";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Payload JSON変換失敗", e);
            return "{}";
        }
    }
    
    private ChatResponse buildErrorResponse(String conversationId, String errorMessage) {
        return ChatResponse.builder()
            .conversationId(conversationId)
            .mode("ERROR")
            .assistantMessage(ChatResponse.AssistantMessage.builder()
                .content("エラーが発生しました: " + errorMessage)
                .contentType("text")
                .build())
            .build();
    }
}
