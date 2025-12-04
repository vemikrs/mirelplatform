/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeaderLegacy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SchemaManageServiceTest {

    @Mock
    private StudioModelService studioModelService;
    @Mock
    private SchemaEngineService schemaEngine;

    @InjectMocks
    private SchemaManageService schemaManageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createDraft() {
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId("id-1");
        when(studioModelService.createDraft("Name", "Desc")).thenReturn(model);

        String id = schemaManageService.createDraft("Name", "Desc");

        assertEquals("id-1", id);
    }

    @Test
    void updateDraft() {
        String modelId = "id-1";
        List<StuField> fields = Collections.emptyList();

        schemaManageService.updateDraft(modelId, "Name", "Desc", fields);

        verify(studioModelService).updateDraft(modelId, "Name", "Desc", fields);
    }

    @Test
    void publish() {
        String modelId = "id-1";

        schemaManageService.publish(modelId);

        verify(studioModelService).publish(modelId, schemaEngine);
    }
}
