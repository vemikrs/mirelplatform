package jp.vemi.mirel.apps.schema.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jp.vemi.mirel.apps.schema.domain.entity.SchRecord;

import java.util.Optional;

@Repository
public interface SchRecordRepository extends JpaRepository<SchRecord, String> {
    List<SchRecord> findBySchemaAndTenantId(String schema, String tenantId);

    Optional<SchRecord> findByIdAndTenantId(String id, String tenantId);

    void deleteByIdAndTenantId(String id, String tenantId);
}
