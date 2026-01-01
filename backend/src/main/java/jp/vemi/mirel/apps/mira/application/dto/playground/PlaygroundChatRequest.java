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
public class PlaygroundChatRequest {
    private List<Message> messages;
    private String model;
    private String provider;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private String systemInstruction;
    private RagSettings ragSettings;
    private RerankerSettings rerankerSettings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RagSettings {
        private boolean enabled;
        private String scope;
        private Integer topK;
        private String targetTenantId; // For simulation
        private String targetUserId; // For simulation
    }

    /**
     * リランカー設定オーバーライド.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RerankerSettings {
        /** リランカー有効化（nullの場合はシステム設定を使用） */
        private Boolean enabled;
        /** 最終採用件数（nullの場合はシステム設定を使用） */
        private Integer topN;
    }
}
