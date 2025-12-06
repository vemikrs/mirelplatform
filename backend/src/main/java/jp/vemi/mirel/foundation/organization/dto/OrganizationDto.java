/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import lombok.Data;

@Data
public class OrganizationDto {
    private String organizationId;
    private String name;
    private String code;
    private String description;
    private Integer fiscalYearStart;
    private Boolean isActive;
}
