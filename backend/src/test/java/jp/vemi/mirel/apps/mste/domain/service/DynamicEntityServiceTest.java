/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import jp.vemi.mirel.apps.mste.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.mste.domain.dao.repository.StuFieldRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
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
}
