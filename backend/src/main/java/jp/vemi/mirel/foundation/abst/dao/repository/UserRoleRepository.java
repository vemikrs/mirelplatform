/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.abst.dao.entity.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {
}
