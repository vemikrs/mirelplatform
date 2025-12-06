/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.exception;

import lombok.Getter;

/**
 * Mira 基底例外クラス.
 * 
 * <p>Mira モジュールの全例外の基底クラスです。</p>
 */
@Getter
public class MiraException extends RuntimeException {

    /** エラーコード */
    private final MiraErrorCode errorCode;
    
    /** 詳細コンテキスト */
    private final String context;

    public MiraException(MiraErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.context = null;
    }

    public MiraException(MiraErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }

    public MiraException(MiraErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = null;
    }

    public MiraException(MiraErrorCode errorCode, String message, String context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public MiraException(MiraErrorCode errorCode, String message, String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }

    /**
     * リトライ可能な例外かどうか.
     * 
     * @return リトライ可能な場合 true
     */
    public boolean isRetryable() {
        return errorCode.isRetryable();
    }
}
