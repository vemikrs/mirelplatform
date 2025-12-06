/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.request;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * チャットリクエスト DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    /** 会話セッションID（null/空の場合は新規セッション） */
    private String conversationId;

    /** モード（general_chat / context_help / error_analyze / studio_agent / workflow_agent） */
    private String mode;

    /** コンテキスト情報 */
    private Context context;

    /** メッセージ */
    private Message message;

    /**
     * コンテキスト情報.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Context {
        /** コンテキストスナップショットID（省略可） */
        private String snapshotId;

        /** アプリケーションID（studio / workflow / admin 等） */
        private String appId;

        /** 画面ID */
        private String screenId;

        /** システムロール（ROLE_ADMIN / ROLE_USER） */
        private String systemRole;

        /** アプリケーションロール（SystemAdmin / Builder / Operator / Viewer） */
        private String appRole;

        /** 画面固有コンテキスト */
        private Map<String, Object> payload;
    }

    /**
     * メッセージ.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        /** メッセージ内容 */
        private String content;

        /** コンテンツタイプ（plain / markdown） */
        @Builder.Default
        private String contentType = "plain";
    }
}
