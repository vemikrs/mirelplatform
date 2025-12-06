/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 組織ノード（ツリー構造）.
 */
@Setter
@Getter
@Entity
@Table(name = "mir_organization_unit")
public class OrganizationUnit {

    @Id
    @Column(name = "unit_id")
    private String unitId;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "parent_unit_id")
    private String parentUnitId; // null = ルートノード

    @Column(nullable = false)
    private String name; // 開発部

    @Column(unique = true)
    private String code; // DEV001

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type")
    private UnitType unitType; // DIVISION, DEPARTMENT, SECTION, TEAM, PROJECT

    private Integer level; // 階層レベル（ROOT=0）

    @Column(name = "sort_order")
    private Integer sortOrder; // 同一階層内の表示順

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo; // 組織改編対応

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

    // ツリー操作用
    @Transient
    private List<OrganizationUnit> children;

    @PrePersist
    public void onPrePersist() {
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final OrganizationUnit entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        if (entity.updateDate == null) {
            entity.updateDate = new Date();
        }
    }
}
