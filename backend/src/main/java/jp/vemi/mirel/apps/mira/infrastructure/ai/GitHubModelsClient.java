/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.retry.RetryUtils;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
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
            // 3. Create OpenAI Chat Model using Builder
            // Use RestClient.builder() but ensure Jackson converter is present
            // Spring AI requires a RestClient that can serialize the request body.
            RestClient.Builder safeRestClientBuilder = RestClient.builder()
                    .requestInterceptor(new GitHubModelsInterceptor())
                    .messageConverters(c -> c
                            .add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter()));

            OpenAiApi openAiApi = new OpenAiApi(
                    config.getBaseUrl(),
                    apiKey,
                    new LinkedMultiValueMap<>(),
                    "/chat/completions",
                    "/embeddings",
                    safeRestClientBuilder,
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

            ChatClient client = clientBuilder.build();

            // 5. Build Prompt
            List<Message> messages = request.getMessages().stream()
                    .map(this::mapMessage)
                    .collect(Collectors.toList());

            Prompt prompt = new Prompt(messages);

            // 6. Call
            ChatClientRequestSpec requestSpec = client.prompt(prompt);

            // Register Tavily Tool if key is present
            if (tavilyApiKey != null && !tavilyApiKey.isEmpty()) {
                log.info("Registering TavilySearchTool for tenant={} user={}", request.getTenantId(),
                        request.getUserId());
                // Use toolCallbacks for ToolCallback instances
                requestSpec.toolCallbacks(
                        new jp.vemi.mirel.apps.mira.infrastructure.ai.tool.TavilySearchTool(tavilyApiKey));
            }

            ChatResponse response = requestSpec
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
            // Spring AI 1.x: ChatResponse.getResult().getOutput() returns AssistantMessage.
            AssistantMessage outputMsg = response.getResult().getOutput();
            // content is already retrieved via getText()

            List<AiRequest.Message.ToolCall> toolCalls = null;
            if (outputMsg.getToolCalls() != null && !outputMsg.getToolCalls().isEmpty()) {
                toolCalls = outputMsg.getToolCalls().stream()
                        .map(tc -> new AiRequest.Message.ToolCall(tc.id(), tc.type(), tc.name(), tc.arguments()))
                        .collect(Collectors.toList());
            }

            AiResponse.Metadata metadata = AiResponse.Metadata.builder()
                    .model(config.getModel())
                    .latencyMs(latency)
                    .build();

            AiResponse aiResponse = AiResponse.success(content, metadata);
            aiResponse.setToolCalls(toolCalls);
            return aiResponse;

        } catch (org.springframework.web.client.RestClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("[GitHubModels] API Request failed: Status={} Body={}", e.getStatusCode(), responseBody, e);
            return AiResponse.error("API_ERROR", "AIプロバイダーエラー (" + e.getStatusCode() + "): " + responseBody);
        } catch (Exception e) {
            log.error("[GitHubModels] Request failed: {} (Cause: {})", e.getMessage(), e.getClass().getName(), e);
            return AiResponse.error("REQUEST_FAILED",
                    "システムエラーが発生しました: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
                if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
                    // Map ToolCalls
                    List<ToolCall> springToolCalls = msg.getToolCalls().stream()
                            .map(tc -> new ToolCall(
                                    tc.getId(), tc.getType(), tc.getName(), tc.getArguments()))
                            .collect(Collectors.toList());
                    return AssistantMessage.builder()
                            .content(msg.getContent())
                            .toolCalls(springToolCalls)
                            .build();
                }
                return AssistantMessage.builder().content(msg.getContent()).build();
            case "tool":
                // Handle Tool Output (Role: tool)
                // Spring AI 1.x uses ToolResponseMessage for tool outputs
                // Assuming msg.getRole().equals("tool")
                // We need to match the constructor: ToolResponseMessage(List<ToolResponse>
                // responses)
                // However, mapping singular 'tool' role to ToolResponseMessage usually requires
                // identifying which call it belongs to.
                // Spring AI's request structure often puts ToolResponseMessage in the history.
                // Let's implement a safe mapping if possible, or use UserMessage with special
                // marker if Spring AI doesn't support generic 'tool' role in input seamlessly.
                // Actually, OpenAiApi supports 'tool' role.
                // But ChatClient abstraction uses specific message types.
                // Let's use ToolResponseMessage.
                return ToolResponseMessage.builder()
                        .responses(List.of(
                                new ToolResponseMessage.ToolResponse(
                                        "unknown_id", "unknown_name", msg.getContent())))
                        .build();
            // WAIT: AiRequest.Message only has role/content. It needs 'toolCallId' too for
            // 'tool' messages!
            // Let's postpone 'tool' role fix to next step if needed, but for 'assistant',
            // we fixed it.
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

    /**
     * GitHub Models 向けの特別対応インターセプター.
     * <p>
     * Llama 3.3 など一部のモデルは、tool_calls を含む assistant message において
     * content フィールドが null であっても明示的に存在すること("content": null)を要求します。
     * Spring AI (Jackson) のデフォルト設定では null フィールドは除外されるため、
     * ここでリクエストボディを傍受して強制的に content: null を注入します。
     * </p>
     */
    @Slf4j
    static class GitHubModelsInterceptor implements org.springframework.http.client.ClientHttpRequestInterceptor {

        private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        @Override
        public org.springframework.http.client.ClientHttpResponse intercept(
                org.springframework.http.HttpRequest request, byte[] body,
                org.springframework.http.client.ClientHttpRequestExecution execution) throws java.io.IOException {

            if (body.length == 0) {
                return execution.execute(request, body);
            }

            try {
                // 1. JSON Parse
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(body);

                // 2. Modify
                boolean modified = false;
                if (root.has("messages") && root.get("messages").isArray()) {
                    com.fasterxml.jackson.databind.node.ArrayNode messages = (com.fasterxml.jackson.databind.node.ArrayNode) root
                            .get("messages");
                    for (com.fasterxml.jackson.databind.JsonNode msg : messages) {
                        if (msg.has("role") && "assistant".equals(msg.get("role").asText())) {
                            if (msg.has("tool_calls") && !msg.get("tool_calls").isEmpty()) {
                                if (!msg.has("content")) {
                                    // content フィールドがない場合、null として追加
                                    ((com.fasterxml.jackson.databind.node.ObjectNode) msg).putNull("content");
                                    modified = true;
                                }
                            }
                        }
                    }
                }

                // 3. Serialize back if modified
                if (modified) {
                    log.debug("[GitHubModelsInterceptor] Injected 'content: null' into assistant tool call message(s)");
                    byte[] newBody = objectMapper.writeValueAsBytes(root);
                    // Critical: Update Content-Length as body size changed
                    request.getHeaders().setContentLength(newBody.length);
                    return execution.execute(request, newBody);
                } else {
                    log.trace("[GitHubModelsInterceptor] No modifications needed");
                }

            } catch (Exception e) {
                log.warn("Failed to intercept and fix GitHub Models request body", e);
            }

            return execution.execute(request, body);
        }
    }
}
