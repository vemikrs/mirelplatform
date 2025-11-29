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
        return schDicModelRepository.findByPkModelId(modelId);
    }

    public SchDicModelHeader findHeader(String modelId) {
        return schDicModelHeaderRepository.findById(modelId).orElse(null);
    }

    public void saveModel(List<SchDicModel> models) {
        models.forEach(model -> {
            if (model.getTenantId() == null) {
                model.setTenantId(TenantContext.getTenantId());
            }
        });
        schDicModelRepository.saveAll(models);
    }

    public void saveHeader(SchDicModelHeader header) {
        if (header.getTenantId() == null) {
            header.setTenantId(TenantContext.getTenantId());
        }
        schDicModelHeaderRepository.save(header);
    }

    public void deleteModel(String modelId) {
        // Delete header
        schDicModelHeaderRepository.deleteById(modelId);
        // Delete fields
        List<SchDicModel> fields = schDicModelRepository.findByPkModelId(modelId);
        schDicModelRepository.deleteAll(fields);
    }
}
