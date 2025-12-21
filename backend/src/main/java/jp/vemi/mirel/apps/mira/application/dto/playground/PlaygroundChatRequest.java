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
}
