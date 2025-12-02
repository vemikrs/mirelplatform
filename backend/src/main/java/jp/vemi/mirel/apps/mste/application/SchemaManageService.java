/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.application;

import jp.vemi.mirel.apps.mste.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.mste.domain.dao.entity.StuModelHeader;
import jp.vemi.mirel.apps.mste.domain.dao.repository.StuFieldRepository;
import jp.vemi.mirel.apps.mste.domain.dao.repository.StuModelHeaderRepository;
import jp.vemi.mirel.apps.mste.domain.service.SchemaEngineService;
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
