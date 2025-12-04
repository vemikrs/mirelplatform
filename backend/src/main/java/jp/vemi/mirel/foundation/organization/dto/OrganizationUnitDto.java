/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import java.time.LocalDate;
import java.util.List;

import jp.vemi.mirel.foundation.organization.model.UnitType;
import lombok.Data;

@Data
public class OrganizationUnitDto {
    private String unitId;
    private String organizationId;
    private String parentUnitId;
    private String name;
    private String code;
    private UnitType unitType;
    private Integer level;
    private Integer sortOrder;
    private Boolean isActive;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    private List<OrganizationUnitDto> children;
}
