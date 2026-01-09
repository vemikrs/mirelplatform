/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import jp.vemi.mirel.apps.mira.domain.service.TokenQuotaService;
import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * メトリクス計測とトークン使用量記録をラップするAIクライアントデコレーター.
 * 
 * <p>
 * 全てのAIプロバイダー呼び出しに対して透過的にメトリクスとトークン使用量を記録します。
 * </p>
 */
@Slf4j
public class MetricsWrappedAiClient implements AiProviderClient {

    private final AiProviderClient delegate;
    private final MiraMetrics metrics;
    private final TokenQuotaService tokenQuotaService;
    private final TokenCounter tokenCounter;
    private final String tenantId;

    public MetricsWrappedAiClient(
            AiProviderClient delegate,
            MiraMetrics metrics,
            TokenQuotaService tokenQuotaService,
            TokenCounter tokenCounter,
            String tenantId) {
        this.delegate = delegate;
        this.metrics = metrics;
        this.tokenQuotaService = tokenQuotaService;
        this.tokenCounter = tokenCounter;
        this.tenantId = tenantId;
    }

    @Override
    public AiResponse chat(AiRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            AiResponse response = delegate.chat(request);
            long latency = System.currentTimeMillis() - startTime;

            if (response.hasError()) {
                metrics.recordChatError("ai_error", tenantId);
            } else {
                String model = response.getModel() != null ? response.getModel() : delegate.getProviderName();
                int promptTokens = getTokensOrZero(response.getPromptTokens());
                int completionTokens = getTokensOrZero(response.getCompletionTokens());

                // Prometheus メトリクス
                metrics.recordChatCompletion(model, tenantId, latency, promptTokens, completionTokens);

                // トークン使用量をDBに記録（インサイト用）
                recordTokenUsage(request, model, promptTokens, completionTokens);
            }
            return response;
        } catch (Exception e) {
            metrics.recordChatError("exception", tenantId);
            throw e;
        }
    }

    @Override
    public Flux<AiResponse> stream(AiRequest request) {
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        AtomicInteger totalTokens = new AtomicInteger(0);
        AtomicReference<String> modelRef = new AtomicReference<>(delegate.getProviderName() + "-streaming");

        // プロンプトトークンを事前に推定（ストリーミングではレスポンスから取得できないため）
        int estimatedPromptTokens = estimatePromptTokens(request);

        return delegate.stream(request)
                .doOnNext(response -> {
                    if (response.getCompletionTokens() != null) {
                        totalTokens.addAndGet(response.getCompletionTokens());
                    }
                    if (response.getModel() != null) {
                        modelRef.set(response.getModel());
                    }
                })
                .doOnComplete(() -> {
                    long latency = System.currentTimeMillis() - startTime.get();
                    String model = modelRef.get();
                    int tokens = totalTokens.get();

                    // Prometheus メトリクス
                    metrics.recordChatCompletion(model, tenantId, latency, estimatedPromptTokens, tokens);

                    // トークン使用量をDBに記録（インサイト用）
                    recordTokenUsage(request, model, estimatedPromptTokens, tokens);

                    log.debug(
                            "[MetricsWrappedAiClient] Stream completed. Latency={}ms, PromptTokens={} (estimated), CompletionTokens={}",
                            latency, estimatedPromptTokens, tokens);
                })
                .doOnError(e -> {
                    metrics.recordChatError("stream_error", tenantId);
                    log.warn("[MetricsWrappedAiClient] Stream error: {}", e.getMessage());
                });
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public String getProviderName() {
        return delegate.getProviderName();
    }

    private int getTokensOrZero(Integer tokens) {
        if (tokens == null) {
            log.debug("[MetricsWrappedAiClient] Token count unavailable, defaulting to 0");
            return 0;
        }
        return tokens;
    }

    /**
     * トークン使用量をDBに記録.
     */
    private void recordTokenUsage(AiRequest request, String model, int promptTokens, int completionTokens) {
        try {
            String userId = request.getUserId() != null ? request.getUserId() : "unknown";
            String conversationId = request.getConversationId() != null ? request.getConversationId() : "unknown";
            tokenQuotaService.consume(tenantId, userId, conversationId, model, promptTokens, completionTokens);
        } catch (Exception e) {
            log.warn("[MetricsWrappedAiClient] Failed to record token usage: {}", e.getMessage());
        }
    }

    /**
     * リクエストからプロンプトトークン数を推定.
     * 
     * <p>
     * ストリーミングではレスポンスからプロンプトトークンを取得できないため、
     * TokenCounterを使用してリクエストのメッセージからトークン数を推定する。
     * </p>
     * 
     * @param request
     *            AIリクエスト
     * @return 推定トークン数
     */
    private int estimatePromptTokens(AiRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return 0;
        }

        int totalTokens = 0;
        for (AiRequest.Message msg : request.getMessages()) {
            if (msg.getContent() != null) {
                totalTokens += tokenCounter.count(msg.getContent(), null);
            }
        }
        return totalTokens;
    }
}
