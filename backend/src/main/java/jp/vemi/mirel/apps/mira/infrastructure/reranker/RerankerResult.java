/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;

import lombok.Builder;
import lombok.Data;

/**
 * リランキング結果DTO.
 */
@Data
@Builder
public class RerankerResult {

    /** リランク後のドキュメントリスト（スコア順） */
    private List<Document> documents;

    /** リランキングが実行されたかどうか */
    private boolean applied;

    /** 使用されたプロバイダー名 */
    private String providerName;

    /** リランキング処理時間（ミリ秒） */
    private long latencyMs;

    /** エラーメッセージ（フォールバック時） */
    private String errorMessage;

    /**
     * フォールバック結果を生成.
     * <p>
     * リランキングが失敗またはスキップされた場合に使用。
     * 元のドキュメントリストをそのまま返却します。
     * </p>
     *
     * @param originalDocs
     *            元のドキュメントリスト
     * @param topN
     *            返却件数
     * @return フォールバック結果
     */
    public static RerankerResult fallback(List<Document> originalDocs, int topN) {
        return RerankerResult.builder()
                .documents(originalDocs.stream().limit(topN).collect(Collectors.toList()))
                .applied(false)
                .providerName("fallback")
                .latencyMs(0)
                .build();
    }

    /**
     * フォールバック結果を生成（エラー付き）.
     *
     * @param originalDocs
     *            元のドキュメントリスト
     * @param topN
     *            返却件数
     * @param errorMessage
     *            エラーメッセージ
     * @return フォールバック結果
     */
    public static RerankerResult fallbackWithError(List<Document> originalDocs, int topN, String errorMessage) {
        return RerankerResult.builder()
                .documents(originalDocs.stream().limit(topN).collect(Collectors.toList()))
                .applied(false)
                .providerName("fallback")
                .latencyMs(0)
                .errorMessage(errorMessage)
                .build();
    }
}
