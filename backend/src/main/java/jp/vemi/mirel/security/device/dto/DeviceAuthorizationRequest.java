/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ユーザーコード承認/拒否リクエストDTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAuthorizationRequest {
    
    /**
     * ユーザーコード
     */
    @JsonProperty("user_code")
    private String userCode;
}
