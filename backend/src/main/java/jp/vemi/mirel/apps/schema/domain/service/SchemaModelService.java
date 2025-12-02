package jp.vemi.mirel.apps.schema.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicModel;
import jp.vemi.mirel.apps.schema.domain.entity.SchDicModelHeader;
import jp.vemi.mirel.apps.schema.domain.repository.SchDicModelHeaderRepository;
import jp.vemi.mirel.apps.schema.domain.repository.SchDicModelRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SchemaModelService {

    private final SchDicModelRepository schDicModelRepository;
    private final SchDicModelHeaderRepository schDicModelHeaderRepository;

    public List<SchDicModel> findModels(String modelId) {
        return schDicModelRepository.findByPk_ModelIdAndTenantId(modelId, TenantContext.getTenantId());
    }

    public SchDicModelHeader findHeader(String modelId) {
        return schDicModelHeaderRepository.findByModelIdAndTenantId(modelId, TenantContext.getTenantId()).orElse(null);
    }

    public void saveModel(List<SchDicModel> models) {
        String tenantId = TenantContext.getTenantId();
        models.forEach(model -> {
            if (model.getTenantId() == null) {
                model.setTenantId(tenantId);
            } else if (!model.getTenantId().equals(tenantId)) {
                throw new SecurityException("Cannot save model field for another tenant");
            }
        });
        schDicModelRepository.saveAll(models);
    }

    public void saveHeader(SchDicModelHeader header) {
        if (header.getTenantId() == null) {
            header.setTenantId(TenantContext.getTenantId());
        }
        // Ensure we are not overwriting another tenant's header
        if (schDicModelHeaderRepository.existsById(header.getModelId())) {
            schDicModelHeaderRepository.findByModelIdAndTenantId(header.getModelId(), TenantContext.getTenantId())
                    .orElseThrow(() -> new SecurityException("Access denied to model header: " + header.getModelId()));
        }
        schDicModelHeaderRepository.save(header);
    }

    public void deleteModel(String modelId) {
        String tenantId = TenantContext.getTenantId();
        // Delete header
        schDicModelHeaderRepository.deleteByModelIdAndTenantId(modelId, tenantId);
        // Delete fields
        List<SchDicModel> fields = schDicModelRepository.findByPk_ModelIdAndTenantId(modelId, tenantId);
        schDicModelRepository.deleteAll(fields);
    }
}
