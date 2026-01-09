/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * メトリクス計測をラップするAIクライアントデコレーター.
 * 
 * <p>
 * 全てのAIプロバイダー呼び出しに対して透過的にメトリクスを計測します。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class MetricsWrappedAiClient implements AiProviderClient {

    private final AiProviderClient delegate;
    private final MiraMetrics metrics;
    private final String tenantId;

    @Override
    public AiResponse chat(AiRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            AiResponse response = delegate.chat(request);
            long latency = System.currentTimeMillis() - startTime;

            if (response.hasError()) {
                metrics.recordChatError("ai_error", tenantId);
            } else {
                metrics.recordChatCompletion(
                        response.getModel() != null ? response.getModel() : delegate.getProviderName(),
                        tenantId,
                        latency,
                        getTokensOrZero(response.getPromptTokens()),
                        getTokensOrZero(response.getCompletionTokens()));
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

        return delegate.stream(request)
                .doOnNext(response -> {
                    if (response.getCompletionTokens() != null) {
                        totalTokens.addAndGet(response.getCompletionTokens());
                    }
                })
                .doOnComplete(() -> {
                    long latency = System.currentTimeMillis() - startTime.get();
                    metrics.recordChatCompletion(
                            delegate.getProviderName() + "-streaming",
                            tenantId,
                            latency,
                            0,
                            totalTokens.get());
                    log.debug("[MetricsWrappedAiClient] Stream completed. Latency={}ms, Tokens={}",
                            latency, totalTokens.get());
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
        return tokens != null ? tokens : 0;
    }
}
