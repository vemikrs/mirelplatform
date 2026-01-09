/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Mira AI 設定クラス.
 */
@Data
@ConfigurationProperties(prefix = "mira.ai")
public class MiraAiProperties {

    /** プロバイダ名定数: Vertex AI. */
    public static final String PROVIDER_VERTEX_AI_GEMINI = "vertex-ai-gemini";

    /** プロバイダ名定数: GitHub Models. */
    public static final String PROVIDER_GITHUB_MODELS = "github-models";

    /** プロバイダ名定数: Azure OpenAI. */
    public static final String PROVIDER_AZURE_OPENAI = "azure-openai";

    /** プロバイダ名定数: OpenAI. */
    public static final String PROVIDER_OPENAI = "openai";

    /** Mira AI 機能全体の有効化フラグ. */
    private boolean enabled = true;

    /**
     * AI プロバイダ種別 (github-models | azure-openai | vertex-ai-gemini | openai | mock).
     */
    private String provider = "github-models";

    private GitHubModelsConfig githubModels = new GitHubModelsConfig();
    private AzureOpenAiConfig azureOpenai = new AzureOpenAiConfig();
    private OpenAiConfig openai = new OpenAiConfig();
    private VertexAiConfig vertexAi = new VertexAiConfig();
    private MockConfig mock = new MockConfig();

    /** 監査ログ設定. */
    private Audit audit = new Audit();

    /** クォータ設定. */
    private Quota quota = new Quota();

    /** レート制限設定. */
    private RateLimit rateLimit = new RateLimit();

    /** セキュリティ設定. */
    private SecurityConfig security = new SecurityConfig();

    /** モニタリング設定. */
    private MonitoringConfig monitoring = new MonitoringConfig();

    /** リランカー設定. */
    private RerankerConfig reranker = new RerankerConfig();

    @Data
    public static class GitHubModelsConfig {
        private String apiKey;
        private String baseUrl = "https://models.inference.ai.azure.com";
        private String model = "meta-llama-3.3-70b-instruct";
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
        private Integer timeoutSeconds = 120;
    }

    @Data
    public static class AzureOpenAiConfig {
        private String endpoint;
        private String apiKey;
        private String deploymentName = "gpt-4o";
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
        private Integer timeoutSeconds = 60;
    }

    @Data
    public static class OpenAiConfig {
        private String apiKey;
        private String model = "gpt-4o";
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
    }

    @Data
    public static class VertexAiConfig {
        private String projectId;
        private String location = "us-central1";
        private String model = "gemini-2.5-flash";
        private Double temperature = 0.7;
        private Integer maxTokens = 4096;
        /** Google Search Grounding の有効化フラグ. */
        private boolean googleSearchRetrieval = false;
    }

    @Data
    public static class MockConfig {
        private boolean enabled = false;
        private Integer responseDelayMs = 500;
        private String defaultResponse = "ご質問ありがとうございます。詳細をお知らせください。";
        private Map<String, String> responses = new HashMap<>();
    }

    @Data
    public static class Audit {
        private boolean enabled = true;
        private boolean logContent = false;
        private String storagePolicy = "METADATA_ONLY";
        private int retentionDays = 90;
    }

    @Data
    public static class Quota {
        private boolean enabled = true;
        private long dailyTokenLimit = 100000;
        private double warningThreshold = 0.8;
    }

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int requestsPerMinute = 60;
    }

    @Data
    public static class SecurityConfig {
        private PromptInjectionConfig promptInjection = new PromptInjectionConfig();
        private PiiMaskingConfig piiMasking = new PiiMaskingConfig();
        private OutputFilteringConfig outputFiltering = new OutputFilteringConfig();

        @Data
        public static class PromptInjectionConfig {
            private boolean enabled = true;
            private int softBlockThreshold = 3;
            private int hardBlockThreshold = 5;
            private SemanticTagSensitivity semanticTagSensitivity = SemanticTagSensitivity.MEDIUM;
        }

        /**
         * 意味タグ検出の感度レベル.
         */
        public enum SemanticTagSensitivity {
            /** LOW: <system>, <assistant> のみ検出 - 誤検知最小、基本防御 */
            LOW,
            /** MEDIUM: すべてのLLM関連タグ検出 - バランス型 (推奨) */
            MEDIUM,
            /** HIGH: すべてのXML風タグ検出 - 最大防御、誤検知増加 */
            HIGH
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
    }

    @Data
    public static class MonitoringConfig {
        private boolean enabled = true;
        private boolean detailedTiming = true;
        private long responseTimeThresholdMs = 5000;
        private double errorRateThreshold = 0.05;
    }

    /** ベクトル検索設定. */
    private VectorConfig vector = new VectorConfig();

    @Data
    public static class VectorConfig {
        /** 検索時の類似度閾値 (デフォルト: 0.6). */
        private double searchThreshold = 0.6;
    }

    /**
     * リランカー設定.
     */
    @Data
    public static class RerankerConfig {
        /** リランカー有効化フラグ. */
        private boolean enabled = true;

        /** プロバイダー: vertex-ai, cohere, none. */
        private String provider = "vertex-ai";

        /** Vertex AI モデル名. */
        private String model = "semantic-ranker-default-004";

        /** 最終採用件数. */
        private int topN = 5;

        /** リランキング実行の候補数閾値（この件数以上で実行）. */
        private int minCandidates = 10;

        /** タイムアウト（ミリ秒）. */
        private int timeoutMs = 5000;
    }
}
