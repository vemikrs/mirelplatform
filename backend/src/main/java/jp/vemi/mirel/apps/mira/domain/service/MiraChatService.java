/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraContextSnapshot;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraMessage;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraContextSnapshotRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraConversationRepository;
import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraMessageRepository;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ContextSnapshotRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ErrorReportRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.GenerateTitleRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.UpdateTitleRequest;
import jp.vemi.mirel.apps.mira.domain.dto.response.ChatResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ContextSnapshotResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.GenerateTitleResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.UpdateTitleResponse;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderClient;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiResponse;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.MessageConfig; // Import MessageConfig
import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
import jp.vemi.mirel.apps.mira.infrastructure.ai.TokenCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira チャットサービス.
 * 
 * <p>
 * AI アシスタント機能のコア実装を提供します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraChatService {

    private final AiProviderFactory aiProviderFactory;
    private final ModeResolver modeResolver;
    private final PromptBuilder promptBuilder;
    private final PolicyEnforcer policyEnforcer;
    private final ResponseFormatter responseFormatter;
    private final MiraConversationRepository conversationRepository;
    private final MiraMessageRepository messageRepository;
    private final MiraContextSnapshotRepository contextSnapshotRepository;
    private final MiraContextMergeService contextMergeService;
    private final MiraAuditService auditService;
    private final MiraRateLimitService rateLimitService;
    private final MiraMetrics metrics;
    private final TokenQuotaService tokenQuotaService;
    private final TokenCounter tokenCounter;

    /**
     * チャット実行
     */
    @Transactional
    public ChatResponse chat(ChatRequest request, String tenantId, String userId) {
        long startTime = System.currentTimeMillis();

        // 0. レート制限とクォータチェック
        rateLimitService.checkRateLimit(tenantId, userId);
        // Token Counting (G-2): Use JTokkit for accurate counting
        int estimatedInputTokens = tokenCounter.count(request.getMessage().getContent(), "gpt-4o");
        tokenQuotaService.checkQuota(tenantId, estimatedInputTokens);

        // 1. ポリシー検証
        PolicyEnforcer.ValidationResult validation = policyEnforcer.validateRequest(request);
        if (!validation.valid()) {
            auditService.logChatResponse(tenantId, userId, request.getConversationId(),
                    "UNKNOWN", null, (int) (System.currentTimeMillis() - startTime), 0, 0,
                    MiraAuditLog.AuditStatus.ERROR);
            return buildErrorResponse(request.getConversationId(), validation.errorMessage());
        }

        // 2. モード解決
        MiraMode mode = resolveMode(request);
        String systemRole = extractSystemRole(request);
        String appRole = extractAppRole(request);

        // モードアクセス権限チェック
        if (!policyEnforcer.canAccessMode(mode, systemRole, appRole)) {
            auditService.logChatResponse(tenantId, userId, request.getConversationId(),
                    mode.name(), null, (int) (System.currentTimeMillis() - startTime), 0, 0,
                    MiraAuditLog.AuditStatus.ERROR);
            return buildErrorResponse(request.getConversationId(),
                    "このモードへのアクセス権限がありません");
        }

        // 3. 会話取得または作成
        MiraConversation conversation = getOrCreateConversation(
                request.getConversationId(), tenantId, userId, mode);

        // 4. ユーザーメッセージ保存
        saveUserMessage(conversation, request.getMessage().getContent());

        // 5. 会話履歴取得 (設定に応じてフィルタリング)
        MessageConfig msgConfig = request.getContext() != null ? request.getContext().getMessageConfig() : null;

        List<AiRequest.Message> history = loadConversationHistory(
                conversation.getId(), msgConfig);

        // 6. プロンプト構築（コンテキストレイヤーを反映）
        String finalContext = contextMergeService.buildFinalContextPrompt(
                tenantId, null, userId, msgConfig);

        AiRequest aiRequest = promptBuilder.buildChatRequestWithContext(
                request, mode, history, finalContext);

        // 7. AI 呼び出し
        AiProviderClient client;
        if (request.getForceProvider() != null && !request.getForceProvider().isEmpty()) {
            // Admin role check for force provider
            if (systemRole != null && systemRole.contains("ADMIN")) {
                client = aiProviderFactory.getProvider(request.getForceProvider())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Provider not found: " + request.getForceProvider()));
            } else {
                log.warn("User {} tried to force provider {} without admin role", userId, request.getForceProvider());
                client = aiProviderFactory.getProvider();
            }
        } else {
            client = aiProviderFactory.getProvider();
        }
        AiResponse aiResponse = client.chat(aiRequest);

        long latency = System.currentTimeMillis() - startTime;

        // 8. 応答処理
        if (!aiResponse.isSuccess()) {
            log.error("AI呼び出し失敗: {}", aiResponse.getErrorMessage());
            auditService.logChatResponse(tenantId, userId, conversation.getId(),
                    mode.name(), aiResponse.getModel(), (int) latency,
                    getTokensOrDefault(aiResponse.getPromptTokens()), 0, MiraAuditLog.AuditStatus.ERROR);

            metrics.incrementChatRequest(aiResponse.getModel() != null ? aiResponse.getModel() : "unknown", tenantId,
                    "error");
            metrics.incrementError("ai_api_error", tenantId);

            return buildErrorResponse(conversation.getId(),
                    "AI応答の取得に失敗しました: " + aiResponse.getErrorMessage());
        }

        // 9. 応答フィルタリング
        String filteredContent = policyEnforcer.filterResponse(
                aiResponse.getContent(), systemRole);
        String formattedContent = responseFormatter.formatAsMarkdown(filteredContent);

        // 10. アシスタントメッセージ保存
        MiraMessage assistantMessage = saveAssistantMessage(conversation, formattedContent);

        // 11. 監査ログ記録 (成功)
        int promptTokens = getTokensOrDefault(aiResponse.getPromptTokens());
        int completionTokens = getTokensOrDefault(aiResponse.getCompletionTokens());

        auditService.logChatResponse(tenantId, userId, conversation.getId(),
                mode.name(), aiResponse.getModel(), (int) latency,
                promptTokens, completionTokens, MiraAuditLog.AuditStatus.SUCCESS);

        // 12. トークン使用量記録
        tokenQuotaService.consume(tenantId, userId, conversation.getId(),
                aiResponse.getModel(), promptTokens, completionTokens);

        // 13. メトリクス記録
        metrics.incrementChatRequest(aiResponse.getModel(), tenantId, "success");
        metrics.recordChatLatency(aiResponse.getModel(), tenantId, latency);
        metrics.recordTokenUsage(aiResponse.getModel(), tenantId, promptTokens, completionTokens);

        // 14. レスポンス構築
        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(assistantMessage.getId())
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

    @Transactional
    public ContextSnapshotResponse saveContextSnapshot(
            ContextSnapshotRequest request,
            String tenantId,
            String userId) {

        MiraContextSnapshot snapshot = MiraContextSnapshot.builder()
                .id(UUID.randomUUID().toString())
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
                .snapshotId(saved.getId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public ChatResponse analyzeError(ErrorReportRequest request, String tenantId, String userId) {
        long startTime = System.currentTimeMillis();

        // 新規会話作成
        MiraConversation conversation = createConversation(
                tenantId, userId, MiraMode.ERROR_ANALYZE);

        // エラー分析用プロンプト構築
        AiRequest aiRequest = promptBuilder.buildErrorAnalyzeRequest(request);

        // AI 呼び出し
        AiProviderClient client = aiProviderFactory.getProvider();
        AiResponse aiResponse = client.chat(aiRequest);

        long latency = System.currentTimeMillis() - startTime;

        if (!aiResponse.isSuccess()) {
            return buildErrorResponse(conversation.getId(),
                    "エラー分析に失敗しました");
        }

        String formattedContent = responseFormatter.formatAsMarkdown(aiResponse.getContent());
        MiraMessage assistantMessage = saveAssistantMessage(conversation, formattedContent);

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(assistantMessage.getId())
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

    @Transactional
    public void clearConversation(String conversationId, String tenantId, String userId) {
        Optional<MiraConversation> conversation = conversationRepository.findById(conversationId);
        if (conversation.isPresent()
                && conversation.get().getTenantId().equals(tenantId)
                && conversation.get().getUserId().equals(userId)) {

            MiraConversation conv = conversation.get();
            conv.setStatus(MiraConversation.ConversationStatus.CLOSED);
            conversationRepository.save(conv);

            log.info("会話をクリア: conversationId={}", conversationId);
        }
    }

    public boolean isConversationActive(String conversationId, String tenantId, String userId) {
        try {
            Optional<MiraConversation> conversation = conversationRepository.findById(conversationId);

            return conversation
                    .filter(c -> c.getTenantId().equals(tenantId))
                    .filter(c -> c.getUserId().equals(userId))
                    .filter(c -> MiraConversation.ConversationStatus.ACTIVE.equals(c.getStatus()))
                    .isPresent();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 会話タイトルを AI で生成.
     * 
     * @param request
     *            タイトル生成リクエスト
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @return 生成されたタイトル
     */
    public GenerateTitleResponse generateTitle(GenerateTitleRequest request, String tenantId, String userId) {
        try {
            // 会話履歴からプロンプト生成
            StringBuilder conversationSummary = new StringBuilder();
            for (GenerateTitleRequest.MessageSummary msg : request.getMessages()) {
                String role = "user".equals(msg.getRole()) ? "ユーザー" : "アシスタント";
                String content = msg.getContent();
                // 長すぎるメッセージは切り詰め
                if (content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                conversationSummary.append(role).append(": ").append(content).append("\n");
            }

            // タイトル生成用プロンプト
            String prompt = """
                    以下の会話内容を要約し、会話のタイトルを生成してください。

                    要件:
                    - タイトルは15文字以内の簡潔な日本語
                    - 会話の主題や目的を端的に表現
                    - 絵文字や記号は使用しない
                    - タイトルのみを出力（説明や装飾は不要）

                    会話内容:
                    %s
                    """.formatted(conversationSummary.toString());

            // AI 呼び出し
            AiRequest aiRequest = AiRequest.builder()
                    .messages(List.of(
                            AiRequest.Message.builder()
                                    .role("user")
                                    .content(prompt)
                                    .build()))
                    .maxTokens(50)
                    .temperature(0.3)
                    .build();

            AiProviderClient client = aiProviderFactory.getProvider();
            AiResponse aiResponse = client.chat(aiRequest);

            if (!aiResponse.isSuccess()) {
                log.warn("タイトル生成失敗: {}", aiResponse.getErrorMessage());
                return GenerateTitleResponse.error(request.getConversationId(),
                        "タイトル生成に失敗しました");
            }

            // タイトル整形（余計な改行や空白を除去）
            String title = aiResponse.getContent()
                    .trim()
                    .replaceAll("[\r\n]+", "")
                    .replaceAll("^[「『]|[」』]$", ""); // 括弧があれば除去

            // 15文字を超える場合は切り詰め
            if (title.length() > 15) {
                title = title.substring(0, 14) + "…";
            }

            log.debug("タイトル生成完了: conversationId={}, title={}",
                    request.getConversationId(), title);

            return GenerateTitleResponse.success(request.getConversationId(), title);

        } catch (Exception e) {
            log.error("タイトル生成エラー: conversationId={}", request.getConversationId(), e);
            return GenerateTitleResponse.error(request.getConversationId(),
                    "タイトル生成中にエラーが発生しました");
        }
    }

    /**
     * 会話タイトル更新.
     */
    @Transactional
    public UpdateTitleResponse updateTitle(UpdateTitleRequest request, String tenantId, String userId) {
        try {
            // 会話を取得
            Optional<MiraConversation> conversationOpt = conversationRepository.findById(request.getConversationId());

            if (conversationOpt.isEmpty()) {
                log.warn("会話が見つかりません: conversationId={}", request.getConversationId());
                return UpdateTitleResponse.error(request.getConversationId(), "会話が見つかりません");
            }

            MiraConversation conversation = conversationOpt.get();

            // テナント・ユーザー検証
            if (!conversation.getTenantId().equals(tenantId) || !conversation.getUserId().equals(userId)) {
                log.warn("会話へのアクセス権限がありません: conversationId={}, tenantId={}, userId={}",
                        request.getConversationId(), tenantId, userId);
                return UpdateTitleResponse.error(request.getConversationId(), "会話へのアクセス権限がありません");
            }

            // タイトル更新
            conversation.setTitle(request.getTitle());
            conversationRepository.save(conversation);

            log.debug("タイトル更新完了: conversationId={}, title={}",
                    request.getConversationId(), request.getTitle());

            return UpdateTitleResponse.success(request.getConversationId(), request.getTitle());

        } catch (Exception e) {
            log.error("タイトル更新エラー: conversationId={}", request.getConversationId(), e);
            return UpdateTitleResponse.error(request.getConversationId(),
                    "タイトル更新中にエラーが発生しました");
        }
    }

    // ========================================
    // Private Methods
    // ========================================

    private MiraMode resolveMode(ChatRequest request) {
        return modeResolver.resolve(request);
    }

    private MiraConversation getOrCreateConversation(
            String conversationId, String tenantId, String userId, MiraMode mode) {

        if (conversationId != null && !conversationId.isEmpty()) {
            Optional<MiraConversation> existing = conversationRepository.findById(conversationId);

            if (existing.isPresent()
                    && existing.get().getTenantId().equals(tenantId)
                    && existing.get().getUserId().equals(userId)
                    && MiraConversation.ConversationStatus.ACTIVE.equals(existing.get().getStatus())) {
                return existing.get();
            }
        }

        return createConversation(tenantId, userId, mode);
    }

    private MiraConversation createConversation(String tenantId, String userId, MiraMode mode) {
        MiraConversation conversation = MiraConversation.builder()
                .id(UUID.randomUUID().toString())
                .tenantId(tenantId)
                .userId(userId)
                .mode(toConversationMode(mode))
                .status(MiraConversation.ConversationStatus.ACTIVE)
                .build();

        return conversationRepository.save(conversation);
    }

    private MiraConversation.ConversationMode toConversationMode(MiraMode mode) {
        return switch (mode) {
            case GENERAL_CHAT -> MiraConversation.ConversationMode.GENERAL_CHAT;
            case CONTEXT_HELP -> MiraConversation.ConversationMode.CONTEXT_HELP;
            case ERROR_ANALYZE -> MiraConversation.ConversationMode.ERROR_ANALYZE;
            case STUDIO_AGENT -> MiraConversation.ConversationMode.STUDIO_AGENT;
            case WORKFLOW_AGENT -> MiraConversation.ConversationMode.WORKFLOW_AGENT;
        };
    }

    private void saveUserMessage(MiraConversation conversation, String content) {
        MiraMessage message = MiraMessage.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversation.getId())
                .senderType(MiraMessage.SenderType.USER)
                .content(content)
                .contentType(MiraMessage.ContentType.PLAIN)
                .build();

        messageRepository.save(message);
    }

    private MiraMessage saveAssistantMessage(MiraConversation conversation, String content) {
        MiraMessage message = MiraMessage.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversation.getId())
                .senderType(MiraMessage.SenderType.ASSISTANT)
                .content(content)
                .contentType(MiraMessage.ContentType.MARKDOWN)
                .build();

        return messageRepository.save(message);
    }

    private List<AiRequest.Message> loadConversationHistory(String conversationId, MessageConfig config) {
        // historyScope チェック
        if (config != null) {
            String scope = config.getHistoryScope();
            if ("none".equals(scope)) {
                return new ArrayList<>();
            }
        }

        List<MiraMessage> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<AiRequest.Message> history = new ArrayList<>();
        for (MiraMessage msg : messages) {
            if (MiraMessage.SenderType.USER.equals(msg.getSenderType())) {
                history.add(AiRequest.Message.builder()
                        .role("user")
                        .content(msg.getContent())
                        .build());
            } else if (MiraMessage.SenderType.ASSISTANT.equals(msg.getSenderType())) {
                history.add(AiRequest.Message.builder()
                        .role("assistant")
                        .content(msg.getContent())
                        .build());
            }
        }

        // recent チェック
        if (config != null && "recent".equals(config.getHistoryScope())) {
            int recentCount = config.getRecentCount() != null ? config.getRecentCount() : 5;
            // 最後のN件を取得
            if (history.size() > recentCount) {
                return history.subList(history.size() - recentCount, history.size());
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
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Payload JSON変換失敗", e);
            return "{}";
        }
    }

    private int getTokensOrDefault(Integer tokens) {
        return tokens != null ? tokens : 0;
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
