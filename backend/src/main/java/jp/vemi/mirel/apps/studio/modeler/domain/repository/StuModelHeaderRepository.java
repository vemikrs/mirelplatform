package jp.vemi.mirel.apps.studio.modeler.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;

import java.util.Optional;

@Repository
public interface StuModelHeaderRepository extends JpaRepository<StuModelHeader, String> {
    Optional<StuModelHeader> findByModelIdAndTenantId(String modelId, String tenantId);

    void deleteByModelIdAndTenantId(String modelId, String tenantId);

    java.util.List<StuModelHeader> findByTenantId(String tenantId);
}
