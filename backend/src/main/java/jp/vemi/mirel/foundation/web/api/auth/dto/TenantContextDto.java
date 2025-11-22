/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * テナントコンテキストDTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantContextDto {
    private String tenantId;
    private String tenantName;
    private String displayName;
}
