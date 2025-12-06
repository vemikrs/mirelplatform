package jp.vemi.mirel.apps.studio.modeler.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.studio.modeler.domain.dto.request.StuApiRequest;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuRecord;
import jp.vemi.mirel.apps.studio.modeler.domain.facade.CodeMaintenanceEngine;
import jp.vemi.mirel.apps.studio.modeler.domain.facade.DictionaryMaintenanceEngine;
import jp.vemi.mirel.apps.studio.modeler.domain.facade.StuEngine;

class StuApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StuEngine schemaEngine;

    @Mock
    private DictionaryMaintenanceEngine dictionaryMaintenanceEngine;

    @Mock
    private CodeMaintenanceEngine codeMaintenanceEngine;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StuApiController stuApiController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(stuApiController).build();
    }

    @Test
    void testList() throws Exception {
        when(schemaEngine.getRecordList(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/studio/modeler/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": {\"modelId\": \"testModel\"}}"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoad() throws Exception {
        StuRecord mockRecord = StuRecord.builder().id("testId").build();
        when(schemaEngine.getRecord(anyString())).thenReturn(mockRecord);

        mockMvc.perform(post("/api/studio/modeler/load")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": {\"recordId\": \"testId\"}}"))
                .andExpect(status().isOk());
    }
}
