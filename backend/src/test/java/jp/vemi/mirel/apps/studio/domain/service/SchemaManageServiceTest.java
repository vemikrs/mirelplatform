/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchemaManageServiceTest {

    @Mock
    private StudioModelService studioModelService;
    @Mock
    private SchemaEngineService schemaEngine;

    @InjectMocks
    private SchemaManageService service;

    @Test
    void createDraft_shouldDelegateToDomainService() {
        String name = "New Model";
        String description = "Description";
        StuModelHeader model = new StuModelHeader();
        model.setModelId("id");

        when(studioModelService.createDraft(name, description)).thenReturn(model);

        String resultId = service.createDraft(name, description);

        assertThat(resultId).isEqualTo("id");
        verify(studioModelService).createDraft(name, description);
    }

    @Test
    void updateDraft_shouldDelegateToDomainService() {
        String modelId = "test_model";
        List<StuField> fields = Collections.emptyList();

        service.updateDraft(modelId, "Name", "Desc", fields);

        verify(studioModelService).updateDraft(modelId, "Name", "Desc", fields);
    }

    @Test
    void publish_shouldDelegateToDomainService() {
        String modelId = "test_model";
        service.publish(modelId);
        verify(studioModelService).publish(modelId, schemaEngine);
    }
}
