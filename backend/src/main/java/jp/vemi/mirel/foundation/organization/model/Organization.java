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
 * 統合組織エンティティ（再帰的ツリー構造）.
 * 
 * 旧 Organization（会社）と OrganizationUnit（部署）を統合。
 * type=COMPANY のノードがルートとなり、その配下に階層構造を持つ。
 */
@Setter
@Getter
@Entity
@Table(name = "mir_organization")
public class Organization {

    @Id
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "parent_id")
    private String parentId; // null = ルートノード（COMPANY）

    @Column(nullable = false)
    private String name; // 正式名称（株式会社○○、開発部など）

    @Column(name = "display_name")
    private String displayName; // 表示名

    @Column(unique = true)
    private String code; // 組織コード

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private OrganizationType type; // COMPANY, DIVISION, DEPARTMENT, SECTION, TEAM, PROJECT, VIRTUAL

    @Column(name = "path", length = 1024)
    private String path; // 階層パス（例: /root/div/dept）

    private Integer level; // 階層レベル（ROOT=0）

    @Column(name = "sort_order")
    private Integer sortOrder; // 同一階層内の表示順

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "period_code")
    private String periodCode; // 期間コード（リレーション管理用）

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

    // ツリー操作用（永続化対象外）
    @Transient
    private List<Organization> children;

    // 設定（永続化対象外、APIで別途取得）
    @Transient
    private CompanySettings companySettings;

    @Transient
    private OrganizationSettings organizationSettings;

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
        entity.updateDate = new Date();
    }

    /**
     * このノードがルート（COMPANY）かどうか.
     */
    public boolean isRoot() {
        return parentId == null || type == OrganizationType.COMPANY;
    }
}
