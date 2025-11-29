package jp.vemi.mirel.apps.schema.domain.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.schema.domain.entity.SchDicModel;
import jp.vemi.mirel.apps.schema.domain.entity.SchDicModelHeader;
import jp.vemi.mirel.apps.schema.domain.service.SchemaModelService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DictionaryMaintenanceEngine {

    private final SchemaModelService schemaModelService;

    public List<SchDicModel> getModelFields(String modelId) {
        return schemaModelService.findModels(modelId);
    }

    public void saveModel(String modelId, String modelName, Boolean isHidden, List<SchDicModel> fields) {
        SchDicModelHeader header = SchDicModelHeader.builder()
                .modelId(modelId)
                .modelName(modelName)
                .isHidden(isHidden)
                .build();
        schemaModelService.saveHeader(header);
        schemaModelService.saveModel(fields);
    }

    public void deleteModel(String modelId) {
        schemaModelService.deleteModel(modelId);
    }
}
