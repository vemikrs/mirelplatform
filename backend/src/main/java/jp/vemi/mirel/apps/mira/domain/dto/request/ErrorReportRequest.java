/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * エラーレポートリクエスト DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorReportRequest {

    /** エラー発生元（api / workflow / studio 等） */
    private String source;

    /** エラーコード */
    private String code;

    /** ユーザ向けメッセージ */
    private String message;

    /** 詳細情報（JSON形式） */
    private Object detail;

    /** コンテキスト情報 */
    private Context context;

    /**
     * コンテキスト情報.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Context {
        /** アプリケーションID（studio / workflow / admin 等） */
        private String appId;

        /** 画面ID */
        private String screenId;
    }
}
