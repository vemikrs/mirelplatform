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

    private static final int RRF_K = 60;

    /**
     * Performs a hybrid search.
     *
     * @param query
     *            Search query
     * @param similarityThreshold
     *            Threshold for vector search
     * @param topK
     *            Number of results to return
     * @param filterExpression
     *            metadata filter expression (Spring AI format) or manually handling
     *            for keyword search
     *            Note: Keyword search via JdbcTemplate needs manual SQL
     *            construction for filters if complex.
     *            For Phase 1/2, we'll simplify scope filtering for keyword search
     *            or assume standard metadata structure.
     * @return List of documents sorted by RRF score
     */
    public List<Document> search(String query, SearchRequest vectorRequest, String scope, String tenantId,
            String userId) {
        // 1. Vector Search
        List<Document> vectorResults = vectorStore.similaritySearch(vectorRequest);
        log.info("Hybrid/Vector results: {}", vectorResults.size());

        // Process Vector Results (Score prep, Map creation)
        prepareVectorDocs(vectorResults);

        // 2. Keyword Search (Native SQL)
        List<Document> keywordResults = performKeywordSearch(query, 20); // Fetch more for re-ranking

        // Sort by Term Frequency DESC before filtering/ranking
        keywordResults.sort(Comparator
                .comparingLong((Document d) -> ((Number) d.getMetadata().getOrDefault("termFrequency", 0L)).longValue())
                .reversed());

        // Apply scope filtering in memory for keyword results
        keywordResults = filterInMemory(keywordResults, scope, tenantId, userId);
        log.info("Hybrid/Keyword results (after filter): {}", keywordResults.size());

        // 3. Reciprocal Rank Fusion
        return applyRRF(vectorResults, keywordResults, vectorRequest.getTopK());
    }

    private List<Document> performKeywordSearch(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // Split query by whitespace (full-width or half-width)
        String[] keywords = query.trim().split("[\\s　]+");

        if (keywords.length == 0) {
            return Collections.emptyList();
        }

        StringBuilder sqlBuilder = new StringBuilder("""
                    SELECT id, content, metadata
                    FROM mir_mira_vector_store
                    WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        for (String keyword : keywords) {
            sqlBuilder.append(" AND content ILIKE ?");
            params.add("%" + keyword + "%");
        }

        sqlBuilder.append(" LIMIT ?");
        params.add(limit);

        try {
            return jdbcTemplate.query(sqlBuilder.toString(), (rs, rowNum) -> {
                String id = rs.getString("id");
                String content = rs.getString("content");
                String metadataJson = rs.getString("metadata");
                Map<String, Object> metadata = parseMetadata(metadataJson);
                // Flag as keyword result
                metadata.put("source", "keyword");

                // Calculate Term Frequency (Sum of all keyword occurrences)
                long tf = 0;
                if (content != null) {
                    String lowerContent = content.toLowerCase();
                    for (String keyword : keywords) {
                        String lowerQuery = keyword.toLowerCase();
                        int index = 0;
                        while ((index = lowerContent.indexOf(lowerQuery, index)) != -1) {
                            tf++;
                            index += lowerQuery.length();
                        }
                    }
                }
                metadata.put("termFrequency", tf);

                return new Document(id, content, metadata);
            }, params.toArray());
        } catch (Exception e) {
            log.error("Keyword search failed", e);
            return Collections.emptyList();
        }
    }

    private Map<String, Object> parseMetadata(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private List<Document> filterInMemory(List<Document> docs, String scope, String tenantId, String userId) {
        return docs.stream().filter(doc -> {
            Map<String, Object> meta = doc.getMetadata();

            // Check Scope
            if (scope != null) {
                String docScope = (String) meta.get("scope");
                if (!scope.equals(docScope)) {
                    return false;
                }
            }

            // Check Tenant (if TENANT or USER scope)
            if ("TENANT".equals(scope) || "USER".equals(scope)) {
                String docTenant = (String) meta.get("tenantId");
                if (tenantId != null && !tenantId.equals(docTenant)) {
                    return false;
                }
            }

            // Check User (if USER scope)
            if ("USER".equals(scope)) {
                String docUser = (String) meta.get("userId");
                if (userId != null && !userId.equals(docUser)) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());
    }

    public HybridSearchResult searchDebug(String query, SearchRequest vectorRequest, String scope, String tenantId,
            String userId) {
        // 1. Vector Search
        List<Document> vectorResults = vectorStore.similaritySearch(vectorRequest);
        log.info("Hybrid/Vector results: {}", vectorResults.size());

        // Process Vector Results (Score prep, Map creation)
        prepareVectorDocs(vectorResults);

        // 2. Keyword Search (Native SQL)
        List<Document> keywordResults = performKeywordSearch(query, vectorRequest.getTopK());

        // Sort by Term Frequency DESC before filtering/ranking
        keywordResults.sort(Comparator
                .comparingLong((Document d) -> ((Number) d.getMetadata().getOrDefault("termFrequency", 0L)).longValue())
                .reversed());

        // Apply scope filtering in memory for keyword results
        keywordResults = filterInMemory(keywordResults, scope, tenantId, userId);
        log.info("Hybrid/Keyword results (after filter): {}", keywordResults.size());

        // 3. Reciprocal Rank Fusion
        List<Document> rrfResults = applyRRF(vectorResults, keywordResults, vectorRequest.getTopK());

        return new HybridSearchResult(vectorResults, keywordResults, rrfResults);
    }

    private void prepareVectorDocs(List<Document> vectorDocs) {
        for (int i = 0; i < vectorDocs.size(); i++) {
            Document doc = vectorDocs.get(i);
            // Metadata from Spring AI might be immutable, so we create a new map
            Map<String, Object> mutableMetadata = new HashMap<>(doc.getMetadata());
            mutableMetadata.put("vector_rank", i + 1);

            // Explicitly preserve raw vector score for debugger
            if (mutableMetadata.containsKey("distance")) {
                Double distance = ((Number) mutableMetadata.get("distance")).doubleValue();
                Double score = 1.0 - distance; // Convert distance to similarity
                mutableMetadata.put("score", score);
                log.info("[DEBUG] Found distance: {}, converted to score: {}", distance, score);
            } else {
                log.info("[DEBUG] No distance found in doc ID: {}. Keys: {}", doc.getId(), mutableMetadata.keySet());
            }

            // Debug: List all keys
            mutableMetadata.put("_debug_keys", String.join(",", mutableMetadata.keySet()));

            // Update document with mutable metadata
            Document newDoc = new Document(doc.getId(), doc.getText(), mutableMetadata);
            vectorDocs.set(i, newDoc);
        }
    }

    private List<Document> applyRRF(List<Document> vectorDocs, List<Document> keywordDocs, int topK) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, Document> docMap = new HashMap<>();

        // Process Vector Results
        for (int i = 0; i < vectorDocs.size(); i++) {
            Document doc = vectorDocs.get(i);
            docMap.put(doc.getId(), doc);
            double score = 1.0 / (RRF_K + (i + 1));
            scoreMap.merge(doc.getId(), score, Double::sum);
            // vector_rank is implicitly i+1
        }

        // Process Keyword Results
        for (int i = 0; i < keywordDocs.size(); i++) {
            Document doc = keywordDocs.get(i);
            docMap.putIfAbsent(doc.getId(), doc);
            double score = 1.0 / (RRF_K + (i + 1));
            scoreMap.merge(doc.getId(), score, Double::sum);

            // Access doc from map to handle metadata updates
            Document storedDoc = docMap.get(doc.getId());
            // Ensure metadata is mutable for keyword_rank
            // Note: Keyword search results create fresh Documents with mutable maps, so
            // this is safe for new docs
            // For docs that came from Vector search (immutable), they might fail here if
            // not prepared.
            try {
                storedDoc.getMetadata().put("keyword_rank", i + 1);
            } catch (UnsupportedOperationException e) {
                // Fallback: Recreate document with mutable map
                Map<String, Object> mutable = new HashMap<>(storedDoc.getMetadata());
                mutable.put("keyword_rank", i + 1);
                Document newDoc = new Document(storedDoc.getId(), storedDoc.getText(), mutable);
                docMap.put(storedDoc.getId(), newDoc);
            }
        }

        // Sort by RRF Score
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    Document doc = docMap.get(entry.getKey());
                    try {
                        doc.getMetadata().put("rrf_score", entry.getValue());
                        doc.getMetadata().put("hybrid_score", entry.getValue());
                    } catch (UnsupportedOperationException e) {
                        Map<String, Object> mutable = new HashMap<>(doc.getMetadata());
                        mutable.put("rrf_score", entry.getValue());
                        mutable.put("hybrid_score", entry.getValue());
                        doc = new Document(doc.getId(), doc.getText(), mutable);
                    }
                    return doc;
                })
                .collect(Collectors.toList());
    }

    public record HybridSearchResult(
            List<Document> vectorDocs,
            List<Document> keywordDocs,
            List<Document> rrfResults) {
    }
}
