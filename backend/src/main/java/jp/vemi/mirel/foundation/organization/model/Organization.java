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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

/**
 * 組織ルート（テナント配下に複数持てる設計）.
 */
@Setter
@Getter
@Entity
@Table(name = "mir_organization")
public class Organization {

    @Id
    @Column(name = "organization_id")
    private String organizationId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name; // 株式会社○○

    @Column(unique = true)
    private String code; // CORP001

    private String description;

    @Column(name = "fiscal_year_start")
    private Integer fiscalYearStart; // 4 = 4月始まり

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

    public static void setDefault(final Organization entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }
}
