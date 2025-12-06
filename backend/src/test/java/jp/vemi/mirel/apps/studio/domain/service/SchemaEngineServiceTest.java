/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
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
        StuModelHeader model = new StuModelHeader();
        model.setModelId("test_model");

        StuModel field = new StuModel();
        field.setFieldName("field1");
        field.setDataType("STRING");

        schemaEngineService.createTable(model, List.of(field));

        verify(jdbcTemplate, times(1)).execute(anyString());
    }

    @Test
    void generateCreateTableSql() {
        StuModelHeader model = new StuModelHeader();
        model.setModelId("test_model");

        StuModel field1 = new StuModel();
        field1.setFieldName("name");
        field1.setDataType("STRING");
        field1.setIsRequired(true);

        StuModel field2 = new StuModel();
        field2.setFieldName("age");
        field2.setDataType("INTEGER");

        String sql = schemaEngineService.generateCreateTableSql(model, List.of(field1, field2));

        assertTrue(sql.contains("CREATE TABLE dyn_test_model"));
        assertTrue(sql.contains("name VARCHAR(255) NOT NULL"));
        assertTrue(sql.contains("age INTEGER"));
    }

    @Test
    void validateName_Invalid() {
        StuModelHeader model = new StuModelHeader();
        model.setModelId("invalid-name"); // Hyphen not allowed

        assertThrows(IllegalArgumentException.class, () -> {
            schemaEngineService.generateCreateTableSql(model, Collections.emptyList());
        });
    }
}
