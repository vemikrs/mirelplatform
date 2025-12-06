/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.application.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.vemi.mirel.apps.studio.application.dto.CreateDraftRequest;
import jp.vemi.mirel.apps.studio.application.dto.UpdateDraftRequest;
import jp.vemi.mirel.apps.studio.domain.service.SchemaManageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(controllers = StudioSchemaController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class StudioSchemaControllerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import(StudioSchemaController.class)
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemaManageService schemaManageService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createDraft_shouldReturnOk() throws Exception {
        CreateDraftRequest request = new CreateDraftRequest();
        request.setName("Test Model");
        request.setDescription("Description");

        when(schemaManageService.createDraft(any(), any())).thenReturn("model_id");

        mockMvc.perform(post("/api/studio/schema/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(schemaManageService).createDraft("Test Model", "Description");
    }

    @Test
    void updateDraft_shouldReturnOk() throws Exception {
        UpdateDraftRequest request = new UpdateDraftRequest();
        request.setName("Updated Name");
        request.setDescription("Updated Desc");
        request.setFields(Collections.emptyList());

        mockMvc.perform(put("/api/studio/schema/draft/model_id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(schemaManageService).updateDraft(eq("model_id"), eq("Updated Name"), eq("Updated Desc"), any());
    }

    @Test
    void deleteModel_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/studio/schema/model_id"))
                .andExpect(status().isOk());

        verify(schemaManageService).deleteModel("model_id");
    }

    @Test
    void publish_shouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/studio/schema/model_id/publish"))
                .andExpect(status().isOk());

        verify(schemaManageService).publish("model_id");
    }
}
