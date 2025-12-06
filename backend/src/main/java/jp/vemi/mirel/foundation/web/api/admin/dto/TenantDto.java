/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import jp.vemi.mirel.foundation.feature.tenant.dto.TenantPlan;
import jp.vemi.mirel.foundation.feature.tenant.dto.TenantStatus;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class TenantDto {
    private String tenantId;
    private String tenantName;
    private String domain;
    private TenantPlan plan;
    private TenantStatus status;
    private String adminUser;
    private Integer userCount;
    private Date createdAt;
}
