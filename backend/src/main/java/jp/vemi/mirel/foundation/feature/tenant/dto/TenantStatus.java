/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.feature.tenant.dto;

/**
 * テナントステータス.
 */
public enum TenantStatus {
    ACTIVE("Active"), SUSPENDED("Suspended");

    private final String label;

    TenantStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
