package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PasswordResetToken entity
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    /**
     * Find password reset token by token hash
     * 
     * @param tokenHash Token hash
     * @return Optional PasswordResetToken
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    
    /**
     * Find valid (not used and not expired) token by token hash
     * 
     * @param tokenHash Token hash
     * @param isUsed Used status
     * @param now Current timestamp
     * @return Optional PasswordResetToken
     */
    Optional<PasswordResetToken> findByTokenHashAndIsUsedAndExpiresAtAfter(
            String tokenHash, Boolean isUsed, LocalDateTime now);
    
    /**
     * Find all tokens for a system user
     * 
     * @param systemUserId System user ID
     * @return List of PasswordResetToken
     */
    List<PasswordResetToken> findBySystemUserId(UUID systemUserId);
    
    /**
     * Delete all expired tokens
     * 
     * @param now Current timestamp
     * @return Number of deleted tokens
     */
    int deleteByExpiresAtBefore(LocalDateTime now);
    
    /**
     * Delete all used tokens
     * 
     * @param isUsed Used status
     * @return Number of deleted tokens
     */
    int deleteByIsUsed(Boolean isUsed);
}
