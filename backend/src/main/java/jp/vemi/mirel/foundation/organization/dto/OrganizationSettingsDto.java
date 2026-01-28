/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import java.util.Map;

import lombok.Data;

/**
 * 組織設定DTO.
 */
@Data
public class OrganizationSettingsDto {
    private String id;
    private String organizationId;
    private String periodCode;
    private Boolean allowFlexibleSchedule;
    private Boolean requireApproval;
    private Integer maxMemberCount;
    private Map<String, Object> extendedSettings;
}
