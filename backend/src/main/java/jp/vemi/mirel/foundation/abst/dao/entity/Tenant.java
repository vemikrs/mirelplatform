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
import java.util.Date;
import jp.vemi.mirel.foundation.feature.tenant.dto.TenantPlan;
import jp.vemi.mirel.foundation.feature.tenant.dto.TenantStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Setter
@Getter
@Entity
@Table(name = "mir_tenant")
public class Tenant {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "tenant_name")
    private String tenantName;

    @Column(name = "domain", unique = true)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan")
    private TenantPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TenantStatus status;

    // SaaS拡張フィールド
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "description")
    private String description;

    @Column(name = "org_id")
    private String orgId;

    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings; // JSON形式でテナント設定を格納

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

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

    public static void setDefault(final Tenant entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }
}
