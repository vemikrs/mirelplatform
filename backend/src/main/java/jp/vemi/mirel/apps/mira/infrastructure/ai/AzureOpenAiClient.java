/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure OpenAI AI クライアント.
 * 
 * <p>
 * Spring AI 1.1 を使用した Azure OpenAI Service への接続を提供します。
 * </p>
 * 
 * <p>
 * このコンポーネントは {@code mira.ai.mock.enabled=false}（デフォルト）かつ
 * {@link ChatClient.Builder} Bean が存在する場合にのみ有効になります。
 * </p>
 */
@Slf4j
@Component
public class AzureOpenAiClient implements AiProviderClient {

    private final MiraAiProperties properties;
    private final ChatClient chatClient;
    private final boolean available;

    private static final String PROVIDER_NAME = "azure-openai";

    public AzureOpenAiClient(MiraAiProperties properties) {
        this.properties = properties;
        var config = properties.getAzureOpenai();

        if (config.getEndpoint() == null || config.getEndpoint().isBlank() ||
                config.getApiKey() == null || config.getApiKey().isBlank()) {
            log.warn("Azure OpenAI config is missing. Client will be disabled.");
            this.chatClient = null;
            this.available = false;
        } else {
            this.chatClient = buildChatClient(config);
            this.available = true;
        }
        log.info("AzureOpenAiClient initialized with deployment: {}", config.getDeploymentName());
    }

    private ChatClient buildChatClient(MiraAiProperties.AzureOpenAiConfig config) {
        // Build Azure OpenAI Client with timeout
        com.azure.core.util.HttpClientOptions clientOptions = new com.azure.core.util.HttpClientOptions()
                .setResponseTimeout(java.time.Duration.ofSeconds(config.getTimeoutSeconds()));

        com.azure.ai.openai.OpenAIClientBuilder clientBuilder = new com.azure.ai.openai.OpenAIClientBuilder()
                .endpoint(config.getEndpoint())
                .credential(new com.azure.core.credential.AzureKeyCredential(config.getApiKey()))
                .httpClient(com.azure.core.http.HttpClient.createDefault(clientOptions))
                .httpLogOptions(new com.azure.core.http.policy.HttpLogOptions()
                        .setLogLevel(com.azure.core.http.policy.HttpLogDetailLevel.BASIC));

        org.springframework.ai.azure.openai.AzureOpenAiChatOptions options = org.springframework.ai.azure.openai.AzureOpenAiChatOptions
                .builder()
                .deploymentName(config.getDeploymentName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .build();

        org.springframework.ai.azure.openai.AzureOpenAiChatModel chatModel = org.springframework.ai.azure.openai.AzureOpenAiChatModel
                .builder()
                .openAIClientBuilder(clientBuilder)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem("You are a helpful assistant.")
                .build();
    }

    @Override
    public AiResponse chat(AiRequest request) {
        log.debug("AzureOpenAiClient.chat() called");

        if (!available || chatClient == null) {
            return AiResponse.error("PROVIDER_NOT_AVAILABLE", "Azure OpenAI is not configured.");
        }

        long startTime = System.currentTimeMillis();

        try {
            // メッセージリストを構築
            List<Message> messages = buildMessages(request);

            // プロンプトを作成して送信
            Prompt prompt = new Prompt(messages);
            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();

            long latencyMs = System.currentTimeMillis() - startTime;

            // 応答を変換
            String content = chatResponse.getResult().getOutput().getText();

            return AiResponse.success(
                    content,
                    AiResponse.Metadata.builder()
                            .model(properties.getAzureOpenai().getDeploymentName())
                            .finishReason(chatResponse.getResult().getMetadata().getFinishReason())
                            .latencyMs(latencyMs)
                            .build());

        } catch (Exception e) {
            log.error("Azure OpenAI API call failed", e);
            long latencyMs = System.currentTimeMillis() - startTime;

            return AiResponse.builder()
                    .error(AiResponse.ErrorInfo.builder()
                            .code("AZURE_OPENAI_ERROR")
                            .message(e.getMessage())
                            .detail(e.getClass().getName())
                            .build())
                    .metadata(AiResponse.Metadata.builder()
                            .latencyMs(latencyMs)
                            .build())
                    .build();
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    /**
     * リクエストからメッセージリストを構築.
     */
    private List<Message> buildMessages(AiRequest request) {
        List<Message> messages = new ArrayList<>();

        if (request.getMessages() != null) {
            for (AiRequest.Message msg : request.getMessages()) {
                switch (msg.getRole().toLowerCase()) {
                    case "user":
                        messages.add(new UserMessage(msg.getContent()));
                        break;
                    case "assistant":
                        messages.add(new AssistantMessage(msg.getContent()));
                        break;
                    case "system":
                        messages.add(new SystemMessage(msg.getContent()));
                        break;
                    default:
                        log.warn("Unknown message role: {}", msg.getRole());
                }
            }
        }

        return messages;
    }
}
