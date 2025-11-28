/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.FeatureStatus;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.LicenseResolveStrategy;
import lombok.Data;

/**
 * フィーチャーフラグ更新リクエスト.
 */
@Data
public class UpdateFeatureFlagRequest {

    private String featureName;
    private String description;
    private String applicationId;
    private FeatureStatus status;
    private Boolean inDevelopment;
    private LicenseTier requiredLicenseTier;
    private Boolean enabledByDefault;
    private String enabledForUserIds;
    private String enabledForTenantIds;
    private Integer rolloutPercentage;
    private LicenseResolveStrategy licenseResolveStrategy;
    private String metadata;
}
