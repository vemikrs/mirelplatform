/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing schema lifecycle (publish, etc.).
 */
@Service
public class SchemaManageService {

    private final StudioModelService studioModelService;
    private final SchemaEngineService schemaEngine;

    public SchemaManageService(
            StudioModelService studioModelService,
            SchemaEngineService schemaEngine) {
        this.studioModelService = studioModelService;
        this.schemaEngine = schemaEngine;
    }

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
        StuModelHeader model = studioModelService.createDraft(name, description);
        return model.getModelId();
    }

    /**
     * Update a model draft.
     *
     * @param modelId
     *            Model ID
     * @param name
     *            Model name
     * @param description
     *            Model description
     * @param fields
     *            List of fields
     */
    @Transactional
    public void updateDraft(String modelId, String name, String description, List<StuField> fields) {
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
        StuModelHeader model = studioModelService.getModel(modelId);

        // If published, we might want to drop the table too.
        // For Phase 1, we will drop the table if it exists.
        if ("PUBLISHED".equals(model.getStatus())) {
            // TODO: Implement drop table logic in SchemaEngineService if needed.
            // For now, we just delete metadata.
        }

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
        studioModelService.publish(modelId, schemaEngine);
    }
}
