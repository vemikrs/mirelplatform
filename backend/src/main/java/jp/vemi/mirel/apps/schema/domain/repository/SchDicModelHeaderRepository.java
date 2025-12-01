package jp.vemi.mirel.apps.schema.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicModelHeader;

import java.util.Optional;

@Repository
public interface SchDicModelHeaderRepository extends JpaRepository<SchDicModelHeader, String> {
    Optional<SchDicModelHeader> findByModelIdAndTenantId(String modelId, String tenantId);

    void deleteByModelIdAndTenantId(String modelId, String tenantId);
}
