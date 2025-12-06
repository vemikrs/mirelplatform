/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.feature.tenant.dto;

/**
 * テナントプラン.
 */
public enum TenantPlan {
    ENTERPRISE("Enterprise"), PROFESSIONAL("Professional"), STARTER("Starter");

    private final String label;

    TenantPlan(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
