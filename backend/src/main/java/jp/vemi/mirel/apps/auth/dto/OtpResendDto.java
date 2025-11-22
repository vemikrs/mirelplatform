/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.auth.dto;

import lombok.*;

/**
 * OTP再送信DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpResendDto {
    
    /**
     * メールアドレス
     */
    private String email;
    
    /**
     * 用途 (LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION)
     */
    private String purpose;
}
