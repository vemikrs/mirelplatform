/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import java.util.List;
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
 *     provider: github-models # github-models | azure-openai | mock
 *     github-models:
 *       token: ${GITHUB_TOKEN}
 *       model: meta/llama-3.3-70b-instruct
 *     azure-openai:
 *       endpoint: ${AZURE_OPENAI_ENDPOINT}
 *       api-key: ${AZURE_OPENAI_API_KEY}
 *       deployment-name: gpt-4o
 *     mock:
 *       enabled: false        # provider=xxx でも true にすると mock を使用
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
     *   <li>{@code github-models} - GitHub Models API（デフォルト）</li>
     *   <li>{@code azure-openai} - Azure OpenAI Service</li>
     *   <li>{@code mock} - テスト用モックプロバイダ</li>
     * </ul>
     * 
     * <p>注意: {@code mock.enabled=true} の場合、この設定に関わらず mock が使用されます。</p>
     */
    private String provider = "github-models";

    /**
     * GitHub Models 設定.
     */
    private GitHubModelsConfig githubModels = new GitHubModelsConfig();

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
     * セキュリティ設定.
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * クォータ設定.
     */
    private QuotaConfig quota = new QuotaConfig();

    /**
     * モニタリング設定.
     */
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * GitHub Models 設定.
     * 
     * <p>GitHub Models API を使用する場合の設定です。</p>
     */
    @Data
    public static class GitHubModelsConfig {
        /** GitHub トークン（gh auth token で取得可能） */
        private String token;
        
        /** ベース URL */
        private String baseUrl = "https://models.github.ai/inference";
        
        /** モデル名 */
        private String model = "meta/llama-3.3-70b-instruct";
        
        /** Temperature (0.0 - 2.0) */
        private Double temperature = 0.7;
        
        /** 最大トークン数 */
        private Integer maxTokens = 4096;
        
        /** タイムアウト（秒） */
        private Integer timeoutSeconds = 60;
    }

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
        
        /** メッセージ内容をログに含めるか */
        private boolean logContent = false;
    }

    /**
     * セキュリティ設定.
     */
    @Data
    public static class SecurityConfig {
        /** プロンプトインジェクション検出 */
        private PromptInjectionConfig promptInjection = new PromptInjectionConfig();
        
        /** PII マスキング */
        private PiiMaskingConfig piiMasking = new PiiMaskingConfig();
        
        /** 出力フィルタリング */
        private OutputFilteringConfig outputFiltering = new OutputFilteringConfig();
        
        /** レート制限 */
        private RateLimitConfig rateLimit = new RateLimitConfig();
        
        /** データ保持 */
        private RetentionConfig retention = new RetentionConfig();
        
        @Data
        public static class PromptInjectionConfig {
            private boolean enabled = true;
            private int softBlockThreshold = 3;
            private int hardBlockThreshold = 5;
        }
        
        @Data
        public static class PiiMaskingConfig {
            private boolean enabled = true;
            private List<String> patterns = List.of("email", "phone", "credit_card", "my_number");
        }
        
        @Data
        public static class OutputFilteringConfig {
            private boolean enabled = true;
            private boolean blockSystemPromptLeak = true;
        }
        
        @Data
        public static class RateLimitConfig {
            private boolean enabled = true;
            private int requestsPerMinute = 60;
            private int requestsPerMinutePerUser = 20;
        }
        
        @Data
        public static class RetentionConfig {
            private int conversationDays = 90;
            private int auditLogDays = 365;
        }
    }

    /**
     * クォータ設定.
     */
    @Data
    public static class QuotaConfig {
        /** クォータ有効化フラグ */
        private boolean enabled = true;
        
        /** 日次トークン制限 */
        private long dailyTokenLimit = 1_000_000L;
        
        /** 警告閾値 (0.0 - 1.0) */
        private double warningThreshold = 0.8;
    }

    /**
     * モニタリング設定.
     */
    @Data
    public static class MonitoringConfig {
        /** メトリクス有効化フラグ */
        private boolean enabled = true;
        
        /** 詳細タイミング計測 */
        private boolean detailedTiming = true;
        
        /** レスポンス時間閾値（ミリ秒） */
        private long responseTimeThresholdMs = 5000;
        
        /** エラー率閾値 (0.0 - 1.0) */
        private double errorRateThreshold = 0.05;
    }
}
