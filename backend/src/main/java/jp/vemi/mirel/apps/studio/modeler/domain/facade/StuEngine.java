package jp.vemi.mirel.apps.studio.modeler.domain.facade;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.studio.modeler.domain.dto.model.dictionary.HierarchicalTopNode;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuRecord;
import jp.vemi.mirel.apps.studio.modeler.domain.service.StuModelService;
import jp.vemi.mirel.apps.studio.modeler.domain.service.StuRecordService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StuEngine {

    private final StuRecordService schemaRecordService;
    private final StuModelService schemaModelService;

    public HierarchicalTopNode getSchemaRecursive(String modelId) {
        // TODO: Implement recursive schema retrieval logic
        return HierarchicalTopNode.builder()
                .modelId(modelId)
                .nodeType("ROOT")
                .build();
    }

    public List<StuRecord> getRecordList(String modelId) {
        return schemaRecordService.findByModelId(modelId);
    }

    public StuRecord getRecord(String recordId) {
        return schemaRecordService.findById(recordId);
    }

    public StuRecord saveRecord(String modelId, String recordId, Map<String, Object> recordData) {
        StuRecord record = StuRecord.builder()
                .id(recordId)
                .modelId(modelId)
                .recordData(recordData)
                .build();
        return schemaRecordService.save(record);
    }

    public void deleteRecord(String recordId) {
        schemaRecordService.delete(recordId);
    }
}
