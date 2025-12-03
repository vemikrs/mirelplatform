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
 * トークンポーリングリクエストDTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenRequest {
    
    /**
     * クライアントID
     */
    @JsonProperty("client_id")
    private String clientId;
    
    /**
     * デバイスコード
     */
    @JsonProperty("device_code")
    private String deviceCode;
}
