/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.controller;

import jp.vemi.mirel.foundation.web.api.admin.dto.AdminUserDto;
import jp.vemi.mirel.foundation.web.api.admin.dto.UpdateUserRequest;
import jp.vemi.mirel.foundation.web.api.admin.dto.UserListResponse;
import jp.vemi.mirel.foundation.web.api.admin.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

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

        try {
            UserListResponse response = adminUserService.listUsers(page, size, q, role, active);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to list users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ユーザー詳細取得
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> getUser(@PathVariable String id) {
        try {
            AdminUserDto user = adminUserService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            logger.error("Failed to get user: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get user: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ユーザー更新
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable String id,
            @RequestBody UpdateUserRequest request) {
        try {
            AdminUserDto user = adminUserService.updateUser(id, request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            logger.error("Failed to update user: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to update user: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ユーザー作成
     */
    @PostMapping
    public ResponseEntity<AdminUserDto> createUser(
            @RequestBody jp.vemi.mirel.foundation.web.api.admin.dto.CreateUserRequest request) {
        try {
            AdminUserDto user = adminUserService.createUser(request);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            logger.error("Failed to create user", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to create user", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ユーザー削除
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        try {
            adminUserService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            logger.error("Failed to delete user: {}", id, e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
