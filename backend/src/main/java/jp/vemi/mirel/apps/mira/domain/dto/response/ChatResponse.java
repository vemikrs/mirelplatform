/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * チャットレスポンス DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    /** 会話セッションID */
    private String conversationId;

    /** メッセージID */
    private String messageId;

    /** モード */
    private String mode;

    /** アシスタントメッセージ */
    private AssistantMessage assistantMessage;

    /** メタデータ */
    private Metadata metadata;

    /**
     * アシスタントメッセージ.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssistantMessage {
        /** メッセージ内容 */
        private String content;

        /** コンテンツタイプ（plain / markdown） */
        @Builder.Default
        private String contentType = "markdown";
    }

    /**
     * メタデータ.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        /** プロバイダー名 */
        private String provider;

        /** 使用モデル名 */
        private String model;

        /** レイテンシ（ミリ秒） */
        private Long latencyMs;

        /** プロンプトトークン数 */
        private Integer promptTokens;

        /** 応答トークン数 */
        private Integer completionTokens;
    }
}
