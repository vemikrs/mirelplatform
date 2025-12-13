/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai.tool;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * Tavily Search API を使用したWeb検索プロバイダ.
 * <p>
 * WebSearchProviderインターフェースを実装し、Tavily APIを疎結合で提供。
 * APIキーはリクエスト時に外部から注入される設計。
 * </p>
 */
@Slf4j
public class TavilySearchProvider implements WebSearchProvider {

    private static final String API_BASE_URL = "https://api.tavily.com";

    private final RestClient restClient;
    private final String apiKey;

    public TavilySearchProvider(String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(API_BASE_URL)
                .build();
    }

    @Override
    public String getName() {
        return "tavily";
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public SearchResult search(String query, SearchOptions options) {
        if (!isAvailable()) {
            return SearchResult.error("Tavily API key is not configured");
        }

        try {
            log.info("Executing Tavily Search: query={}, maxResults={}", query, options.maxResults());

            TavilyResponse response = restClient.post()
                    .uri("/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "api_key", apiKey,
                            "query", query,
                            "search_depth", options.searchDepth(),
                            "include_answer", options.includeAnswer(),
                            "max_results", options.maxResults()))
                    .retrieve()
                    .body(TavilyResponse.class);

            if (response == null) {
                return SearchResult.error("No response from Tavily API");
            }

            List<ResultItem> items = response.results() != null
                    ? response.results().stream()
                            .map(r -> new ResultItem(r.title(), r.url(), r.content(), r.score()))
                            .toList()
                    : List.of();

            return SearchResult.success(response.query(), response.answer(), items);

        } catch (Exception e) {
            log.error("Tavily Search failed: query={}", query, e);
            return SearchResult.error("Search failed: " + e.getMessage());
        }
    }

    // Tavily API Response DTOs
    record TavilyResponse(
            @JsonProperty("query") String query,
            @JsonProperty("answer") String answer,
            @JsonProperty("results") List<TavilyResult> results) {
    }

    record TavilyResult(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("content") String content,
            @JsonProperty("score") Double score) {
    }
}
