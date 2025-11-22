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
 * テナント招待トークンエンティティ.
 * 招待制テナントへのユーザー招待を管理
 */
@Entity
@Table(name = "mir_invitation_token", indexes = {
    @Index(name = "idx_token_email", columnList = "token, email", unique = true),
    @Index(name = "idx_tenant_email", columnList = "tenant_id, email"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
@Getter
@Setter
public class InvitationToken {
    
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * 招待トークン (UUID v4)
     */
    @Column(name = "token", nullable = false, unique = true, length = 36)
    private String token;
    
    /**
     * Tenant ID
     */
    @Column(name = "tenant_id", nullable = false, columnDefinition = "UUID")
    private UUID tenantId;
    
    /**
     * 招待対象メールアドレス
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    /**
     * 招待者のSystemUser ID
     */
    @Column(name = "invited_by", nullable = false, columnDefinition = "UUID")
    private UUID invitedBy;
    
    /**
     * 有効期限
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * 使用済みフラグ
     */
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
    
    /**
     * 使用日時
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    /**
     * 使用者のSystemUser ID
     */
    @Column(name = "used_by", columnDefinition = "UUID")
    private UUID usedBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        if (isUsed == null) {
            isUsed = false;
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
        return !isUsed && !isExpired();
    }
}
