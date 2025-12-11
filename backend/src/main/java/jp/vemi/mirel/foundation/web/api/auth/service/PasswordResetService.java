package jp.vemi.mirel.foundation.web.api.auth.service;

import jp.vemi.mirel.foundation.abst.dao.entity.PasswordResetToken;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.repository.PasswordResetTokenRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.config.AppProperties;
import jp.vemi.mirel.foundation.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * Password Reset Service
 * 
 * Handles password reset request and confirmation logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    
    private final SystemUserRepository systemUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AppProperties appProperties;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_EXPIRATION_HOURS = 1;
    
    /**
     * Request password reset - generates token and prepares for email sending
     * 
     * @param email User email
     * @param requestIp Request IP address
     * @param userAgent User agent
     * @return Plain token (to be sent via email)
     */
    @Transactional
    public String requestPasswordReset(String email, String requestIp, String userAgent) {
        // Find SystemUser by email
        SystemUser systemUser = systemUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Check if account is active and not locked
        if (!systemUser.getIsActive()) {
            throw new IllegalStateException("Account is not active");
        }
        if (systemUser.getAccountLocked()) {
            throw new IllegalStateException("Account is locked");
        }
        
        // Generate random token (32 bytes = 256 bits)
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        // Hash token with SHA-256
        String tokenHash = hashToken(plainToken);
        
        // Invalidate previous unused tokens for this user
        passwordResetTokenRepository.findBySystemUserId(systemUser.getId())
                .stream()
                .filter(t -> !t.getIsUsed())
                .forEach(t -> {
                    t.setIsUsed(true);
                    t.setUsedAt(LocalDateTime.now());
                    passwordResetTokenRepository.save(t);
                });
        
        // Create new password reset token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setSystemUserId(systemUser.getId());
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS));
        resetToken.setRequestIp(requestIp);
        resetToken.setUserAgent(userAgent);
        
        passwordResetTokenRepository.save(resetToken);
        
        log.info("Password reset requested for user: {} (SystemUser ID: {})", email, systemUser.getId());
        
        // Send password reset email
        sendPasswordResetEmail(email, plainToken);
        
        // Return plain token (to be sent via email)
        return plainToken;
    }
    
    /**
     * Reset password using token
     * 
     * @param token Reset token (plain text from email)
     * @param newPassword New password
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Hash token for lookup
        String tokenHash = hashToken(token);
        
        // Find valid token
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHashAndIsUsedAndExpiresAtAfter(tokenHash, false, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        
        // Get SystemUser
        SystemUser systemUser = systemUserRepository.findById(resetToken.getSystemUserId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        
        // Check if account is active
        if (!systemUser.getIsActive()) {
            throw new IllegalStateException("Account is not active");
        }
        
        // Update password
        systemUser.setPasswordHash(passwordEncoder.encode(newPassword));
        systemUser.setPasswordUpdatedAt(LocalDateTime.now());
        
        // Reset failed login attempts
        systemUser.setFailedLoginAttempts(0);
        systemUser.setLastFailedLoginAt(null);
        
        // Unlock account if it was locked
        if (systemUser.getAccountLocked()) {
            systemUser.setAccountLocked(false);
            systemUser.setLockReason(null);
            systemUser.setLockedUntil(null);
        }
        
        systemUserRepository.save(systemUser);
        
        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        
        log.info("Password reset successful for SystemUser ID: {}", systemUser.getId());
    }
    
    /**
     * Verify token validity without consuming it
     * 
     * @param token Reset token (plain text)
     * @return true if token is valid, false otherwise
     */
    public boolean verifyToken(String token) {
        try {
            String tokenHash = hashToken(token);
            return passwordResetTokenRepository
                    .findByTokenHashAndIsUsedAndExpiresAtAfter(tokenHash, false, LocalDateTime.now())
                    .isPresent();
        } catch (Exception e) {
            log.error("Error verifying token", e);
            return false;
        }
    }
    
    /**
     * Hash token with SHA-256
     * 
     * @param token Plain token
     * @return Hashed token (hex string)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
    
    /**
     * Cleanup expired tokens (should be called periodically)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        int deletedCount = passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Cleaned up {} expired password reset tokens", deletedCount);
    }
    
    /**
     * Send password reset email with link
     * 
     * @param email Recipient email address
     * @param token Plain reset token
     */
    private void sendPasswordResetEmail(String email, String token) {
        try {
            // Build reset link: /auth/password-reset?token={token}
            String resetLink = String.format("%s/auth/password-reset?token=%s",
                    appProperties.getBaseUrl(), token);
            
            Map<String, Object> variables = Map.of(
                    "resetLink", resetLink,
                    "expirationHours", TOKEN_EXPIRATION_HOURS);
            
            emailService.sendTemplateEmail(
                    email,
                    "パスワードリセットのリクエスト - mirelplatform",
                    "password-reset-link",
                    variables);
            
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            // メール送信失敗はトークン生成を無効化しない
            // ユーザーはトークンを直接利用できる可能性がある
        }
    }
}
