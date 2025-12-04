package jp.vemi.mirel.foundation.menu.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.foundation.menu.dto.MenuDto;
import jp.vemi.mirel.foundation.menu.service.MenuService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    public ResponseEntity<List<MenuDto>> getMenuTree() {
        return ResponseEntity.ok(menuService.getMenuTree());
    }
}
