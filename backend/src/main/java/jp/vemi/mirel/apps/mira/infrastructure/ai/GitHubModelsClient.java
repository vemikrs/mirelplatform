/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub Models API クライアント.
 * 
 * <p>GitHub Models (Azure AI Model Inference API) を使用して
 * Llama 3.3 等のモデルにアクセスします。</p>
 * 
 * <h3>認証</h3>
 * <p>GitHub Personal Access Token (PAT) または {@code gh auth token} で
 * 取得したトークンを使用します。</p>
 * 
 * <h3>API エンドポイント</h3>
 * <ul>
 *   <li>Base URL: https://models.inference.ai.azure.com</li>
 *   <li>Chat Completions: POST /chat/completions</li>
 * </ul>
 * 
 * @see <a href="https://github.com/marketplace/models">GitHub Models</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubModelsClient implements AiProviderClient {

    private static final String PROVIDER_NAME = "github-models";

    private final MiraAiProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public AiResponse chat(AiRequest request) {
        var config = properties.getGithubModels();
        long startTime = System.currentTimeMillis();

        try {
            // リクエストボディを構築
            ChatCompletionRequest chatRequest = buildChatRequest(request, config);
            String requestBody = objectMapper.writeValueAsString(chatRequest);

            if (log.isDebugEnabled()) {
                log.debug("[GitHubModels] Request: model={}, messages={}",
                        chatRequest.getModel(), chatRequest.getMessages().size());
            }

            // HTTP リクエストを構築
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // リクエスト送信
            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            long latency = System.currentTimeMillis() - startTime;

            // レスポンス処理
            if (response.statusCode() == 200) {
                return parseSuccessResponse(response.body(), latency);
            } else {
                return parseErrorResponse(response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("[GitHubModels] Request failed: {}", e.getMessage(), e);
            return AiResponse.error("REQUEST_FAILED", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        var config = properties.getGithubModels();
        return config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    // ========================================
    // Private Methods
    // ========================================

    private ChatCompletionRequest buildChatRequest(AiRequest request,
            MiraAiProperties.GitHubModelsConfig config) {

        List<ChatMessage> messages = request.getMessages().stream()
                .map(m -> new ChatMessage(m.getRole(), m.getContent()))
                .collect(Collectors.toList());

        return ChatCompletionRequest.builder()
                .model(config.getModel())
                .messages(messages)
                .temperature(request.getTemperature() != null
                        ? request.getTemperature()
                        : config.getTemperature())
                .maxTokens(request.getMaxTokens() != null
                        ? request.getMaxTokens()
                        : config.getMaxTokens())
                .build();
    }

    private AiResponse parseSuccessResponse(String body, long latency) {
        try {
            ChatCompletionResponse response = objectMapper.readValue(body,
                    ChatCompletionResponse.class);

            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                return AiResponse.error("EMPTY_RESPONSE", "No choices in response");
            }

            ChatCompletionResponse.Choice choice = response.getChoices().get(0);
            String content = choice.getMessage() != null
                    ? choice.getMessage().getContent()
                    : "";

            AiResponse.Metadata metadata = AiResponse.Metadata.builder()
                    .model(response.getModel())
                    .finishReason(choice.getFinishReason())
                    .promptTokens(response.getUsage() != null
                            ? response.getUsage().getPromptTokens()
                            : null)
                    .completionTokens(response.getUsage() != null
                            ? response.getUsage().getCompletionTokens()
                            : null)
                    .totalTokens(response.getUsage() != null
                            ? response.getUsage().getTotalTokens()
                            : null)
                    .latencyMs(latency)
                    .build();

            if (log.isDebugEnabled()) {
                log.debug("[GitHubModels] Response: model={}, tokens={}, latency={}ms",
                        response.getModel(),
                        metadata.getTotalTokens(),
                        latency);
            }

            return AiResponse.success(content, metadata);

        } catch (Exception e) {
            log.error("[GitHubModels] Failed to parse response: {}", e.getMessage());
            return AiResponse.error("PARSE_ERROR", "Failed to parse response: " + e.getMessage());
        }
    }

    private AiResponse parseErrorResponse(int statusCode, String body) {
        String errorCode = switch (statusCode) {
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 429 -> "RATE_LIMIT_EXCEEDED";
            case 500, 502, 503 -> "SERVICE_UNAVAILABLE";
            default -> "API_ERROR";
        };

        String errorMessage = "HTTP " + statusCode;
        try {
            Map<?, ?> errorBody = objectMapper.readValue(body, Map.class);
            if (errorBody.containsKey("error")) {
                Object error = errorBody.get("error");
                if (error instanceof Map<?, ?> errorMap) {
                    Object msgObj = errorMap.get("message");
                    if (msgObj instanceof String msg) {
                        errorMessage = msg;
                    }
                } else if (error instanceof String errorStr) {
                    errorMessage = errorStr;
                }
            }
        } catch (Exception ignored) {
            // パースできなくてもエラーコードは返す
        }

        log.warn("[GitHubModels] API error: status={}, message={}", statusCode, errorMessage);
        return AiResponse.error(errorCode, errorMessage);
    }

    // ========================================
    // Request/Response DTOs
    // ========================================

    @Data
    @lombok.Builder
    private static class ChatCompletionRequest {
        private String model;
        private List<ChatMessage> messages;
        private Double temperature;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
    }

    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class ChatMessage {
        private String role;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ChatCompletionResponse {
        private String id;
        private String model;
        private List<Choice> choices;
        private Usage usage;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Choice {
            private int index;
            private ChatMessage message;
            @JsonProperty("finish_reason")
            private String finishReason;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Usage {
            @JsonProperty("prompt_tokens")
            private Integer promptTokens;
            @JsonProperty("completion_tokens")
            private Integer completionTokens;
            @JsonProperty("total_tokens")
            private Integer totalTokens;
        }
    }
}
