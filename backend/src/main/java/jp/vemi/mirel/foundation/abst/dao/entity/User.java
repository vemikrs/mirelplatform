/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "mir_user")
public class User {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "tenant_id")
    private String tenantId;

    // SystemUser参照（システム系DB連携）
    @Column(name = "system_user_id", columnDefinition = "UUID")
    private UUID systemUserId;

    // SaaS拡張フィールド（後方互換性のため残存、将来的にSystemUserに移行）
    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "salt")
    private String salt;

    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes; // JSON形式で拡張属性を格納

    @Column(name = "roles", columnDefinition = "TEXT")
    private String roles; // システムロール（カンマ区切り）

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @Column(name = "email_verified", columnDefinition = "boolean default false")
    private Boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** バージョン */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    /** 削除フラグ */
    @Column(columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    /** 作成ユーザ */
    @Column()
    private String createUserId;

    /** 作成日 */
    @Column()
    private Date createDate;

    /** 更新ユーザ */
    @Column()
    private String updateUserId;

    /** 更新日 */
    @Column()
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final User entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }
}
