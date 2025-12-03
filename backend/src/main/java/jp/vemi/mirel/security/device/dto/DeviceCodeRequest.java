/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * デバイスコード発行リクエストDTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCodeRequest {
    
    /**
     * クライアントID
     */
    private String clientId;
    
    /**
     * 要求するスコープ
     */
    private String scope;
}
