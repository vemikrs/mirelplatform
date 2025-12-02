/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service;

import jp.vemi.mirel.foundation.abst.dao.entity.OtpAuditLog;
import jp.vemi.mirel.foundation.abst.dao.entity.OtpToken;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.repository.OtpAuditLogRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.OtpTokenRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jp.vemi.mirel.foundation.config.AppProperties;
import jp.vemi.mirel.foundation.config.OtpProperties;
import jp.vemi.mirel.foundation.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OtpService単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService単体テスト")
class OtpServiceTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private OtpAuditLogRepository otpAuditLogRepository;

    @Mock
    private SystemUserRepository systemUserRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private OtpProperties otpProperties;

    @Mock
    private RateLimitProperties rateLimitProperties;

    @Mock
    private AppProperties appProperties;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private OtpService otpService;

    private SystemUser testUser;
    private String testEmail;
    private String testIp;
    private String testUserAgent;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testIp = "192.168.1.1";
        testUserAgent = "Mozilla/5.0";

        testUser = new SystemUser();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(testEmail);

        // デフォルトプロパティ設定
        lenient().when(otpProperties.getExpirationMinutes()).thenReturn(5);
        lenient().when(otpProperties.getMaxAttempts()).thenReturn(5);
        lenient().when(otpProperties.getResendCooldownSeconds()).thenReturn(60);

        // RateLimitProperties のクラス名変更に追従 (OtpRateLimitConfig -> OtpRateLimit)
        RateLimitProperties.OtpRateLimit otpConfig = new RateLimitProperties.OtpRateLimit();
        otpConfig.setRequestPerMinute(3);
        otpConfig.setVerifyPerMinute(10);
        lenient().when(rateLimitProperties.getOtp()).thenReturn(otpConfig);

        // AppProperties設定
        lenient().when(appProperties.getBaseUrl()).thenReturn("http://localhost:5173");
        lenient().when(appProperties.getDomain()).thenReturn("localhost");
    }

    @Test
    @DisplayName("OTPリクエスト成功: 新規トークン生成とメール送信")
    void testRequestOtp_Success() {
        // Given: レート制限OK
        when(systemUserRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitService.isInCooldown(anyString())).thenReturn(false);

        // When: OTPリクエスト
        String requestId = otpService.requestOtp(testEmail, "LOGIN", testIp, testUserAgent);

        // Then: リクエストID生成
        assertThat(requestId).isNotNull();

        // Then: OTPトークン保存
        ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
        verify(otpTokenRepository).save(tokenCaptor.capture());
        OtpToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getSystemUserId()).isEqualTo(testUser.getId());
        assertThat(savedToken.getPurpose()).isEqualTo("LOGIN");
        assertThat(savedToken.getOtpHash()).isNotNull();

        // Then: メール送信
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendTemplateEmail(eq(testEmail), anyString(), eq("otp-login"), variablesCaptor.capture());

        Map<String, Object> variables = variablesCaptor.getValue();
        assertThat(variables).containsEntry("domain", "localhost");
        assertThat(variables).containsKey("magicLink");
        assertThat(variables.get("magicLink").toString()).contains("http://localhost:5173/auth/otp-verify");
        assertThat(variables.get("magicLink").toString()).contains("email=" + testEmail);
        assertThat(variables.get("magicLink").toString()).contains("purpose=LOGIN");

        // Then: クールダウン設定
        verify(rateLimitService).setCooldown(eq("otp:cooldown:" + testEmail), eq(60));

        // Then: 監査ログ
        verify(otpAuditLogRepository).save(any(OtpAuditLog.class));
    }

    @Test
    @DisplayName("OTPリクエスト失敗: レート制限超過")
    void testRequestOtp_RateLimitExceeded() {
        // Given: レート制限超過
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(false);

        // When & Then: 例外発生
        assertThatThrownBy(() -> otpService.requestOtp(testEmail, "LOGIN", testIp, testUserAgent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("リクエスト制限");

        // Then: トークン保存されない
        verify(otpTokenRepository, never()).save(any());
        verify(emailService, never()).sendTemplateEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("OTPリクエスト失敗: クールダウン中")
    void testRequestOtp_InCooldown() {
        // Given: クールダウン中
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitService.isInCooldown(anyString())).thenReturn(true);

        // When & Then: 例外発生
        assertThatThrownBy(() -> otpService.requestOtp(testEmail, "LOGIN", testIp, testUserAgent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("しばらく待って");

        // Then: トークン保存されない
        verify(otpTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("OTP検証成功: 正しいコードで検証")
    void testVerifyOtp_Success() {
        // Given: 有効なOTPトークン
        String otpCode = "123456";
        OtpToken token = new OtpToken();
        token.setId(UUID.randomUUID());
        token.setSystemUserId(testUser.getId());
        token.setOtpHash(hashOtp(otpCode)); // 実装と同じハッシュ化が必要
        token.setPurpose("LOGIN");
        token.setMaxAttempts(5);
        token.setAttemptCount(0);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(systemUserRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(otpTokenRepository.findBySystemUserIdAndPurposeAndIsVerifiedAndExpiresAtAfter(
                eq(testUser.getId()), eq("LOGIN"), eq(false), any(LocalDateTime.class)))
                        .thenReturn(Optional.of(token));

        // When: OTP検証
        boolean result = otpService.verifyOtp(testEmail, otpCode, "LOGIN", testIp, testUserAgent);

        // Then: 検証成功
        assertThat(result).isTrue();
        assertThat(token.getIsVerified()).isTrue();
        assertThat(token.getVerifiedAt()).isNotNull();

        // Then: トークン保存
        verify(otpTokenRepository).save(token);

        // Then: レート制限クリア
        verify(rateLimitService).clearRateLimit("otp:verify:" + testEmail);
        verify(rateLimitService).clearRateLimit("otp:request:" + testEmail);
        verify(rateLimitService).clearRateLimit("otp:cooldown:" + testEmail);
    }

    @Test
    @DisplayName("OTP検証失敗: 無効なコード")
    void testVerifyOtp_InvalidCode() {
        // Given: 有効なトークンだが、コードが不一致
        OtpToken token = new OtpToken();
        token.setSystemUserId(testUser.getId());
        token.setOtpHash(hashOtp("123456"));
        token.setPurpose("LOGIN");
        token.setMaxAttempts(5);
        token.setAttemptCount(0);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(systemUserRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(otpTokenRepository.findBySystemUserIdAndPurposeAndIsVerifiedAndExpiresAtAfter(
                any(UUID.class), anyString(), anyBoolean(), any(LocalDateTime.class)))
                        .thenReturn(Optional.of(token));

        // When: 無効なコードで検証
        boolean result = otpService.verifyOtp(testEmail, "999999", "LOGIN", testIp, testUserAgent);

        // Then: 検証失敗
        assertThat(result).isFalse();
        assertThat(token.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("OTP検証失敗: トークン未検出")
    void testVerifyOtp_TokenNotFound() {
        // Given: トークンが見つからない
        when(systemUserRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(otpTokenRepository.findBySystemUserIdAndPurposeAndIsVerifiedAndExpiresAtAfter(
                any(UUID.class), anyString(), anyBoolean(), any(LocalDateTime.class)))
                        .thenReturn(Optional.empty());

        // When: OTP検証
        boolean result = otpService.verifyOtp(testEmail, "123456", "LOGIN", testIp, testUserAgent);

        // Then: 検証失敗
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("OTP検証失敗: 最大試行回数超過")
    void testVerifyOtp_MaxAttemptsExceeded() {
        // Given: 最大試行回数に達したトークン
        OtpToken token = new OtpToken();
        token.setSystemUserId(testUser.getId());
        token.setOtpHash(hashOtp("123456"));
        token.setPurpose("LOGIN");
        token.setMaxAttempts(5);
        token.setAttemptCount(5);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(systemUserRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(otpTokenRepository.findBySystemUserIdAndPurposeAndIsVerifiedAndExpiresAtAfter(
                any(UUID.class), anyString(), anyBoolean(), any(LocalDateTime.class)))
                        .thenReturn(Optional.of(token));

        // When: OTP検証
        boolean result = otpService.verifyOtp(testEmail, "123456", "LOGIN", testIp, testUserAgent);

        // Then: 検証失敗
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("OTP検証失敗: ユーザー未検出")
    void testVerifyOtp_UserNotFound() {
        // Given: ユーザーが見つからない
        when(systemUserRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);

        // When & Then: 例外発生
        assertThatThrownBy(() -> otpService.verifyOtp(testEmail, "123456", "LOGIN", testIp, testUserAgent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ユーザーが見つかりません");
    }

    @Test
    @DisplayName("OTP再送信: requestOtpを呼び出し")
    void testResendOtp() {
        // Given: レート制限OK
        when(systemUserRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(rateLimitService.checkRateLimit(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(rateLimitService.isInCooldown(anyString())).thenReturn(false);

        // When: OTP再送信
        String requestId = otpService.resendOtp(testEmail, "LOGIN", testIp, testUserAgent);

        // Then: requestOtpが呼ばれる
        assertThat(requestId).isNotNull();
        verify(otpTokenRepository).save(any(OtpToken.class));
        verify(emailService).sendTemplateEmail(eq(testEmail), anyString(), eq("otp-login"), anyMap());
    }

    @Test
    @DisplayName("期限切れトークン削除: スケジュールタスク")
    void testCleanupExpiredTokens() {
        // Given: 削除対象トークン
        when(otpTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(10);

        // When: クリーンアップ実行
        otpService.cleanupExpiredTokens();

        // Then: 削除処理が呼ばれる
        verify(otpTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("古い監査ログ削除: スケジュールタスク")
    void testCleanupOldAuditLogs() {
        // Given: 削除対象ログ
        when(otpAuditLogRepository.deleteByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(50);

        // When: クリーンアップ実行
        otpService.cleanupOldAuditLogs();

        // Then: 削除処理が呼ばれる
        verify(otpAuditLogRepository).deleteByCreatedAtBefore(any(LocalDateTime.class));
    }

    /**
     * OTPコードをSHA-256でハッシュ化（テスト用）
     * OtpService.hashOtp()と同じ実装
     */
    private String hashOtp(String otpCode) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otpCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256アルゴリズムが見つかりません", e);
        }
    }
}
