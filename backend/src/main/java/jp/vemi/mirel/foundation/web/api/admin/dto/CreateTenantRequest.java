/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateTenantRequest {
    private String tenantName;
    private String domain;
    private String plan;
    private String adminEmail;
    private String adminPassword;
}
