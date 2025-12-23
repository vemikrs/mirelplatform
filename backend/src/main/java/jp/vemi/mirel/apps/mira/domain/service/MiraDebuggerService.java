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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.application.dto.debugger.MiraDebuggerAnalytics;
import jp.vemi.mirel.apps.mira.application.dto.debugger.MiraDebuggerStats;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiraDebuggerService {

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    // We reuse logic from HybridSearch but need raw access, so we implement some
    // logic here.

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
                    .build();

        } catch (Exception e) {
            log.error("Failed to get debugger stats", e);
            return MiraDebuggerStats.builder().totalDocumentCount(-1).isIndexEmpty(true).build();
        }
    }

    /**
     * Analyze Search (Zero-Hit Analysis).
     */
    public MiraDebuggerAnalytics analyze(String query, String scope, String tenantId, String userId, double threshold) {
        MiraDebuggerAnalytics.StepCounts.StepCountsBuilder counts = MiraDebuggerAnalytics.StepCounts.builder();
        List<MiraDebuggerAnalytics.RejectedDocument> rejected = new ArrayList<>();

        // 1. Raw Vector Search (Debug Mode: Relaxed Top 50)
        SearchRequest vectorReq = SearchRequest.builder()
                .query(query)
                .topK(50)
                .similarityThreshold(0.0)
                .build();

        List<Document> vectorDocs = vectorStore.similaritySearch(vectorReq);
        counts.vectorSearchRaw(vectorDocs.size());

        // 2. Raw Keyword Search (Debug Mode: Relaxed Top 50)
        List<Document> keywordDocs = performKeywordSearch(query, 50);
        counts.keywordSearchRaw(keywordDocs.size());

        // --- RRF Calculation Start ---
        // Map ID to RRF components
        Map<String, Integer> vectorRanks = new HashMap<>();
        for (int i = 0; i < vectorDocs.size(); i++) {
            vectorRanks.put(vectorDocs.get(i).getId(), i + 1); // 1-based rank
        }

        Map<String, Integer> keywordRanks = new HashMap<>();
        for (int i = 0; i < keywordDocs.size(); i++) {
            keywordRanks.put(keywordDocs.get(i).getId(), i + 1); // 1-based rank
        }

        // Merge all unique docs
        Map<String, Document> allDocs = new HashMap<>();
        vectorDocs.forEach(d -> allDocs.putIfAbsent(d.getId(), d));
        keywordDocs.forEach(d -> allDocs.putIfAbsent(d.getId(), d));

        // Calculate RRF Score for each
        int k = 60; // Standard RRF constant
        Map<String, Double> rrfScores = new HashMap<>();

        allDocs.keySet().forEach(id -> {
            double vScore = vectorRanks.containsKey(id) ? 1.0 / (k + vectorRanks.get(id)) : 0.0;
            double kScore = keywordRanks.containsKey(id) ? 1.0 / (k + keywordRanks.get(id)) : 0.0;
            rrfScores.put(id, vScore + kScore);
        });

        // Sort by RRF Descending
        List<Document> sortedDocs = new ArrayList<>(allDocs.values());
        sortedDocs.sort(Comparator.comparingDouble((Document d) -> rrfScores.get(d.getId())).reversed());
        // --- RRF Calculation End ---

        // 3. Filter Simulation (Scope/Tenant/User)
        List<Document> afterUser = new ArrayList<>();

        for (Document doc : sortedDocs) {
            Map<String, Object> meta = doc.getMetadata();
            String docScope = (String) meta.get("scope");
            String docTenant = (String) meta.get("tenantId");
            String docUser = (String) meta.get("userId");

            boolean reject = false;
            String reason = "";

            // Check Scope
            if (scope != null && !scope.equals(docScope)) {
                reject = true;
                reason = String.format("Scope mismatch: Req='%s' vs Doc='%s'", scope, docScope);
            }

            // Check Tenant
            if (!reject && ("TENANT".equals(scope) || "USER".equals(scope))) {
                if (tenantId != null && !tenantId.equals(docTenant)) {
                    reject = true;
                    reason = String.format("Tenant mismatch: Req='%s' vs Doc='%s'", tenantId, docTenant);
                }
            }

            // Check User
            if (!reject && "USER".equals(scope)) {
                if (userId != null && !userId.equals(docUser)) {
                    reject = true;
                    reason = String.format("User mismatch: Req='%s' vs Doc='%s'", userId, docUser);
                }
            }

            if (reject) {
                if (rejected.size() < 10) {
                    rejected.add(toRejected(doc, reason, rrfScores.get(doc.getId()), vectorRanks.get(doc.getId()),
                            keywordRanks.get(doc.getId())));
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
                            vectorRanks.get(doc.getId()), keywordRanks.get(doc.getId())));
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
            map.put("keywordRank", keywordRanks.get(doc.getId()));
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
            Integer kRank) {
        Object fileName = doc.getMetadata().getOrDefault("fileName", "Unknown");
        return MiraDebuggerAnalytics.RejectedDocument.builder()
                .id(doc.getId())
                .fileName(fileName.toString())
                .reason(reason)
                .score(score != null ? score : 0.0)
                .vectorRank(vRank)
                .keywordRank(kRank)
                .metadata(doc.getMetadata())
                .build();
    }

    // Duplicated from HybridSearchService to be independent
    private List<Document> performKeywordSearch(String query, int limit) {
        String sql = """
                    SELECT id, content, metadata
                    FROM mir_mira_vector_store
                    WHERE content ILIKE ?
                    LIMIT ?
                """;
        // ... implementation ...
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            // ... mapper ...
            String id = rs.getString("id");
            String content = rs.getString("content");
            Map<String, Object> metadata = new HashMap<>();
            try {
                metadata = objectMapper.readValue(rs.getString("metadata"), Map.class);
            } catch (Exception e) {
            }
            return new Document(id, content, metadata);
        }, "%" + query + "%", limit);
    }
}
