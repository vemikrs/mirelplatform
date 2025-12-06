package jp.vemi.mirel.apps.studio.modeler.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuRecord;

import java.util.Optional;

@Repository
public interface StuRecordRepository extends JpaRepository<StuRecord, String> {
    List<StuRecord> findByModelIdAndTenantId(String modelId, String tenantId);

    Optional<StuRecord> findByIdAndTenantId(String id, String tenantId);

    void deleteByIdAndTenantId(String id, String tenantId);
}
