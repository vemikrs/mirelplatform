/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import jp.vemi.mirel.foundation.organization.model.ApproverType;
import lombok.Data;

/**
 * 承認ステップDTO.
 */
@Data
public class ApprovalStep {
    private ApproverType type;
    private String approverUserId;
    private String approverName;
    private String organizationId; // 旧: unitId
    private String organizationName; // 旧: unitName
}
