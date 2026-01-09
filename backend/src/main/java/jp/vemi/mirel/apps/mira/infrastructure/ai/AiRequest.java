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
 * <p>
 * Spring AI 1.1 ChatClient との互換性を持つリクエスト構造を提供します。
 * </p>
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

    /** Top P (Nucleus Sampling) for Playground */
    @Builder.Default
    private Double topP = 0.95;

    /** Top K for Playground */
    @Builder.Default
    private Integer topK = 40;

    /** モデル指定（オプション） */
    private String model;

    /** テナントID（コンテキスト解決用） */
    private String tenantId;

    /** ユーザーID（コンテキスト解決用） */
    private String userId;

    /** 会話ID（トークン使用量記録用） */
    private String conversationId;

    /** 追加パラメータ */
    private Map<String, Object> additionalParams;

    /** Google Search Grounding の有効化フラグ */
    @Builder.Default
    private boolean googleSearchRetrieval = false;

    /** ツールコールバック（関数定義と実行ロジック） */
    private List<org.springframework.ai.tool.ToolCallback> toolCallbacks;

    /**
     * 会話メッセージ.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        /** ロール（user / assistant / system / tool） */
        private String role;
        /** 内容 */
        private String content;

        /**
         * システムメッセージを生成.
         *
         * @param content
         *            内容
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
         * @param content
         *            内容
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
         * @param content
         *            内容
         * @return Message
         */
        public static Message assistant(String content) {
            return Message.builder()
                    .role("assistant")
                    .content(content)
                    .build();
        }

        /** ツール呼び出しリスト (Spring AI ToolCall serialization compatibility) */
        private List<ToolCall> toolCalls;

        /** ツール呼び出しID（role=toolの場合） */
        private String toolCallId;

        /** ツール名（role=toolの場合、オプション） */
        private String toolName;

        /** 添付ファイルリスト (Multimodal input support) */
        private List<AttachedFile> attachedFiles;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ToolCall {
            String id;
            String type;
            String name;
            String arguments;
        }

        /**
         * 添付ファイル情報.
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class AttachedFile {
            /** ファイルID */
            private String fileId;
            /** ファイル名 */
            private String fileName;
            /** MIMEタイプ */
            private String mimeType;
            /** ファイルサイズ */
            private Long fileSize;
            /** ファイルパス (サーバー上の実際のパス) */
            private String filePath;
        }
    }
}
