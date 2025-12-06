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

    public List<MenuDto> getMenuTree(List<String> roles) {
        List<MenuEntity> allMenus = menuRepository.findAllByOrderBySortOrderAsc();
        List<MenuEntity> filtered = allMenus.stream()
                .filter(menu -> isAccessible(menu, roles))
                .collect(Collectors.toList());
        return buildTree(filtered);
    }

    public MenuDto getMenu(String menuId) {
        MenuEntity entity = menuRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("Menu not found: " + menuId));
        return toDto(entity);
    }

    public void createMenu(MenuDto menuDto) {
        if (menuRepository.existsById(menuDto.getId())) {
            throw new RuntimeException("Menu already exists: " + menuDto.getId());
        }
        MenuEntity entity = toEntity(menuDto);
        menuRepository.save(entity);
    }

    public void updateMenu(String menuId, MenuDto menuDto) {
        MenuEntity entity = menuRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("Menu not found: " + menuId));

        entity.setLabel(menuDto.getLabel());
        entity.setPath(menuDto.getPath());
        entity.setIcon(menuDto.getIcon());
        entity.setParentId(menuDto.getParentId());
        entity.setSortOrder(menuDto.getSortOrder());
        entity.setRequiredPermission(menuDto.getRequiredPermission());
        entity.setDescription(menuDto.getDescription());

        menuRepository.save(entity);
    }

    public void deleteMenu(String menuId) {
        if (!menuRepository.existsById(menuId)) {
            throw new RuntimeException("Menu not found: " + menuId);
        }
        // Check for children and prevent deletion if children exist
        List<MenuEntity> children = menuRepository.findByParentIdOrderBySortOrderAsc(menuId);
        if (!children.isEmpty()) {
            throw new RuntimeException("Cannot delete menu with children. Please delete or move children first.");
        }
        menuRepository.deleteById(menuId);
    }

    public void batchUpdate(List<MenuDto> menuList) {
        // Only update parentId and sortOrder for tree restructuring
        for (MenuDto dto : menuList) {
            MenuEntity entity = menuRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Menu not found: " + dto.getId()));
            entity.setParentId(dto.getParentId());
            entity.setSortOrder(dto.getSortOrder());
            menuRepository.save(entity);
        }
    }

    private boolean isAccessible(MenuEntity menu, List<String> roles) {
        if (menu.getRequiredPermission() == null || menu.getRequiredPermission().isEmpty()) {
            return true;
        }
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        String[] requiredPermissions = menu.getRequiredPermission().split("\\|");
        for (String permission : requiredPermissions) {
            if (roles.contains(permission.trim())) {
                return true;
            }
        }
        return false;
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

    private MenuEntity toEntity(MenuDto dto) {
        return MenuEntity.builder()
                .menuId(dto.getId())
                .label(dto.getLabel())
                .path(dto.getPath())
                .icon(dto.getIcon())
                .parentId(dto.getParentId())
                .sortOrder(dto.getSortOrder())
                .requiredPermission(dto.getRequiredPermission())
                .description(dto.getDescription())
                .build();
    }
}
