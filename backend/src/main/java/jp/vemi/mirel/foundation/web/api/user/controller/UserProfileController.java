/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.user.controller;

import jakarta.validation.Valid;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.web.api.user.dto.UpdatePasswordRequest;
import jp.vemi.mirel.foundation.web.api.user.dto.UpdateProfileRequest;
import jp.vemi.mirel.foundation.web.api.user.dto.UserProfileDto;
import jp.vemi.mirel.foundation.web.api.user.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ユーザープロフィールAPIコントローラ.
 */
@RestController
@RequestMapping("/users/me")
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private UserProfileService userProfileService;

    /**
     * 現在のユーザー情報取得
     */
    @GetMapping
    public ResponseEntity<UserProfileDto> getCurrentUser() {
        logger.info("GET /users/me - Getting current user profile");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            UserProfileDto profile = userProfileService.getUserProfile(userId);

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            logger.error("Failed to get current user profile", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * プロフィール更新
     */
    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        logger.info("PUT /users/me - Updating user profile");

        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = executionContext.getCurrentUserId();
        UserProfileDto profile = userProfileService.updateProfile(userId, request);

        logger.info("Profile updated successfully for user: {}", userId);

        return ResponseEntity.ok(profile);
    }

    /**
     * パスワード変更
     */
    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        logger.info("PUT /users/me/password - Updating password");

        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userId = executionContext.getCurrentUserId();

        try {
            userProfileService.updatePassword(userId, request);
            logger.info("Password updated successfully for user: {}", userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.error("Password update failed: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * 所属テナント一覧取得
     */
    @GetMapping("/tenants")
    public ResponseEntity<?> getUserTenants() {
        logger.info("GET /users/me/tenants - Getting user tenants");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            var tenants = userProfileService.getUserTenants(userId);

            return ResponseEntity.ok(tenants);
        } catch (Exception e) {
            logger.error("Failed to get user tenants", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 有効ライセンス一覧取得
     */
    @GetMapping("/licenses")
    public ResponseEntity<?> getUserLicenses() {
        logger.info("GET /users/me/licenses - Getting user licenses");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            String tenantId = executionContext.getCurrentTenantId();

            var licenses = userProfileService.getUserLicenses(userId, tenantId);

            return ResponseEntity.ok(licenses);
        } catch (Exception e) {
            logger.error("Failed to get user licenses", e);
            return ResponseEntity.status(500).build();
        }
    }
}
