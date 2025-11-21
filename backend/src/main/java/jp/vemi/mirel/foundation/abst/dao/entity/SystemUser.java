package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SystemUser Entity - System-level authentication and user management
 * 
 * Stores centralized authentication information and security attributes
 * that are shared across all applications in the system.
 * This entity is designed to reside in a system-wide database or separate schema.
 */
@Entity
@Table(name = "system_user")
@Getter
@Setter
public class SystemUser {
    
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * Email address - used as primary authentication identifier
     * Must be unique across the entire system
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    /**
     * BCrypt hashed password
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    /**
     * Email verification status
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;
    
    /**
     * Account active status
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    /**
     * Account locked status (for security purposes)
     */
    @Column(name = "account_locked", nullable = false)
    private Boolean accountLocked = false;
    
    /**
     * Reason for account lock (if locked)
     */
    @Column(name = "lock_reason", length = 500)
    private String lockReason;
    
    /**
     * Timestamp of last password update
     */
    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;
    
    /**
     * Number of failed login attempts (for rate limiting)
     */
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    /**
     * Timestamp of last failed login attempt
     */
    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;
    
    /**
     * Account locked until timestamp (temporary locks)
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (accountLocked == null) {
            accountLocked = false;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }
}
