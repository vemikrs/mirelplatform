package jp.vemi.mirel.apps.schema.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.schema.domain.dto.request.SchemaApiRequest;
import jp.vemi.mirel.apps.schema.domain.entity.SchRecord;
import jp.vemi.mirel.apps.schema.domain.facade.CodeMaintenanceEngine;
import jp.vemi.mirel.apps.schema.domain.facade.DictionaryMaintenanceEngine;
import jp.vemi.mirel.apps.schema.domain.facade.SchemaEngine;

@org.springframework.boot.test.context.SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
public class SchemaApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemaEngine schemaEngine;

    @MockBean
    private DictionaryMaintenanceEngine dictionaryMaintenanceEngine;

    @MockBean
    private CodeMaintenanceEngine codeMaintenanceEngine;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testList() throws Exception {
        when(schemaEngine.getRecordList(anyString())).thenReturn(Collections.emptyList());

        SchemaApiRequest request = SchemaApiRequest.builder()
                .content(Map.of("modelId", "testModel"))
                .build();

        mockMvc.perform(post("/apps/schema/api/list")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    public void testSave() throws Exception {
        SchRecord mockRecord = SchRecord.builder().id("testId").build();
        when(schemaEngine.saveRecord(anyString(), anyString(), any())).thenReturn(mockRecord);

        SchemaApiRequest request = SchemaApiRequest.builder()
                .content(Map.of(
                        "modelId", "testModel",
                        "recordId", "testId",
                        "record", Map.of("field", "value")))
                .build();

        mockMvc.perform(post("/apps/schema/api/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }
}
