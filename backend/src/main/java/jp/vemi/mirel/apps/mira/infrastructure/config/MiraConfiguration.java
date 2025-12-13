/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Mira モジュール設定クラス.
 * 
 * <p>
 * Spring AI のベストプラクティスに基づき、自動設定を無効化し手動で Bean を構成します。
 * </p>
 * 
 * <h3>設計方針</h3>
 * <ul>
 * <li>{@code spring.ai.model.chat=none} で Spring AI autoconfigure を無効化</li>
 * <li>{@code mira.ai.enabled=true} の場合のみ AI 関連 Bean を登録</li>
 * <li>{@code mira.ai.mock.enabled=true} の場合、実際の AI Bean は登録しない</li>
 * <li>Mock は常に登録（MockAiClient は @Component で自動登録）</li>
 * </ul>
 * 
 * <h3>プロバイダ選択</h3>
 * <ul>
 * <li>{@code mira.ai.provider=github-models} - GitHub Models (Llama 3.3 等)</li>
 * <li>{@code mira.ai.provider=azure-openai} - Azure OpenAI Service</li>
 * <li>{@code mira.ai.provider=mock} - テスト用モック</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MiraAiProperties.class)
public class MiraConfiguration {

}
