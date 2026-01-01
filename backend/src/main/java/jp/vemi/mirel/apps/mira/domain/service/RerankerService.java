/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.infrastructure.reranker.NoOpReranker;
import jp.vemi.mirel.apps.mira.infrastructure.reranker.Reranker;
import jp.vemi.mirel.apps.mira.infrastructure.reranker.RerankerResult;
import lombok.extern.slf4j.Slf4j;

/**
 * リランキングオーケストレーションサービス.
 * <p>
 * 条件付きリランキングのオーケストレーションを担当します。
 * テナント設定に基づいてリランキングの実行可否を判断し、
 * 適切なリランカー実装を選択して実行します。
 * </p>
 */
@Service
@Slf4j
public class RerankerService {

    private final MiraSettingService settingService;
    private final Map<String, Reranker> rerankerMap;
    private final NoOpReranker noOpReranker;

    public RerankerService(
            MiraSettingService settingService,
            List<Reranker> rerankers,
            NoOpReranker noOpReranker) {
        this.settingService = settingService;
        this.noOpReranker = noOpReranker;

        // プロバイダー名でリランカーをマップ
        this.rerankerMap = new java.util.HashMap<>();
        for (Reranker reranker : rerankers) {
            rerankerMap.put(reranker.getProviderName(), reranker);
        }
        log.info("RerankerService initialized with providers: {}", rerankerMap.keySet());
    }

    /**
     * リランキングを実行すべきかどうかを判定.
     *
     * @param tenantId
     *            テナントID
     * @param candidateCount
     *            候補ドキュメント数
     * @return リランキングを実行すべき場合true
     */
    public boolean shouldRerank(String tenantId, int candidateCount) {
        // リランカーが無効の場合
        if (!settingService.isRerankerEnabled(tenantId)) {
            return false;
        }

        // 候補数が閾値未満の場合
        int minCandidates = settingService.getRerankerMinCandidates(tenantId);
        if (candidateCount < minCandidates) {
            log.debug("Skipping rerank: candidate count {} < minCandidates {}",
                    candidateCount, minCandidates);
            return false;
        }

        return true;
    }

    /**
     * リランキングを実行.
     *
     * @param query
     *            検索クエリ
     * @param documents
     *            リランク対象ドキュメント
     * @param tenantId
     *            テナントID
     * @return リランキング結果
     */
    public List<Document> rerank(String query, List<Document> documents, String tenantId) {
        int topN = settingService.getRerankerTopN(tenantId);
        return rerankWithResult(query, documents, tenantId).getDocuments();
    }

    /**
     * リランキングを実行し、詳細な結果を返却.
     *
     * @param query
     *            検索クエリ
     * @param documents
     *            リランク対象ドキュメント
     * @param tenantId
     *            テナントID
     * @return リランキング結果（詳細情報付き）
     */
    public RerankerResult rerankWithResult(String query, List<Document> documents, String tenantId) {
        String provider = settingService.getRerankerProvider(tenantId);
        int topN = settingService.getRerankerTopN(tenantId);

        Reranker reranker = rerankerMap.getOrDefault(provider, noOpReranker);

        if (!reranker.isAvailable()) {
            log.warn("Reranker '{}' is not available, falling back to NoOp", provider);
            reranker = noOpReranker;
        }

        log.debug("Reranking {} documents with provider '{}', topN={}",
                documents.size(), reranker.getProviderName(), topN);

        return reranker.rerank(query, documents, topN);
    }

    /**
     * オーバーライド設定でリランキングを実行.
     * <p>
     * Playground等で設定をオーバーライドする場合に使用します。
     * </p>
     *
     * @param query
     *            検索クエリ
     * @param documents
     *            リランク対象ドキュメント
     * @param tenantId
     *            テナントID
     * @param enabledOverride
     *            有効化オーバーライド（nullの場合はテナント設定を使用）
     * @param topNOverride
     *            topNオーバーライド（nullの場合はテナント設定を使用）
     * @return リランキング結果
     */
    public RerankerResult rerankWithOverride(
            String query,
            List<Document> documents,
            String tenantId,
            Boolean enabledOverride,
            Integer topNOverride) {

        // 有効化判定
        boolean enabled = enabledOverride != null ? enabledOverride : settingService.isRerankerEnabled(tenantId);
        if (!enabled) {
            int topN = topNOverride != null ? topNOverride : settingService.getRerankerTopN(tenantId);
            return RerankerResult.fallback(documents, topN);
        }

        String provider = settingService.getRerankerProvider(tenantId);
        int topN = topNOverride != null ? topNOverride : settingService.getRerankerTopN(tenantId);

        Reranker reranker = rerankerMap.getOrDefault(provider, noOpReranker);

        if (!reranker.isAvailable()) {
            log.warn("Reranker '{}' is not available, falling back to NoOp", provider);
            reranker = noOpReranker;
        }

        log.info("Reranking {} documents with provider '{}', topN={} (override: enabled={}, topN={})",
                documents.size(), reranker.getProviderName(), topN, enabledOverride, topNOverride);

        return reranker.rerank(query, documents, topN);
    }
}
