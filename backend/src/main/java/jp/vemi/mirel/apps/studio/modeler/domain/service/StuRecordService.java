package jp.vemi.mirel.apps.studio.modeler.domain.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuRecord;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuRecordRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StuRecordService {

    private final StuRecordRepository stuRecordRepository;

    public List<StuRecord> findByModelId(String schema) {
        return stuRecordRepository.findByModelIdAndTenantId(schema, TenantContext.getTenantId());
    }

    public StuRecord findById(String id) {
        return stuRecordRepository.findByIdAndTenantId(id, TenantContext.getTenantId()).orElse(null);
    }

    public StuRecord save(StuRecord record) {
        if (record.getId() == null || record.getId().isEmpty()) {
            record.setId(UUID.randomUUID().toString());
        }
        if (record.getTenantId() == null) {
            record.setTenantId(TenantContext.getTenantId());
        }
        // Ensure we are not overwriting another tenant's record if ID is provided
        if (stuRecordRepository.existsById(record.getId())) {
            stuRecordRepository.findByIdAndTenantId(record.getId(), TenantContext.getTenantId())
                    .orElseThrow(() -> new SecurityException("Access denied to record: " + record.getId()));
        }
        return stuRecordRepository.save(record);
    }

    public void delete(String id) {
        stuRecordRepository.deleteByIdAndTenantId(id, TenantContext.getTenantId());
    }
}
