/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.feature.TenantContext;
import jp.vemi.mirel.foundation.organization.dto.OrganizationDto;
import jp.vemi.mirel.foundation.organization.model.CompanySettings;
import jp.vemi.mirel.foundation.organization.model.Organization;
import jp.vemi.mirel.foundation.organization.model.OrganizationType;
import jp.vemi.mirel.foundation.organization.repository.CompanySettingsRepository;
import jp.vemi.mirel.foundation.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;

/**
 * 組織サービス（統合版）.
 * 旧 OrganizationService と OrganizationUnitService のロジックを統合。
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CompanySettingsRepository companySettingsRepository;

    /**
     * 組織一覧を取得します.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> findAll() {
        return organizationRepository.findByTenantId(TenantContext.getTenantId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * テナントIDで組織一覧を取得します.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> findByTenantId(String tenantId) {
        return organizationRepository.findByTenantId(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 組織ツリーを取得します.
     * 
     * @param tenantId テナントID
     * @return ルートノードのリスト（通常は1つだが、複数ルートも許容）
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getTree(String tenantId) {
        // 全ノード取得
        List<Organization> allOrgs = organizationRepository.findByTenantId(tenantId);

        // DTOに変換
        List<OrganizationDto> allDtos = allOrgs.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // IDでマップ化
        Map<String, OrganizationDto> dtoMap = allDtos.stream()
                .collect(Collectors.toMap(OrganizationDto::getId, dto -> dto));

        // ツリー構築
        List<OrganizationDto> roots = new ArrayList<>();
        for (OrganizationDto dto : allDtos) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                OrganizationDto parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                } else {
                    // 親が見つからない場合はルート扱い
                    roots.add(dto);
                }
            }
        }

        return roots;
    }

    /**
     * 組織を作成します.
     * type=COMPANY の場合は CompanySettings も同時作成。
     */
    public OrganizationDto create(OrganizationDto dto) {
        Organization entity = new Organization();
        entity.setId(UUID.randomUUID().toString());
        entity.setTenantId(TenantContext.getTenantId());
        entity.setParentId(dto.getParentId());
        entity.setName(dto.getName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setCode(dto.getCode());
        entity.setType(dto.getType());
        entity.setLevel(dto.getLevel() != null ? dto.getLevel() : calculateLevel(dto.getParentId()));
        entity.setPath(calculatePath(dto.getParentId(), entity.getId()));
        entity.setSortOrder(dto.getSortOrder());
        entity.setIsActive(true);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setPeriodCode(dto.getPeriodCode());

        organizationRepository.save(entity);

        // COMPANY の場合は CompanySettings を自動作成
        if (entity.getType() == OrganizationType.COMPANY) {
            CompanySettings settings = new CompanySettings();
            settings.setId(UUID.randomUUID().toString());
            settings.setOrganizationId(entity.getId());
            companySettingsRepository.save(settings);
        }

        return toDto(entity);
    }

    /**
     * 上位組織（祖先）を取得します.
     * 
     * @param organizationId 組織ID
     * @return 祖先ノードのリスト（近い順）
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getAncestors(String organizationId) {
        List<OrganizationDto> ancestors = new ArrayList<>();

        Organization current = organizationRepository.findById(organizationId).orElse(null);
        while (current != null && current.getParentId() != null) {
            Organization parent = organizationRepository.findById(current.getParentId()).orElse(null);
            if (parent != null) {
                ancestors.add(toDto(parent));
                current = parent;
            } else {
                break;
            }
        }

        return ancestors;
    }

    /**
     * 配下組織（子孫）を取得します.
     * 
     * @param organizationId 組織ID
     * @return 子孫ノードのリスト
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getDescendants(String organizationId) {
        Organization target = organizationRepository.findById(organizationId).orElse(null);
        if (target == null) {
            return new ArrayList<>();
        }

        // 同一テナント内の全ノードを取得
        List<Organization> allOrgs = organizationRepository.findByTenantId(target.getTenantId());

        // 子孫を抽出（再帰）
        List<OrganizationDto> descendants = new ArrayList<>();
        collectDescendants(organizationId, allOrgs, descendants);

        return descendants;
    }

    /**
     * パスを計算します.
     */
    public String calculatePath(String parentId, String currentId) {
        if (parentId == null) {
            return "/" + currentId;
        }
        Organization parent = organizationRepository.findById(parentId).orElse(null);
        if (parent != null && parent.getPath() != null) {
            return parent.getPath() + "/" + currentId;
        }
        return "/" + currentId;
    }

    private int calculateLevel(String parentId) {
        if (parentId == null) {
            return 0;
        }
        Organization parent = organizationRepository.findById(parentId).orElse(null);
        if (parent != null && parent.getLevel() != null) {
            return parent.getLevel() + 1;
        }
        return 0;
    }

    private void collectDescendants(String parentId, List<Organization> allOrgs, List<OrganizationDto> result) {
        List<Organization> children = allOrgs.stream()
                .filter(o -> parentId.equals(o.getParentId()))
                .collect(Collectors.toList());

        for (Organization child : children) {
            result.add(toDto(child));
            collectDescendants(child.getId(), allOrgs, result);
        }
    }

    private OrganizationDto toDto(Organization entity) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(entity.getId());
        dto.setTenantId(entity.getTenantId());
        dto.setParentId(entity.getParentId());
        dto.setName(entity.getName());
        dto.setDisplayName(entity.getDisplayName());
        dto.setCode(entity.getCode());
        dto.setType(entity.getType());
        dto.setPath(entity.getPath());
        dto.setLevel(entity.getLevel());
        dto.setSortOrder(entity.getSortOrder());
        dto.setIsActive(entity.getIsActive());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setPeriodCode(entity.getPeriodCode());
        return dto;
    }
}
