/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.auth.api;

import jakarta.servlet.http.HttpServletRequest;
import jp.vemi.mirel.apps.auth.dto.OtpRequestDto;
import jp.vemi.mirel.apps.auth.dto.OtpResendDto;
import jp.vemi.mirel.apps.auth.dto.OtpResponseDto;
import jp.vemi.mirel.apps.auth.dto.OtpVerifyDto;
import jp.vemi.mirel.foundation.config.OtpProperties;
import jp.vemi.mirel.foundation.service.OtpService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP認証コントローラー.
 * パスワードレス認証・パスワードリセット・メールアドレス検証用API
 */
@RestController
@RequestMapping("/auth/otp")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "otp.enabled", havingValue = "true")
public class OtpController {
    
    private final OtpService otpService;
    private final OtpProperties otpProperties;
    
    /**
     * OTPリクエスト
     * 
     * @param request リクエスト
     * @param httpRequest HTTPリクエスト
     * @return OTPレスポンス
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<OtpResponseDto>> requestOtp(
        @RequestBody ApiRequest<OtpRequestDto> request,
        HttpServletRequest httpRequest
    ) {
        OtpRequestDto dto = request.getModel();
        
        // バリデーション
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<OtpResponseDto>builder()
                    .errors(java.util.List.of("メールアドレスは必須です"))
                    .build());
        }
        
        if (dto.getPurpose() == null || dto.getPurpose().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<OtpResponseDto>builder()
                    .errors(java.util.List.of("用途は必須です"))
                    .build());
        }
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            String requestId = otpService.requestOtp(
                dto.getEmail(), 
                dto.getPurpose(), 
                ipAddress, 
                userAgent
            );
            
            OtpResponseDto response = OtpResponseDto.builder()
                .requestId(requestId)
                .message("認証コードをメールに送信しました")
                .expirationMinutes(otpProperties.getExpirationMinutes())
                .build();
            
            return ResponseEntity.ok(ApiResponse.<OtpResponseDto>builder()
                .data(response)
                .build());
                
        } catch (RuntimeException e) {
            log.error("OTPリクエスト失敗: email={}, error={}", dto.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<OtpResponseDto>builder()
                    .errors(java.util.List.of(e.getMessage()))
                    .build());
        }
    }
    
    /**
     * OTP検証
     * 
     * @param request リクエスト
     * @param httpRequest HTTPリクエスト
     * @return 検証結果
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Boolean>> verifyOtp(
        @RequestBody ApiRequest<OtpVerifyDto> request,
        HttpServletRequest httpRequest
    ) {
        OtpVerifyDto dto = request.getModel();
        
        // バリデーション
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Boolean>builder()
                    .errors(java.util.List.of("メールアドレスは必須です"))
                    .build());
        }
        
        if (dto.getOtpCode() == null || dto.getOtpCode().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Boolean>builder()
                    .errors(java.util.List.of("OTPコードは必須です"))
                    .build());
        }
        
        if (!dto.getOtpCode().matches("\\d{6}")) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Boolean>builder()
                    .errors(java.util.List.of("OTPコードは6桁の数字である必要があります"))
                    .build());
        }
        
        if (dto.getPurpose() == null || dto.getPurpose().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Boolean>builder()
                    .errors(java.util.List.of("用途は必須です"))
                    .build());
        }
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            boolean verified = otpService.verifyOtp(
                dto.getEmail(), 
                dto.getOtpCode(), 
                dto.getPurpose(), 
                ipAddress, 
                userAgent
            );
            
            if (verified) {
                return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .data(true)
                    .messages(java.util.List.of("認証に成功しました"))
                    .build());
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<Boolean>builder()
                        .data(false)
                        .errors(java.util.List.of("認証コードが正しくありません"))
                        .build());
            }
            
        } catch (RuntimeException e) {
            log.error("OTP検証失敗: email={}, error={}", dto.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<Boolean>builder()
                    .data(false)
                    .errors(java.util.List.of(e.getMessage()))
                    .build());
        }
    }
    
    /**
     * OTP再送信
     * 
     * @param request リクエスト
     * @param httpRequest HTTPリクエスト
     * @return OTPレスポンス
     */
    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<OtpResponseDto>> resendOtp(
        @RequestBody ApiRequest<OtpResendDto> request,
        HttpServletRequest httpRequest
    ) {
        OtpResendDto dto = request.getModel();
        
        // バリデーション
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<OtpResponseDto>builder()
                    .errors(java.util.List.of("メールアドレスは必須です"))
                    .build());
        }
        
        if (dto.getPurpose() == null || dto.getPurpose().isBlank()) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.<OtpResponseDto>builder()
                    .errors(java.util.List.of("用途は必須です"))
                    .build());
        }
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            String requestId = otpService.resendOtp(
                dto.getEmail(), 
                dto.getPurpose(), 
                ipAddress, 
                userAgent
            );
            
            OtpResponseDto response = OtpResponseDto.builder()
                .requestId(requestId)
                .message("認証コードを再送信しました")
                .expirationMinutes(otpProperties.getExpirationMinutes())
                .build();
            
            return ResponseEntity.ok(ApiResponse.<OtpResponseDto>builder()
                .data(response)
                .build());
                
        } catch (RuntimeException e) {
            log.error("OTP再送信失敗: email={}, error={}", dto.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.<OtpResponseDto>builder()
                    .errors(java.util.List.of(e.getMessage()))
                    .build());
        }
    }
    
    /**
     * クライアントIPアドレスを取得
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
