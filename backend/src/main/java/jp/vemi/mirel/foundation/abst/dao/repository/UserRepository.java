/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    /**
     * SystemUserIDでUserを検索
     */
    Optional<User> findBySystemUserId(UUID systemUserId);

    /**
     * UsernameでUserを検索
     */
    Optional<User> findByUsername(String username);

    /**
     * EmailでUserを検索
     */
    Optional<User> findByEmail(String email);
}
