package jp.vemi.mirel.apps.studio.modeler.application.controller;

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

import jp.vemi.mirel.apps.studio.modeler.domain.dto.request.StuApiRequest;
import jp.vemi.mirel.apps.studio.modeler.domain.dto.response.StuApiResponse;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuCode;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuRecord;
import jp.vemi.mirel.apps.studio.modeler.domain.facade.CodeMaintenanceEngine;
import jp.vemi.mirel.apps.studio.modeler.domain.facade.DictionaryMaintenanceEngine;
import jp.vemi.mirel.apps.studio.modeler.domain.facade.StuEngine;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/studio/modeler")
@RequiredArgsConstructor
public class StuApiController {

    private final StuEngine schemaEngine;
    private final DictionaryMaintenanceEngine dictionaryMaintenanceEngine;
    private final CodeMaintenanceEngine codeMaintenanceEngine;
    private final ObjectMapper objectMapper;

    @PostMapping("/{path}")
    public ResponseEntity<StuApiResponse> handle(@PathVariable String path, @RequestBody StuApiRequest request) {
        Map<String, Object> content = request.getContent();
        StuApiResponse response = new StuApiResponse();
        response.setMessages(Collections.emptyList());
        response.setErrors(Collections.emptyList());

        try {
            switch (path) {
                case "list":
                    String modelId = (String) content.get("modelId");
                    List<StuRecord> records = schemaEngine.getRecordList(modelId);
                    response.setData(Map.of("records", records));
                    break;
                case "load":
                    String recordId = (String) content.get("recordId");
                    StuRecord record = schemaEngine.getRecord(recordId);
                    response.setData(Map.of("record", record));
                    break;
                case "save":
                    String saveModelId = (String) content.get("modelId");
                    String saveRecordId = (String) content.get("recordId");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> recordData = (Map<String, Object>) content.get("record");
                    StuRecord savedRecord = schemaEngine.saveRecord(saveModelId, saveRecordId, recordData);
                    response.setData(Map.of("recordId", savedRecord.getId()));
                    response.setMessages(List.of("保存しました。"));
                    break;
                case "deleteRecord":
                    String deleteRecordId = (String) content.get("recordId");
                    schemaEngine.deleteRecord(deleteRecordId);
                    response.setMessages(List.of("削除しました。"));
                    break;
                case "listModel":
                    String schemaModelId = (String) content.get("modelId");
                    List<StuModel> schemas = dictionaryMaintenanceEngine.getModelFields(schemaModelId);
                    response.setData(Map.of("schemas", schemas));
                    break;
                case "saveModel":
                    String saveSchemaModelId = (String) content.get("modelId");
                    String modelName = (String) content.get("modelName");
                    Boolean isHidden = (Boolean) content.get("isHiddenModel");
                    String modelType = (String) content.get("modelType");
                    List<Map<String, Object>> fieldsMap = (List<Map<String, Object>>) content.get("fields");
                    List<StuModel> fields = fieldsMap.stream()
                            .map(f -> objectMapper.convertValue(f, StuModel.class))
                            .toList();
                    dictionaryMaintenanceEngine.saveModel(saveSchemaModelId, modelName, isHidden, modelType, fields);
                    response.setMessages(List.of("保存しました。"));
                    break;
                case "deleteModel":
                    String deleteModelId = (String) content.get("modelId");
                    dictionaryMaintenanceEngine.deleteModel(deleteModelId);
                    break;
                case "listCode":
                    String groupId = (String) content.get("id");
                    List<StuCode> codes = codeMaintenanceEngine.getCodeList(groupId);
                    response.setData(Map.of("valueTexts", codes));
                    break;
                case "saveCode":
                    String saveGroupId = (String) content.get("groupId");
                    List<Map<String, Object>> detailsMap = (List<Map<String, Object>>) content.get("details");
                    List<StuCode> saveCodes = detailsMap.stream()
                            .map(d -> objectMapper.convertValue(d, StuCode.class))
                            .toList();
                    codeMaintenanceEngine.saveCode(saveGroupId, saveCodes);
                    response.setMessages(List.of("保存しました。"));
                    break;
                case "deleteCode":
                    String deleteCodeGroupId = (String) content.get("codeGroupId");
                    codeMaintenanceEngine.deleteCodeGroup(deleteCodeGroupId);
                    break;
                case "listCodeGroups":
                    List<String> groups = codeMaintenanceEngine.getGroupList();
                    response.setData(Map.of("groups", groups));
                    break;
                case "listModels":
                    List<StuModelHeader> headers = dictionaryMaintenanceEngine.getModelList();
                    response.setData(Map.of("models", headers));
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
