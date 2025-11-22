/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * テナント情報DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoDto {
    private String tenantId;
    private String tenantName;
    private String displayName;
    private String roleInTenant;
    private Boolean isDefault;
}
