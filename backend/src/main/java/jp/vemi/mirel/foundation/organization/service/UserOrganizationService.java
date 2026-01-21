/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.organization.dto.OrganizationDto;
import jp.vemi.mirel.foundation.organization.dto.UserOrganizationDto;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;
import jp.vemi.mirel.foundation.organization.repository.OrganizationRepository;
import jp.vemi.mirel.foundation.organization.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;

/**
 * ユーザー所属サービス.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserOrganizationService {

    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;

    /**
     * ユーザーの所属一覧を取得します.
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
     * @param organizationId 組織ID
     * @param includeSubOrgs 配下組織を含めるか
     * @return 所属DTOリスト
     */
    @Transactional(readOnly = true)
    public List<UserOrganizationDto> findMembers(String organizationId, boolean includeSubOrgs) {
        List<String> orgIds = new java.util.ArrayList<>();
        orgIds.add(organizationId);

        if (includeSubOrgs) {
            List<OrganizationDto> descendants = organizationService.getDescendants(organizationId);
            descendants.forEach(d -> orgIds.add(d.getId()));
        }

        return userOrganizationRepository.findByOrganizationIdIn(orgIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * ユーザーを組織に所属させます.
     */
    public UserOrganizationDto assignUser(String userId, UserOrganizationDto dto) {
        UserOrganization entity = new UserOrganization();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(userId);
        entity.setOrganizationId(dto.getOrganizationId());
        entity.setPositionType(dto.getPositionType());
        entity.setRole(dto.getRole());
        entity.setJobTitle(dto.getJobTitle());
        entity.setJobGrade(dto.getJobGrade());
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
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setPositionType(entity.getPositionType());
        dto.setRole(entity.getRole());
        dto.setJobTitle(entity.getJobTitle());
        dto.setJobGrade(entity.getJobGrade());
        dto.setCanApprove(entity.getCanApprove());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());

        // 組織情報を取得して設定
        organizationRepository.findById(entity.getOrganizationId()).ifPresent(org -> {
            dto.setOrganizationName(org.getName());
            dto.setOrganizationCode(org.getCode());
        });

        return dto;
    }
}
