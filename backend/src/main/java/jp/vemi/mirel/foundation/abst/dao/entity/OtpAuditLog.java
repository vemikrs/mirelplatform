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
 * OTP監査ログエンティティ.
 * 全てのOTP操作を記録し、セキュリティ分析・不正アクセス検知に使用
 */
@Entity
@Table(name = "mir_otp_audit_log", indexes = {
    @Index(name = "idx_user_created", columnList = "system_user_id, created_at"),
    @Index(name = "idx_email_created", columnList = "email, created_at"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
public class OtpAuditLog {
    
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * リクエストID (ExecutionContext連携)
     */
    @Column(name = "request_id", length = 36)
    private String requestId;
    
    /**
     * SystemUser ID
     */
    @Column(name = "system_user_id", columnDefinition = "UUID")
    private UUID systemUserId;
    
    /**
     * メールアドレス
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    /**
     * 用途 (LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION)
     */
    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;
    
    /**
     * アクション (REQUEST, VERIFY, RESEND, EXPIRE, RATE_LIMIT)
     */
    @Column(name = "action", nullable = false, length = 20)
    private String action;
    
    /**
     * 成功フラグ
     */
    @Column(name = "success", nullable = false)
    private Boolean success;
    
    /**
     * 失敗理由
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    
    /**
     * IPアドレス
     */
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    
    /**
     * User Agent
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * レート制限情報 (JSON)
     */
    @Column(name = "rate_limit_info", columnDefinition = "TEXT")
    private String rateLimitInfo;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
