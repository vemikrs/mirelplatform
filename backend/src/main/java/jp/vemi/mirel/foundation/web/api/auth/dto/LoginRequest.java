/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * ログインリクエストDTO.
 */
@Data
public class LoginRequest {
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;

    private String tenantId; // オプション: 初回テナント指定

    /**
     * ログイン状態を永続化するか.
     * trueの場合、リフレッシュトークンの有効期限が延長される（例: 90日）。
     * falseまたはnullの場合、通常の有効期限（例: 24時間）。
     */
    private Boolean rememberMe;

    // Controller から注入される（バリデーション不要）
    private String ipAddress;
    private String userAgent;

    /**
     * toString()でパスワードをマスク
     */
    @Override
    public String toString() {
        return "LoginRequest(usernameOrEmail=" + usernameOrEmail
                + ", password=***MASKED***, tenantId=" + tenantId + ")";
    }
}
