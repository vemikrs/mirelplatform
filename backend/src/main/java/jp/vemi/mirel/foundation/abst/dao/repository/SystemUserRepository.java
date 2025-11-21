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
}
