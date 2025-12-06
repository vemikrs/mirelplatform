/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

/**
 * Mira モード種別.
 * 
 * <p>AI アシスタントの動作モードを定義します。</p>
 */
public enum MiraMode {

    /** 汎用チャット */
    GENERAL_CHAT("general_chat", "汎用チャット", "一般的な質問や会話に対応します"),

    /** コンテキストヘルプ */
    CONTEXT_HELP("context_help", "コンテキストヘルプ", "現在の画面や操作に関するヘルプを提供します"),

    /** エラー解析 */
    ERROR_ANALYZE("error_analyze", "エラー解析", "エラーの原因分析と解決策を提案します"),

    /** Studio エージェント */
    STUDIO_AGENT("studio_agent", "Studio エージェント", "Studio でのモデリング作業を支援します"),

    /** Workflow エージェント */
    WORKFLOW_AGENT("workflow_agent", "Workflow エージェント", "Workflow の設計・デバッグを支援します");

    private final String code;
    private final String displayName;
    private final String description;

    MiraMode(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * コードからモードを取得.
     * 
     * @param code モードコード
     * @return MiraMode（見つからない場合は GENERAL_CHAT）
     */
    public static MiraMode fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return GENERAL_CHAT;
        }
        for (MiraMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return GENERAL_CHAT;
    }
}
