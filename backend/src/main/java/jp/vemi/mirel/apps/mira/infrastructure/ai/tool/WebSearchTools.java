/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Web検索ツール (Spring AI 1.1 @Tool方式).
 * <p>
 * Spring AI 1.1のベストプラクティスに従い、{@code @Tool}アノテーションを使用。
 * WebSearchProviderを注入し、疎結合で検索プロバイダを切り替え可能。
 * </p>
 * 
 * <p>
 * 使用例:
 * <pre>
 * WebSearchTools tools = new WebSearchTools(tavilyProvider);
 * // Spring AIがツールとして自動認識
 * </pre>
 * </p>
 * 
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Spring AI Tool Calling</a>
 */
@Slf4j
@RequiredArgsConstructor
public class WebSearchTools {

    private final WebSearchProvider searchProvider;

    /**
     * Web検索を実行.
     * <p>
     * 最新のニュース、天気、事実確認など、リアルタイム情報が必要な場合に使用。
     * </p>
     * 
     * @param query 検索クエリ
     * @return 検索結果のテキスト形式
     */
    @Tool(description = "Search the web for current information, news, facts, or real-time data. Use this when you need up-to-date information that may not be in your training data.")
    public String webSearch(
            @ToolParam(description = "The search query to find information on the web") String query) {
        
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
