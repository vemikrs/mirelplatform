package jp.vemi.mirel.apps.studio.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;

@ExtendWith(MockitoExtension.class)
class DynamicEntityServiceCsvTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private StuFieldRepository fieldRepository;

    @InjectMocks
    private DynamicEntityService dynamicEntityService;

    @Test
    void exportCsv_shouldReturnCsvBytes() {
        // Arrange
        String modelId = "testModel";
        StuField field1 = new StuField();
        field1.setFieldCode("name");
        StuField field2 = new StuField();
        field2.setFieldCode("age");

        when(fieldRepository.findByModelIdOrderBySortOrder(modelId)).thenReturn(Arrays.asList(field1, field2));

        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("name", "Alice");
        row1.put("age", 30);

        // Mock findAll (which uses jdbcTemplate)
        // Since findAll is complex to mock due to jdbcTemplate, we might need to
        // partial mock or refactor.
        // For this unit test, let's assume we can mock the internal call if possible,
        // or better,
        // let's test the CSV logic by mocking the data retrieval.
        // However, DynamicEntityService.findAll uses jdbcTemplate directly.
        // A better approach for unit testing *just* the CSV logic would be to extract
        // the CSV conversion to a utility or
        // verify that exportCsv calls findAll and then converts.

        // Given the constraints and the existing code structure, let's try to mock the
        // jdbcTemplate query response.
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.singletonList(row1));

        // Act
        byte[] result = dynamicEntityService.exportCsv(modelId);

        // Assert
        assertNotNull(result);
        String csvContent = new String(result);
        assertTrue(csvContent.contains("name,age,id")); // Header
        assertTrue(csvContent.contains("Alice,30,1")); // Data
    }
}
