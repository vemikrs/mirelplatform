/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * デバイス認証セッション。
 * OAuth2 Device Authorization Grant (RFC 8628) に準拠したセッション情報を保持。
 * 
 * <p>CLIからの認証リクエストに対して発行され、ブラウザでの承認を待つ間、
 * インメモリまたはRedisに保存されます。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAuthSession {
    
    /**
     * デバイスコード - サーバー側で管理する一意識別子（UUID形式）
     */
    private String deviceCode;
    
    /**
     * ユーザーコード - ユーザーがブラウザで入力する8文字コード（XXXX-XXXX形式）
     */
    private String userCode;
    
    /**
     * クライアントID - 認証を要求するクライアントの識別子
     */
    private String clientId;
    
    /**
     * スコープ - 要求される権限範囲
     */
    private String scope;
    
    /**
     * ステータス - 認証の状態
     */
    private DeviceAuthStatus status;
    
    /**
     * ユーザーID - 承認したユーザーのID（認証完了後に設定）
     */
    private String userId;
    
    /**
     * ユーザー名 - 承認したユーザーの名前
     */
    private String userName;
    
    /**
     * ユーザーメール - 承認したユーザーのメールアドレス
     */
    private String userEmail;
    
    /**
     * 作成日時
     */
    private Instant createdAt;
    
    /**
     * 有効期限（デフォルト: 15分後）
     */
    private Instant expiresAt;
    
    /**
     * 最終ポーリング日時 - レート制限用
     */
    private Instant lastPolledAt;
    
    /**
     * セッションが有効期限内かどうかを判定
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
    
    /**
     * セッションが有効かどうかを判定（期限内かつPENDING状態）
     */
    public boolean isValid() {
        return !isExpired() && status == DeviceAuthStatus.PENDING;
    }
}
