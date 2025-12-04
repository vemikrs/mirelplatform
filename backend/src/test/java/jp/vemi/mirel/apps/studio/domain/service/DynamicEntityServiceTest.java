/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicEntityServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private StuFieldRepository fieldRepository;

    @InjectMocks
    private DynamicEntityService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Default to true for most tests unless overridden
        org.mockito.Mockito.lenient().when(fieldRepository.existsByModelId(anyString())).thenReturn(true);
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

        StuField field = new StuField();
        field.setFieldName("name");
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.singletonList(field));

        service.insert(modelId, data);

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void update_shouldExecuteUpdate() {
        String modelId = "test_model";
        String id = UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Updated Name");

        StuField field = new StuField();
        field.setFieldName("name");
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.singletonList(field));

        service.update(modelId, id, data);

        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void delete_shouldExecuteDelete() {
        String modelId = "test_model";
        String id = UUID.randomUUID().toString();

        service.delete(modelId, id);

        verify(jdbcTemplate).update(eq("DELETE FROM dyn_test_model WHERE id = ?"), any(UUID.class));
    }

    @Test
    void insert_shouldThrowException_whenRequiredFieldIsMissing() {
        String modelId = "test_model";
        Map<String, Object> data = new HashMap<>();
        // Missing "name"

        StuField field = new StuField();
        field.setFieldCode("name");
        field.setFieldName("Name");
        field.setIsRequired(true);
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.singletonList(field));

        try {
            service.insert(modelId, data);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Field Name is required");
        }
    }

    @Test
    void insert_shouldThrowException_whenMinValueViolated() {
        String modelId = "test_model";
        Map<String, Object> data = new HashMap<>();
        data.put("age", "10");

        StuField field = new StuField();
        field.setFieldCode("age");
        field.setFieldName("Age");
        field.setFieldType("NUMBER");
        field.setMinValue(18.0);
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.singletonList(field));

        try {
            service.insert(modelId, data);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Field Age must be >= 18.0");
        }
    }

    @Test
    void insert_shouldThrowException_whenRegexViolated() {
        String modelId = "test_model";
        Map<String, Object> data = new HashMap<>();
        data.put("code", "abc");

        StuField field = new StuField();
        field.setFieldCode("code");
        field.setFieldName("Code");
        field.setFieldType("STRING");
        field.setValidationRegex("^[0-9]+$");
        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Collections.singletonList(field));

        try {
            service.insert(modelId, data);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Field Code format is invalid");
        }
    }

    @Test
    void findAll_shouldThrowException_whenModelIdInvalid() {
        String modelId = "invalid_model";
        when(fieldRepository.existsByModelId(modelId)).thenReturn(false);

        try {
            service.findAll(modelId);
        } catch (IllegalArgumentException e) {
            assert e.getMessage().equals("Model not found: " + modelId);
        }
    }
}
