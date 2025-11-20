/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.controller;

import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.web.api.auth.dto.*;
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
