/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeaderLegacy;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuModelHeaderLegacyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service for managing Studio Models.
 */
@Service
@RequiredArgsConstructor
public class StudioModelService {

    private final StuModelHeaderLegacyRepository headerRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private StuFieldRepository fieldRepository;

    /**
     * Create a new model draft.
     *
     * @param name
     *            Model name
     * @param description
     *            Model description
     * @return The created model
     */
    @Transactional
    public StuModelHeaderLegacy createDraft(String name, String description) {
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId(java.util.UUID.randomUUID().toString());
        model.setModelName(name);
        model.setDescription(description);
        model.setStatus("DRAFT");
        model.setVersion(1);

        return headerRepository.save(model);
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
        StuModelHeaderLegacy model = headerRepository.findById(modelId)
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
        StuModelHeaderLegacy model = headerRepository.findById(modelId)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelId));

        List<StuField> fields = fieldRepository.findByModelIdOrderBySortOrder(modelId);
        fieldRepository.deleteAll(fields);
        headerRepository.delete(model);
    }

    /**
     * Get model by ID.
     * 
     * @param modelId
     *            Model ID
     * @return The model
     */
    public StuModelHeaderLegacy getModel(String modelId) {
        return headerRepository.findById(modelId)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelId));
    }

    /**
     * Get fields for a model.
     * 
     * @param modelId
     *            Model ID
     * @return List of fields
     */
    public List<StuField> getFields(String modelId) {
        return fieldRepository.findByModelIdOrderBySortOrder(modelId);
    }

    /**
     * Publish a model, creating the physical table.
     * 
     * @param modelId
     *            The model ID to publish
     * @param schemaEngine
     *            The schema engine service
     */
    @Transactional
    public void publish(String modelId, SchemaEngineService schemaEngine) {
        StuModelHeaderLegacy model = headerRepository.findById(modelId)
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

    /**
     * Find all models.
     * 
     * @return List of models
     */
    public List<StuModelHeaderLegacy> findAll() {
        return headerRepository.findAll();
    }

    /**
     * Save model.
     * 
     * @param model
     *            The model to save
     */
    public void saveModel(StuModelHeaderLegacy model) {
        headerRepository.save(model);
    }
}
