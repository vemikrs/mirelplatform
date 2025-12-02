/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.auth.dto;

import lombok.*;

/**
 * OTPリクエストDTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRequestDto {
    
    /**
     * メールアドレス
     */
    private String email;
    
    /**
     * 用途 (LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION)
     */
    private String purpose;
}
