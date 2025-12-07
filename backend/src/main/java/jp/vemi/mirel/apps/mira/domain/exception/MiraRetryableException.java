/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.exception;

/**
 * Mira リトライ可能例外.
 * 
 * <p>リトライ戦略が適用される一時的なエラーを表します。</p>
 */
public class MiraRetryableException extends MiraException {

    /** リトライまでの推奨待機時間（秒） */
    private final Integer retryAfterSeconds;

    public MiraRetryableException(MiraErrorCode errorCode) {
        super(errorCode);
        this.retryAfterSeconds = null;
    }

    public MiraRetryableException(MiraErrorCode errorCode, String message) {
        super(errorCode, message);
        this.retryAfterSeconds = null;
    }

    public MiraRetryableException(MiraErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.retryAfterSeconds = null;
    }

    public MiraRetryableException(MiraErrorCode errorCode, String message, Integer retryAfterSeconds) {
        super(errorCode, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public MiraRetryableException(MiraErrorCode errorCode, String message, Integer retryAfterSeconds, Throwable cause) {
        super(errorCode, message, cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * リトライまでの推奨待機時間を取得.
     * 
     * @return 待機時間（秒）、未設定の場合は null
     */
    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
