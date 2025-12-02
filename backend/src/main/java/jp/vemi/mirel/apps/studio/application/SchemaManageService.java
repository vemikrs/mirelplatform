/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuModelHeaderRepository;
import jp.vemi.mirel.apps.studio.domain.service.SchemaEngineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Service for managing schema lifecycle (publish, etc.).
 */
@Service
public class SchemaManageService {

    private final StuModelHeaderRepository headerRepository;
    private final StuFieldRepository fieldRepository;
    private final SchemaEngineService schemaEngine;

    public SchemaManageService(
            StuModelHeaderRepository headerRepository,
            StuFieldRepository fieldRepository,
            SchemaEngineService schemaEngine) {
        this.headerRepository = headerRepository;
        this.fieldRepository = fieldRepository;
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
        StuModelHeader model = new StuModelHeader();
        model.setModelId(java.util.UUID.randomUUID().toString());
        model.setModelName(name);
        model.setDescription(description);
        model.setStatus("DRAFT");
        model.setVersion(1);

        headerRepository.save(model);
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
        StuModelHeader model = headerRepository.findById(modelId)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelId));

        if ("PUBLISHED".equals(model.getStatus())) {
            throw new IllegalStateException("Cannot update published model. Create a new version.");
        }

        model.setModelName(name);
        model.setDescription(description);
        headerRepository.save(model);

        // Replace fields
        List<StuField> existingFields = fieldRepository.findByModelIdOrderBySortOrder(modelId);
        fieldRepository.deleteAll(existingFields);

        for (StuField field : fields) {
            field.setFieldId(java.util.UUID.randomUUID().toString());
            field.setModelId(modelId);
            fieldRepository.save(field);
        }
    }

    /**
     * Delete a model.
     *
     * @param modelId
     *            Model ID
     */
    @Transactional
    public void deleteModel(String modelId) {
        StuModelHeader model = headerRepository.findById(modelId)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelId));

        // If published, we might want to drop the table too.
        // For Phase 1, we will drop the table if it exists.
        if ("PUBLISHED".equals(model.getStatus())) {
            // TODO: Implement drop table logic in SchemaEngineService if needed.
            // For now, we just delete metadata.
        }

        List<StuField> fields = fieldRepository.findByModelIdOrderBySortOrder(modelId);
        fieldRepository.deleteAll(fields);
        headerRepository.delete(model);
    }

    /**
     * Publish a model, creating the physical table.
     *
     * @param modelId
     *            The model ID to publish
     */
    @Transactional
    public void publish(String modelId) {
        StuModelHeader model = headerRepository.findById(modelId)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelId));

        if ("PUBLISHED".equals(model.getStatus())) {
            throw new IllegalStateException("Model is already published");
        }

        List<StuField> fields = fieldRepository.findByModelIdOrderBySortOrder(modelId);

        // Execute DDL
        schemaEngine.createTable(model, fields);

        // Update status
        model.setStatus("PUBLISHED");
        headerRepository.save(model);
    }
}
