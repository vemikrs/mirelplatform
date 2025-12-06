/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.organization.dto.UserOrganizationDto;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;
import jp.vemi.mirel.foundation.organization.repository.OrganizationUnitRepository;
import jp.vemi.mirel.foundation.organization.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserOrganizationService {

    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final OrganizationUnitService organizationUnitService;

    /**
     * ユーザーの所属一覧を取得します.
     * 
     * @param userId
     *            ユーザーID
     * @return 所属DTOリスト
     */
    @Transactional(readOnly = true)
    public List<UserOrganizationDto> findByUserId(String userId) {
        return userOrganizationRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 組織のメンバーを取得します.
     * 
     * @param unitId
     *            組織ID
     * @param includeSubUnits
     *            配下組織を含めるか
     * @return 所属DTOリスト
     */
    @Transactional(readOnly = true)
    public List<UserOrganizationDto> findMembers(String unitId, boolean includeSubUnits) {
        List<String> unitIds = new java.util.ArrayList<>();
        unitIds.add(unitId);

        if (includeSubUnits) {
            List<jp.vemi.mirel.foundation.organization.dto.OrganizationUnitDto> descendants = organizationUnitService
                    .getDescendants(unitId);
            descendants.forEach(d -> unitIds.add(d.getUnitId()));
        }

        return userOrganizationRepository.findByUnitIdIn(unitIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * ユーザーを組織に所属させます.
     * 
     * @param userId
     *            ユーザーID
     * @param dto
     *            所属DTO
     * @return 作成された所属DTO
     */
    public UserOrganizationDto assignUser(String userId, UserOrganizationDto dto) {
        UserOrganization entity = new UserOrganization();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setUnitId(dto.getUnitId());
        entity.setPositionType(dto.getPositionType());
        entity.setJobTitle(dto.getJobTitle());
        entity.setJobGrade(dto.getJobGrade());
        entity.setIsManager(dto.getIsManager());
        entity.setCanApprove(dto.getCanApprove());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());

        userOrganizationRepository.save(entity);
        return toDto(entity);
    }

    private UserOrganizationDto toDto(UserOrganization entity) {
        UserOrganizationDto dto = new UserOrganizationDto();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUnitId(entity.getUnitId());
        dto.setPositionType(entity.getPositionType());
        dto.setJobTitle(entity.getJobTitle());
        dto.setJobGrade(entity.getJobGrade());
        dto.setIsManager(entity.getIsManager());
        dto.setCanApprove(entity.getCanApprove());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());

        // 組織情報を取得して設定
        organizationUnitRepository.findById(entity.getUnitId()).ifPresent(unit -> {
            dto.setUnitName(unit.getName());
            dto.setUnitCode(unit.getCode());
        });

        return dto;
    }
}
