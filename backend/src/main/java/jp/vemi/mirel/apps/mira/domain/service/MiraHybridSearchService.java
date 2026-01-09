/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hybrid Search Service combining Vector Search and Keyword Search using RRF.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraHybridSearchService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RerankerService rerankerService;
    private final MiraSettingService settingService;
    private final jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties aiProperties;

    private static final int RRF_K = 60;

    /**
     * Performs a hybrid search.
     *
     * @param query
     *            Search query
     * @param vectorRequest
     *            Vector search request
     * @param scope
     *            Scope filter
     * @param tenantId
     *            Tenant ID
     * @param userId
     *            User ID
     * @return List of documents sorted by RRF score
     */
    public List<Document> search(String query, SearchRequest vectorRequest, String scope, String tenantId,
            String userId) {
        // 共通の検索パイプラインを実行
        HybridSearchResult result = executeSearchPipeline(query, vectorRequest, scope, tenantId, userId);

        // リランキング（条件付き）
        List<Document> finalResults = result.rrfResults();
        if (rerankerService.shouldRerank(tenantId, finalResults.size())) {
            finalResults = rerankerService.rerank(query, finalResults, tenantId);
        }

        // 最終カットオフ
        int finalTopK = settingService.getRerankerTopN(tenantId);
        return finalResults.stream().limit(finalTopK).collect(Collectors.toList());
    }

    /**
     * Performs a hybrid search with debug information.
     */
    public HybridSearchResult searchDebug(String query, SearchRequest vectorRequest, String scope, String tenantId,
            String userId) {
        return executeSearchPipeline(query, vectorRequest, scope, tenantId, userId);
    }

    /**
     * 共通の検索パイプラインを実行.
     * <p>
     * Vector検索、Keyword検索、フィルタリング、RRF計算を行います。
     * searchとsearchDebugの両方からこのメソッドを呼び出すことで
     * ロジックの重複を排除します。
     * </p>
     */
    private HybridSearchResult executeSearchPipeline(String query, SearchRequest vectorRequest,
            String scope, String tenantId, String userId) {

        // 1. Vector Search
        List<Document> vectorResults = vectorStore.similaritySearch(vectorRequest);
        log.info("Hybrid/Vector results: {}", vectorResults.size());

        // Process Vector Results (Score prep, Map creation)
        vectorResults = prepareVectorDocs(vectorResults);

        // 2. Keyword Search (Native SQL)
        int keywordLimit = Math.max(vectorRequest.getTopK(), 20);
        List<Document> keywordResults = performKeywordSearch(query, keywordLimit);

        // Sort by Term Frequency DESC before filtering/ranking
        keywordResults.sort(Comparator
                .comparingLong((Document d) -> ((Number) d.getMetadata().getOrDefault("termFrequency", 0L)).longValue())
                .reversed());

        // Apply scope filtering in memory for keyword results
        keywordResults = filterInMemory(keywordResults, scope, tenantId, userId);
        log.info("Hybrid/Keyword results (after filter): {}", keywordResults.size());

        // 3. Reciprocal Rank Fusion (候補を多めに取得)
        int rrfTopK = Math.max(vectorRequest.getTopK(), 30);
        List<Document> rrfResults = applyRRF(vectorResults, keywordResults, rrfTopK);

        return new HybridSearchResult(vectorResults, keywordResults, rrfResults);
    }

    private List<Document> performKeywordSearch(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Split query by whitespace (full-width or half-width)
        String[] terms = query.split("[\\s　]+");
        if (terms.length == 0) {
            return Collections.emptyList();
        }

        // Build LIKE clauses for each term
        StringBuilder sqlBuilder = new StringBuilder();
        String tableName = aiProperties.getVectorStore().getTableName();
        sqlBuilder.append("SELECT id, content, metadata FROM ").append(tableName).append(" WHERE ");

        List<String> likeClauses = new ArrayList<>();
        List<String> params = new ArrayList<>();

        for (String term : terms) {
            if (!term.isBlank()) {
                likeClauses.add("content ILIKE ?");
                params.add("%" + term.trim() + "%");
            }
        }

        if (likeClauses.isEmpty()) {
            return Collections.emptyList();
        }

        sqlBuilder.append(likeClauses.stream().collect(Collectors.joining(" OR ")));

        // Add scoring (approximate term frequency)
        sqlBuilder.append(" ORDER BY (");
        List<String> scoreParts = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            scoreParts.add("CASE WHEN content ILIKE ? THEN 1 ELSE 0 END");
        }
        sqlBuilder.append(scoreParts.stream().collect(Collectors.joining(" + ")));
        sqlBuilder.append(") DESC");
        sqlBuilder.append(" LIMIT ?");

        // Duplicate params for ORDER BY clause
        List<Object> allParams = new ArrayList<>();
        allParams.addAll(params);
        allParams.addAll(params);
        allParams.add(limit);

        try {
            return jdbcTemplate.query(
                    sqlBuilder.toString(),
                    allParams.toArray(),
                    (rs, rowNum) -> {
                        String id = rs.getString("id");
                        String content = rs.getString("content");
                        String metadataJson = rs.getString("metadata");
                        Map<String, Object> metadata = parseMetadata(metadataJson);
                        // Term frequency is derived from matching terms
                        long matchCount = params.stream()
                                .filter(term -> content.toLowerCase().contains(term.replace("%", "").toLowerCase()))
                                .count();
                        metadata.put("termFrequency", matchCount);
                        return new Document(id, content, metadata);
                    });
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseMetadata(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return new HashMap<>();
        }
    }

    private List<Document> filterInMemory(List<Document> docs, String scope, String tenantId, String userId) {
        return docs.stream().filter(doc -> {
            Map<String, Object> meta = doc.getMetadata();
            String docScope = (String) meta.getOrDefault("scope", "");

            // SYSTEM scope: always visible
            // USER scope: check userid
            // TENANT scope: check tenantid
            if ("SYSTEM".equals(docScope)) {
                return true;
            }
            if ("USER".equals(docScope)) {
                String docUserId = (String) meta.getOrDefault("userid", "");
                return docUserId.equals(userId);
            }
            if ("TENANT".equals(docScope)) {
                String docTenantId = (String) meta.getOrDefault("tenantid", "");
                return docTenantId.equals(tenantId);
            }
            // If scope is empty (migration), default to visible for now
            return true;
        }).collect(Collectors.toList());
    }

    /**
     * Vector検索結果を前処理.
     * <p>
     * メタデータを可変マップにコピーし、ランキング情報を付与します。
     * </p>
     */
    private List<Document> prepareVectorDocs(List<Document> vectorDocs) {
        List<Document> prepared = new ArrayList<>();
        for (int i = 0; i < vectorDocs.size(); i++) {
            Document doc = vectorDocs.get(i);
            // 常にメタデータを可変マップにコピー（try-catch不要）
            Map<String, Object> mutableMetadata = new HashMap<>(doc.getMetadata());
            mutableMetadata.put("vector_rank", i + 1);

            // Explicitly preserve raw vector score for debugger
            if (mutableMetadata.containsKey("distance")) {
                Double distance = ((Number) mutableMetadata.get("distance")).doubleValue();
                Double score = 1.0 - distance; // Convert distance to similarity
                mutableMetadata.put("score", score);
                log.debug("Vector doc ID: {}, distance: {}, score: {}", doc.getId(), distance, score);
            }

            prepared.add(new Document(doc.getId(), doc.getText(), mutableMetadata));
        }
        return prepared;
    }

    /**
     * Reciprocal Rank Fusion (RRF) を適用.
     * <p>
     * メタデータは常に可変マップを使用し、try-catch による例外駆動を排除。
     * </p>
     */
    private List<Document> applyRRF(List<Document> vectorDocs, List<Document> keywordDocs, int topK) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, Document> docMap = new HashMap<>();

        // Process Vector Results (既にprepareVectorDocsで可変化済み)
        for (int i = 0; i < vectorDocs.size(); i++) {
            Document doc = vectorDocs.get(i);
            docMap.put(doc.getId(), doc);
            double score = 1.0 / (RRF_K + (i + 1));
            scoreMap.merge(doc.getId(), score, Double::sum);
        }

        // Process Keyword Results
        for (int i = 0; i < keywordDocs.size(); i++) {
            Document doc = keywordDocs.get(i);
            double score = 1.0 / (RRF_K + (i + 1));
            scoreMap.merge(doc.getId(), score, Double::sum);

            // keyword_rank を付与（常に可変マップを作成）
            Map<String, Object> metadata;
            if (docMap.containsKey(doc.getId())) {
                // Vector検索でも見つかったドキュメント：メタデータをマージ
                Document existing = docMap.get(doc.getId());
                metadata = new HashMap<>(existing.getMetadata());
            } else {
                // Keyword検索のみで見つかったドキュメント
                metadata = new HashMap<>(doc.getMetadata());
            }
            metadata.put("keyword_rank", i + 1);
            docMap.put(doc.getId(), new Document(doc.getId(), doc.getText(), metadata));
        }

        // Sort by RRF Score and add rrf_score to metadata
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    Document doc = docMap.get(entry.getKey());
                    Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                    metadata.put("rrf_score", entry.getValue());
                    metadata.put("hybrid_score", entry.getValue());
                    return new Document(doc.getId(), doc.getText(), metadata);
                })
                .collect(Collectors.toList());
    }

    /**
     * ハイブリッド検索結果（デバッグ用）.
     */
    public record HybridSearchResult(
            List<Document> vectorDocs,
            List<Document> keywordDocs,
            List<Document> rrfResults,
            List<Document> rerankedResults,
            boolean rerankerApplied,
            String rerankerProvider,
            long rerankerLatencyMs) {

        /** レガシーコンストラクタ（リランキングなし） */
        public HybridSearchResult(List<Document> vectorDocs, List<Document> keywordDocs, List<Document> rrfResults) {
            this(vectorDocs, keywordDocs, rrfResults, rrfResults, false, "none", 0);
        }
    }
}
