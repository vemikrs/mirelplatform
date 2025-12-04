/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.feature.TenantContext;
import jp.vemi.mirel.foundation.organization.dto.OrganizationDto;
import jp.vemi.mirel.foundation.organization.model.Organization;
import jp.vemi.mirel.foundation.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    /**
     * 組織一覧を取得します.
     * 
     * @return 組織DTOリスト
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> findAll() {
        // テナントフィルタリングが必要な場合はここで実装する
        // 現状は全件取得としているが、本来はTenantContext.getTenantId()でフィルタリングすべき
        return organizationRepository.findAll().stream()
                .filter(org -> org.getTenantId().equals(TenantContext.getTenantId()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 組織を作成します.
     * 
     * @param dto
     *            組織DTO
     * @return 作成された組織DTO
     */
    public OrganizationDto create(OrganizationDto dto) {
        Organization entity = new Organization();
        entity.setOrganizationId(UUID.randomUUID().toString());
        entity.setTenantId(TenantContext.getTenantId());
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setDescription(dto.getDescription());
        entity.setFiscalYearStart(dto.getFiscalYearStart());
        entity.setIsActive(true);

        organizationRepository.save(entity);
        return toDto(entity);
    }

    private OrganizationDto toDto(Organization entity) {
        OrganizationDto dto = new OrganizationDto();
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setFiscalYearStart(entity.getFiscalYearStart());
        dto.setIsActive(entity.getIsActive());
        return dto;
    }
}
