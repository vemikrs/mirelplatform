package jp.vemi.mirel.apps.mira.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

/**
 * Mira AI サービスのメトリクス定義と記録用コンポーネント
 */
@Component
@RequiredArgsConstructor
public class MiraMetrics {

    private final MeterRegistry registry;

    private static final String METRIC_CHAT_REQUESTS = "mira.chat.requests";
    private static final String METRIC_CHAT_LATENCY = "mira.chat.latency";
    private static final String METRIC_TOKENS_PROMPT = "mira.tokens.prompt";
    private static final String METRIC_TOKENS_COMPLETION = "mira.tokens.completion";
    private static final String METRIC_ERRORS = "mira.errors";
    private static final String METRIC_SERVICE_EXECUTION = "mira.service.execution";

    /**
     * チャットリクエスト数を記録
     * 
     * @param model
     *            使用したモデル (e.g., gpt-4o)
     * @param tenantId
     *            テナントID
     * @param status
     *            ステータス (success/error)
     */
    public void incrementChatRequest(String model, String tenantId, String status) {
        Counter.builder(METRIC_CHAT_REQUESTS)
                .tag("model", model)
                .tag("tenant", tenantId)
                .tag("status", status)
                .description("Number of chat requests")
                .register(registry)
                .increment();
    }

    /**
     * チャット応答時間を記録
     * 
     * @param model
     *            使用したモデル
     * @param tenantId
     *            テナントID
     * @param durationMs
     *            所要時間(ミリ秒)
     */
    public void recordChatLatency(String model, String tenantId, long durationMs) {
        Timer.builder(METRIC_CHAT_LATENCY)
                .tag("model", model)
                .tag("tenant", tenantId)
                .description("Latency of chat requests")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * トークン使用量を記録
     * 
     * @param model
     *            使用したモデル
     * @param tenantId
     *            テナントID
     * @param promptTokens
     *            プロンプトトークン数
     * @param completionTokens
     *            完了トークン数
     */
    public void recordTokenUsage(String model, String tenantId, int promptTokens, int completionTokens) {
        Counter.builder(METRIC_TOKENS_PROMPT)
                .tag("model", model)
                .tag("tenant", tenantId)
                .description("Number of prompt tokens used")
                .register(registry)
                .increment(promptTokens);

        Counter.builder(METRIC_TOKENS_COMPLETION)
                .tag("model", model)
                .tag("tenant", tenantId)
                .description("Number of completion tokens used")
                .register(registry)
                .increment(completionTokens);
    }

    /**
     * エラー発生数を記録
     * 
     * @param type
     *            エラータイプ
     * @param tenantId
     *            テナントID
     */
    public void incrementError(String type, String tenantId) {
        Counter.builder(METRIC_ERRORS)
                .tag("type", type)
                .tag("tenant", tenantId)
                .description("Number of errors in Mira service")
                .register(registry)
                .increment();
    }

    /**
     * サービス実行時間を記録
     * 
     * @param className
     *            クラス名
     * @param methodName
     *            メソッド名
     * @param durationMs
     *            所要時間(ミリ秒)
     */
    public void recordServiceExecution(String className, String methodName, long durationMs) {
        Timer.builder(METRIC_SERVICE_EXECUTION)
                .tag("class", className)
                .tag("method", methodName)
                .description("Execution time of Mira services")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
