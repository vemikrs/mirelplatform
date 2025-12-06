/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import java.time.Duration;

import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.http.HttpClient;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.util.HttpClientOptions;

import lombok.extern.slf4j.Slf4j;

/**
 * Mira モジュール設定クラス.
 * 
 * <p>Spring AI のベストプラクティスに基づき、自動設定を無効化し手動で Bean を構成します。</p>
 * 
 * <h3>設計方針</h3>
 * <ul>
 *   <li>{@code spring.ai.model.chat=none} で Spring AI autoconfigure を無効化</li>
 *   <li>{@code mira.ai.enabled=true} の場合のみ AI 関連 Bean を登録</li>
 *   <li>{@code mira.ai.mock.enabled=true} の場合、実際の AI Bean は登録しない</li>
 *   <li>Mock は常に登録（MockAiClient は @Component で自動登録）</li>
 * </ul>
 * 
 * <h3>プロバイダ選択</h3>
 * <ul>
 *   <li>{@code mira.ai.provider=github-models} - GitHub Models (Llama 3.3 等)</li>
 *   <li>{@code mira.ai.provider=azure-openai} - Azure OpenAI Service</li>
 *   <li>{@code mira.ai.provider=mock} - テスト用モック</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MiraAiProperties.class)
public class MiraConfiguration {

    /**
     * GitHub Models 関連 Bean を定義する内部設定クラス.
     * 
     * <p>GitHub Models は OpenAI 互換 API を提供するため、Spring AI の OpenAI クライアントを使用。</p>
     * <p>条件: {@code mira.ai.provider=github-models} の場合に有効</p>
     */
    @Configuration
    @ConditionalOnProperty(name = "mira.ai.provider", havingValue = "github-models")
    static class GitHubModelsConfiguration {

        /**
         * GitHub Models 用 ChatModel を構築.
         * 
         * <p>Spring AI の OpenAI クライアントを使用し、base-url を GitHub Models に向ける。</p>
         */
        @Bean
        @Primary
        ChatModel githubModelsChatModel(MiraAiProperties properties) {
            var githubConfig = properties.getGithubModels();
            
            if (githubConfig.getApiKey() == null || githubConfig.getApiKey().isBlank()) {
                log.error("GitHub Models API key not configured. Set GITHUB_TOKEN environment variable.");
                throw new IllegalStateException("GitHub Models API key is required when provider=github-models");
            }
            
            // OpenAI 互換 API として GitHub Models に接続
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .baseUrl(githubConfig.getBaseUrl())
                    .apiKey(githubConfig.getApiKey())
                    .build();
            
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(githubConfig.getModel())
                    .temperature(githubConfig.getTemperature())
                    .maxTokens(githubConfig.getMaxTokens())
                    .build();
            
            log.info("[MiraConfiguration] GitHub Models ChatModel initialized: baseUrl={}, model={}",
                    githubConfig.getBaseUrl(), githubConfig.getModel());
            
            return OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(options)
                    .build();
        }

        /**
         * ChatClient.Builder を構築.
         */
        @Bean
        ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
            return ChatClient.builder(chatModel);
        }
    }

    /**
     * Azure OpenAI 関連 Bean を定義する内部設定クラス.
     * 
     * <p>条件: {@code mira.ai.provider=azure-openai} の場合に有効</p>
     */
    @Configuration
    @ConditionalOnProperty(name = "mira.ai.provider", havingValue = "azure-openai")
    static class AzureOpenAiConfiguration {

        /**
         * Azure OpenAI ChatModel を構築.
         */
        @Bean
        @Primary
        ChatModel azureOpenAiChatModel(MiraAiProperties properties) {
            var azureConfig = properties.getAzureOpenai();
            
            if (azureConfig.getEndpoint() == null || azureConfig.getEndpoint().isBlank()) {
                log.error("Azure OpenAI endpoint not configured");
                throw new IllegalStateException("Azure OpenAI endpoint is required when provider=azure-openai");
            }
            
            if (azureConfig.getApiKey() == null || azureConfig.getApiKey().isBlank()) {
                log.error("Azure OpenAI API key not configured");
                throw new IllegalStateException("Azure OpenAI API key is required when provider=azure-openai");
            }
            
            HttpClientOptions clientOptions = new HttpClientOptions()
                    .setResponseTimeout(Duration.ofSeconds(azureConfig.getTimeoutSeconds()));
            
            OpenAIClientBuilder clientBuilder = new OpenAIClientBuilder()
                    .endpoint(azureConfig.getEndpoint())
                    .credential(new AzureKeyCredential(azureConfig.getApiKey()))
                    .httpClient(HttpClient.createDefault(clientOptions))
                    .httpLogOptions(new HttpLogOptions().setLogLevel(HttpLogDetailLevel.BASIC));
            
            AzureOpenAiChatOptions options = AzureOpenAiChatOptions.builder()
                    .deploymentName(azureConfig.getDeploymentName())
                    .temperature(azureConfig.getTemperature())
                    .maxTokens(azureConfig.getMaxTokens())
                    .build();
            
            log.info("[MiraConfiguration] Azure OpenAI ChatModel initialized: endpoint={}, deployment={}",
                    azureConfig.getEndpoint(), azureConfig.getDeploymentName());
            
            return AzureOpenAiChatModel.builder()
                    .openAIClientBuilder(clientBuilder)
                    .defaultOptions(options)
                    .build();
        }

        /**
         * ChatClient.Builder を構築.
         */
        @Bean
        ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
            return ChatClient.builder(chatModel);
        }
    }
}
