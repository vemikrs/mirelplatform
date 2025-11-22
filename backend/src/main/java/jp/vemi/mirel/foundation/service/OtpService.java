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
import jp.vemi.mirel.foundation.config.OtpProperties;
import jp.vemi.mirel.foundation.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * OTPサービス.
 * ワンタイムパスワードの生成・検証・再送信を管理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    
    private final OtpTokenRepository otpTokenRepository;
    private final OtpAuditLogRepository otpAuditLogRepository;
    private final SystemUserRepository systemUserRepository;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final OtpProperties otpProperties;
    private final RateLimitProperties rateLimitProperties;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * OTPをリクエスト
     * 
     * @param email メールアドレス
     * @param purpose 用途 (LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION)
     * @param ipAddress リクエスト元IPアドレス
     * @param userAgent User Agent
     * @return リクエストID
     */
    @Transactional
    public String requestOtp(String email, String purpose, String ipAddress, String userAgent) {
        String requestId = java.util.UUID.randomUUID().toString();
        
        // レート制限チェック
        String rateLimitKey = "otp:request:" + email;
        int requestPerMinute = rateLimitProperties.getOtp().getRequestPerMinute();
        if (!rateLimitService.checkRateLimit(rateLimitKey, requestPerMinute, 60)) {
            logAudit(requestId, null, email, purpose, "REQUEST", false, 
                "レート制限超過", ipAddress, userAgent, 
                String.format("{\"limit\": %d, \"window\": 60}", requestPerMinute));
            throw new RuntimeException("リクエスト制限に達しました。しばらく待ってから再度お試しください。");
        }
        
        // クールダウンチェック
        String cooldownKey = "otp:cooldown:" + email;
        if (rateLimitService.isInCooldown(cooldownKey)) {
            logAudit(requestId, null, email, purpose, "REQUEST", false, 
                "クールダウン中", ipAddress, userAgent, null);
            throw new RuntimeException("しばらく待ってから再度お試しください。");
        }
        
        // SystemUser取得（新規ユーザーの場合は作成しない）
        SystemUser systemUser = systemUserRepository.findByEmail(email).orElse(null);
        
        // OTPコード生成
        String otpCode = generateOtpCode();
        String otpHash = hashOtp(otpCode);
        
        // 既存の未検証トークンを無効化
        if (systemUser != null) {
            otpTokenRepository.invalidatePreviousTokens(systemUser.getId(), purpose);
        }
        
        // OTPトークン保存
        OtpToken token = new OtpToken();
        if (systemUser != null) {
            token.setSystemUserId(systemUser.getId());
        } else {
            // 新規ユーザーの場合は仮ID（メール検証後に実SystemUserIdに更新）
            token.setSystemUserId(java.util.UUID.randomUUID());
        }
        token.setOtpHash(otpHash);
        token.setPurpose(purpose);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()));
        token.setMaxAttempts(otpProperties.getMaxAttempts());
        token.setRequestIp(ipAddress);
        token.setUserAgent(userAgent);
        otpTokenRepository.save(token);
        
        // メール送信
        sendOtpEmail(email, otpCode, purpose);
        
        // クールダウン設定
        rateLimitService.setCooldown(cooldownKey, otpProperties.getResendCooldownSeconds());
        
        // 監査ログ
        logAudit(requestId, systemUser != null ? systemUser.getId() : null, email, purpose, "REQUEST", true, 
            null, ipAddress, userAgent, null);
        
        log.info("OTPリクエスト成功: email={}, purpose={}, requestId={}", email, purpose, requestId);
        return requestId;
    }
    
    /**
     * OTPを検証
     * 
     * @param email メールアドレス
     * @param otpCode OTPコード
     * @param purpose 用途
     * @param ipAddress リクエスト元IPアドレス
     * @param userAgent User Agent
     * @return 検証成功の場合true
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode, String purpose, String ipAddress, String userAgent) {
        String requestId = java.util.UUID.randomUUID().toString();
        
        // レート制限チェック
        String rateLimitKey = "otp:verify:" + email;
        int verifyPerMinute = rateLimitProperties.getOtp().getVerifyPerMinute();
        if (!rateLimitService.checkRateLimit(rateLimitKey, verifyPerMinute, 60)) {
            logAudit(requestId, null, email, purpose, "VERIFY", false, 
                "検証レート制限超過", ipAddress, userAgent, 
                String.format("{\"limit\": %d, \"window\": 60}", verifyPerMinute));
            throw new RuntimeException("検証試行制限に達しました。しばらく待ってから再度お試しください。");
        }
        
        // SystemUser取得
        SystemUser systemUser = systemUserRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
        
        // 有効なOTPトークン取得
        OtpToken token = otpTokenRepository
            .findBySystemUserIdAndPurposeAndIsVerifiedAndExpiresAtAfter(
                systemUser.getId(), purpose, false, LocalDateTime.now())
            .orElse(null);
        
        if (token == null) {
            logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", false, 
                "トークンが見つからないか期限切れ", ipAddress, userAgent, null);
            return false;
        }
        
        // 試行回数チェック
        if (token.getAttemptCount() >= token.getMaxAttempts()) {
            logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", false, 
                "最大試行回数超過", ipAddress, userAgent, null);
            return false;
        }
        
        // OTPコード検証
        String otpHash = hashOtp(otpCode);
        token.incrementAttemptCount();
        
        if (!token.getOtpHash().equals(otpHash)) {
            otpTokenRepository.save(token);
            logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", false, 
                "OTPコード不一致", ipAddress, userAgent, null);
            return false;
        }
        
        // 検証成功
        token.setIsVerified(true);
        token.setVerifiedAt(LocalDateTime.now());
        otpTokenRepository.save(token);
        
        // レート制限クリア
        rateLimitService.clearRateLimit(rateLimitKey);
        rateLimitService.clearRateLimit("otp:request:" + email);
        rateLimitService.clearRateLimit("otp:cooldown:" + email);
        
        logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", true, 
            null, ipAddress, userAgent, null);
        
        log.info("OTP検証成功: email={}, purpose={}", email, purpose);
        return true;
    }
    
    /**
     * OTPを再送信
     * 
     * @param email メールアドレス
     * @param purpose 用途
     * @param ipAddress リクエスト元IPアドレス
     * @param userAgent User Agent
     * @return リクエストID
     */
    @Transactional
    public String resendOtp(String email, String purpose, String ipAddress, String userAgent) {
        log.info("OTP再送信リクエスト: email={}, purpose={}", email, purpose);
        return requestOtp(email, purpose, ipAddress, userAgent);
    }
    
    /**
     * 6桁のOTPコードを生成
     */
    private String generateOtpCode() {
        int code = secureRandom.nextInt(1000000);
        return String.format("%06d", code);
    }
    
    /**
     * OTPコードをSHA-256でハッシュ化
     */
    private String hashOtp(String otpCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otpCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256アルゴリズムが見つかりません", e);
        }
    }
    
    /**
     * OTPメールを送信
     */
    private void sendOtpEmail(String email, String otpCode, String purpose) {
        String templateName = switch (purpose) {
            case "LOGIN" -> "otp-login";
            case "PASSWORD_RESET" -> "otp-password-reset";
            case "EMAIL_VERIFICATION" -> "otp-email-verification";
            default -> throw new RuntimeException("不明なOTP用途: " + purpose);
        };
        
        String subject = switch (purpose) {
            case "LOGIN" -> "ログイン認証コード";
            case "PASSWORD_RESET" -> "パスワードリセット認証コード";
            case "EMAIL_VERIFICATION" -> "メールアドレス検証コード";
            default -> "認証コード";
        };
        
        Map<String, Object> variables = Map.of(
            "otpCode", otpCode,
            "expirationMinutes", otpProperties.getExpirationMinutes()
        );
        
        emailService.sendTemplateEmail(email, subject, templateName, variables);
    }
    
    /**
     * 監査ログを記録
     */
    private void logAudit(String requestId, java.util.UUID systemUserId, String email, 
                         String purpose, String action, Boolean success, 
                         String failureReason, String ipAddress, String userAgent, 
                         String rateLimitInfo) {
        OtpAuditLog log = new OtpAuditLog();
        log.setRequestId(requestId);
        log.setSystemUserId(systemUserId);
        log.setEmail(email);
        log.setPurpose(purpose);
        log.setAction(action);
        log.setSuccess(success);
        log.setFailureReason(failureReason);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setRateLimitInfo(rateLimitInfo);
        otpAuditLogRepository.save(log);
    }
    
    /**
     * 期限切れOTPトークンを削除（毎日3時実行）
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        int deleted = otpTokenRepository.deleteByExpiresAtBefore(cutoffDate);
        log.info("期限切れOTPトークン削除: {} 件", deleted);
    }
    
    /**
     * 古い監査ログを削除（毎日3時実行）
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        int deleted = otpAuditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("古い監査ログ削除: {} 件", deleted);
    }
}
