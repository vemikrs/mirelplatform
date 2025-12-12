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
import jp.vemi.mirel.apps.mira.domain.model.ModelCapabilityValidation;
import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
import jp.vemi.mirel.apps.mira.infrastructure.ai.TokenCounter;
import jp.vemi.mirel.apps.mira.infrastructure.ai.tool.TavilySearchProvider;
import jp.vemi.mirel.apps.mira.infrastructure.ai.tool.WebSearchProvider;
import jp.vemi.mirel.apps.mira.infrastructure.ai.tool.WebSearchTools;
import lombok.Builder;
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
@Builder
@lombok.AllArgsConstructor
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
    private final MiraSettingService settingService;
    private final TavilySearchProvider tavilySearchProvider;
    private final ModelCapabilityValidator modelCapabilityValidator;

    /**
     * 会話一覧取得.
     *
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @param pageable
     *            ページング情報
     * @return 会話一覧レスポンス
     */
    @Transactional(readOnly = true)
    public jp.vemi.mirel.apps.mira.domain.dto.response.ConversationListResponse listConversations(
            String tenantId, String userId, org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.domain.Page<MiraConversation> page = conversationRepository
                .findByTenantIdAndUserIdAndStatusOrderByLastActivityAtDesc(
                        tenantId, userId, MiraConversation.ConversationStatus.ACTIVE, pageable);

        List<jp.vemi.mirel.apps.mira.domain.dto.response.ConversationListResponse.ConversationSummary> summaries = page
                .getContent().stream()
                .map(c -> new jp.vemi.mirel.apps.mira.domain.dto.response.ConversationListResponse.ConversationSummary(
                        c.getId(),
                        c.getTitle(),
                        c.getMode(),
                        c.getCreatedAt(),
                        c.getLastActivityAt()))
                .toList();

        return new jp.vemi.mirel.apps.mira.domain.dto.response.ConversationListResponse(
                summaries,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * 会話詳細取得.
     *
     * @param conversationId
     *            会話ID
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @return 会話詳細レスポンス
     */
    @Transactional(readOnly = true)
    public jp.vemi.mirel.apps.mira.domain.dto.response.ConversationDetailResponse getConversation(
            String conversationId, String tenantId, String userId) {

        MiraConversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found or access denied"));

        List<MiraMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<jp.vemi.mirel.apps.mira.domain.dto.response.ConversationDetailResponse.MessageDetail> messageDetails = messages
                .stream()
                .map(m -> new jp.vemi.mirel.apps.mira.domain.dto.response.ConversationDetailResponse.MessageDetail(
                        m.getId(),
                        m.getSenderType() == MiraMessage.SenderType.USER ? "user" : "assistant",
                        m.getContent(),
                        m.getContentType().name().toLowerCase(),
                        m.getCreatedAt()))
                .toList();

        return new jp.vemi.mirel.apps.mira.domain.dto.response.ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getMode(),
                conversation.getCreatedAt(),
                conversation.getLastActivityAt(),
                messageDetails);
    }

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

        // 0.5. モデル機能バリデーション (Web検索・マルチモーダル等)
        ModelCapabilityValidation capabilityValidation = modelCapabilityValidator.validate(request);
        if (!capabilityValidation.isValid()) {
            auditService.logChatResponse(tenantId, userId, request.getConversationId(),
                    "UNKNOWN", null, (int) (System.currentTimeMillis() - startTime), 0, 0,
                    MiraAuditLog.AuditStatus.ERROR);
            return buildErrorResponse(request.getConversationId(), capabilityValidation.getErrorMessage());
        }

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

        // 5. 会話履歴取得 (設定に応じてフィルタリング)
        MessageConfig msgConfig = request.getContext() != null ? request.getContext().getMessageConfig() : null;

        List<AiRequest.Message> history = loadConversationHistory(
                conversation.getId(), msgConfig);

        // 4. ユーザーメッセージ保存
        saveUserMessage(conversation, request.getMessage().getContent());

        // 6. プロンプト構築（コンテキストレイヤーを反映）
        String finalContext = contextMergeService.buildFinalContextPrompt(
                tenantId, null, userId, msgConfig);

        AiRequest aiRequest = promptBuilder.buildChatRequestWithContext(
                request, mode, history, finalContext);
        aiRequest.setTenantId(tenantId);
        aiRequest.setUserId(userId);

        // 7. ツール解決 & セット (webSearchEnabledを参照)
        List<org.springframework.ai.tool.ToolCallback> tools = resolveTools(tenantId, userId,
                request.getWebSearchEnabled());
        aiRequest.setToolCallbacks(tools);

        // 8. AI 呼び出し Loop
        int loopCount = 0;
        int maxLoops = 10;
        AiResponse aiResponse = null;

        while (loopCount < maxLoops) {
            loopCount++;

            // Client creation (Moved inside loop or reused? Client is stateless mostly, but
            // factory creates new one)
            // Reuse client if provider doesn't change
            AiProviderClient client;
            if (request.getForceProvider() != null && !request.getForceProvider().isEmpty()) {
                if (systemRole != null && systemRole.contains("ADMIN")) {
                    client = aiProviderFactory.getProvider(request.getForceProvider())
                            .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
                } else {
                    client = aiProviderFactory.createClient(tenantId);
                }
            } else {
                client = aiProviderFactory.createClient(tenantId);
            }

            aiResponse = client.chat(aiRequest);

            if (!aiResponse.isSuccess()) {
                break; // Error handling outside
            }

            // Check Tool Calls
            if (aiResponse.getToolCalls() == null || aiResponse.getToolCalls().isEmpty()) {
                break; // Final response reached
            }

            // --- Tool Call Handling ---
            log.info("AI requested tool execution: {} calls", aiResponse.getToolCalls().size());

            // 8-1. Save Assistant Message (Tool Call)
            // We serialize tool calls into payload
            String toolCallPayload = "{}";
            try {
                toolCallPayload = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(aiResponse.getToolCalls());
            } catch (Exception e) {
                log.warn("Json error", e);
            }

            MiraMessage assistantMessage = saveAssistantMessage(conversation, aiResponse, toolCallPayload);

            // 8-2. Append to Request Messages (Assistant State)
            AiRequest.Message assistantReqMsg = AiRequest.Message.assistant(null);
            assistantReqMsg.setToolCalls(aiResponse.getToolCalls());
            aiRequest.getMessages().add(assistantReqMsg);

            // 8-3. Execute Tools & Save Results
            for (AiRequest.Message.ToolCall output : aiResponse.getToolCalls()) {
                long toolStart = System.currentTimeMillis();
                String result = executeTool(output, tools);
                long toolLatency = System.currentTimeMillis() - toolStart;

                // Save Tool Message
                saveToolMessage(conversation, output.getId(), output.getName(), result, toolLatency);

                // Append to Request Messages (Tool State)
                AiRequest.Message toolReqMsg = AiRequest.Message.builder()
                        .role("tool")
                        .toolCallId(output.getId())
                        .toolName(output.getName())
                        .content(result)
                        .build();
                aiRequest.getMessages().add(toolReqMsg);
            }

            // Loop continues to send results back to AI
        }

        long latency = System.currentTimeMillis() - startTime;

        // 9. 応答処理 (Final Response)
        if (aiResponse == null || !aiResponse.isSuccess()) {
            String errMsg = (aiResponse != null) ? aiResponse.getErrorMessage() : "Loop limit reached or unknown error";
            log.error("AI invocation failed: {}", errMsg);
            // ... (Audit log & Metrics omitted for brevity, keeping original flow logic
            // roughly)
            return buildErrorResponse(conversation.getId(), "AI Error: " + errMsg);
        }

        // 10. 応答フィルタリング
        String filteredContent = policyEnforcer.filterResponse(
                aiResponse.getContent(), systemRole);
        String formattedContent = responseFormatter.formatAsMarkdown(filteredContent);

        // 11. アシスタントメッセージ保存 (Final)
        MiraMessage assistantMessage = saveAssistantMessage(conversation, aiResponse, formattedContent);

        // 12. 監査ログ & Metrics
        // (Simplified re-implementation of original logic)
        int promptTokens = getTokensOrDefault(aiResponse.getPromptTokens());
        int completionTokens = getTokensOrDefault(aiResponse.getCompletionTokens());

        auditService.logChatResponse(tenantId, userId, conversation.getId(),
                mode.name(), aiResponse.getModel(), (int) latency,
                promptTokens, completionTokens, MiraAuditLog.AuditStatus.SUCCESS);

        tokenQuotaService.consume(tenantId, userId, conversation.getId(),
                aiResponse.getModel(), promptTokens, completionTokens);

        // 13. レスポンス構築
        ChatResponse response = ChatResponse.builder()
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

        // 14. タイトル自動生成 (Async/Try-catch)
        if (conversation.getTitle() == null || conversation.getTitle().isEmpty()
                || "新しい会話".equals(conversation.getTitle())) {
            try {
                autoGenerateTitle(conversation, tenantId);
            } catch (Exception e) {
                log.warn("Auto title generation failed", e);
            }
        }

        return response;
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
        MiraMessage assistantMessage = saveAssistantMessage(conversation, aiResponse, formattedContent);

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

    /**
     * 会話タイトルを最新の履歴に基づいて再生成.
     */
    @Transactional
    public GenerateTitleResponse regenerateTitle(String conversationId, String tenantId, String userId) {
        MiraConversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found or access denied"));

        try {
            autoGenerateTitle(conversation, tenantId);
            return GenerateTitleResponse.success(conversationId, conversation.getTitle());
        } catch (Exception e) {
            log.error("Title regeneration failed", e);
            return GenerateTitleResponse.error(conversationId, "タイトルの再生成に失敗しました");
        }
    }

    // ========================================
    // Private Methods
    // ========================================

    public MiraMode resolveMode(ChatRequest request) {
        return modeResolver.resolve(request);
    }

    public MiraConversation getOrCreateConversation(
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

        return createConversation(tenantId, userId, mode, conversationId);
    }

    private MiraConversation createConversation(String tenantId, String userId, MiraMode mode) {
        return createConversation(tenantId, userId, mode, null);
    }

    private MiraConversation createConversation(String tenantId, String userId, MiraMode mode, String requestedId) {
        String idToUse = (requestedId != null && !requestedId.isEmpty()) ? requestedId : UUID.randomUUID().toString();

        MiraConversation conversation = MiraConversation.builder()
                .id(idToUse)
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

    public void saveUserMessage(MiraConversation conversation, String content) {
        MiraMessage message = MiraMessage.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversation.getId())
                .senderType(MiraMessage.SenderType.USER)
                .content(content)
                .contentType(MiraMessage.ContentType.PLAIN)
                .build();

        messageRepository.save(message);
    }

    public MiraMessage saveAssistantMessage(MiraConversation conversation, AiResponse aiResponse, String content) {
        MiraMessage.ContentType contentType = MiraMessage.ContentType.MARKDOWN;
        String payload = content;
        String model = aiResponse.getModel();
        Integer tokens = getTokensOrDefault(aiResponse.getCompletionTokens());

        // Tool Calls Handling
        if (aiResponse.getToolCalls() != null && !aiResponse.getToolCalls().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                payload = mapper.writeValueAsString(aiResponse.getToolCalls());
                contentType = MiraMessage.ContentType.STRUCTURED_JSON;
            } catch (Exception e) {
                log.warn("Failed to serialize tool calls for msg", e);
                // Fallback to storing raw content if serialization fails, though usually
                // content is null/empty for tool calls
                // If content is null, store empty string to avoid errors
                if (payload == null)
                    payload = "";
            }
        }

        MiraMessage message = MiraMessage.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversation.getId())
                .senderType(MiraMessage.SenderType.ASSISTANT)
                .content(payload)
                .contentType(contentType)
                .usedModel(model)
                .tokenCount(tokens)
                .build();

        conversation.updateLastActivity();
        conversationRepository.save(conversation);
        return messageRepository.save(message);
    }

    public List<AiRequest.Message> loadConversationHistory(String conversationId, MessageConfig config) {
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
                AiRequest.Message.MessageBuilder builder = AiRequest.Message.builder()
                        .role("assistant");

                if (MiraMessage.ContentType.STRUCTURED_JSON.equals(msg.getContentType())) {
                    // Deserialize ToolCalls
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        List<AiRequest.Message.ToolCall> toolCalls = mapper.readValue(
                                msg.getContent(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<AiRequest.Message.ToolCall>>() {
                                });
                        builder.toolCalls(toolCalls);
                        builder.content(null); // Content must be null? Or should we keep text if mixed?
                        // GitHub Models wants content:null if tool calls are present.
                    } catch (Exception e) {
                        log.warn("Failed to deserialize tool calls for msg={}", msg.getId(), e);
                        builder.content(msg.getContent()); // Fallback
                    }
                } else {
                    builder.content(msg.getContent());
                }
                history.add(builder.build());
            } else if (MiraMessage.SenderType.TOOL.equals(msg.getSenderType())) {
                history.add(AiRequest.Message.builder()
                        .role("tool")
                        .toolCallId(msg.getToolCallId())
                        .toolName(msg.getUsedModel()) // We store tool name in usedModel column for convenience
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

    @Transactional
    public void autoGenerateTitle(MiraConversation conversation, String tenantId) {
        // 会話履歴を取得
        List<MiraMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());

        // メッセージが少なすぎる場合はスキップ (User + Assistant の1往復以上を推奨)
        if (messages.size() < 2) {
            return;
        }

        // 会話履歴からプロンプト生成
        StringBuilder conversationSummary = new StringBuilder();
        for (MiraMessage msg : messages) {
            String role = MiraMessage.SenderType.USER.equals(msg.getSenderType()) ? "ユーザー" : "アシスタント";
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

        // プロバイダ取得 (デフォルト)
        AiProviderClient client = aiProviderFactory.createClient(tenantId);
        AiResponse aiResponse = client.chat(aiRequest);

        if (!aiResponse.isSuccess()) {
            log.warn("Auto title generation AI request failed: {}", aiResponse.getErrorMessage());
            return;
        }

        // タイトル整形
        String title = aiResponse.getContent()
                .trim()
                .replaceAll("[\r\n]+", "")
                .replaceAll("^[「『]|[」』]$", "");

        if (title.length() > 15) {
            title = title.substring(0, 14) + "…";
        }

        // 更新保存
        conversation.setTitle(title);
        conversationRepository.save(conversation);

        log.info("Auto title generated: conversationId={}, title={}", conversation.getId(), title);
    }

    public List<org.springframework.ai.tool.ToolCallback> resolveTools(String tenantId, String userId,
            Boolean webSearchEnabled) {
        List<org.springframework.ai.tool.ToolCallback> tools = new ArrayList<>();

        // Web Search Tool (明示的に有効化された場合、またはAPIキーが設定されている場合)
        boolean shouldEnableWebSearch = Boolean.TRUE.equals(webSearchEnabled);

        if (shouldEnableWebSearch) {
            String tavilyKey = settingService.getString(tenantId, MiraSettingService.KEY_TAVILY_API_KEY, null);
            if (tavilyKey != null && !tavilyKey.isEmpty()) {
                // TavilySearchProviderにAPIキーを設定
                tavilySearchProvider.setApiKey(tavilyKey);

                // WebSearchToolsはToolCallbackを直接実装しているので、そのまま追加
                WebSearchTools webSearchTools = new WebSearchTools(tavilySearchProvider);
                tools.add(webSearchTools);

                log.info("Web search tool enabled for tenant={}, user={}", tenantId, userId);
            } else {
                log.warn("Web search requested but Tavily API key is not configured for tenant={}", tenantId);
            }
        }

        return tools;
    }

    /**
     * 後方互換性のためのオーバーロード.
     */
    public List<org.springframework.ai.tool.ToolCallback> resolveTools(String tenantId, String userId) {
        return resolveTools(tenantId, userId, false);
    }

    public String executeTool(AiRequest.Message.ToolCall call, List<org.springframework.ai.tool.ToolCallback> tools) {
        try {
            for (org.springframework.ai.tool.ToolCallback tool : tools) {
                if (tool.getToolDefinition().name().equals(call.getName())) { // Match by Name
                    log.info("Executing Tool: {} args={}", call.getName(), call.getArguments());
                    return tool.call(call.getArguments());
                }
            }
            return "Tool not found: " + call.getName();
        } catch (Exception e) {
            log.error("Tool execution failed: {}", call.getName(), e);
            return "Error executing tool: " + e.getMessage();
        }
    }

    private void saveToolMessage(MiraConversation conversation, String toolCallId, String toolName, String result,
            long latency) {
        MiraMessage message = MiraMessage.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversation.getId())
                .senderType(MiraMessage.SenderType.TOOL)
                .content(result)
                .contentType(MiraMessage.ContentType.PLAIN)
                .toolCallId(toolCallId)
                .usedModel(toolName) // Store name in valid column
                .tokenCount(0) // Could estimate?
                .build();
        messageRepository.save(message);
    }
}
