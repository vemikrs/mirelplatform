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

import jp.vemi.mirel.foundation.organization.dto.OrganizationUnitDto;
import jp.vemi.mirel.foundation.organization.model.OrganizationUnit;
import jp.vemi.mirel.foundation.organization.repository.OrganizationUnitRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationUnitService {

    private final OrganizationUnitRepository organizationUnitRepository;

    /**
     * 組織ツリーを取得します.
     * 
     * @param organizationId
     *            組織ID
     * @return ルートノードのリスト（通常は1つだが、複数ルートも許容）
     */
    @Transactional(readOnly = true)
    public List<OrganizationUnitDto> getTree(String organizationId) {
        // 全ノード取得
        List<OrganizationUnit> allUnits = organizationUnitRepository.findByOrganizationId(organizationId);

        // DTOに変換
        List<OrganizationUnitDto> allDtos = allUnits.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // IDでマップ化
        Map<String, OrganizationUnitDto> dtoMap = allDtos.stream()
                .collect(Collectors.toMap(OrganizationUnitDto::getUnitId, dto -> dto));

        // ツリー構築
        List<OrganizationUnitDto> roots = new ArrayList<>();
        for (OrganizationUnitDto dto : allDtos) {
            if (dto.getParentUnitId() == null) {
                roots.add(dto);
            } else {
                OrganizationUnitDto parent = dtoMap.get(dto.getParentUnitId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                } else {
                    // 親が見つからない場合はルート扱い（データ不整合の可能性あり）
                    roots.add(dto);
                }
            }
        }

        return roots;
    }

    /**
     * 組織ノードを作成します.
     * 
     * @param organizationId
     *            組織ID
     * @param dto
     *            組織ノードDTO
     * @return 作成された組織ノードDTO
     */
    public OrganizationUnitDto create(String organizationId, OrganizationUnitDto dto) {
        OrganizationUnit entity = new OrganizationUnit();
        entity.setUnitId(UUID.randomUUID().toString());
        entity.setOrganizationId(organizationId);
        entity.setParentUnitId(dto.getParentUnitId());
        entity.setName(dto.getName());
        entity.setCode(dto.getCode());
        entity.setUnitType(dto.getUnitType());
        entity.setLevel(dto.getLevel()); // 本来は親のレベル+1などで自動計算すべき
        entity.setSortOrder(dto.getSortOrder());
        entity.setIsActive(true);
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());

        organizationUnitRepository.save(entity);
        return toDto(entity);
    }

    /**
     * 上位組織（祖先）を取得します.
     * 
     * @param unitId
     *            組織ノードID
     * @return 祖先ノードのリスト（近い順）
     */
    @Transactional(readOnly = true)
    public List<OrganizationUnitDto> getAncestors(String unitId) {
        List<OrganizationUnitDto> ancestors = new ArrayList<>();

        OrganizationUnit current = organizationUnitRepository.findById(unitId).orElse(null);
        while (current != null && current.getParentUnitId() != null) {
            OrganizationUnit parent = organizationUnitRepository.findById(current.getParentUnitId()).orElse(null);
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
     * @param unitId
     *            組織ノードID
     * @return 子孫ノードのリスト
     */
    @Transactional(readOnly = true)
    public List<OrganizationUnitDto> getDescendants(String unitId) {
        OrganizationUnit target = organizationUnitRepository.findById(unitId).orElse(null);
        if (target == null) {
            return new ArrayList<>();
        }

        // 同一組織内の全ノードを取得
        List<OrganizationUnit> allUnits = organizationUnitRepository.findByOrganizationId(target.getOrganizationId());

        // 子孫を抽出（再帰）
        List<OrganizationUnitDto> descendants = new ArrayList<>();
        collectDescendants(unitId, allUnits, descendants);

        return descendants;
    }

    private void collectDescendants(String parentId, List<OrganizationUnit> allUnits,
            List<OrganizationUnitDto> result) {
        List<OrganizationUnit> children = allUnits.stream()
                .filter(u -> parentId.equals(u.getParentUnitId()))
                .collect(Collectors.toList());

        for (OrganizationUnit child : children) {
            result.add(toDto(child));
            collectDescendants(child.getUnitId(), allUnits, result);
        }
    }

    private OrganizationUnitDto toDto(OrganizationUnit entity) {
        OrganizationUnitDto dto = new OrganizationUnitDto();
        dto.setUnitId(entity.getUnitId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setParentUnitId(entity.getParentUnitId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setUnitType(entity.getUnitType());
        dto.setLevel(entity.getLevel());
        dto.setSortOrder(entity.getSortOrder());
        dto.setIsActive(entity.getIsActive());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        return dto;
    }
}
