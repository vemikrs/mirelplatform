package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SystemUser entity
 */
@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, UUID> {
    
    /**
     * Find SystemUser by email address
     * 
     * @param email Email address
     * @return Optional SystemUser
     */
    Optional<SystemUser> findByEmail(String email);
    
    /**
     * Find SystemUser by username
     * 
     * @param username Username
     * @return Optional SystemUser
     */
    Optional<SystemUser> findByUsername(String username);
    
    /**
     * Check if email already exists
     * 
     * @param email Email address to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Find active SystemUser by email
     * 
     * @param email Email address
     * @param isActive Active status
     * @return Optional SystemUser
     */
    Optional<SystemUser> findByEmailAndIsActive(String email, Boolean isActive);
    
    /**
     * Find SystemUser by OAuth2 provider and provider ID
     * 
     * @param oauth2Provider OAuth2 provider name (e.g., "github")
     * @param oauth2ProviderId OAuth2 provider-specific user ID
     * @return Optional SystemUser
     */
    Optional<SystemUser> findByOauth2ProviderAndOauth2ProviderId(String oauth2Provider, String oauth2ProviderId);
}
