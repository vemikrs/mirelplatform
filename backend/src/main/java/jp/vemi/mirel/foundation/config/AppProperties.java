/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * アプリケーション全般の設定プロパティ.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    /**
     * アプリケーションのベースURL
     */
    private String baseUrl = "http://localhost:5173";

    /**
     * アプリケーションのドメイン
     */
    private String domain = "localhost";
}
