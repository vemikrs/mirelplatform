/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeaderLegacy;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuModelHeaderLegacyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudioModelServiceTest {

    @Mock
    private StuModelHeaderLegacyRepository headerRepository;

    @Mock
    private StuFieldRepository fieldRepository;

    @InjectMocks
    private StudioModelService studioModelService;

    @Test
    void createDraft() {
        when(headerRepository.save(any(StuModelHeaderLegacy.class))).thenAnswer(i -> i.getArguments()[0]);

        StuModelHeaderLegacy result = studioModelService.createDraft("Test Model", "Description");

        assertNotNull(result);
        assertEquals("Test Model", result.getModelName());
        assertEquals("Description", result.getDescription());
        assertEquals("DRAFT", result.getStatus());
        assertEquals(1, result.getVersion());
        assertNotNull(result.getModelId());
    }

    @Test
    void updateDraft() {
        String modelId = "test-id";
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId(modelId);
        model.setStatus("DRAFT");

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.emptyList());

        studioModelService.updateDraft(modelId, "New Name", "New Desc", Collections.emptyList());

        assertEquals("New Name", model.getModelName());
        assertEquals("New Desc", model.getDescription());
        verify(headerRepository, times(1)).save(model);
    }

    @Test
    void updateDraft_Published() {
        String modelId = "test-id";
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId(modelId);
        model.setStatus("PUBLISHED");

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThrows(IllegalStateException.class, () -> {
            studioModelService.updateDraft(modelId, "New Name", "New Desc", Collections.emptyList());
        });
    }

    @Test
    void deleteModel() {
        String modelId = "test-id";
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId(modelId);

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.emptyList());

        studioModelService.deleteModel(modelId);

        verify(headerRepository, times(1)).delete(model);
    }

    @Test
    void getModel() {
        String modelId = "test-id";
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId(modelId);

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        StuModelHeaderLegacy result = studioModelService.getModel(modelId);

        assertEquals(model, result);
    }

    @Test
    void publish() {
        String modelId = "test-id";
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId(modelId);
        model.setStatus("DRAFT");

        SchemaEngineService schemaEngine = mock(SchemaEngineService.class);

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.emptyList());

        studioModelService.publish(modelId, schemaEngine);

        assertEquals("PUBLISHED", model.getStatus());
        verify(schemaEngine, times(1)).createTable(eq(model), anyList());
        verify(headerRepository, times(1)).save(model);
    }

    @Test
    void publish_AlreadyPublished() {
        String modelId = "test-id";
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId(modelId);
        model.setStatus("PUBLISHED");

        SchemaEngineService schemaEngine = mock(SchemaEngineService.class);

        when(headerRepository.findById(modelId)).thenReturn(Optional.of(model));

        assertThrows(IllegalStateException.class, () -> {
            studioModelService.publish(modelId, schemaEngine);
        });
    }
}
