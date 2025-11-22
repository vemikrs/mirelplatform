/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.user.dto;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * ライセンス情報DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LicenseInfoDto {
    private String id;
    private SubjectType subjectType;
    private String subjectId;
    private String applicationId;
    private LicenseTier tier;
    private Instant grantedAt;
    private Instant expiresAt;
}
