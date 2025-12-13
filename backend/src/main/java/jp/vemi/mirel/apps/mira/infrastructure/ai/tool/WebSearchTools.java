/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.DefaultToolMetadata;
import org.springframework.ai.tool.metadata.ToolMetadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Web検索ツール (Spring AI 1.1 ToolCallback実装).
 * <p>
 * Spring AI 1.1のToolCallbackインターフェースを直接実装。
 * WebSearchProviderを注入し、疎結合で検索プロバイダを切り替え可能。
 * </p>
 * 
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Spring AI Tool Calling</a>
 */
@Slf4j
public class WebSearchTools implements ToolCallback {

    public static final String TOOL_NAME = "webSearch";
    public static final String TOOL_DESCRIPTION = "Search the web for current information, news, facts, or real-time data. " +
            "Use this when you need up-to-date information that may not be in your training data.";
    public static final String INPUT_SCHEMA = """
            {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The search query to find information on the web"
                    }
                },
                "required": ["query"]
            }
            """;

    private final WebSearchProvider searchProvider;
    private final ObjectMapper objectMapper;
    private final ToolDefinition toolDefinition;
    private final ToolMetadata toolMetadata;

    public WebSearchTools(WebSearchProvider searchProvider) {
        this.searchProvider = searchProvider;
        this.objectMapper = new ObjectMapper();
        this.toolDefinition = DefaultToolDefinition.builder()
                .name(TOOL_NAME)
                .description(TOOL_DESCRIPTION)
                .inputSchema(INPUT_SCHEMA)
                .build();
        this.toolMetadata = DefaultToolMetadata.builder()
                .returnDirect(false)
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return toolMetadata;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, org.springframework.ai.chat.model.ToolContext toolContext) {
        try {
            // Parse input JSON
            JsonNode inputNode = objectMapper.readTree(toolInput);
            String query = inputNode.has("query") ? inputNode.get("query").asText() : toolInput;
            
            return executeWebSearch(query);
        } catch (Exception e) {
            log.error("Failed to parse tool input: {}", toolInput, e);
            // Fallback: treat the entire input as query
            return executeWebSearch(toolInput);
        }
    }

    /**
     * Web検索を実行.
     */
    private String executeWebSearch(String query) {
        log.info("WebSearchTools.webSearch called: query={}, provider={}", query, searchProvider.getName());
        
        if (!searchProvider.isAvailable()) {
            log.warn("Web search provider is not available: {}", searchProvider.getName());
            return "Web search is currently unavailable. Please try again later.";
        }
        
        WebSearchProvider.SearchOptions options = WebSearchProvider.SearchOptions.defaults();
        WebSearchProvider.SearchResult result = searchProvider.search(query, options);
        
        if (!result.success()) {
            log.error("Web search failed: {}", result.errorMessage());
            return "Search failed: " + result.errorMessage();
        }
        
        return formatSearchResult(result);
    }

    /**
     * 検索結果をLLMが理解しやすいテキスト形式にフォーマット.
     */
    private String formatSearchResult(WebSearchProvider.SearchResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Search Results for: ").append(result.query()).append("\n\n");
        
        // Answerがある場合は先頭に表示
        if (result.answer() != null && !result.answer().isEmpty()) {
            String answer = truncate(result.answer(), 300);
            sb.append("Summary: ").append(answer).append("\n\n");
        }
        
        // 各結果
        if (result.results() != null && !result.results().isEmpty()) {
            sb.append("Sources:\n");
            for (int i = 0; i < result.results().size(); i++) {
                WebSearchProvider.ResultItem item = result.results().get(i);
                sb.append(i + 1).append(". ").append(item.title()).append("\n");
                sb.append("   URL: ").append(item.url()).append("\n");
                if (item.content() != null && !item.content().isEmpty()) {
                    String content = truncate(item.content(), 200);
                    // 改行を除去して読みやすく
                    content = content.replaceAll("[\\n\\r\\t]+", " ").trim();
                    sb.append("   ").append(content).append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("No results found.\n");
        }
        
        return sb.toString();
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
