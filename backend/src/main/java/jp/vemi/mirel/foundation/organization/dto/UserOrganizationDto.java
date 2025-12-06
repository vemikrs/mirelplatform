/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import java.time.LocalDate;

import jp.vemi.mirel.foundation.organization.model.PositionType;
import lombok.Data;

@Data
public class UserOrganizationDto {
    private String id;
    private String userId;
    private String unitId;
    private PositionType positionType;
    private String jobTitle;
    private Integer jobGrade;
    private Boolean isManager;
    private Boolean canApprove;
    private LocalDate startDate;
    private LocalDate endDate;

    // 拡張情報（表示用）
    private String unitName;
    private String unitCode;
}
