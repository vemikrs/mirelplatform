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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRequest {

    /** システムプロンプト */
    private String systemPrompt;

    /** ユーザプロンプト */
    private String userPrompt;

    /** コンテキスト情報（追加プロンプト） */
    private String contextPrompt;

    /** 会話履歴 */
    private List<Message> conversationHistory;

    /** オプション */
    @Builder.Default
    private Options options = new Options();

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
    }

    /**
     * AI オプション.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Options {
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
    }
}
