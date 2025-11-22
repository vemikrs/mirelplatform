/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PasswordResetToken Entity - Password reset token management
 * 
 * Stores temporary tokens for password reset operations.
 * Tokens expire after a configured period (default: 1 hour)
 */
@Entity
@Table(name = "mir_password_reset_token")
@Getter
@Setter
public class PasswordResetToken {
    
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * Reference to SystemUser
     */
    @Column(name = "system_user_id", nullable = false, columnDefinition = "UUID")
    private UUID systemUserId;
    
    /**
     * Reset token (hashed with SHA-256)
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    
    /**
     * Token expiration timestamp
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Whether this token has been used
     */
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
    
    /**
     * Timestamp when token was used
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    /**
     * IP address of the request that created this token
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;
    
    /**
     * User agent of the request that created this token
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (isUsed == null) {
            isUsed = false;
        }
        // Default expiration: 1 hour from now
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(1);
        }
    }
    
    /**
     * Check if token is expired
     * 
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Check if token is valid (not used and not expired)
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return !isUsed && !isExpired();
    }
}
