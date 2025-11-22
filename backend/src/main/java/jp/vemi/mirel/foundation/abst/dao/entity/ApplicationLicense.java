/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.Date;

/**
 * アプリケーションライセンスエンティティ.
 * ユーザまたはテナント単位でライセンスを付与
 */
@Setter
@Getter
@Entity
@Table(name = "mir_application_license",
       indexes = {
           @Index(name = "idx_license_subject", columnList = "subject_type,subject_id,application_id")
       })
public class ApplicationLicense {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType; // USER or TENANT

    @Column(name = "subject_id", nullable = false, length = 36)
    private String subjectId;

    @Column(name = "application_id", nullable = false, length = 50)
    private String applicationId; // promarker, etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private LicenseTier tier; // FREE, PRO, MAX

    @Column(name = "features", columnDefinition = "TEXT")
    private String features; // JSON形式で有効機能リスト

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "granted_by", length = 36)
    private String grantedBy;

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
        if (this.grantedAt == null) {
            this.grantedAt = Instant.now();
        }
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final ApplicationLicense entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }

    /**
     * ライセンスの対象タイプ
     */
    public enum SubjectType {
        USER,
        TENANT
    }

    /**
     * ライセンスティア
     */
    public enum LicenseTier {
        FREE,
        PRO,
        MAX
    }
}
