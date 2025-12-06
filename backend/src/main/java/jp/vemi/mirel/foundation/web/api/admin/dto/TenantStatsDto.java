/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * テナント統計情報DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStatsDto {
    /** 総テナント数 */
    private long totalTenants;
    /** アクティブテナント数 */
    private long activeTenants;
    /** 総ユーザー数 */
    private long totalUsers;
    /** アクティブユーザー数 */
    private long activeUsers;
}
