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
    private final ChatClient.Builder chatClientBuilder;

    @Override
    public AiResponse chat(AiRequest request) {
        var config = properties.getGithubModels();
        long startTime = System.currentTimeMillis();

        try {
            // 2. Create OpenAI API (GitHub Models)
            ApiKey apiKey = new SimpleApiKey(config.getApiKey());

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
            // IMPORTANT: disable auto tool execution by NOT setting a ToolCallingManager or
            // setting a no-op one if possible.
            // By default, if we don't pass toolCallingManager, Spring AI might default to
            // one if toolCallbacks are present in prompt.
            // However, we want to Pass Tool Definitions but NOT Execute them.
            // To do this, we should pass tools via Options, but not via
            // prompt.toolCallbacks?
            // Spring AI 1.x: chatModel.call(prompt) -> if prompt has toolCallbacks, it
            // formats them into options AND prepares execution.
            // Since we want manual control, we'll strip the ToolCallback implementation and
            // only pass definitions if possible,
            // OR we rely on `FunctionCallback` interface but we must stop `call()` from
            // looping.
            // The cleanest way in Spring AI (without internal hacks) is tricky.
            // Strategy: We will configure the ChatModel WITHOUT ToolCallingManager, and
            // manually set Function Definitions in Options.

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(config.getModel())
                            .temperature(request.getTemperature() != null ? request.getTemperature()
                                    : config.getTemperature())
                            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
                            .build())
                    // .toolCallingManager(ToolCallingManager.builder().build()) // ABOLISHED:
                    // Disable auto execution
                    .build();

            // 4. Prepare ChatClient
            ChatClient.Builder clientBuilder = ChatClient.builder(chatModel)
                    .defaultSystem("You are a helpful assistant.");

            ChatClient client = clientBuilder.build();

            // 5. Build Prompt
            List<Message> messages = request.getMessages().stream()
                    .map(this::mapMessage)
                    .collect(Collectors.toList());

            // Prepare Options with Tool Definitions
            OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder();

            // Register Tools from AiRequest
            if (request.getToolCallbacks() != null && !request.getToolCallbacks().isEmpty()) {
                log.info("Registering {} Tools for manual execution handling.", request.getToolCallbacks().size());
                // Manually mapping ToolCallback to Function Callback (runtime) is hard if we
                // don't let ChatModel handle it.
                // But we can extract ToolDefinition and pass it via options 'functions' or
                // 'tools'.
                // Ideally, we want to say "Here are the tools", but "Don't run them".
                // If we pass them as callbacks to the prompt, ChatModel will run them.
                // NOTE: For now, we utilize the property that if ToolCallingManager is not set,
                // OpenAiChatModel MIGHT still execute if it uses internal logic?
                // Actually OpenAiChatModel uses `this.toolCallingManager.execute(...)`.
                // If we don't provide it, is it null?
                // The builder defaults it? No, checking source... usually defaults to null or
                // disabled if not provided.
                // IF it loops, we will need another strategy.
                // Assuming it DOES NOT loop if manager is missing.

                // We must pass the callbacks to the RequestSpec so they are formatted into the
                // API request.
                // But wait, requestSpec.tools() adds them to the Prompt.
            }

            Prompt prompt = new Prompt(messages);
            ChatClientRequestSpec requestSpec = client.prompt(prompt);

            // Add tools to request spec (so they appear in API request)
            if (request.getToolCallbacks() != null) {
                for (org.springframework.ai.tool.ToolCallback tc : request.getToolCallbacks()) {
                    requestSpec.tools(tc);
                }
            }

            // Execute
            ChatResponse response = requestSpec
                    .call()
                    .chatResponse();

            long latency = System.currentTimeMillis() - startTime;

            // 7. Map to AiResponse
            if (response.getResult() == null) {
                return AiResponse.error("EMPTY_RESPONSE", "No result");
            }

            String content = response.getResult().getOutput().getText();
            AssistantMessage outputMsg = response.getResult().getOutput();

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
        // ... (Old logic moved to ChatService or unused, but keep for now if needed,
        // though removed from chat() flow)
        // Since we now rely on AiRequest.toolCallbacks, we don't strictly need this
        // here.
        // But keeping the method to avoid breaking other calls (not used in new chat
        // method).
        return null;
    }

    @Override
    public reactor.core.publisher.Flux<AiResponse> stream(AiRequest request) {
        var config = properties.getGithubModels();

        try {
            // 2. Create OpenAI API (GitHub Models)
            ApiKey apiKey = new SimpleApiKey(config.getApiKey());

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
            ChatClientRequestSpec requestSpec = client.prompt(prompt);

            // Add tools to request spec (so they appear in API request)
            if (request.getToolCallbacks() != null) {
                for (org.springframework.ai.tool.ToolCallback tc : request.getToolCallbacks()) {
                    requestSpec.tools(tc);
                }
            }

            // Execute Stream
            return requestSpec.stream()
                    .chatResponse()
                    .map(this::mapStreamResponse)
                    .onErrorResume(e -> {
                        log.error("[GitHubModels] Stream Request failed", e);
                        return reactor.core.publisher.Flux.just(
                                AiResponse.error("STREAM_ERROR",
                                        "ストリーム処理中にエラーが発生しました: " + e.getMessage()));
                    });

        } catch (Exception e) {
            log.error("[GitHubModels] Request setup failed", e);
            return reactor.core.publisher.Flux.just(
                    AiResponse.error("SETUP_FAILED",
                            "リクエスト設定中にエラーが発生しました: " + e.getMessage()));
        }
    }

    private AiResponse mapStreamResponse(ChatResponse response) {
        String content = "";
        if (response.getResult() != null && response.getResult().getOutput().getText() != null) {
            content = response.getResult().getOutput().getText();
        }

        // Basic metadata mapping (Note: streaming chunks often have minimal metadata)
        AiResponse.Metadata metadata = AiResponse.Metadata.builder()
                .model(properties.getGithubModels().getModel())
                .build();

        return AiResponse.builder()
                .content(content)
                .metadata(metadata)
                .build();
    }

    private Message mapMessage(AiRequest.Message msg) {
        switch (msg.getRole()) {
            case "user":
                return new UserMessage(msg.getContent());
            case "system":
                return new SystemMessage(msg.getContent());
            case "assistant":
                if (msg.getToolCalls() != null && !msg.getToolCalls().isEmpty()) {
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
                return ToolResponseMessage.builder()
                        .responses(List.of(
                                new ToolResponseMessage.ToolResponse(
                                        msg.getToolCallId() != null ? msg.getToolCallId() : "unknown_id",
                                        msg.getToolName() != null ? msg.getToolName() : "unknown_name",
                                        msg.getContent())))
                        .build();
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
