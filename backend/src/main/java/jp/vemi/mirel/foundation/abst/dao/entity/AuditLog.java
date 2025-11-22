/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Date;

/**
 * 監査ログエンティティ.
 * すべての重要操作を記録
 */
@Setter
@Getter
@Entity
@Table(name = "mir_audit_log",
       indexes = {
           @Index(name = "idx_audit_user", columnList = "user_id,created_at"),
           @Index(name = "idx_audit_tenant", columnList = "tenant_id,created_at"),
           @Index(name = "idx_audit_event", columnList = "event_type,created_at")
       })
public class AuditLog {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // LOGIN, LOGOUT, LICENSE_CHANGE, etc.

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON形式で詳細情報

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** 削除フラグ */
    @Column(name = "delete_flag", columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
