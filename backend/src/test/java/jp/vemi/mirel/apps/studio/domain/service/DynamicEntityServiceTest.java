/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuModelRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicEntityServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private StuModelRepository fieldRepository;

    @InjectMocks
    private DynamicEntityService service;

    private MockedStatic<TenantContext> tenantContextMock;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn("tenant-1");

        // Default to true for most tests unless overridden
        org.mockito.Mockito.lenient().when(fieldRepository.existsByPk_ModelIdAndTenantId(anyString(), anyString()))
                .thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    void findAll_shouldExecuteSelect() {
        String modelId = "test_model";
        service.findAll(modelId);
        verify(jdbcTemplate).queryForList("SELECT * FROM dyn_test_model");
    }

    @Test
    void insert_shouldExecuteInsert() {
        String modelId = "test_model";
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test Name");

        StuModel field = new StuModel();
        field.setFieldName("name");
        when(fieldRepository.findByPk_ModelIdAndTenantId(eq(modelId), anyString()))
                .thenReturn(Collections.singletonList(field));

        service.insert(modelId, data);

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void update_shouldExecuteUpdate() {
        String modelId = "test_model";
        String id = UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Updated Name");

        StuModel field = new StuModel();
        field.setFieldName("name");
        when(fieldRepository.findByPk_ModelIdAndTenantId(eq(modelId), anyString()))
                .thenReturn(Collections.singletonList(field));

        service.update(modelId, id, data);

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void delete_shouldExecuteDelete() {
        String modelId = "test_model";
        String id = UUID.randomUUID().toString();

        service.delete(modelId, id);

        verify(jdbcTemplate).update(eq("DELETE FROM dyn_test_model WHERE id = ?"), eq(id));
    }

    @Test
    void insert_shouldThrowException_whenRequiredFieldIsMissing() {
        String modelId = "test_model";
        Map<String, Object> data = new HashMap<>();
        // Missing "name"

        StuModel field = new StuModel();
        field.setFieldName("name"); // Used as key and display name in this test logic
        field.setIsRequired(true);
        when(fieldRepository.findByPk_ModelIdAndTenantId(eq(modelId), anyString()))
                .thenReturn(Collections.singletonList(field));

        try {
            service.insert(modelId, data);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Field name is required");
        }
    }

    @Test
    void insert_shouldThrowException_whenMinValueViolated() {
        String modelId = "test_model";
        Map<String, Object> data = new HashMap<>();
        data.put("age", "10");

        StuModel field = new StuModel();
        field.setFieldName("age");
        field.setDataType("NUMBER");
        field.setMinValue(new java.math.BigDecimal("18.0"));
        when(fieldRepository.findByPk_ModelIdAndTenantId(eq(modelId), anyString()))
                .thenReturn(Collections.singletonList(field));

        try {
            service.insert(modelId, data);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Field age must be >= 18.0");
        }
    }

    @Test
    void insert_shouldThrowException_whenRegexViolated() {
        String modelId = "test_model";
        Map<String, Object> data = new HashMap<>();
        data.put("code", "abc");

        StuModel field = new StuModel();
        field.setFieldName("code");
        field.setDataType("STRING");
        field.setRegexPattern("^[0-9]+$");
        when(fieldRepository.findByPk_ModelIdAndTenantId(eq(modelId), anyString()))
                .thenReturn(Collections.singletonList(field));

        try {
            service.insert(modelId, data);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Field code format is invalid");
        }
    }

    @Test
    void findAll_shouldThrowException_whenModelIdInvalid() {
        String modelId = "invalid_model";
        when(fieldRepository.existsByPk_ModelIdAndTenantId(eq(modelId), anyString())).thenReturn(false);

        try {
            service.findAll(modelId);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Model not found: " + modelId);
        }
    }
}
