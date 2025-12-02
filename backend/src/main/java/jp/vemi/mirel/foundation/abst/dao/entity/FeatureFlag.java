/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * フィーチャーフラグエンティティ.
 * 機能の有効化/無効化、段階的ロールアウト、ライセンス連携を管理
 */
@Setter
@Getter
@Entity
@Table(name = "mir_feature_flag",
       indexes = {
           @Index(name = "idx_ff_feature_key", columnList = "feature_key", unique = true),
           @Index(name = "idx_ff_application", columnList = "application_id"),
           @Index(name = "idx_ff_status", columnList = "status"),
           @Index(name = "idx_ff_in_development", columnList = "in_development"),
           @Index(name = "idx_ff_rollout", columnList = "rollout_percentage")
       })
public class FeatureFlag {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    /** 機能キー（一意識別子） */
    @Column(name = "feature_key", nullable = false, length = 100, unique = true)
    private String featureKey;

    /** 機能名（表示用） */
    @Column(name = "feature_name", nullable = false, length = 200)
    private String featureName;

    /** 機能説明 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** アプリケーションID（promarker, mirelplatform等） */
    @Column(name = "application_id", nullable = false, length = 50)
    private String applicationId;

    /** 機能ステータス */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeatureStatus status = FeatureStatus.STABLE;

    /** 開発中フラグ */
    @Column(name = "in_development", columnDefinition = "boolean default false")
    private Boolean inDevelopment = false;

    /** 必要なライセンスティア（null = ライセンス不要） */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_license_tier", length = 20)
    private ApplicationLicense.LicenseTier requiredLicenseTier;

    /** デフォルトで有効かどうか */
    @Column(name = "enabled_by_default", columnDefinition = "boolean default true")
    private Boolean enabledByDefault = true;

    /** 特定ユーザーのみ有効化（JSON配列） */
    @Column(name = "enabled_for_user_ids", columnDefinition = "TEXT")
    private String enabledForUserIds;

    /** 特定テナントのみ有効化（JSON配列） */
    @Column(name = "enabled_for_tenant_ids", columnDefinition = "TEXT")
    private String enabledForTenantIds;

    /** 特定ユーザーを無効化（JSON配列） - Phase 2+ */
    @Column(name = "disabled_for_user_ids", columnDefinition = "TEXT")
    private String disabledForUserIds;

    /** 特定テナントを無効化（JSON配列） - Phase 2+ */
    @Column(name = "disabled_for_tenant_ids", columnDefinition = "TEXT")
    private String disabledForTenantIds;

    /** ロールアウト比率 (0-100) - Phase 3+ */
    @Column(name = "rollout_percentage")
    private Integer rolloutPercentage = 100;

    /** ターゲットセグメント（JSON配列） - Phase 3+ */
    @Column(name = "target_segments", columnDefinition = "TEXT")
    private String targetSegments;

    /** ライセンス判定戦略 - Phase 2+ */
    @Enumerated(EnumType.STRING)
    @Column(name = "license_resolve_strategy", length = 20)
    private LicenseResolveStrategy licenseResolveStrategy = LicenseResolveStrategy.TENANT_PRIORITY;

    /** 拡張メタデータ（JSON） */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /** バージョン */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    /** 削除フラグ */
    @Column(name = "delete_flag", columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    /** 作成ユーザ */
    @Column(name = "create_user_id", length = 36)
    private String createUserId;

    /** 作成日 */
    @Column(name = "create_date")
    private Date createDate;

    /** 更新ユーザ */
    @Column(name = "update_user_id", length = 36)
    private String updateUserId;

    /** 更新日 */
    @Column(name = "update_date")
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        setDefault(this);
    }

    @PreUpdate
    public void onPreUpdate() {
        setDefault(this);
    }

    public static void setDefault(final FeatureFlag entity) {
        if (entity.createDate == null) {
            entity.createDate = new Date();
        }
        entity.updateDate = new Date();
        if (entity.status == null) {
            entity.status = FeatureStatus.STABLE;
        }
        if (entity.inDevelopment == null) {
            entity.inDevelopment = false;
        }
        if (entity.enabledByDefault == null) {
            entity.enabledByDefault = true;
        }
        if (entity.rolloutPercentage == null) {
            entity.rolloutPercentage = 100;
        }
        if (entity.licenseResolveStrategy == null) {
            entity.licenseResolveStrategy = LicenseResolveStrategy.TENANT_PRIORITY;
        }
        if (entity.deleteFlag == null) {
            entity.deleteFlag = false;
        }
    }

    /**
     * 機能ステータス
     */
    public enum FeatureStatus {
        /** 安定版 */
        STABLE,
        /** ベータ版 */
        BETA,
        /** アルファ版 */
        ALPHA,
        /** 計画中 */
        PLANNING,
        /** 非推奨 */
        DEPRECATED
    }

    /**
     * ライセンス判定戦略
     * テナントライセンスとユーザーライセンスのどちらを優先するかを制御
     */
    public enum LicenseResolveStrategy {
        /** テナントライセンスを優先（B2B向け） */
        TENANT_PRIORITY,
        /** ユーザーライセンスを優先（B2C向け） */
        USER_PRIORITY,
        /** テナントライセンスのみ参照 */
        TENANT_ONLY,
        /** ユーザーライセンスのみ参照 */
        USER_ONLY,
        /** どちらか一方でも有効なら許可 */
        EITHER
    }
}
