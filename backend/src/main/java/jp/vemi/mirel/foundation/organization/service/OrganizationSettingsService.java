/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.organization.dto.OrganizationSettingsDto;
import jp.vemi.mirel.foundation.organization.model.OrganizationSettings;
import jp.vemi.mirel.foundation.organization.repository.OrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;

/**
 * 組織設定サービス.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationSettingsService {

    private final OrganizationSettingsRepository organizationSettingsRepository;

    /**
     * 組織IDで組織設定一覧を取得.
     */
    @Transactional(readOnly = true)
    public List<OrganizationSettingsDto> findByOrganizationId(String organizationId) {
        return organizationSettingsRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 組織IDと期間コードで組織設定を取得.
     */
    @Transactional(readOnly = true)
    public Optional<OrganizationSettingsDto> findByOrganizationIdAndPeriodCode(String organizationId, String periodCode) {
        return organizationSettingsRepository.findByOrganizationIdAndPeriodCode(organizationId, periodCode)
                .map(this::toDto);
    }

    /**
     * 組織設定を作成.
     */
    public OrganizationSettingsDto create(OrganizationSettingsDto dto) {
        OrganizationSettings entity = new OrganizationSettings();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setPeriodCode(dto.getPeriodCode());
        entity.setAllowFlexibleSchedule(dto.getAllowFlexibleSchedule());
        entity.setRequireApproval(dto.getRequireApproval());
        entity.setMaxMemberCount(dto.getMaxMemberCount());
        entity.setExtendedSettings(dto.getExtendedSettings());

        organizationSettingsRepository.save(entity);
        return toDto(entity);
    }

    /**
     * 組織設定を更新.
     */
    public Optional<OrganizationSettingsDto> update(String id, OrganizationSettingsDto dto) {
        return organizationSettingsRepository.findById(id).map(entity -> {
            entity.setAllowFlexibleSchedule(dto.getAllowFlexibleSchedule());
            entity.setRequireApproval(dto.getRequireApproval());
            entity.setMaxMemberCount(dto.getMaxMemberCount());
            entity.setExtendedSettings(dto.getExtendedSettings());
            organizationSettingsRepository.save(entity);
            return toDto(entity);
        });
    }

    private OrganizationSettingsDto toDto(OrganizationSettings entity) {
        OrganizationSettingsDto dto = new OrganizationSettingsDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setPeriodCode(entity.getPeriodCode());
        dto.setAllowFlexibleSchedule(entity.getAllowFlexibleSchedule());
        dto.setRequireApproval(entity.getRequireApproval());
        dto.setMaxMemberCount(entity.getMaxMemberCount());
        dto.setExtendedSettings(entity.getExtendedSettings());
        return dto;
    }
}
