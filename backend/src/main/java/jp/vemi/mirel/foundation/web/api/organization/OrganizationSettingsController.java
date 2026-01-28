/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.organization;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.organization.dto.CompanySettingsDto;
import jp.vemi.mirel.foundation.organization.dto.OrganizationSettingsDto;
import jp.vemi.mirel.foundation.organization.service.CompanySettingsService;
import jp.vemi.mirel.foundation.organization.service.OrganizationSettingsService;
import lombok.RequiredArgsConstructor;

/**
 * 組織設定コントローラー.
 */
@RestController
@RequestMapping("/api/admin/organizations/{organizationId}/settings")
@RequiredArgsConstructor
public class OrganizationSettingsController {

    private final CompanySettingsService companySettingsService;
    private final OrganizationSettingsService organizationSettingsService;

    // === 会社設定 ===

    /**
     * 会社設定一覧を取得.
     */
    @GetMapping("/company")
    public List<CompanySettingsDto> getCompanySettings(@PathVariable String organizationId) {
        return companySettingsService.findByOrganizationId(organizationId);
    }

    /**
     * 会社設定を作成.
     */
    @PostMapping("/company")
    public CompanySettingsDto createCompanySettings(
            @PathVariable String organizationId,
            @RequestBody CompanySettingsDto dto) {
        dto.setOrganizationId(organizationId);
        return companySettingsService.create(dto);
    }

    /**
     * 会社設定を更新.
     */
    @PutMapping("/company/{settingsId}")
    public Optional<CompanySettingsDto> updateCompanySettings(
            @PathVariable String organizationId,
            @PathVariable String settingsId,
            @RequestBody CompanySettingsDto dto) {
        return companySettingsService.update(settingsId, dto);
    }

    // === 組織設定 ===

    /**
     * 組織設定一覧を取得.
     */
    @GetMapping
    public List<OrganizationSettingsDto> getOrganizationSettings(@PathVariable String organizationId) {
        return organizationSettingsService.findByOrganizationId(organizationId);
    }

    /**
     * 期間コード指定で組織設定を取得.
     */
    @GetMapping("/period")
    public Optional<OrganizationSettingsDto> getOrganizationSettingsByPeriod(
            @PathVariable String organizationId,
            @RequestParam String periodCode) {
        return organizationSettingsService.findByOrganizationIdAndPeriodCode(organizationId, periodCode);
    }

    /**
     * 組織設定を作成.
     */
    @PostMapping
    public OrganizationSettingsDto createOrganizationSettings(
            @PathVariable String organizationId,
            @RequestBody OrganizationSettingsDto dto) {
        dto.setOrganizationId(organizationId);
        return organizationSettingsService.create(dto);
    }

    /**
     * 組織設定を更新.
     */
    @PutMapping("/{settingsId}")
    public Optional<OrganizationSettingsDto> updateOrganizationSettings(
            @PathVariable String organizationId,
            @PathVariable String settingsId,
            @RequestBody OrganizationSettingsDto dto) {
        return organizationSettingsService.update(settingsId, dto);
    }
}
