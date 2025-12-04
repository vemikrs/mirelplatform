/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.organization.model.Organization;

public interface OrganizationRepository extends JpaRepository<Organization, String> {
}
