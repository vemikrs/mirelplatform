package jp.vemi.mirel.apps.schema.application.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.schema.domain.dto.request.SchemaApiRequest;
import jp.vemi.mirel.apps.schema.domain.dto.response.SchemaApiResponse;
import jp.vemi.mirel.apps.schema.domain.entity.SchDicCode;
import jp.vemi.mirel.apps.schema.domain.entity.SchDicModel;
import jp.vemi.mirel.apps.schema.domain.entity.SchRecord;
import jp.vemi.mirel.apps.schema.domain.facade.CodeMaintenanceEngine;
import jp.vemi.mirel.apps.schema.domain.facade.DictionaryMaintenanceEngine;
import jp.vemi.mirel.apps.schema.domain.facade.SchemaEngine;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/apps/schema/api")
@RequiredArgsConstructor
public class SchemaApiController {

    private final SchemaEngine schemaEngine;
    private final DictionaryMaintenanceEngine dictionaryMaintenanceEngine;
    private final CodeMaintenanceEngine codeMaintenanceEngine;
    private final ObjectMapper objectMapper;

    @PostMapping("/{path}")
    public ResponseEntity<SchemaApiResponse> handle(@PathVariable String path, @RequestBody SchemaApiRequest request) {
        Map<String, Object> content = request.getContent();
        SchemaApiResponse response = new SchemaApiResponse();
        response.setMessages(Collections.emptyList());
        response.setErrors(Collections.emptyList());

        try {
            switch (path) {
                case "list":
                    String modelId = (String) content.get("modelId");
                    List<SchRecord> records = schemaEngine.getRecordList(modelId);
                    response.setData(Map.of("records", records));
                    break;
                case "load":
                    String recordId = (String) content.get("recordId");
                    SchRecord record = schemaEngine.getRecord(recordId);
                    response.setData(Map.of("record", record));
                    break;
                case "save":
                    String saveModelId = (String) content.get("modelId");
                    String saveRecordId = (String) content.get("recordId");
                    Map<String, Object> recordData = (Map<String, Object>) content.get("record");
                    SchRecord savedRecord = schemaEngine.saveRecord(saveModelId, saveRecordId, recordData);
                    response.setData(Map.of("recordId", savedRecord.getId()));
                    response.setMessages(List.of("保存しました。"));
                    break;
                case "deleteRecord":
                    String deleteRecordId = (String) content.get("recordId");
                    schemaEngine.deleteRecord(deleteRecordId);
                    response.setMessages(List.of("削除しました。"));
                    break;
                case "listSchema":
                    String schemaModelId = (String) content.get("modelId");
                    List<SchDicModel> schemas = dictionaryMaintenanceEngine.getModelFields(schemaModelId);
                    response.setData(Map.of("schemas", schemas));
                    break;
                case "saveSchema":
                    String saveSchemaModelId = (String) content.get("modelId");
                    String modelName = (String) content.get("modelName");
                    Boolean isHidden = (Boolean) content.get("isHiddenModel");
                    List<Map<String, Object>> fieldsMap = (List<Map<String, Object>>) content.get("fields");
                    List<SchDicModel> fields = fieldsMap.stream()
                            .map(f -> objectMapper.convertValue(f, SchDicModel.class))
                            .toList();
                    dictionaryMaintenanceEngine.saveModel(saveSchemaModelId, modelName, isHidden, fields);
                    response.setMessages(List.of("保存しました。"));
                    break;
                case "deleteModel":
                    String deleteModelId = (String) content.get("modelId");
                    dictionaryMaintenanceEngine.deleteModel(deleteModelId);
                    break;
                case "listCode":
                    String groupId = (String) content.get("id");
                    List<SchDicCode> codes = codeMaintenanceEngine.getCodeList(groupId);
                    response.setData(Map.of("valueTexts", codes));
                    break;
                case "saveCode":
                    String saveGroupId = (String) content.get("groupId");
                    List<Map<String, Object>> detailsMap = (List<Map<String, Object>>) content.get("details");
                    List<SchDicCode> saveCodes = detailsMap.stream()
                            .map(d -> objectMapper.convertValue(d, SchDicCode.class))
                            .toList();
                    codeMaintenanceEngine.saveCode(saveGroupId, saveCodes);
                    response.setMessages(List.of("保存しました。"));
                    break;
                case "deleteCode":
                    String deleteCodeGroupId = (String) content.get("codeGroupId");
                    codeMaintenanceEngine.deleteCodeGroup(deleteCodeGroupId);
                    break;
                default:
                    response.setErrors(List.of("Unknown path: " + path));
                    return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setErrors(List.of(e.getMessage()));
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
