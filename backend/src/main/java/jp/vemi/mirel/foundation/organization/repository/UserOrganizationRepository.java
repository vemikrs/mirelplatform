/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.organization.model.UserOrganization;

public interface UserOrganizationRepository extends JpaRepository<UserOrganization, String> {
    List<UserOrganization> findByUserId(String userId);

    List<UserOrganization> findByUnitId(String unitId);

    List<UserOrganization> findByUnitIdIn(List<String> unitIds);
}
