/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.organization.dto.CompanySettingsDto;
import jp.vemi.mirel.foundation.organization.model.CompanySettings;
import jp.vemi.mirel.foundation.organization.repository.CompanySettingsRepository;
import lombok.RequiredArgsConstructor;

/**
 * 会社設定サービス.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CompanySettingsService {

    private final CompanySettingsRepository companySettingsRepository;

    /**
     * 組織IDで会社設定一覧を取得.
     */
    @Transactional(readOnly = true)
    public List<CompanySettingsDto> findByOrganizationId(String organizationId) {
        return companySettingsRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 組織IDと期間コードで会社設定を取得.
     */
    @Transactional(readOnly = true)
    public Optional<CompanySettingsDto> findByOrganizationIdAndPeriodCode(String organizationId, String periodCode) {
        return companySettingsRepository.findByOrganizationIdAndPeriodCode(organizationId, periodCode)
                .map(this::toDto);
    }

    /**
     * 組織IDで最新の会社設定を取得.
     */
    @Transactional(readOnly = true)
    public Optional<CompanySettingsDto> findLatestByOrganizationId(String organizationId) {
        return companySettingsRepository.findFirstByOrganizationIdAndPeriodCodeIsNullOrderByCreateDateDesc(organizationId)
                .map(this::toDto);
    }

    /**
     * 会社設定を作成.
     */
    public CompanySettingsDto create(CompanySettingsDto dto) {
        CompanySettings entity = new CompanySettings();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setPeriodCode(dto.getPeriodCode());
        entity.setFiscalYearStart(dto.getFiscalYearStart());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setTimezone(dto.getTimezone());
        entity.setLocale(dto.getLocale());

        companySettingsRepository.save(entity);
        return toDto(entity);
    }

    /**
     * 会社設定を更新.
     */
    public Optional<CompanySettingsDto> update(String id, CompanySettingsDto dto) {
        return companySettingsRepository.findById(id).map(entity -> {
            entity.setFiscalYearStart(dto.getFiscalYearStart());
            entity.setCurrencyCode(dto.getCurrencyCode());
            entity.setTimezone(dto.getTimezone());
            entity.setLocale(dto.getLocale());
            companySettingsRepository.save(entity);
            return toDto(entity);
        });
    }

    private CompanySettingsDto toDto(CompanySettings entity) {
        CompanySettingsDto dto = new CompanySettingsDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setPeriodCode(entity.getPeriodCode());
        dto.setFiscalYearStart(entity.getFiscalYearStart());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setTimezone(entity.getTimezone());
        dto.setLocale(entity.getLocale());
        return dto;
    }
}
