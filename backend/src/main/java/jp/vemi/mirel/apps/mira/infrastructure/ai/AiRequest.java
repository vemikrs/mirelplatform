/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI リクエスト DTO.
 * 
 * <p>Spring AI 1.1 ChatClient との互換性を持つリクエスト構造を提供します。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequest {

    /** メッセージリスト（system, user, assistant の順） */
    private List<Message> messages;

    /** Temperature (0.0 - 2.0) */
    @Builder.Default
    private Double temperature = 0.7;

    /** 最大トークン数 */
    @Builder.Default
    private Integer maxTokens = 4096;

    /** モデル指定（オプション） */
    private String model;

    /** 追加パラメータ */
    private Map<String, Object> additionalParams;

    /**
     * 会話メッセージ.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        /** ロール（user / assistant / system） */
        private String role;
        /** 内容 */
        private String content;

        /**
         * システムメッセージを生成.
         *
         * @param content 内容
         * @return Message
         */
        public static Message system(String content) {
            return Message.builder()
                .role("system")
                .content(content)
                .build();
        }

        /**
         * ユーザーメッセージを生成.
         *
         * @param content 内容
         * @return Message
         */
        public static Message user(String content) {
            return Message.builder()
                .role("user")
                .content(content)
                .build();
        }

        /**
         * アシスタントメッセージを生成.
         *
         * @param content 内容
         * @return Message
         */
        public static Message assistant(String content) {
            return Message.builder()
                .role("assistant")
                .content(content)
                .build();
        }
    }
}
