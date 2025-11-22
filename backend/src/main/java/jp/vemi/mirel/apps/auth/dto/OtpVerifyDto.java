/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.auth.dto;

import lombok.*;

/**
 * OTP検証DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerifyDto {
    
    /**
     * メールアドレス
     */
    private String email;
    
    /**
     * OTPコード（6桁）
     */
    private String otpCode;
    
    /**
     * 用途 (LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION)
     */
    private String purpose;
}
