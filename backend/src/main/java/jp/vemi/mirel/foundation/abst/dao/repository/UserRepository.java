/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, String> {

    /**
     * SystemUserIDでUserを検索
     */
    Optional<User> findBySystemUserId(UUID systemUserId);

    /**
     * UsernameでUserを検索
     */
    Optional<User> findByUsername(String username);
}
