/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import jp.vemi.mirel.foundation.organization.model.ApproverType;
import lombok.Data;

@Data
public class ApprovalStep {
    private ApproverType type;
    private String approverUserId;
    private String approverName;
    private String unitId;
    private String unitName;
}
