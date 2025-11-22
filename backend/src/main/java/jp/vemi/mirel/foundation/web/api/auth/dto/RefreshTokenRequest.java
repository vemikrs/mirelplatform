/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.dto;

import lombok.Data;

/**
 * トークンリフレッシュリクエストDTO.
 */
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
