/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.abst.dao.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {}
