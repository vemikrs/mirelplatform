/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 応答 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResponse {

    /** 応答内容 */
    private String content;

    /** ツール呼び出しリスト */
    private java.util.List<jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest.Message.ToolCall> toolCalls;

    /** メタデータ */
    @Builder.Default
    private Metadata metadata = new Metadata();

    /** エラー情報（エラー時のみ） */
    private ErrorInfo error;

    /**
     * 応答メタデータ.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        /** 使用モデル名 */
        private String model;

        /** 終了理由（stop / length / content_filter 等） */
        private String finishReason;

        /** プロンプトトークン数 */
        private Integer promptTokens;

        /** 応答トークン数 */
        private Integer completionTokens;

        /** 合計トークン数 */
        private Integer totalTokens;

        /** レイテンシ（ミリ秒） */
        private Long latencyMs;
    }

    /**
     * エラー情報.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorInfo {
        /** エラーコード */
        private String code;

        /** エラーメッセージ */
        private String message;

        /** 詳細情報 */
        private String detail;
    }

    /**
     * 成功応答を生成.
     */
    public static AiResponse success(String content, Metadata metadata) {
        return AiResponse.builder()
                .content(content)
                .metadata(metadata)
                .build();
    }

    /**
     * エラー応答を生成.
     */
    public static AiResponse error(String code, String message) {
        return AiResponse.builder()
                .error(ErrorInfo.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }

    /**
     * エラーかどうか判定.
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * 成功かどうか判定.
     */
    public boolean isSuccess() {
        return error == null && content != null;
    }

    /**
     * エラーメッセージを取得.
     */
    public String getErrorMessage() {
        if (error == null) {
            return null;
        }
        return error.getMessage();
    }

    /**
     * プロバイダ名を取得.
     */
    public String getProvider() {
        return metadata != null ? metadata.getModel() : "unknown";
    }

    /**
     * モデル名を取得.
     */
    public String getModel() {
        return metadata != null ? metadata.getModel() : "unknown";
    }

    /**
     * プロンプトトークン数を取得.
     */
    public Integer getPromptTokens() {
        return metadata != null ? metadata.getPromptTokens() : null;
    }

    /**
     * 応答トークン数を取得.
     */
    public Integer getCompletionTokens() {
        return metadata != null ? metadata.getCompletionTokens() : null;
    }
}
