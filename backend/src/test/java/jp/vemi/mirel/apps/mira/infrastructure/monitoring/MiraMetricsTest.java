package jp.vemi.mirel.apps.mira.infrastructure.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MiraMetricsTest {

    private SimpleMeterRegistry registry;
    private MiraMetrics miraMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        miraMetrics = new MiraMetrics(registry);
    }

    @Test
    void incrementChatRequest() {
        miraMetrics.incrementChatRequest("gpt-4o", "tenant-1", "success");

        assertThat(registry.counter("mira.chat.requests", "model", "gpt-4o", "tenant", "tenant-1", "status", "success")
                .count())
                        .isEqualTo(1.0);
    }

    @Test
    void recordChatLatency() {
        miraMetrics.recordChatLatency("gpt-4o", "tenant-1", 500L);

        assertThat(registry.timer("mira.chat.latency", "model", "gpt-4o", "tenant", "tenant-1").count())
                .isEqualTo(1L);
    }

    @Test
    void recordTokenUsage() {
        miraMetrics.recordTokenUsage("gpt-4o", "tenant-1", 100, 50);

        assertThat(registry.counter("mira.tokens.prompt", "model", "gpt-4o", "tenant", "tenant-1").count())
                .isEqualTo(100.0);
        assertThat(registry.counter("mira.tokens.completion", "model", "gpt-4o", "tenant", "tenant-1").count())
                .isEqualTo(50.0);
    }

    @Test
    void incrementError() {
        miraMetrics.incrementError("timeout", "tenant-1");

        assertThat(registry.counter("mira.errors", "type", "timeout", "tenant", "tenant-1").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordServiceExecution() {
        miraMetrics.recordServiceExecution("TestService", "testMethod", 100L);

        assertThat(registry.timer("mira.service.execution", "class", "TestService", "method", "testMethod").count())
                .isEqualTo(1L);
    }
}
