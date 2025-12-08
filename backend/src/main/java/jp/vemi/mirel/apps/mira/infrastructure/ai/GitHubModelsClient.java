/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.retry.RetryUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import jp.vemi.mirel.apps.mira.domain.service.MiraSettingService;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub Models API クライアント.
 * 
 * <p>
 * Spring AI ChatClient を使用して実装（OpenAiApiを手動構築）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubModelsClient implements AiProviderClient {

    private static final String PROVIDER_NAME = "github-models";

    private final MiraAiProperties properties;
    private final MiraSettingService settingService;
    private final jp.vemi.mirel.apps.mira.domain.service.MiraContextLayerService contextLayerService;
    private final ChatClient.Builder chatClientBuilder;

    @Override
    public AiResponse chat(AiRequest request) {
        var config = properties.getGithubModels();
        long startTime = System.currentTimeMillis();

        try {
            // 1. Resolve Tavily API Key
            String tavilyApiKey = resolveTavilyApiKey(request);

            // 2. Create OpenAI API (GitHub Models)
            // Constructor signature verified:
            // OpenAiApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String>
            // headers,
            // String completionsPath, String embeddingsPath,
            // RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
            // ResponseErrorHandler responseErrorHandler)
            ApiKey apiKey = new SimpleApiKey(config.getApiKey());
            OpenAiApi openAiApi = new OpenAiApi(
                    config.getBaseUrl(),
                    apiKey,
                    new LinkedMultiValueMap<>(),
                    "/chat/completions",
                    "/embeddings",
                    RestClient.builder(),
                    WebClient.builder(),
                    RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);

            // 3. Create OpenAI Chat Model using Builder
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(config.getModel())
                            .temperature(request.getTemperature() != null ? request.getTemperature()
                                    : config.getTemperature())
                            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
                            .build())
                    .toolCallingManager(ToolCallingManager.builder().build()) // Ensure simpler non-null manager if
                                                                              // needed
                    .build();

            // 4. Prepare ChatClient
            ChatClient.Builder clientBuilder = ChatClient.builder(chatModel)
                    .defaultSystem("You are a helpful assistant.");

            // Register Tavily Tool if key is present
            if (tavilyApiKey != null && !tavilyApiKey.isEmpty()) {
                log.info("Registering TavilySearchTool for tenant={} user={}", request.getTenantId(),
                        request.getUserId());
                clientBuilder.defaultTools(
                        new jp.vemi.mirel.apps.mira.infrastructure.ai.tool.TavilySearchTool(tavilyApiKey));
            }

            ChatClient client = clientBuilder.build();

            // 5. Build Prompt
            List<Message> messages = request.getMessages().stream()
                    .map(this::mapMessage)
                    .collect(Collectors.toList());

            Prompt prompt = new Prompt(messages);

            // 6. Call
            ChatResponse response = client.prompt(prompt)
                    .call()
                    .chatResponse();

            long latency = System.currentTimeMillis() - startTime;

            // 7. Map to AiResponse
            if (response.getResult() == null) {
                return AiResponse.error("EMPTY_RESPONSE", "No result");
            }

            // Using getText() as verified in OpenAiApi/OpenAiChatModel source or assumed
            // correct
            String content = response.getResult().getOutput().getText();

            AiResponse.Metadata metadata = AiResponse.Metadata.builder()
                    .model(config.getModel())
                    .latencyMs(latency)
                    .build();

            return AiResponse.success(content, metadata);

        } catch (Exception e) {
            log.error("[GitHubModels] Request failed: {}", e.getMessage(), e);
            return AiResponse.error("REQUEST_FAILED", e.getMessage());
        }
    }

    private String resolveTavilyApiKey(AiRequest request) {
        String apiKey = null;

        // 1. User Context
        if (request.getUserId() != null) {
            try {
                var context = contextLayerService.buildMergedContext(
                        request.getTenantId(), null, request.getUserId());
                apiKey = context.get("tavilyApiKey");
            } catch (Exception e) {
                log.warn("Failed to resolve User Context for Tavily Key: {}", e.getMessage());
            }
        }

        // 2. Tenant Setting
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = settingService.getString(request.getTenantId(), MiraSettingService.KEY_TAVILY_API_KEY, null);
        }

        // 3. System Setting (Handled by getString null check if tenantId provided, but
        // if getString doesn't fallback to System for specific cases, we check
        // manually)
        // settingService.getString checks Tenant then System if tenantId is provided.
        // If tenantId is null, it checks System.
        // So step 2 actually covers both Tenant and System if getString is implemented
        // correctly.

        return apiKey;
    }

    private Message mapMessage(AiRequest.Message msg) {
        switch (msg.getRole()) {
            case "user":
                return new UserMessage(msg.getContent());
            case "system":
                return new SystemMessage(msg.getContent());
            case "assistant":
                return new AssistantMessage(msg.getContent());
            default:
                return new UserMessage(msg.getContent());
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.getGithubModels().getApiKey() != null;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
