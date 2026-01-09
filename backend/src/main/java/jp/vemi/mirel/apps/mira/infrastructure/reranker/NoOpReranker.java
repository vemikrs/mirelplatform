/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * NoOp（何もしない）リランカー実装.
 * <p>
 * リランキングが無効化されている場合、または他のリランカーが利用不可の場合に使用される
 * フォールバック実装です。入力されたドキュメントをそのまま返却します。
 * </p>
 */
@Slf4j
@Component
public class NoOpReranker implements Reranker {

    @Override
    public RerankerResult rerank(String query, List<Document> documents, int topN) {
        log.debug("NoOpReranker: returning {} documents without reranking",
                Math.min(documents.size(), topN));
        return RerankerResult.fallback(documents, topN);
    }

    @Override
    public boolean isAvailable() {
        return true; // 常に利用可能
    }

    @Override
    public String getProviderName() {
        return "noop";
    }
}
