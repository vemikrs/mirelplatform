/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeader;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuModelHeaderRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudioModelServiceTest {

    @Mock
    private StuModelHeaderRepository headerRepository;
    @Mock
    private StuFieldRepository fieldRepository;

    @InjectMocks
    private StudioModelService service;

    @Test
    void createDraft_shouldSaveNewModel() {
        String name = "New Model";
        String description = "Description";

        StuModelHeader created = service.createDraft(name, description);

        assertThat(created).isNotNull();
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
        SchemaEngineService schemaEngine = org.mockito.Mockito.mock(SchemaEngineService.class);

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(fields);

        service.publish(modelId, schemaEngine);

        verify(schemaEngine).createTable(model, fields);
        verify(headerRepository).save(model);
        assertThat(model.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void publish_shouldThrowExceptionIfAlreadyPublished() {
        String modelId = "published_model";
        StuModelHeader model = new StuModelHeader();
        model.setModelId(modelId);
        model.setStatus("PUBLISHED");

        SchemaEngineService schemaEngine = org.mockito.Mockito.mock(SchemaEngineService.class);

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThatThrownBy(() -> service.publish(modelId, schemaEngine))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already published");
    }
}
