/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import lombok.Data;

/**
 * テナント切替リクエストDTO.
 */
@Data
public class SwitchTenantRequest {
    private String tenantId;
}
