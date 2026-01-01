/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import java.util.List;

import org.springframework.ai.document.Document;

/**
 * リランカー抽象インターフェース.
 * <p>
 * 検索結果をクエリとの関連性で再順位付けするためのインターフェース。
 * 疎結合設計により、複数のプロバイダー（Vertex AI, Cohere等）を切り替え可能です。
 * </p>
 */
public interface Reranker {

    /**
     * ドキュメントをクエリとの関連性で再順位付け.
     *
     * @param query
     *            検索クエリ
     * @param documents
     *            リランク対象ドキュメント
     * @param topN
     *            返却件数
     * @return リランキング結果
     */
    RerankerResult rerank(String query, List<Document> documents, int topN);

    /**
     * このリランカーが利用可能かどうか.
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * プロバイダー名を取得.
     *
     * @return プロバイダー名（例: "vertex-ai", "cohere", "noop"）
     */
    String getProviderName();
}
