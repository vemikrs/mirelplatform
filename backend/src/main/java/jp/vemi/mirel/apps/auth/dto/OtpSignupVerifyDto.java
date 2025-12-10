/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * OTPサインアップ検証DTO.
 * OTP検証とサインアップデータを同時に送信
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpSignupVerifyDto {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be 6 digits")
    private String otpCode;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Display name is required")
    private String displayName;
    
    private String firstName;
    private String lastName;
}
