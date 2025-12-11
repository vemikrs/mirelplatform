/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * ユーザーテナント割り当てリクエスト.
 */
@Data
public class UserTenantAssignmentRequest {
    private List<TenantAssignment> tenants;

    @Data
    public static class TenantAssignment {
        private String tenantId;
        private String roleInTenant;
        private Boolean isDefault;
    }
}
