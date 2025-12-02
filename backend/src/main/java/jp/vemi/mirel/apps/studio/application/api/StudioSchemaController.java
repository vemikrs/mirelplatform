/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.api;

import jp.vemi.mirel.apps.studio.application.dto.CreateDraftRequest;
import jp.vemi.mirel.apps.studio.application.dto.UpdateDraftRequest;
import jp.vemi.mirel.apps.studio.domain.service.SchemaManageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/studio/schema")
public class StudioSchemaController {

    private final SchemaManageService schemaManageService;

    public StudioSchemaController(SchemaManageService schemaManageService) {
        this.schemaManageService = schemaManageService;
    }

    @PostMapping("/draft")
    public String createDraft(@RequestBody CreateDraftRequest request) {
        return schemaManageService.createDraft(request.getName(), request.getDescription());
    }

    @PutMapping("/draft/{modelId}")
    public void updateDraft(@PathVariable String modelId, @RequestBody UpdateDraftRequest request) {
        schemaManageService.updateDraft(modelId, request.getName(), request.getDescription(), request.getFields());
    }

    @DeleteMapping("/{modelId}")
    public void deleteModel(@PathVariable String modelId) {
        schemaManageService.deleteModel(modelId);
    }

    @PostMapping("/{modelId}/publish")
    public void publish(@PathVariable String modelId) {
        schemaManageService.publish(modelId);
    }
}
