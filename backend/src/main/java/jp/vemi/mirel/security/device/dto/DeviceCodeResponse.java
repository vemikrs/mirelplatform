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
 * デバイスコード発行レスポンスDTO。
 * RFC 8628 準拠のレスポンス形式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCodeResponse {
    
    /**
     * デバイスコード - CLIがポーリング時に使用
     */
    @JsonProperty("device_code")
    private String deviceCode;
    
    /**
     * ユーザーコード - ユーザーがブラウザで入力
     */
    @JsonProperty("user_code")
    private String userCode;
    
    /**
     * 認証ページURL
     */
    @JsonProperty("verification_uri")
    private String verificationUri;
    
    /**
     * 有効期限（秒）
     */
    @JsonProperty("expires_in")
    private int expiresIn;
    
    /**
     * ポーリング間隔（秒）
     */
    private int interval;
}
