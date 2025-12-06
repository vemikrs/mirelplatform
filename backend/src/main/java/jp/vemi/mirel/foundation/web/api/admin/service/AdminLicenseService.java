/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.SubjectType;
import jp.vemi.mirel.foundation.abst.dao.repository.ApplicationLicenseRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserTenantRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminLicenseService {

    private final ApplicationLicenseRepository licenseRepository;
    private final UserTenantRepository userTenantRepository;

    /**
     * ライセンス情報を取得します (System wide or detailed)
     */
    @Transactional(readOnly = true)
    public LicenseSummaryDto getLicenseSummary() {
        // TODO: Real logic would aggregate active licenses
        // For now, return a system-wide summary

        // Assuming system license is one where subjectType=TENANT and subjectId=SYSTEM
        // (hypothetically)
        // Or just using the first found active license

        List<ApplicationLicense> licenses = licenseRepository.findAll();

        ApplicationLicense mainLicense = licenses.stream()
                .findFirst()
                .orElse(null);

        String plan = (mainLicense != null) ? mainLicense.getTier().name() : "FREE";
        String expiry = (mainLicense != null && mainLicense.getExpiresAt() != null)
                ? mainLicense.getExpiresAt().toString()
                : "N/A";

        // Mock usage stats based on DB counts
        long totalUsers = userTenantRepository.count();
        long maxUsers = (mainLicense != null && mainLicense.getTier() == LicenseTier.MAX) ? 1000 : 100;

        return LicenseSummaryDto.builder()
                .plan(plan)
                .status("ACTIVE")
                .expiryDate(expiry)
                .maxUsers(maxUsers)
                .currentUsers(totalUsers)
                .maxStorage(1024L) // GB (Mock)
                .currentStorage(350L) // GB (Mock)
                .licenseKey("****-****-****-" + (mainLicense != null ? mainLicense.getId().substring(0, 4) : "XXXX"))
                .build();
    }

    /**
     * ライセンスキーを更新します (Mock)
     */
    @Transactional
    public void updateLicenseKey(String licenseKey) {
        // In reality, verify key signature, extract info, and save ApplicationLicense
        ApplicationLicense license = new ApplicationLicense();
        license.setSubjectType(SubjectType.TENANT);
        license.setSubjectId("SYSTEM"); // System wide
        license.setApplicationId("mirel-platform");
        license.setTier(LicenseTier.PRO);
        license.setGrantedAt(Instant.now());
        // license.setFeatures(...);

        licenseRepository.save(license);
    }

    @Data
    @Builder
    public static class LicenseSummaryDto {
        private String plan;
        private String status;
        private String expiryDate;
        private long maxUsers;
        private long currentUsers;
        private long maxStorage;
        private long currentStorage;
        private String licenseKey;
    }
}
