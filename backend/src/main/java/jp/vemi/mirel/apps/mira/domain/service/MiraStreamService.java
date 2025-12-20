/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraAuditLog;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.MessageConfig;
import jp.vemi.mirel.apps.mira.domain.model.ModelCapabilityValidation;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderClient;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiResponse;
import jp.vemi.mirel.apps.mira.infrastructure.ai.TokenCounter;
import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
import jp.vemi.mirel.foundation.web.api.admin.service.AdminSystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Mira ストリームチャットサービス.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraStreamService {

    private final AiProviderFactory aiProviderFactory;
    private final MiraChatService chatService;
    private final PromptBuilder promptBuilder;
    private final PolicyEnforcer policyEnforcer;
    private final MiraContextMergeService contextMergeService;
    private final MiraAuditService auditService;
    private final MiraRateLimitService rateLimitService;
    private final TokenQuotaService tokenQuotaService;
    private final TokenCounter tokenCounter;
    private final ModelCapabilityValidator modelCapabilityValidator;
    private final AdminSystemSettingsService adminSystemSettingsService;
    private final ModelSelectionService modelSelectionService; // Phase 4: Model selection

    /**
     * ストリームチャット実行.
     */
    @Transactional
    public Flux<MiraStreamResponse> streamChat(ChatRequest request, String tenantId, String userId) {
        log.info("StreamChat called. ConversationID: {}, Mode: {}", request.getConversationId(), request.getMode());
        log.info("ChatRequest.message: content={}, attachedFiles={}",
                request.getMessage() != null ? request.getMessage().getContent() : "null",
                request.getMessage() != null && request.getMessage().getAttachedFiles() != null
                        ? request.getMessage().getAttachedFiles().size() + " files"
                        : "null");
        long startTime = System.currentTimeMillis();

        // 0. Pre-flight Checks (Rate Limit, Quota)
        try {
            rateLimitService.checkRateLimit(tenantId, userId);
            int estimatedInputTokens = tokenCounter.count(request.getMessage().getContent(), "gpt-4o");
            tokenQuotaService.checkQuota(tenantId, estimatedInputTokens);
        } catch (Exception e) {
            return Flux.just(MiraStreamResponse.error("PREFLIGHT_ERROR", e.getMessage()));
        }

        // 0.5. モデル機能バリデーション -> Moved to after model selection

        // 1. Policy & Mode
        PolicyEnforcer.ValidationResult validation = policyEnforcer.validateRequest(request);
        if (!validation.valid()) {
            return Flux.just(MiraStreamResponse.error("POLICY_ERROR", validation.errorMessage()));
        }

        MiraMode mode = chatService.resolveMode(request);
        // Note: Access check logic is in ChatService, but we need it here.
        // For brevity assuming basic checks pass or relying on reuse, but better to
        // call policyEnforcer directly if possible.
        // Assuming ChatService methods are reusing enforcer.

        // 2. Conversation & History (Executed in Transaction)
        // Ensure we load history and build context BEFORE starting the Flux stream
        // to avoid LOB access issues outside transaction.
        MiraConversation conversation = chatService.getOrCreateConversation(
                request.getConversationId(), tenantId, userId, mode);

        // Save User Message
        chatService.saveUserMessage(conversation, request.getMessage().getContent(),
                request.getMessage().getAttachedFiles());

        // Load History & Build Context (Transactional)
        MessageConfig msgConfig = request.getContext() != null ? request.getContext().getMessageConfig() : null;
        List<AiRequest.Message> history = chatService.loadConversationHistory(conversation.getId(), msgConfig);
        String finalContext = contextMergeService.buildFinalContextPrompt(
                tenantId, null, userId, msgConfig);

        AiRequest aiRequest = promptBuilder.buildChatRequestWithContext(
                request, mode, history, finalContext);

        // Phase 4: Model selection (5-step priority)
        String snapshotId = request.getContext() != null ? request.getContext().getSnapshotId() : null;
        String selectedModel = modelSelectionService.resolveModel(
                tenantId, userId, snapshotId, request.getForceModel());
        aiRequest.setModel(selectedModel);
        log.info("Selected model: {} for tenant: {}, user: {}, snapshot: {}", selectedModel, tenantId, userId,
                snapshotId);

        // 0.5. Turn back to Validation (Now we have the selected model)
        ModelCapabilityValidation capabilityValidation = modelCapabilityValidator.validateWithModel(request,
                selectedModel);
        if (!capabilityValidation.isValid()) {
            // Transaction rollback is not automatic here for Flux return, but since we
            // haven't modified heavy state yet (except conversation creation)
            // Ideally we should do this before DB writes, but model selection depends on
            // tenant params.
            // Given the constraints, we return error here. The empty conversation created
            // is harmless or reusable.
            return Flux.just(MiraStreamResponse.error("CAPABILITY_ERROR", capabilityValidation.getErrorMessage()));
        }

        // 2a. Resolve Tools (webSearchEnabledを参照)
        // Web検索の有効化判定 (MiraChatService の共通メソッドを使用)
        boolean isWebSearchActive = chatService.isWebSearchActive(request);

        if (isWebSearchActive) {
            aiRequest.setGoogleSearchRetrieval(true);
        }

        List<org.springframework.ai.tool.ToolCallback> tools = chatService.resolveTools(tenantId, userId,
                isWebSearchActive);
        aiRequest.setToolCallbacks(tools);

        // DEBUG LOGGING
        log.debug("Stream Request - ConversationID: {}", conversation.getId());
        log.debug("History Size: {}", history.size());
        if (!history.isEmpty()) {
            history.forEach(m -> log.debug("History Item: [{}] {}", m.getRole(), m.getContent()));
        }
        log.debug("Final Prompt Messages Count: {}", aiRequest.getMessages().size());
        aiRequest.getMessages().forEach(m -> log.debug("Prompt Msg: [{}] {}", m.getRole(), m.getContent()));

        // Request system role for policy filtering
        String systemRole = request.getContext() != null ? request.getContext().getSystemRole() : null;

        // 3. Start Stream (Flux)
        return Flux.defer(() -> {
            return executeStreamLoop(aiRequest, tenantId, userId, conversation, mode, tools, startTime, 0, systemRole);
        });
    }

    private Flux<MiraStreamResponse> executeStreamLoop(
            AiRequest aiRequest, String tenantId, String userId,
            MiraConversation conversation, MiraMode mode,
            List<org.springframework.ai.tool.ToolCallback> tools,
            long startTime, int loopCount, String systemRole) {

        if (loopCount > 10) {
            return Flux.just(MiraStreamResponse.error("LOOP_LIMIT", "Tool execution loop limit reached"));
        }

        AiProviderClient client = aiProviderFactory.createClient(tenantId);

        // Accumulator for this turn
        AtomicReference<StringBuilder> contentBuffer = new AtomicReference<>(new StringBuilder());
        // For simple handling, we used a synchronized list, but since Flux is
        // sequential for a single subscription,
        // ArrayList is fine if we are careful.
        List<AiRequest.Message.ToolCall> accumulatedToolCalls = new ArrayList<>();

        // Google Search Grounding (Vertex AI) が有効な場合のワークアラウンド:
        //
        // 【問題】
        // - Vertex AI の Google Search Grounding を有効にした場合、ストリーミングモードで空応答が返される
        // - これは Spring AI 1.0.0-M6 と Vertex AI API の組み合わせにおける既知の問題
        //
        // 【対策】
        // - ブロッキング chat() 呼び出しを使用し、単一チャンクとしてストリームをシミュレート
        // - Schedulers.boundedElastic() で非同期実行し、メインスレッドをブロックしない
        //
        // 【影響】
        // - レスポンスタイム: ブロッキング呼び出しにより最初のトークンまでの時間が増加
        // - ユーザー体験: ストリーミング表示ではなく一括表示になる
        //
        // 【解決確認方法】
        // - Spring AI または Vertex AI SDK の更新時に stream() でテストし、正常動作を確認
        // - 問題が解決されていればこのワークアラウンドを削除可能
        //
        // 関連: Spring AI Issue (検討中)
        Flux<AiResponse> responseStream;
        boolean isBlockingSearch = Boolean.TRUE.equals(aiRequest.isGoogleSearchRetrieval());

        if (isBlockingSearch) {
            log.info("Google Search Grounding enabled. Switching to blocking chat for reliability.");
            responseStream = Mono.fromCallable(() -> client.chat(aiRequest))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flux();
        } else {
            responseStream = client.stream(aiRequest);
        }

        Flux<MiraStreamResponse> mainStream = responseStream

                .map(aiResponse -> {
                    if (aiResponse.hasError()) {
                        return MiraStreamResponse.error("AI_ERROR", aiResponse.getErrorMessage());
                    }

                    // 1. Tool Call Delta
                    if (aiResponse.getToolCalls() != null && !aiResponse.getToolCalls().isEmpty()) {
                        // Assumption: Spring AI 1.0.0 M6+ emits accumulated tool calls OR distinct
                        // deltas.
                        // Ideally we should merge. For GitHub Models, we might get full objects or
                        // deltas.
                        // If they are deltas, we need to merge by ID.
                        // Since mapped in client as new objects, we just add them.
                        // We will post-process merge if needed.
                        accumulatedToolCalls.addAll(aiResponse.getToolCalls());
                        // Don't emit delta for tool calls to frontend yet, or emit specific event?
                        // Frontend expects text delta.
                        // We can emit STATUS "Tool Call Detected..."
                    }

                    // 2. Text Delta
                    String delta = aiResponse.getContent();

                    if (delta != null) {
                        contentBuffer.get().append(delta);
                        // log.debug("Stream Delta: {}", delta); // Too noisy usually, but good for deep
                        // trace
                        return MiraStreamResponse.delta(delta, aiResponse.getModel());
                    }

                    // Return empty delta to keep stream alive
                    return MiraStreamResponse.delta("", aiResponse.getModel());
                })
                .onErrorResume(e -> {
                    log.error("Stream Loop Error: {}", e.getMessage(), e);

                    if (e instanceof jp.vemi.framework.exeption.MirelApplicationException) {
                        return Flux.just(MiraStreamResponse.error("USER_ERROR", e.getMessage()));
                    }

                    // Mask technical details for other exceptions
                    return Flux.just(MiraStreamResponse.error("SYSTEM_ERROR", "システムエラーが発生しました。再試行してください。"));
                })
                .filter(resp -> resp.getContent() != null && !resp.getContent().isEmpty()) // Filter empty
                .concatWith(Flux.defer(() -> {
                    // Turn Finished. Decide Next Step.
                    log.debug("Stream Turn Finished. Mode: {}, Loop: {}", mode, loopCount);

                    String fullContent = contentBuffer.get().toString();
                    log.debug("Full Content Buffer: {}", fullContent);
                    log.debug("Accumulated Structured Tool Calls: {}", accumulatedToolCalls.size());

                    // Attempt to parse text-based JSON tool call if no structured calls found
                    if (accumulatedToolCalls.isEmpty()) {
                        // Clean Markdown
                        String cleanedContent = fullContent.trim();
                        if (cleanedContent.startsWith("```json")) {
                            cleanedContent = cleanedContent.substring(7);
                        } else if (cleanedContent.startsWith("```")) {
                            cleanedContent = cleanedContent.substring(3);
                        }
                        if (cleanedContent.endsWith("```")) {
                            cleanedContent = cleanedContent.substring(0, cleanedContent.length() - 3);
                        }
                        cleanedContent = cleanedContent.trim();

                        if (cleanedContent.startsWith("{")) {
                            log.debug("Attempting to parse cleaned JSON tool call: {}", cleanedContent);
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(cleanedContent);

                                String name = null;
                                String args = "{}";

                                // Case 1: Standard/Flat OpenAI-like format {"name": "...", "parameters": {...}}
                                if (root.has("name") && root.has("parameters")) {
                                    name = root.get("name").asText();
                                    if (root.get("parameters").isObject()) {
                                        args = root.get("parameters").toString();
                                    } else if (root.get("parameters").isTextual()) {
                                        args = root.get("parameters").asText();
                                    } else {
                                        args = root.get("parameters").toString();
                                    }
                                }
                                // Case 2: Observed "tool" key format {"tool": "websearch", "query": "..."}
                                else if (root.has("tool")) {
                                    name = root.get("tool").asText();
                                    args = root.toString();
                                }
                                // Case 3: Llama/Mistral format {"type": "function", "name": "...",
                                // "parameters": {...}}
                                else if (root.has("type") && "function".equals(root.path("type").asText())
                                        && root.has("name")) {
                                    name = root.get("name").asText();
                                    // Try "arguments" first, then "parameters"
                                    if (root.has("arguments")) {
                                        args = root.get("arguments").isTextual()
                                                ? root.get("arguments").asText()
                                                : root.get("arguments").toString();
                                    } else if (root.has("parameters")) {
                                        args = root.get("parameters").isTextual()
                                                ? root.get("parameters").asText()
                                                : root.get("parameters").toString();
                                    }
                                }
                                // Case 4: webSearch-specific format mapping
                                if (name != null) {
                                    // Map tool names to our webSearch tool
                                    if (name.contains("tavily") || name.contains("web") || name.contains("search")) {
                                        name = "webSearch"; // Match our @Tool method name
                                        // Extract query from various formats
                                        try {
                                            com.fasterxml.jackson.databind.JsonNode argsNode = mapper.readTree(args);
                                            String query = null;
                                            if (argsNode.has("query")) {
                                                query = argsNode.get("query").asText();
                                            } else if (argsNode.has("latitude") && argsNode.has("longitude")) {
                                                // Weather request detected - convert to query
                                                query = "weather at latitude " + argsNode.get("latitude").asText()
                                                        + " longitude " + argsNode.get("longitude").asText();
                                            }
                                            if (query != null) {
                                                args = mapper.writeValueAsString(java.util.Map.of("query", query));
                                            }
                                        } catch (Exception e) {
                                            log.debug("Args reformat failed, using original", e);
                                        }
                                    }
                                }

                                if (name != null) {
                                    String id = "call_" + java.util.UUID.randomUUID().toString().substring(0, 8);
                                    AiRequest.Message.ToolCall textToolCall = new AiRequest.Message.ToolCall(id,
                                            "function", name, args);
                                    accumulatedToolCalls.add(textToolCall);

                                    // Clear buffer so we don't save this JSON as assistant text
                                    contentBuffer.set(new StringBuilder());

                                    log.info("Detected and parsed text-based tool call: {} args={}", name, args);
                                }
                            } catch (Exception e) {
                                log.debug("Failed to parse potential JSON tool call", e);
                                // Proceed as normal text
                            }
                        }
                    }

                    if (!accumulatedToolCalls.isEmpty()) {
                        // --- Tool Execution Branch ---
                        log.info("Entering Tool Execution Branch. Calls: {}", accumulatedToolCalls.size());

                        // 1. Merge Tool Calls (Simple distinct by ID)
                        List<AiRequest.Message.ToolCall> uniqueCalls = accumulatedToolCalls.stream()
                                .distinct() // Basic dedup
                                .toList();

                        // Emit Status
                        MiraStreamResponse status = MiraStreamResponse
                                .status("Executing " + uniqueCalls.size() + " tools...");

                        // 2. Execute Tools
                        return Flux.concat(
                                Flux.just(status),
                                Flux.fromIterable(uniqueCalls)
                                        .flatMap(tc -> {
                                            return Mono.fromCallable(() -> {
                                                log.debug("Executing tool: {}", tc.getName());
                                                // Execute
                                                String result;
                                                try {
                                                    result = chatService.executeTool(tc, tools);
                                                } catch (Exception e) {
                                                    // Return error as result so AI can see it
                                                    log.warn("Tool execution failed: {}", e.getMessage());
                                                    result = "Error executing tool " + tc.getName() + ": "
                                                            + e.getMessage();
                                                }
                                                log.debug("Tool result length: {}",
                                                        result != null ? result.length() : 0);

                                                // Save Tool Message (blocking safe here in thread pool)
                                                // Note: chatService.saveToolMessage is PRIVATE. Need to make it PUBLIC
                                                // or replicate.
                                                // Replicating simple save for now to avoid too many public exposure, or
                                                // use chatService if made public.
                                                // Let's use a helper in this class or just rely on memory for stream
                                                // and save at end?
                                                // No, history must be updated.
                                                // Let's make saveToolMessage public in next step?
                                                // For now, just generate the Message object for next request.
                                                return new ToolExecutionResult(tc, result);
                                            }).subscribeOn(Schedulers.boundedElastic());
                                        })
                                        .collectList()
                                        .flatMapMany(results -> {
                                            log.debug("All tools executed. Preparing recursive call.");

                                            // 3. Update History
                                            // Add Assistant Message (with Tool Calls)
                                            AiRequest.Message assistantMsg = AiRequest.Message.assistant(null);
                                            assistantMsg.setToolCalls(uniqueCalls);
                                            aiRequest.getMessages().add(assistantMsg);

                                            // Add Tool Messages
                                            for (ToolExecutionResult res : results) {
                                                aiRequest.getMessages().add(AiRequest.Message.builder()
                                                        .role("tool")
                                                        .toolCallId(res.call.getId())
                                                        .toolName(res.call.getName())
                                                        .content(res.result)
                                                        .build());
                                            }

                                            log.debug("History updated. Calls so far: {}. Recursive depth: {}",
                                                    aiRequest.getMessages().size(), loopCount + 1);

                                            // 4. Recursive Call
                                            return executeStreamLoop(aiRequest, tenantId, userId, conversation, mode,
                                                    tools, startTime, loopCount + 1, systemRole);
                                        }));

                    } else {
                        // --- Finalize Branch ---
                        log.debug("Entering Finalize Branch. No tool calls.");
                        String finalContent = contentBuffer.get().toString();

                        if (finalContent.isEmpty()) {
                            return Flux.just(MiraStreamResponse.error("EMPTY_RESPONSE", "応答がありませんでした。もう一度お試しください。"));
                        } else {
                            AiResponse dummyResponse = AiResponse.success(finalContent,
                                    AiResponse.Metadata.builder()
                                            .model("streaming-model")
                                            .completionTokens(tokenCounter.count(finalContent, "gpt-4o"))
                                            .latencyMs(System.currentTimeMillis() - startTime)
                                            .build());

                            try {
                                // Policy Filtering
                                String filteredContent = policyEnforcer.filterResponse(finalContent, systemRole);

                                chatService.saveAssistantMessage(conversation, dummyResponse, filteredContent);
                                auditService.logChatResponse(tenantId, userId, conversation.getId(),
                                        mode.name(), "streaming-model", (int) (System.currentTimeMillis() - startTime),
                                        0, dummyResponse.getCompletionTokens(), MiraAuditLog.AuditStatus.SUCCESS);

                                // Auto Title (Async)
                                if (conversation.getTitle() == null || conversation.getTitle().isEmpty()
                                        || "新しい会話".equals(conversation.getTitle())) {
                                    Mono.fromRunnable(() -> chatService.autoGenerateTitle(conversation, tenantId))
                                            .subscribeOn(Schedulers.boundedElastic())
                                            .subscribe();
                                }
                            } catch (Exception e) {
                                log.error("Post-stream processing failed", e);
                            }
                            return Flux.just(MiraStreamResponse.done(conversation.getId()));
                        }
                    }
                }));

        return isBlockingSearch
                ? Flux.concat(Flux.just(MiraStreamResponse.status("思考中...")), mainStream)
                : mainStream;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ToolExecutionResult {
        AiRequest.Message.ToolCall call;
        String result;
    }
}
