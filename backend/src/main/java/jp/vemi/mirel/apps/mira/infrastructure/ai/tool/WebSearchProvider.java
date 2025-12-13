/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai.tool;

import java.util.List;

/**
 * Web検索プロバイダ・インターフェース.
 * <p>
 * Tavilyなどの検索APIを疎結合で利用するための抽象化レイヤー。
 * </p>
 */
public interface WebSearchProvider {
    
    /**
     * プロバイダ名を取得.
     * @return プロバイダ識別子 (e.g., "tavily", "bing", "google")
     */
    String getName();
    
    /**
     * このプロバイダが利用可能かを判定.
     * @return 利用可能な場合 true
     */
    boolean isAvailable();
    
    /**
     * Web検索を実行.
     * @param query 検索クエリ
     * @param options 検索オプション
     * @return 検索結果
     */
    SearchResult search(String query, SearchOptions options);
    
    /**
     * 検索オプション.
     */
    record SearchOptions(
        int maxResults,
        boolean includeAnswer,
        String searchDepth // "basic" or "advanced"
    ) {
        public static SearchOptions defaults() {
            return new SearchOptions(3, true, "basic");
        }
    }
    
    /**
     * 検索結果.
     */
    record SearchResult(
        boolean success,
        String query,
        String answer,
        List<ResultItem> results,
        String errorMessage
    ) {
        public static SearchResult error(String message) {
            return new SearchResult(false, null, null, List.of(), message);
        }
        
        public static SearchResult success(String query, String answer, List<ResultItem> results) {
            return new SearchResult(true, query, answer, results, null);
        }
    }
    
    /**
     * 検索結果の各項目.
     */
    record ResultItem(
        String title,
        String url,
        String content,
        Double score
    ) {}
}
