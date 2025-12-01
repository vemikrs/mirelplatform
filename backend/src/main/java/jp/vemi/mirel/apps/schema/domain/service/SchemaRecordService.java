package jp.vemi.mirel.apps.schema.domain.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.schema.domain.entity.SchRecord;
import jp.vemi.mirel.apps.schema.domain.repository.SchRecordRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SchemaRecordService {

    private final SchRecordRepository schRecordRepository;

    public List<SchRecord> findBySchema(String schema) {
        return schRecordRepository.findBySchemaAndTenantId(schema, TenantContext.getTenantId());
    }

    public SchRecord findById(String id) {
        return schRecordRepository.findByIdAndTenantId(id, TenantContext.getTenantId()).orElse(null);
    }

    public SchRecord save(SchRecord record) {
        if (record.getId() == null || record.getId().isEmpty()) {
            record.setId(UUID.randomUUID().toString());
        }
        if (record.getTenantId() == null) {
            record.setTenantId(TenantContext.getTenantId());
        }
        // Ensure we are not overwriting another tenant's record if ID is provided
        if (schRecordRepository.existsById(record.getId())) {
            schRecordRepository.findByIdAndTenantId(record.getId(), TenantContext.getTenantId())
                    .orElseThrow(() -> new SecurityException("Access denied to record: " + record.getId()));
        }
        return schRecordRepository.save(record);
    }

    public void delete(String id) {
        schRecordRepository.deleteByIdAndTenantId(id, TenantContext.getTenantId());
    }
}
