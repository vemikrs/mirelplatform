/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import java.time.LocalDate;

import jp.vemi.mirel.foundation.organization.model.PositionType;
import lombok.Data;

/**
 * ユーザー所属DTO.
 */
@Data
public class UserOrganizationDto {
    private String id;
    private String userId;
    private String organizationId; // 旧: unitId
    private PositionType positionType;
    private String role; // 旧: isManager
    private String jobTitle;
    private Integer jobGrade;
    private Boolean canApprove;
    private LocalDate startDate;
    private LocalDate endDate;

    // 拡張情報（表示用）
    private String organizationName; // 旧: unitName
    private String organizationCode; // 旧: unitCode
}
