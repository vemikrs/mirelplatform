/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.vemi.mirel.foundation.context.ExecutionContext;
import jp.vemi.mirel.foundation.web.api.auth.dto.*;
import jp.vemi.mirel.foundation.web.api.auth.service.AuthenticationServiceImpl;
import jp.vemi.mirel.foundation.web.api.auth.service.PasswordResetService;
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

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * ログイン
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        try {
            AuthenticationResponse response = authenticationService.login(request);

            // Set access token in HttpOnly cookie
            if (response.getTokens() != null) {
                setTokenCookies(httpResponse, response.getTokens());
            }

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
    public ResponseEntity<AuthenticationResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletResponse httpResponse) {
        try {
            AuthenticationResponse response = authenticationService.signup(request);

            // Set access token in HttpOnly cookie
            if (response.getTokens() != null) {
                setTokenCookies(httpResponse, response.getTokens());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * OAuth2サインアップ（既存のSystemUserにUserを紐付け）
     */
    @PostMapping("/signup/oauth2")
    public ResponseEntity<AuthenticationResponse> signupOAuth2(
            @Valid @RequestBody OAuth2SignupRequest request,
            HttpServletResponse httpResponse) {
        try {
            // 認証済み（SystemUserとして）であることを確認
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            AuthenticationResponse response = authenticationService.signupWithOAuth2(request, auth.getName());

            // Set access token in HttpOnly cookie
            if (response.getTokens() != null) {
                setTokenCookies(httpResponse, response.getTokens());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("OAuth2 signup failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * トークンリフレッシュ
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse httpResponse) {
        try {
            AuthenticationResponse response = authenticationService.refresh(request);

            // Set access token in HttpOnly cookie
            if (response.getTokens() != null) {
                setTokenCookies(httpResponse, response.getTokens());
            }

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
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            String refreshToken = request != null ? request.getRefreshToken() : null;
            authenticationService.logout(refreshToken);

            // Invalidate session
            if (httpRequest.getSession(false) != null) {
                httpRequest.getSession(false).invalidate();
            }

            // Clear cookies
            clearTokenCookies(httpResponse);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage());

            // Clear cookies even on error
            clearTokenCookies(httpResponse);

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

        try {
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
        } catch (Exception e) {
            logger.error("Failed to get current user context", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * ヘルスチェック用エンドポイント
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * パスワードリセット要求
     * トークンを生成し、メール送信の準備をする
     */
    @PostMapping("/password-reset-request")
    public ResponseEntity<String> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request,
            HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");

            String token = passwordResetService.requestPasswordReset(
                    request.getEmail(),
                    clientIp,
                    userAgent);

            // TODO: Send email with reset link containing token
            // For now, return token in response (development only)
            logger.info("Password reset requested for email: {}", request.getEmail());

            // In production, don't return the token, just success message
            return ResponseEntity.ok("Password reset email sent");

        } catch (IllegalArgumentException e) {
            // Don't reveal if user exists - always return success
            logger.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return ResponseEntity.ok("Password reset email sent");
        } catch (Exception e) {
            logger.error("Password reset request failed", e);
            return ResponseEntity.status(500).body("Error processing request");
        }
    }

    /**
     * パスワードリセット実行
     * トークンを検証して新しいパスワードを設定
     */
    @PostMapping("/password-reset")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody PasswordResetDto request) {
        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            logger.info("Password reset successful");
            return ResponseEntity.ok("Password reset successful");
        } catch (IllegalArgumentException e) {
            logger.error("Password reset failed: Invalid token");
            return ResponseEntity.status(400).body("Invalid or expired token");
        } catch (IllegalStateException e) {
            logger.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Password reset failed", e);
            return ResponseEntity.status(500).body("Error processing request");
        }
    }

    /**
     * トークン検証エンドポイント（オプショナル）
     * フロントエンドがトークンの有効性を事前確認するために使用
     */
    @GetMapping("/password-reset/verify")
    public ResponseEntity<Boolean> verifyResetToken(@RequestParam String token) {
        boolean isValid = passwordResetService.verifyToken(token);
        return ResponseEntity.ok(isValid);
    }

    /**
     * Set JWT tokens as HttpOnly cookies
     */
    private void setTokenCookies(HttpServletResponse response, TokenDto tokens) {
        // Access token cookie (HttpOnly, Secure in production)
        Cookie accessTokenCookie = new Cookie("accessToken", tokens.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // Set true in production with HTTPS
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60); // 1 hour
        response.addCookie(accessTokenCookie);

        // Refresh token cookie (HttpOnly, Secure in production)
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokens.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set true in production with HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7); // 7 days
        response.addCookie(refreshTokenCookie);

        logger.debug("JWT tokens set in HttpOnly cookies");
    }

    /**
     * Clear JWT token cookies
     */
    private void clearTokenCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);

        logger.debug("JWT token cookies cleared");
    }

    /**
     * クライアントIPアドレスを取得
     * プロキシ経由の場合も考慮
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
