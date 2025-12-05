/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jp.vemi.mirel.foundation.abst.dao.entity.TenantSystemMaster;
import java.util.Optional;

public interface TenantSystemMasterRepository extends JpaRepository<TenantSystemMaster, String> {
    Optional<TenantSystemMaster> findByTenantIdAndKey(String tenantId, String key);

    java.util.List<TenantSystemMaster> findByTenantId(String tenantId);
}
