/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Date;
import java.util.Map;

/**
 * 組織設定.
 * 各組織ノードに紐づく設定を管理する。
 */
@Setter
@Getter
@Entity
@Table(name = "mir_organization_settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "period_code"}))
public class OrganizationSettings {

    @Id
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "period_code")
    private String periodCode;

    @Column(name = "allow_flexible_schedule", columnDefinition = "boolean default false")
    private Boolean allowFlexibleSchedule = false;

    @Column(name = "require_approval", columnDefinition = "boolean default true")
    private Boolean requireApproval = true;

    @Column(name = "max_member_count")
    private Integer maxMemberCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_settings", columnDefinition = "jsonb")
    private Map<String, Object> extendedSettings;

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
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final OrganizationSettings entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        entity.updateDate = new Date();
    }
}
