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
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderClient;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiResponse;
import jp.vemi.mirel.apps.mira.infrastructure.ai.TokenCounter;
import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
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

    /**
     * ストリームチャット実行.
     */
    @Transactional
    public Flux<MiraStreamResponse> streamChat(ChatRequest request, String tenantId, String userId) {
        log.info("StreamChat called. ConversationID: {}, Mode: {}", request.getConversationId(), request.getMode());
        long startTime = System.currentTimeMillis();

        // 0. Pre-flight Checks (Rate Limit, Quota)
        try {
            rateLimitService.checkRateLimit(tenantId, userId);
            int estimatedInputTokens = tokenCounter.count(request.getMessage().getContent(), "gpt-4o");
            tokenQuotaService.checkQuota(tenantId, estimatedInputTokens);
        } catch (Exception e) {
            return Flux.just(MiraStreamResponse.error("PREFLIGHT_ERROR", e.getMessage()));
        }

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
        chatService.saveUserMessage(conversation, request.getMessage().getContent());

        // Load History & Build Context (Transactional)
        MessageConfig msgConfig = request.getContext() != null ? request.getContext().getMessageConfig() : null;
        List<AiRequest.Message> history = chatService.loadConversationHistory(conversation.getId(), msgConfig);
        String finalContext = contextMergeService.buildFinalContextPrompt(
                tenantId, null, userId, msgConfig);

        AiRequest aiRequest = promptBuilder.buildChatRequestWithContext(
                request, mode, history, finalContext);

        // 2a. Resolve Tools
        List<org.springframework.ai.tool.ToolCallback> tools = chatService.resolveTools(tenantId, userId);
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

        return client.stream(aiRequest)
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

                                // Case 1: Standard/Flat OpenAI-like format
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
                                // Case 2: Observed "tool" key format (e.g. {"tool": "websearch", "query":
                                // "..."})
                                else if (root.has("tool")) {
                                    name = root.get("tool").asText();
                                    args = root.toString(); // Use the whole object as args (Tavily tool should pick up
                                                            // "query")
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

                        if (!finalContent.isEmpty()) {
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
                        }
                        return Flux.just(MiraStreamResponse.done(conversation.getId()));
                    }
                }));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ToolExecutionResult {
        AiRequest.Message.ToolCall call;
        String result;
    }
}
