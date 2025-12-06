/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.exception;

/**
 * Mira セキュリティ例外.
 * 
 * <p>プロンプトインジェクション、認可拒否などのセキュリティ関連エラーを表します。</p>
 */
public class MiraSecurityException extends MiraException {

    /** セキュリティイベント種別 */
    private final String eventType;
    
    /** 検出されたパターン（インジェクション検出時など） */
    private final String detectedPattern;

    public MiraSecurityException(MiraErrorCode errorCode) {
        super(errorCode);
        this.eventType = null;
        this.detectedPattern = null;
    }

    public MiraSecurityException(MiraErrorCode errorCode, String message) {
        super(errorCode, message);
        this.eventType = null;
        this.detectedPattern = null;
    }

    public MiraSecurityException(MiraErrorCode errorCode, String message, String eventType) {
        super(errorCode, message);
        this.eventType = eventType;
        this.detectedPattern = null;
    }

    public MiraSecurityException(MiraErrorCode errorCode, String message, String eventType, String detectedPattern) {
        super(errorCode, message);
        this.eventType = eventType;
        this.detectedPattern = detectedPattern;
    }

    /**
     * セキュリティイベント種別を取得.
     * 
     * @return イベント種別
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 検出されたパターンを取得.
     * 
     * @return 検出パターン
     */
    public String getDetectedPattern() {
        return detectedPattern;
    }
}
