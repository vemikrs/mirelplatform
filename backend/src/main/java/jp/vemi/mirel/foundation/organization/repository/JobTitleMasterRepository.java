/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.organization.model.JobTitleMaster;

public interface JobTitleMasterRepository extends JpaRepository<JobTitleMaster, String> {
    List<JobTitleMaster> findByOrganizationId(String organizationId);
}
