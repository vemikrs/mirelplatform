/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.application.dto.debugger.MiraDebuggerAnalytics;
import jp.vemi.mirel.apps.mira.application.dto.debugger.MiraDebuggerStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiraDebuggerService {

    private final JdbcTemplate jdbcTemplate;
    private final MiraHybridSearchService miraHybridSearchService;
    private final MiraKnowledgeBaseService miraKnowledgeBaseService;
    private final jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties miraAiProperties;

    // Hardcoded table name as per Schema (Spring AI might use different if
    // configured, but we fixed it to mir_mira_vector_store)
    private static final String TABLE_NAME = "mir_mira_vector_store";

    /**
     * Get Index Statistics.
     */
    public MiraDebuggerStats getStats() {
        try {
            // Total Count
            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_NAME, Long.class);
            if (total == null)
                total = 0L;

            // Group by Scope (using metadata JSON)
            // Note: This relies on PGVector metadata column structure (json/jsonb)
            String scopeSql = "SELECT metadata->>'scope' as scope, COUNT(*) as cnt FROM " + TABLE_NAME
                    + " GROUP BY metadata->>'scope'";
            Map<String, Long> byScope = new HashMap<>();
            try {
                jdbcTemplate.query(scopeSql, (rs) -> {
                    String s = rs.getString("scope");
                    long c = rs.getLong("cnt");
                    byScope.put(s == null ? "UNKNOWN" : s, c);
                });
            } catch (Exception e) {
                log.warn("Failed to group by scope", e);
            }

            return MiraDebuggerStats.builder()
                    .totalDocumentCount(total)
                    .countByScope(byScope)
                    .isIndexEmpty(total == 0)
                    .lastIngestedAt(null) // TODO: track ingestion time in a separate log table or file
                    .activeEmbeddingModel(miraAiProperties.getProvider())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get debugger stats", e);
            return MiraDebuggerStats.builder().totalDocumentCount(-1).isIndexEmpty(true).build();
        }
    }

    /**
     * Re-index Scope.
     */
    public String reindexScope(String scope, String tenantId, String userId) {
        return miraKnowledgeBaseService.reindexScope(
                jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore.Scope.valueOf(scope), tenantId, userId);
    }

    /**
     * Get documents list.
     */
    public List<jp.vemi.mirel.apps.mira.application.dto.MiraKnowledgeDocumentDto> getDocuments(
            String scope, String tenantId, String userId) {
        return miraKnowledgeBaseService.getDocuments(
                jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore.Scope.valueOf(scope), tenantId, userId);
    }

    /**
     * Delete document.
     */
    public void deleteDocument(String fileId) {
        miraKnowledgeBaseService.deleteDocument(fileId);
    }

    /**
     * Analyze Search (Zero-Hit Analysis).
     */
    public MiraDebuggerAnalytics analyze(String query, String scope, String tenantId, String userId, double threshold,
            int topK) {
        MiraDebuggerAnalytics.StepCounts.StepCountsBuilder counts = MiraDebuggerAnalytics.StepCounts.builder();
        List<MiraDebuggerAnalytics.RejectedDocument> rejected = new ArrayList<>();

        // 1. & 2. Execute Hybrid Search in Debug Mode
        SearchRequest vectorReq = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.0) // We rely on HybridSearchService to handle this or pass 0.0 to get everything
                .build();

        var result = miraHybridSearchService.searchDebug(query, vectorReq, scope, tenantId, userId);

        List<Document> vectorDocs = result.vectorDocs();
        List<Document> keywordDocs = result.keywordDocs();

        counts.vectorSearchRaw(vectorDocs.size());
        counts.keywordSearchRaw(keywordDocs.size());

        // --- RRF Calculation Start ---
        // 0. Pre-processing: Sort Keyword Results by Term Frequency DESC (Deep
        // Traceability)
        // Note: MiraHybridSearchService calculates TF, but might not sort by it. We
        // sort here for debug UI clarity.
        keywordDocs.sort((d1, d2) -> {
            Long tf1 = ((Number) d1.getMetadata().getOrDefault("termFrequency", 0L)).longValue();
            Long tf2 = ((Number) d2.getMetadata().getOrDefault("termFrequency", 0L)).longValue();
            return tf2.compareTo(tf1); // DESC
        });

        // Map ID to RRF components & Traceability Metrics
        Map<String, Integer> vectorRanks = new HashMap<>();
        Map<String, Double> vectorSimilarities = new HashMap<>(); // New: Deep Traceability

        for (int i = 0; i < vectorDocs.size(); i++) {
            Document doc = vectorDocs.get(i);
            vectorRanks.put(doc.getId(), i + 1); // 1-based rank
            // Extract similarity (distance/score)
            // Spring AI usually puts it in 'distance' or 'score'
            double sim = 0.0;
            if (doc.getMetadata().containsKey("score")) {
                sim = ((Number) doc.getMetadata().get("score")).doubleValue();
            }
            vectorSimilarities.put(doc.getId(), sim);
        }

        Map<String, Integer> keywordRanks = new HashMap<>();
        Map<String, Long> termFrequencies = new HashMap<>(); // New: Deep Traceability

        for (int i = 0; i < keywordDocs.size(); i++) {
            Document doc = keywordDocs.get(i);
            keywordRanks.put(doc.getId(), i + 1); // 1-based rank (after TF sort)
            long tf = ((Number) doc.getMetadata().getOrDefault("termFrequency", 0L)).longValue();
            termFrequencies.put(doc.getId(), tf);
        }

        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Document> allDocs = new HashMap<>();

        // RRF: Vector
        for (Document doc : vectorDocs) {
            allDocs.put(doc.getId(), doc);
            double score = 1.0 / (60 + vectorRanks.get(doc.getId())); // 60 is hardcoded const in Service too
            rrfScores.merge(doc.getId(), score, Double::sum);
        }

        // RRF: Keyword
        for (Document doc : keywordDocs) {
            allDocs.putIfAbsent(doc.getId(), doc);
            double score = 1.0 / (60 + keywordRanks.get(doc.getId()));
            rrfScores.merge(doc.getId(), score, Double::sum);
        }

        // 3. Scope Filter Analysis
        List<Document> afterUser = new ArrayList<>();
        // Note: HybridSearchService already filters keyword docs by scope.
        // But vector docs might include out-of-scope docs if we used raw vector search.

        // Sort by RRF Descending
        List<Document> sortedDocs = new ArrayList<>(allDocs.values());
        sortedDocs.sort(Comparator.comparingDouble((Document d) -> rrfScores.get(d.getId())).reversed());

        // 3. Scope Filter Analysis
        // List<Document> afterUser = new ArrayList<>(); // Already defined above

        for (Document doc : sortedDocs) {
            String id = doc.getId();
            boolean reject = false;
            String reason = "";
            Map<String, Object> meta = doc.getMetadata();

            // Check Scope
            String docScope = (String) meta.get("scope");
            if (scope != null && !scope.equals(docScope)) {
                reject = true;
                reason = String.format("Scope mismatch: Req='%s' vs Doc='%s'", scope, docScope);
            }

            // Check Tenant
            if (!reject && ("TENANT".equals(scope) || "USER".equals(scope))) {
                String docTenant = (String) meta.get("tenantId");
                if (tenantId != null && !tenantId.equals(docTenant)) {
                    reject = true;
                    reason = String.format("Tenant mismatch: Req='%s' vs Doc='%s'", tenantId, docTenant);
                }
            }

            // Check User
            if (!reject && "USER".equals(scope)) {
                String docUser = (String) meta.get("userId");
                if (userId != null && !userId.equals(docUser)) {
                    reject = true;
                    reason = String.format("User mismatch: Req='%s' vs Doc='%s'", userId, docUser);
                }
            }

            if (reject) {
                if (rejected.size() < 10) {
                    rejected.add(toRejected(doc, reason, rrfScores.get(doc.getId()), vectorRanks.get(doc.getId()),
                            keywordRanks.get(doc.getId()), vectorSimilarities.get(doc.getId()),
                            termFrequencies.get(doc.getId())));
                }
            } else {
                afterUser.add(doc);
            }
        }
        counts.afterScopeFilter(afterUser.size());

        // 4. Threshold Filter Simulation
        List<Document> finalDocs = new ArrayList<>();

        for (Document doc : afterUser) {
            double score = rrfScores.get(doc.getId());

            if (score < threshold) {
                if (rejected.size() < 10) {
                    rejected.add(toRejected(doc, String.format("Score too low: %.4f < %.2f", score, threshold), score,
                            vectorRanks.get(doc.getId()), keywordRanks.get(doc.getId()),
                            vectorSimilarities.get(doc.getId()), termFrequencies.get(doc.getId())));
                }
            } else {
                finalDocs.add(doc);
            }
        }
        counts.afterThresholdFilter(finalDocs.size());

        List<Map<String, Object>> mappedResults = finalDocs.stream().map(doc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", doc.getId());
            map.put("content", doc.getText());
            map.put("metadata", doc.getMetadata());
            map.put("score", rrfScores.get(doc.getId())); // Final RRF Score
            map.put("vectorRank", vectorRanks.get(doc.getId()));
            map.put("vectorSimilarity", vectorSimilarities.get(doc.getId()));
            map.put("keywordRank", keywordRanks.get(doc.getId()));
            map.put("termFrequency", termFrequencies.get(doc.getId()));
            return map;
        }).collect(Collectors.toList());

        return MiraDebuggerAnalytics.builder()
                .query(query)
                .totalIndexCount(getStats().getTotalDocumentCount())
                .stepCounts(counts.build())
                .rejectedDocuments(rejected)
                .results(mappedResults)
                .build();
    }

    private MiraDebuggerAnalytics.RejectedDocument toRejected(Document doc, String reason, Double score, Integer vRank,
            Integer kRank, Double vSim, Long tf) {
        Object fileName = doc.getMetadata().getOrDefault("fileName", "Unknown");
        return MiraDebuggerAnalytics.RejectedDocument.builder()
                .id(doc.getId())
                .fileName(fileName.toString())
                .reason(reason)
                .score(score != null ? score : 0.0)
                .vectorRank(vRank)
                .vectorSimilarity(vSim)
                .keywordRank(kRank)
                .termFrequency(tf)
                .metadata(doc.getMetadata())
                .build();
    }
}
