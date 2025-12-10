/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.controller;

import jakarta.validation.Valid;
import jp.vemi.mirel.foundation.web.api.admin.dto.AdminUserDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.UpdateUserRequest;
import jp.vemi.mirel.foundation.web.api.admin.dto.UserListResponse;
import jp.vemi.mirel.foundation.web.api.admin.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理者用ユーザー管理APIコントローラ.
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * ユーザー一覧取得
     */
    @GetMapping
    public ResponseEntity<UserListResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active) {

        UserListResponse response = adminUserService.listUsers(page, size, q, role, active);
        return ResponseEntity.ok(response);
    }

    /**
     * ユーザー詳細取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUser(@PathVariable String id) {
        AdminUserDto user = adminUserService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * ユーザー更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable String id,
            @RequestBody UpdateUserRequest request) {
        AdminUserDto user = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    /**
     * ユーザー作成
     */
    @PostMapping
    public ResponseEntity<AdminUserDto> createUser(
            @Valid @RequestBody jp.vemi.mirel.foundation.web.api.admin.dto.CreateUserRequest request) {
        AdminUserDto user = adminUserService.createUser(request);
        return ResponseEntity.ok(user);
    }

    /**
     * ユーザー削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
