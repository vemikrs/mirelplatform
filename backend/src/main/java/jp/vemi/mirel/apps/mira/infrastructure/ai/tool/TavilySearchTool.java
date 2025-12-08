/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai.tool;

import java.util.Map;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Tavily Search Tool.
 * <p>
 * Manually implemented ToolCallback for Tavily Search API.
 * </p>
 */
@Slf4j
public class TavilySearchTool implements ToolCallback {

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TavilySearchTool(String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.tavily.com")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name(this.getName())
                .description(this.getDescription())
                .inputSchema(this.getInputTypeSchema())
                .build();
    }

    public String getName() {
        return "tavily_search";
    }

    public String getDescription() {
        return "Search the web for current information and facts using Tavily API. Use this when you need up-to-date information.";
    }

    public String getInputTypeSchema() {
        // Simple JSON schema for the input
        return """
                {
                    "type": "object",
                    "properties": {
                        "query": {
                            "type": "string",
                            "description": "The search query"
                        }
                    },
                    "required": ["query"]
                }
                """;
    }

    @Override
    public String call(String functionInput) {
        try {
            // Parse input
            Request request = objectMapper.readValue(functionInput, Request.class);
            log.info("Executing Tavily Search: query={}", request.query());

            // Execute API call
            TavilyResponse response = restClient.post()
                    .uri("/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "api_key", apiKey,
                            "query", request.query(),
                            "search_depth", "basic",
                            "include_answer", true,
                            "max_results", 5))
                    .retrieve()
                    .body(TavilyResponse.class);

            if (response == null) {
                return "{\"error\": \"No response from Tavily API\"}";
            }

            // Convert to JSON string
            return objectMapper.writeValueAsString(response);

        } catch (Exception e) {
            log.error("Tavily Search failed", e);
            return "{\"error\": \"Search failed: " + e.getMessage() + "\"}";
        }
    }

    // Input DTO
    record Request(@JsonProperty("query") String query) {
    }

    // Output DTO (Simplified)
    record TavilyResponse(
            @JsonProperty("query") String query,
            @JsonProperty("answer") String answer,
            @JsonProperty("results") java.util.List<Result> results) {
        record Result(
                @JsonProperty("title") String title,
                @JsonProperty("url") String url,
                @JsonProperty("content") String content,
                @JsonProperty("score") Double score) {
        }
    }
}
