/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import java.time.LocalDate;
import java.util.List;

import jp.vemi.mirel.foundation.organization.model.OrganizationType;
import lombok.Data;

/**
 * 組織DTO（統合版）.
 */
@Data
public class OrganizationDto {
    private String id;
    private String tenantId;
    private String parentId;
    private String name;
    private String displayName;
    private String code;
    private OrganizationType type;
    private String path;
    private Integer level;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodCode;

    // ツリー操作用
    private List<OrganizationDto> children;

    // 設定（オプション）
    private CompanySettingsDto companySettings;
    private OrganizationSettingsDto organizationSettings;
}
