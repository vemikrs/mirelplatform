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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaEngineServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SchemaEngineService schemaEngineService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createTable() {
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId("test_model");

        StuField field = new StuField();
        field.setFieldCode("field1");
        field.setFieldType("STRING");

        schemaEngineService.createTable(model, List.of(field));

        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void generateCreateTableSql() {
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId("test_model");

        StuField field1 = new StuField();
        field1.setFieldCode("name");
        field1.setFieldType("STRING");
        field1.setIsRequired(true);

        StuField field2 = new StuField();
        field2.setFieldCode("age");
        field2.setFieldType("INTEGER");

        String sql = schemaEngineService.generateCreateTableSql(model, List.of(field1, field2));

        assertTrue(sql.contains("CREATE TABLE dyn_test_model"));
        assertTrue(sql.contains("name VARCHAR(255) NOT NULL"));
        assertTrue(sql.contains("age INTEGER"));
    }

    @Test
    void validateName_Invalid() {
        StuModelHeaderLegacy model = new StuModelHeaderLegacy();
        model.setModelId("invalid-name"); // Hyphen not allowed

        assertThrows(IllegalArgumentException.class, () -> {
            schemaEngineService.generateCreateTableSql(model, Collections.emptyList());
        });
    }
}
