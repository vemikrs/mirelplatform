/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Date;

/**
 * ユーザ・テナント関連エンティティ.
 * 1ユーザが複数テナントに所属可能
 */
@Setter
@Getter
@Entity
@Table(name = "mir_user_tenant", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tenant_id"}),
       indexes = {
           @Index(name = "idx_user_tenant_user", columnList = "user_id"),
           @Index(name = "idx_user_tenant_tenant", columnList = "tenant_id")
       })
public class UserTenant {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    @Column(name = "role_in_tenant", length = 50)
    private String roleInTenant; // OWNER, MANAGER, MEMBER, GUEST

    @Column(name = "is_default", columnDefinition = "boolean default false")
    private Boolean isDefault = false;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    /** バージョン */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    /** 削除フラグ */
    @Column(name = "delete_flag", columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    /** 作成ユーザ */
    @Column(name = "create_user_id")
    private String createUserId;

    /** 作成日 */
    @Column(name = "create_date")
    private Date createDate;

    /** 更新ユーザ */
    @Column(name = "update_user_id")
    private String updateUserId;

    /** 更新日 */
    @Column(name = "update_date")
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.joinedAt == null) {
            this.joinedAt = Instant.now();
        }
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final UserTenant entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }
}
