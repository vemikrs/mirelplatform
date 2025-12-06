/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.organization.model.OrganizationUnit;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, String> {
    List<OrganizationUnit> findByOrganizationId(String organizationId);

    List<OrganizationUnit> findByOrganizationIdAndParentUnitId(String organizationId, String parentUnitId);
}
