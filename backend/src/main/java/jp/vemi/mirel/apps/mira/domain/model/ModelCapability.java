/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.model;

/**
 * AIモデルの機能を定義する列挙型.
 * <p>
 * モデルごとにサポートされる機能が異なるため、
 * リクエスト時にバリデーションを行うために使用。
 * </p>
 */
public enum ModelCapability {

    /**
     * ツール呼び出し (Function Calling).
     * <p>
     * Web検索などの外部ツールを呼び出す機能。
     * </p>
     */
    TOOL_CALLING("ツール呼び出し"),

    /**
     * マルチモーダル入力 (画像/音声).
     * <p>
     * テキスト以外の入力（画像、音声等）を処理する機能。
     * </p>
     */
    MULTIMODAL_INPUT("マルチモーダル入力"),

    /**
     * ストリーミング出力.
     */
    STREAMING("ストリーミング"),

    /**
     * JSON モード (Structured Output).
     */
    JSON_MODE("JSONモード"),

    /**
     * 長文コンテキスト (128K+).
     */
    LONG_CONTEXT("長文コンテキスト"),

    /**
     * Web検索 (Grounding).
     */
    WEB_SEARCH("Web検索");

    private final String displayName;

    ModelCapability(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
