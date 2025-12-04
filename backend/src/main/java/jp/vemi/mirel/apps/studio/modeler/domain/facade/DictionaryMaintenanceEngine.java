package jp.vemi.mirel.apps.studio.modeler.domain.facade;

import java.util.List;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.modeler.domain.service.StuModelService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DictionaryMaintenanceEngine {

    private final StuModelService schemaModelService;

    public List<StuModel> getModelFields(String modelId) {
        return schemaModelService.findModels(modelId);
    }

    public void saveModel(String modelId, String modelName, Boolean isHidden, String modelType,
            List<StuModel> fields) {
        StuModelHeader header = StuModelHeader.builder()
                .modelId(modelId)
                .modelName(modelName)
                .isHidden(isHidden)
                .modelType(modelType)
                .build();
        schemaModelService.saveHeader(header);
        schemaModelService.saveModel(fields);
    }

    public void deleteModel(String modelId) {
        schemaModelService.deleteModel(modelId);
    }
}
