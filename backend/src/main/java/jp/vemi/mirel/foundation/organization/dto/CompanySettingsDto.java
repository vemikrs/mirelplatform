/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.dto;

import lombok.Data;

/**
 * 会社設定DTO.
 */
@Data
public class CompanySettingsDto {
    private String id;
    private String organizationId;
    private String periodCode;
    private Integer fiscalYearStart;
    private String currencyCode;
    private String timezone;
    private String locale;
}
