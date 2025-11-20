/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.controller;

import jakarta.validation.Valid;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.web.api.auth.dto.*;
import jp.vemi.mirel.foundation.web.api.auth.service.AuthenticationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 認証APIコントローラ.
 */
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private AuthenticationServiceImpl authenticationService;

    /**
     * ログイン
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthenticationResponse response = authenticationService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * サインアップ
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthenticationResponse> signup(@Valid @RequestBody SignupRequest request) {
        try {
            AuthenticationResponse response = authenticationService.signup(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * トークンリフレッシュ
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            AuthenticationResponse response = authenticationService.refresh(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * ログアウト
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        try {
            String refreshToken = request != null ? request.getRefreshToken() : null;
            authenticationService.logout(refreshToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.ok().build(); // Always return 200 for logout
        }
    }

    /**
     * テナント切替
     */
    @PostMapping("/switch-tenant")
    public ResponseEntity<UserContextDto> switchTenant(@RequestBody SwitchTenantRequest request) {
        try {
            if (!executionContext.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            String userId = executionContext.getCurrentUserId();
            UserContextDto response = authenticationService.switchTenant(userId, request.getTenantId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Tenant switch failed: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    /**
     * 現在のユーザ情報を取得
     */
    @GetMapping("/me")
    public ResponseEntity<UserContextDto> getCurrentUser() {
        logger.info("GET /auth/me - Getting current user context");

        if (!executionContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        var user = executionContext.getCurrentUser();
        var tenant = executionContext.getCurrentTenant();

        UserContextDto response = UserContextDto.builder()
            .user(UserDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .build())
            .currentTenant(tenant != null ? TenantContextDto.builder()
                .tenantId(tenant.getTenantId())
                .tenantName(tenant.getTenantName())
                .displayName(tenant.getDisplayName())
                .build() : null)
            .build();

        logger.info("Returning user context for user: {}, tenant: {}", 
            user.getUserId(), tenant != null ? tenant.getTenantId() : "none");

        return ResponseEntity.ok(response);
    }

    /**
     * ヘルスチェック用エンドポイント
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
