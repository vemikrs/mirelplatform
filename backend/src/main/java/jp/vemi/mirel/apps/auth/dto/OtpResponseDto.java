/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.auth.dto;

import lombok.*;

/**
 * OTPレスポンスDTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResponseDto {
    
    /**
     * リクエストID
     */
    private String requestId;
    
    /**
     * メッセージ
     */
    private String message;
    
    /**
     * 有効期限（分）
     */
    private Integer expirationMinutes;
}
