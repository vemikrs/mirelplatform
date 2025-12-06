/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Mira モジュール設定クラス.
 * 
 * <p>Mira 関連の設定プロパティを有効化します。</p>
 */
@Configuration
@EnableConfigurationProperties(MiraAiProperties.class)
public class MiraConfiguration {
    // Spring AI の自動構成により ChatClient.Builder が注入される
    // カスタム設定が必要な場合はここに Bean を追加
}
