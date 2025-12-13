/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ユーザーテナント割り当てリクエスト.
 */
@Data
public class UserTenantAssignmentRequest {
    @NotEmpty
    @Valid
    private List<TenantAssignment> tenants;

    @Data
    public static class TenantAssignment {
        @NotNull
        private String tenantId;

        @NotNull
        private String roleInTenant;

        @NotNull
        private Boolean isDefault;
    }
}
