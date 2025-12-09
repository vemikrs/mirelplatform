/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MiraStreamResponse {

    public enum EventType {
        DELTA, STATUS, ERROR, DONE
    }

    private EventType type;
    private String content;
    private String model;
    private String conversationId;
    private String error;

    public static MiraStreamResponse delta(String content, String model) {
        return MiraStreamResponse.builder()
                .type(EventType.DELTA)
                .content(content)
                .model(model)
                .build();
    }

    public static MiraStreamResponse status(String statusMessage) {
        return MiraStreamResponse.builder()
                .type(EventType.STATUS)
                .content(statusMessage)
                .build();
    }

    public static MiraStreamResponse error(String code, String message) {
        return MiraStreamResponse.builder()
                .type(EventType.ERROR)
                .error(message)
                .content(code)
                .build();
    }

    public static MiraStreamResponse done(String conversationId) {
        return MiraStreamResponse.builder()
                .type(EventType.DONE)
                .conversationId(conversationId)
                .build();
    }
}
