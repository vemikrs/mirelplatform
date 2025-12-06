/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import java.util.Map;
import java.util.HashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Mira AI 設定クラス.
 * 
 * <p>application.yml の {@code mira.ai} プレフィックス配下の設定を保持します。</p>
 * 
 * <h3>設定例</h3>
 * <pre>
 * mira:
 *   ai:
 *     enabled: true           # Mira AI 機能全体の有効/無効
 *     provider: azure-openai  # azure-openai | mock
 *     azure-openai:
 *       endpoint: ${AZURE_OPENAI_ENDPOINT}
 *       api-key: ${AZURE_OPENAI_API_KEY}
 *       deployment-name: gpt-4o
 *     mock:
 *       enabled: false        # provider=azure-openai でも true にすると mock を使用
 *       response-delay-ms: 500
 * </pre>
 * 
 * <h3>Spring AI ベストプラクティス</h3>
 * <ul>
 *   <li>Spring AI の autoconfigure は {@code spring.ai.model.chat=none} で無効化</li>
 *   <li>このアプリ独自の {@code mira.ai.*} 設定で AI 機能を制御</li>
 *   <li>開発環境では {@code mira.ai.mock.enabled=true} でモック使用</li>
 * </ul>
 */
@Data
@ConfigurationProperties(prefix = "mira.ai")
public class MiraAiProperties {

    /**
     * Mira AI 機能全体の有効化フラグ.
     * 
     * <p>false の場合、AI 関連の Bean は登録されません。</p>
     */
    private boolean enabled = true;

    /**
     * AI プロバイダ種別.
     * 
     * <ul>
     *   <li>{@code azure-openai} - Azure OpenAI Service（デフォルト）</li>
     *   <li>{@code mock} - テスト用モックプロバイダ</li>
     * </ul>
     * 
     * <p>注意: {@code mock.enabled=true} の場合、この設定に関わらず mock が使用されます。</p>
     */
    private String provider = "azure-openai";

    /**
     * Azure OpenAI 設定.
     */
    private AzureOpenAiConfig azureOpenai = new AzureOpenAiConfig();

    /**
     * OpenAI 設定.
     */
    private OpenAiConfig openai = new OpenAiConfig();

    /**
     * モック設定.
     */
    private MockConfig mock = new MockConfig();

    /**
     * 監査ログ設定.
     */
    private AuditConfig audit = new AuditConfig();

    /**
     * Azure OpenAI 設定.
     */
    @Data
    public static class AzureOpenAiConfig {
        /** エンドポイント URL */
        private String endpoint;
        
        /** API キー */
        private String apiKey;
        
        /** デプロイメント名（モデル） */
        private String deploymentName = "gpt-4o";
        
        /** Temperature (0.0 - 2.0) */
        private Double temperature = 0.7;
        
        /** 最大トークン数 */
        private Integer maxTokens = 4096;
        
        /** タイムアウト（秒） */
        private Integer timeoutSeconds = 60;
    }

    /**
     * OpenAI 設定.
     */
    @Data
    public static class OpenAiConfig {
        /** API キー */
        private String apiKey;
        
        /** モデル名 */
        private String model = "gpt-4o";
        
        /** Temperature (0.0 - 2.0) */
        private Double temperature = 0.7;
        
        /** 最大トークン数 */
        private Integer maxTokens = 4096;
    }

    /**
     * モック設定.
     * 
     * <p>開発/テスト環境で外部 AI サービスに依存せず動作確認を行う場合に使用します。</p>
     */
    @Data
    public static class MockConfig {
        /**
         * モック有効化フラグ.
         * 
         * <p>true の場合、{@code provider} の設定に関わらず mock クライアントが使用されます。
         * 開発環境では通常 true に設定し、本番環境では false にします。</p>
         */
        private boolean enabled = false;
        
        /** 応答遅延シミュレーション（ミリ秒） */
        private Integer responseDelayMs = 500;
        
        /** デフォルト応答 */
        private String defaultResponse = "ご質問ありがとうございます。詳細をお知らせください。";
        
        /** パターンマッチ応答マップ */
        private Map<String, String> responses = new HashMap<>();
    }

    /**
     * 監査ログ設定.
     */
    @Data
    public static class AuditConfig {
        /** 監査ログ有効化フラグ */
        private boolean enabled = true;
        
        /**
         * 保存ポリシー.
         * 
         * <ul>
         *   <li>{@code FULL} - プロンプト・応答本文を暗号化して保存</li>
         *   <li>{@code SUMMARY} - 要約のみ保存</li>
         *   <li>{@code METADATA_ONLY} - メタデータのみ（デフォルト）</li>
         * </ul>
         */
        private String storagePolicy = "METADATA_ONLY";
        
        /** 保持期間（日） */
        private Integer retentionDays = 90;
    }
}
