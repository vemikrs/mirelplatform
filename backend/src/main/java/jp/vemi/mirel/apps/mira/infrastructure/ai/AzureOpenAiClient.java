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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Azure OpenAI AI クライアント.
 * 
 * <p>Spring AI 1.1 を使用した Azure OpenAI Service への接続を提供します。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mira.ai.provider", havingValue = "azure-openai", matchIfMissing = false)
public class AzureOpenAiClient implements AiProviderClient {

    private final ChatClient chatClient;
    private final MiraAiProperties properties;

    private static final String PROVIDER_NAME = "azure-openai";

    public AzureOpenAiClient(ChatClient.Builder chatClientBuilder, MiraAiProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
        log.info("AzureOpenAiClient initialized with deployment: {}",
                properties.getAzureOpenai().getDeploymentName());
    }

    @Override
    public AiResponse chat(AiRequest request) {
        log.debug("AzureOpenAiClient.chat() called");

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
                            .build()
            );

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
        String endpoint = properties.getAzureOpenai().getEndpoint();
        String apiKey = properties.getAzureOpenai().getApiKey();
        return endpoint != null && !endpoint.isEmpty() && apiKey != null && !apiKey.isEmpty();
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

        // システムプロンプト
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }

        // コンテキストプロンプト（システムメッセージとして追加）
        if (request.getContextPrompt() != null && !request.getContextPrompt().isEmpty()) {
            messages.add(new SystemMessage(request.getContextPrompt()));
        }

        // 会話履歴
        if (request.getConversationHistory() != null) {
            for (AiRequest.Message histMsg : request.getConversationHistory()) {
                switch (histMsg.getRole().toLowerCase()) {
                    case "user":
                        messages.add(new UserMessage(histMsg.getContent()));
                        break;
                    case "assistant":
                        messages.add(new AssistantMessage(histMsg.getContent()));
                        break;
                    case "system":
                        messages.add(new SystemMessage(histMsg.getContent()));
                        break;
                }
            }
        }

        // ユーザプロンプト
        if (request.getUserPrompt() != null && !request.getUserPrompt().isEmpty()) {
            messages.add(new UserMessage(request.getUserPrompt()));
        }

        return messages;
    }
}
