/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Mira 非同期処理設定.
 * <p>
 * インデックス処理など時間のかかる処理を非同期で実行するためのスレッドプール設定を提供します。
 * </p>
 */
@Configuration
@EnableAsync
public class MiraAsyncConfig {

    /**
     * インデックス処理専用のスレッドプール.
     * <p>
     * LLM呼び出しを伴う処理に対応するため、適度なプールサイズを設定。
     * </p>
     */
    @Bean(name = "miraIndexingExecutor")
    public Executor miraIndexingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mira-indexing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
