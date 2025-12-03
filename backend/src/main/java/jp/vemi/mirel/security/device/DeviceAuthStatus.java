/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device;

/**
 * デバイス認証セッションのステータス。
 * OAuth2 Device Authorization Grant (RFC 8628) に準拠。
 */
public enum DeviceAuthStatus {
    /**
     * 認証待ち - ユーザーがまだ承認していない
     */
    PENDING,
    
    /**
     * 承認済み - ユーザーが承認した
     */
    AUTHORIZED,
    
    /**
     * 拒否 - ユーザーが拒否した
     */
    DENIED
}
