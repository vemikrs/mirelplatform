/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.Collections;
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

        // 2. Keyword Search (Native SQL)
        List<Document> keywordResults = performKeywordSearch(query, 20); // Fetch more for re-ranking

        // Apply scope filtering in memory for keyword results
        keywordResults = filterInMemory(keywordResults, scope, tenantId, userId);
        log.info("Hybrid/Keyword results (after filter): {}", keywordResults.size());

        // 3. Reciprocal Rank Fusion
        return applyRRF(vectorResults, keywordResults, vectorRequest.getTopK());
    }

    private List<Document> performKeywordSearch(String query, int limit) {
        // Simple ILIKE search on content
        // Assuming table 'vector_store' with columns 'id', 'content', 'metadata'
        String sql = """
                    SELECT id, content, metadata
                    FROM mir_mira_vector_store
                    WHERE content ILIKE ?
                    LIMIT ?
                """;

        String likeQuery = "%" + query + "%";

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String id = rs.getString("id");
                String content = rs.getString("content");
                String metadataJson = rs.getString("metadata");
                Map<String, Object> metadata = parseMetadata(metadataJson);
                // Flag as keyword result
                metadata.put("source", "keyword");
                return new Document(id, content, metadata);
            }, likeQuery, limit);
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

    private List<Document> applyRRF(List<Document> vectorDocs, List<Document> keywordDocs, int topK) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, Document> docMap = new HashMap<>();

        // Process Vector Results
        for (int i = 0; i < vectorDocs.size(); i++) {
            Document doc = vectorDocs.get(i);
            docMap.put(doc.getId(), doc);
            double score = 1.0 / (RRF_K + (i + 1));
            scoreMap.merge(doc.getId(), score, Double::sum);
            doc.getMetadata().put("vector_rank", i + 1);
        }

        // Process Keyword Results
        for (int i = 0; i < keywordDocs.size(); i++) {
            Document doc = keywordDocs.get(i);
            docMap.putIfAbsent(doc.getId(), doc);
            double score = 1.0 / (RRF_K + (i + 1));
            scoreMap.merge(doc.getId(), score, Double::sum);

            Document storedDoc = docMap.get(doc.getId());
            storedDoc.getMetadata().put("keyword_rank", i + 1);
        }

        // Sort by RRF Score
        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    Document doc = docMap.get(entry.getKey());
                    doc.getMetadata().put("rrf_score", entry.getValue());
                    doc.getMetadata().put("hybrid_score", entry.getValue()); // Alias
                    return doc;
                })
                .collect(Collectors.toList());
    }
}
