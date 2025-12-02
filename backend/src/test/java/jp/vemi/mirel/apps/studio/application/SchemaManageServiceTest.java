/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuModelHeaderRepository;
import jp.vemi.mirel.apps.studio.domain.service.SchemaEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaManageServiceTest {

    @Mock
    private StuModelHeaderRepository headerRepository;
    @Mock
    private StuFieldRepository fieldRepository;
    @Mock
    private SchemaEngineService schemaEngine;

    @InjectMocks
    private SchemaManageService service;

    @Test
    void createDraft_shouldSaveNewModel() {
        String name = "New Model";
        String description = "Description";

        String modelId = service.createDraft(name, description);

        assertThat(modelId).isNotNull();
        verify(headerRepository).save(any(StuModelHeader.class));
    }

    @Test
    void updateDraft_shouldUpdateModelAndFields() {
        String modelId = "test_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("DRAFT");

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        StuField field = new StuField();
        field.setFieldName("new_field");
        List<StuField> fields = Collections.singletonList(field);

        service.updateDraft(modelId, "Updated Name", "Updated Desc", fields);

        verify(headerRepository).save(model);
        assertThat(model.getModelName()).isEqualTo("Updated Name");
        verify(fieldRepository).deleteAll(anyList());
        verify(fieldRepository).save(field);
    }

    @Test
    void updateDraft_shouldThrowIfPublished() {
        String modelId = "published_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("PUBLISHED");

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> service.updateDraft(modelId, "Name", "Desc", Collections.emptyList()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteModel_shouldDeleteModelAndFields() {
        String modelId = "test_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("DRAFT");

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.emptyList());

        service.deleteModel(modelId);

        verify(fieldRepository).deleteAll(anyList());
        verify(headerRepository).delete(model);
    }

    @Test
    void publish_shouldPublishModel() {
        String modelId = "test_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("DRAFT");

        List<StuField> fields = Collections.emptyList();

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(fields);

        service.publish(modelId);

        verify(schemaEngine).createTable(model, fields);
        verify(headerRepository).save(model);
    }

    @Test
    void publish_shouldThrowExceptionIfModelNotFound() {
        String modelId = "unknown";
        when(headerRepository.findById(modelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(modelId))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void publish_shouldThrowExceptionIfAlreadyPublished() {
        String modelId = "published_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("PUBLISHED");

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> service.publish(modelId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already published");
    }
}
