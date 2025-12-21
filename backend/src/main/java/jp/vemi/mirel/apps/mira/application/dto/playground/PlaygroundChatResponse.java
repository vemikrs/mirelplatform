/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.dto.playground;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaygroundChatResponse {
    private String content;
    private Usage usage;
    private List<RagDocument> ragDocuments;
    private Long latencyMs;
    private String provider;
    private String model;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RagDocument {
        private String id;
        private String content;
        private Double score;
        private String fileName;
        private String scope;
        private java.util.Map<String, Object> metadata;
    }
}
