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

    /**
     * モード（general_chat / context_help / error_analyze / studio_agent /
     * workflow_agent）
     */
    private String mode;

    /** コンテキスト情報 */
    private Context context;

    /** メッセージ */
    private Message message;

    /** 強制プロバイダ指定 (Admin only, testing purpose) */
    private String forceProvider;

    /** Web検索を有効化 (明示的指定時) */
    private Boolean webSearchEnabled;

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

        /** ロケール（ja / en） */
        @Builder.Default
        private String locale = "ja";

        /** 画面固有コンテキスト */
        private Map<String, Object> payload;

        /** メッセージ送信設定 */
        private MessageConfig messageConfig;
    }

    /**
     * メッセージ送信設定.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageConfig {
        /** 履歴スコープ (auto, recent, none) */
        @Builder.Default
        private String historyScope = "auto";

        /** 直近履歴件数 (historyScope=recentの場合) */
        private Integer recentCount;

        /** コンテキスト設定のオーバーライド */
        private Map<String, ContextOverride> contextOverrides;

        /** 追加プリセットIDリスト */
        private java.util.List<String> additionalPresets;

        /** 一時的な追加コンテキスト */
        private String temporaryContext;
    }

    /**
     * コンテキスト設定のオーバーライド.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContextOverride {
        /** 有効/無効 */
        @Builder.Default
        private Boolean enabled = true;

        /** 優先度 (0=normal, 1=high, -1=low) */
        @Builder.Default
        private Integer priority = 0;
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
