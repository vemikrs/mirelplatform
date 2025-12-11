/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OTPトークンエンティティ.
 * パスワードレス認証・パスワードリセット・メールアドレス検証用のワンタイムパスワードを管理
 */
@Entity
@Table(name = "mir_otp_token", indexes = {
        @Index(name = "idx_otp_user_purpose", columnList = "system_user_id, purpose, is_verified, expires_at")
})
@Getter
@Setter
public class OtpToken {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    /**
     * SystemUser ID
     */
    @Column(name = "system_user_id", nullable = false, columnDefinition = "UUID")
    private UUID systemUserId;

    /**
     * メールアドレス (サインアップ時のOTP検証用)
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * OTPコードのSHA-256ハッシュ
     */
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    /**
     * マジックリンクトークン (ランダムな文字列)
     */
    @Column(name = "magic_link_token", length = 64)
    private String magicLinkToken;

    /**
     * 用途 (LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION)
     */
    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;

    /**
     * 有効期限
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 検証済みフラグ
     */
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    /**
     * 検証日時
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * 検証試行回数
     */
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    /**
     * 最大試行回数
     */
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    /**
     * リクエスト元IPアドレス
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;

    /**
     * User Agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (isVerified == null) {
            isVerified = false;
        }
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (maxAttempts == null) {
            maxAttempts = 3;
        }
    }

    /**
     * 有効期限切れかチェック
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 有効なトークンかチェック
     */
    public boolean isValid() {
        return !isVerified && !isExpired() && attemptCount < maxAttempts;
    }

    /**
     * 試行回数をインクリメント
     */
    public void incrementAttemptCount() {
        this.attemptCount++;
    }
}
