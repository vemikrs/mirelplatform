/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeaderLegacy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing schema lifecycle (publish, etc.).
 */
@Service
public class SchemaManageService {

    @org.springframework.beans.factory.annotation.Autowired
    private jp.vemi.mirel.apps.studio.modeler.domain.service.StuModelService studioModelService;
    @org.springframework.beans.factory.annotation.Autowired
    private SchemaEngineService schemaEngine;

    /**
     * Create a new model draft.
     *
     * @param name
     *            Model name
     * @param description
     *            Model description
     * @return The created model ID
     */
    @Transactional
    public String createDraft(String name, String description) {
        jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader model = studioModelService.createDraft(name,
                description);
        return model.getModelId();
    }

    /**
     * Update a model draft.
     *
     */
    @Transactional
    public void updateDraft(String modelId, String name, String description,
            List<jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel> fields) {
        studioModelService.updateDraft(modelId, name, description, fields);
    }

    /**
     * Delete a model.
     *
     * @param modelId
     *            Model ID
     */
    @Transactional
    public void deleteModel(String modelId) {
        // Drop table logic if needed
        studioModelService.deleteModel(modelId);
    }

    /**
     * Publish a model, creating the physical table.
     *
     * @param modelId
     *            The model ID to publish
     */
    @Transactional
    public void publish(String modelId) {
        // TODO: Call SchemaEngine to create table (needs refactoring to use StuModel)
        // schemaEngine.createTable(modelId);
        studioModelService.publish(modelId);
    }

    /**
     * List all models.
     * 
     * @return List of model summaries
     */
    public List<jp.vemi.mirel.apps.studio.application.dto.SchemaSummaryResponse> listModels() {
        return studioModelService.findAll().stream().map(header -> {
            jp.vemi.mirel.apps.studio.application.dto.SchemaSummaryResponse response = new jp.vemi.mirel.apps.studio.application.dto.SchemaSummaryResponse();
            response.setModelId(header.getModelId());
            response.setModelName(header.getModelName());
            response.setDescription(header.getDescription());
            response.setStatus(header.getStatus());
            response.setVersion(header.getVersion());
            response.setUpdatedAt(header.getUpdatedAt());
            return response;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get model detail.
     * 
     * @param modelId
     *            Model ID
     * @return Model detail
     */
    public jp.vemi.mirel.apps.studio.application.dto.SchemaDetailResponse getModel(String modelId) {
        jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader header = studioModelService.findHeader(modelId);
        List<jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel> fields = studioModelService.findModels(modelId);

        jp.vemi.mirel.apps.studio.application.dto.SchemaDetailResponse response = new jp.vemi.mirel.apps.studio.application.dto.SchemaDetailResponse();
        response.setModelId(header.getModelId());
        response.setModelName(header.getModelName());
        response.setDescription(header.getDescription());
        response.setStatus(header.getStatus());
        response.setVersion(header.getVersion());
        response.setUpdatedAt(header.getUpdatedAt());
        response.setFields(fields);
        return response;
    }
}
