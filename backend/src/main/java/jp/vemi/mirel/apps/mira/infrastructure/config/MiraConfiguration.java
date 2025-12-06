/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import java.time.Duration;

import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
 *   <li>{@code mira.ai.mock.enabled=true} の場合、Azure OpenAI Bean は登録しない</li>
 *   <li>Mock は常に登録（MockAiClient は @Component で自動登録）</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MiraAiProperties.class)
public class MiraConfiguration {

    /**
     * Azure OpenAI 関連 Bean を定義する内部設定クラス.
     * 
     * <p>条件: {@code mira.ai.mock.enabled=false}（デフォルト）の場合のみ有効</p>
     */
    @Configuration
    @ConditionalOnProperty(name = "mira.ai.mock.enabled", havingValue = "false", matchIfMissing = true)
    static class AzureOpenAiConfiguration {

        /**
         * Azure OpenAI ChatModel を構築.
         */
        @Bean
        AzureOpenAiChatModel azureOpenAiChatModel(MiraAiProperties properties) {
            var azureConfig = properties.getAzureOpenai();
            
            if (azureConfig.getEndpoint() == null || azureConfig.getEndpoint().isBlank()) {
                log.warn("Azure OpenAI endpoint not configured, AzureOpenAiChatModel will not be created");
                return null;
            }
            
            if (azureConfig.getApiKey() == null || azureConfig.getApiKey().isBlank()) {
                log.warn("Azure OpenAI API key not configured, AzureOpenAiChatModel will not be created");
                return null;
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
            
            log.info("Azure OpenAI ChatModel initialized: endpoint={}, deployment={}",
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
        ChatClient.Builder chatClientBuilder(AzureOpenAiChatModel chatModel) {
            if (chatModel == null) {
                log.warn("AzureOpenAiChatModel is null, ChatClient.Builder will not be created");
                return null;
            }
            return ChatClient.builder(chatModel);
        }
    }
}
