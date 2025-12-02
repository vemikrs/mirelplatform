/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.security.license;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;

/**
 * ライセンス要求例外.
 * 必要なライセンスがない場合にスローされる
 */
public class LicenseRequiredException extends RuntimeException {

    private final String applicationId;
    private final LicenseTier requiredTier;

    public LicenseRequiredException(String applicationId, LicenseTier requiredTier) {
        super(String.format("License required: application=%s, tier=%s", applicationId, requiredTier));
        this.applicationId = applicationId;
        this.requiredTier = requiredTier;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public LicenseTier getRequiredTier() {
        return requiredTier;
    }
}
