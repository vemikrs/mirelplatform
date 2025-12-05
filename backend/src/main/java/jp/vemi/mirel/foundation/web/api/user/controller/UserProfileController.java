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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

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

    /**
     * メールアドレス変更（OTP検証済み前提）
     */
    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(@RequestBody Map<String, String> request) {
        logger.info("PUT /users/me/email - Updating email");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            String newEmail = request.get("email");

            if (newEmail == null || newEmail.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "メールアドレスは必須です"));
            }

            UserProfileDto profile = userProfileService.updateEmail(userId, newEmail);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            logger.error("Email update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update email", e);
            return ResponseEntity.status(500).body(Map.of("error", "メールアドレスの更新に失敗しました"));
        }
    }

    /**
     * アバター画像アップロード
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        logger.info("POST /users/me/avatar - Uploading avatar");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            String avatarUrl = userProfileService.uploadAvatar(userId, file);

            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl != null ? avatarUrl : ""));
        } catch (IllegalArgumentException e) {
            logger.error("Avatar upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to upload avatar", e);
            return ResponseEntity.status(500).body(Map.of("error", "アバターのアップロードに失敗しました"));
        }
    }

    /**
     * アバター画像削除
     */
    @DeleteMapping("/avatar")
    public ResponseEntity<?> deleteAvatar() {
        logger.info("DELETE /users/me/avatar - Deleting avatar");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            userProfileService.deleteAvatar(userId);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Failed to delete avatar", e);
            return ResponseEntity.status(500).body(Map.of("error", "アバターの削除に失敗しました"));
        }
    }

    /**
     * GitHub連携解除
     */
    @DeleteMapping("/oauth2/github")
    public ResponseEntity<?> unlinkGitHub() {
        logger.info("DELETE /users/me/oauth2/github - Unlinking GitHub");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            userProfileService.unlinkGitHub(userId);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalStateException e) {
            logger.error("GitHub unlink failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to unlink GitHub", e);
            return ResponseEntity.status(500).body(Map.of("error", "GitHub連携の解除に失敗しました"));
        }
    }

    /**
     * パスワードレスログイン有効化
     */
    @PostMapping("/passwordless")
    public ResponseEntity<?> enablePasswordless() {
        logger.info("POST /users/me/passwordless - Enabling passwordless login");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            userProfileService.enablePasswordlessLogin(userId);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalStateException e) {
            logger.error("Passwordless enable failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to enable passwordless login", e);
            return ResponseEntity.status(500).body(Map.of("error", "パスワードレスログインの有効化に失敗しました"));
        }
    }

    /**
     * パスワード設定（パスワードレスから通常に戻す）
     */
    @PostMapping("/password/set")
    public ResponseEntity<?> setPassword(@RequestBody Map<String, String> request) {
        logger.info("POST /users/me/password/set - Setting password");

        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            String newPassword = request.get("newPassword");

            if (newPassword == null || newPassword.length() < 8) {
                return ResponseEntity.badRequest().body(Map.of("error", "パスワードは8文字以上で入力してください"));
            }

            userProfileService.setPassword(userId, newPassword);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Failed to set password", e);
            return ResponseEntity.status(500).body(Map.of("error", "パスワードの設定に失敗しました"));
        }
    }
}
