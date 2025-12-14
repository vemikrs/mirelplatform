package jp.vemi.mirel.foundation.menu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.menu.dto.MenuDto;
import jp.vemi.mirel.foundation.menu.service.MenuService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final ExecutionContext executionContext;

    @GetMapping("/tree")
    public List<MenuDto> getMenuTree() {
        User user = executionContext.getCurrentUser();
        List<String> roles = new java.util.ArrayList<>();
        if (user != null && user.getRoles() != null && !user.getRoles().isEmpty()) {
            roles = java.util.Arrays.asList(user.getRoles().split("[,|]"));
        }
        return menuService.getMenuTree(roles);
    }

    @GetMapping("/{menuId}")
    public MenuDto getMenu(@PathVariable String menuId) {
        return menuService.getMenu(menuId);
    }

    @PostMapping
    public void createMenu(@RequestBody MenuDto menuDto) {
        menuService.createMenu(menuDto);
    }

    @PutMapping("/{menuId}")
    public void updateMenu(@PathVariable String menuId, @RequestBody MenuDto menuDto) {
        menuService.updateMenu(menuId, menuDto);
    }

    @DeleteMapping("/{menuId}")
    public void deleteMenu(@PathVariable String menuId) {
        menuService.deleteMenu(menuId);
    }

    @PutMapping("/tree")
    public void updateMenuTreeStructure(@RequestBody List<MenuDto> menuList) {
        menuService.batchUpdate(menuList);
    }
}
