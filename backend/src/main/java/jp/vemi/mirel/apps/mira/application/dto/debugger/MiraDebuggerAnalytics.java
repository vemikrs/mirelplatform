/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.dto.debugger;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiraDebuggerAnalytics {

    private String query;
    private List<Double> queryVectorSnippet; // First 5 dimensions
    private long totalIndexCount;
    private StepCounts stepCounts;
    private List<RejectedDocument> rejectedDocuments;
    private List<java.util.Map<String, Object>> results; // Simplified document map for frontend

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepCounts {
        private int vectorSearchRaw;
        private int keywordSearchRaw;
        private int afterScopeFilter;
        private int afterThresholdFilter;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectedDocument {
        private String id;
        private String fileName;
        private String reason; // e.g., "Score 0.45 < Threshold 0.6", "Scope mismatch: 'USER' vs 'SYSTEM'"
        private double score; // This is the RRF score
        private Integer vectorRank;
        private Double vectorSimilarity; // New: Deep Traceability
        private Integer keywordRank;
        private Long termFrequency; // New: Deep Traceability
        private Map<String, Object> metadata;
    }
}
