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

    /** Mira AI 機能全体の有効化フラグ. */
    private boolean enabled = true;

    /** AI プロバイダ種別 (github-models | azure-openai | mock). */
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
}
