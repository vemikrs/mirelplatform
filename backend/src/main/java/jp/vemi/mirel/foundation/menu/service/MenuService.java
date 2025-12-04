package jp.vemi.mirel.foundation.menu.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.foundation.menu.dto.MenuDto;
import jp.vemi.mirel.foundation.menu.entity.MenuEntity;
import jp.vemi.mirel.foundation.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final MenuRepository menuRepository;

    public List<MenuDto> getMenuTree() {
        List<MenuEntity> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
        return buildTree(allMenus);
    }

    private List<MenuDto> buildTree(List<MenuEntity> entities) {
        Map<String, MenuDto> dtoMap = entities.stream()
                .collect(Collectors.toMap(
                        MenuEntity::getMenuId,
                        this::toDto));

        List<MenuDto> roots = new ArrayList<>();

        for (MenuEntity entity : entities) {
            MenuDto dto = dtoMap.get(entity.getMenuId());
            String parentId = entity.getParentId();

            if (parentId == null || parentId.isEmpty()) {
                roots.add(dto);
            } else {
                MenuDto parent = dtoMap.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                } else {
                    // Orphaned node, treat as root or handle error
                    roots.add(dto);
                }
            }
        }
        return roots;
    }

    private MenuDto toDto(MenuEntity entity) {
        return MenuDto.builder()
                .id(entity.getMenuId())
                .label(entity.getLabel())
                .path(entity.getPath())
                .icon(entity.getIcon())
                .parentId(entity.getParentId())
                .sortOrder(entity.getSortOrder())
                .requiredPermission(entity.getRequiredPermission())
                .description(entity.getDescription())
                .build();
    }
}
