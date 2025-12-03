/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * トークンポーリングレスポンスDTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceTokenResponse {
    
    /**
     * ステータス: pending, authorized, denied, expired
     */
    private String status;
    
    /**
     * アクセストークン（認証完了時のみ）
     */
    @JsonProperty("access_token")
    private String accessToken;
    
    /**
     * トークンタイプ（認証完了時のみ）
     */
    @JsonProperty("token_type")
    private String tokenType;
    
    /**
     * 有効期限（秒）（認証完了時のみ）
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;
    
    /**
     * ユーザー情報（認証完了時のみ）
     */
    private UserInfo user;
    
    /**
     * エラーコード
     */
    private String error;
    
    /**
     * エラー説明
     */
    @JsonProperty("error_description")
    private String errorDescription;
    
    /**
     * ユーザー情報DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String email;
        private String name;
    }
    
    /**
     * ペンディング状態のレスポンスを生成
     */
    public static DeviceTokenResponse pending() {
        return DeviceTokenResponse.builder()
                .status("pending")
                .build();
    }
    
    /**
     * 拒否状態のレスポンスを生成
     */
    public static DeviceTokenResponse denied() {
        return DeviceTokenResponse.builder()
                .status("denied")
                .build();
    }
    
    /**
     * 期限切れ状態のレスポンスを生成
     */
    public static DeviceTokenResponse expired() {
        return DeviceTokenResponse.builder()
                .status("expired")
                .build();
    }
    
    /**
     * レート制限エラーのレスポンスを生成
     */
    public static DeviceTokenResponse slowDown() {
        return DeviceTokenResponse.builder()
                .error("slow_down")
                .errorDescription("Polling too frequently. Please wait before retrying.")
                .build();
    }
}
