package jp.vemi.mirel.foundation.menu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.menu.dto.MenuDto;
import jp.vemi.mirel.foundation.menu.service.MenuService;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
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
            roles = java.util.Arrays.asList(user.getRoles().split("\\|"));
        }
        return menuService.getMenuTree(roles);
    }
}
