package jp.vemi.mirel.apps.schema.domain.facade;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.schema.domain.dto.model.dictionary.HierarchicalTopNode;
import jp.vemi.mirel.apps.schema.domain.entity.SchRecord;
import jp.vemi.mirel.apps.schema.domain.service.SchemaModelService;
import jp.vemi.mirel.apps.schema.domain.service.SchemaRecordService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SchemaEngine {

    private final SchemaRecordService schemaRecordService;
    private final SchemaModelService schemaModelService;

    public HierarchicalTopNode getSchemaRecursive(String modelId) {
        // TODO: Implement recursive schema retrieval logic
        return HierarchicalTopNode.builder()
                .modelId(modelId)
                .nodeType("ROOT")
                .build();
    }

    public List<SchRecord> getRecordList(String modelId) {
        return schemaRecordService.findBySchema(modelId);
    }

    public SchRecord getRecord(String recordId) {
        return schemaRecordService.findById(recordId);
    }

    public SchRecord saveRecord(String modelId, String recordId, Map<String, Object> recordData) {
        SchRecord record = SchRecord.builder()
                .id(recordId)
                .schema(modelId)
                .recordData(recordData)
                .build();
        return schemaRecordService.save(record);
    }

    public void deleteRecord(String recordId) {
        schemaRecordService.delete(recordId);
    }
}
