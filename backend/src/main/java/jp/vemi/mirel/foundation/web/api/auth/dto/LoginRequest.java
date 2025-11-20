/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import lombok.Data;

/**
 * ログインリクエストDTO.
 */
@Data
public class LoginRequest {
    private String userId;
    private String password;
    private String tenantId; // オプション: 初回テナント指定
}
