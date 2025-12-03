/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.entity.StuModelHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchemaEngineServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SchemaEngineService service;

    @Test
    void generateCreateTableSql_shouldGenerateCorrectSql() {
        StuModelHeader model = new StuModelHeader();
        model.setModelId("test_model");

        StuField field1 = new StuField();
        field1.setFieldName("name");
        field1.setFieldCode("name");
        field1.setFieldType("STRING");
        field1.setIsRequired(true);

        StuField field2 = new StuField();
        field2.setFieldName("age");
        field2.setFieldCode("age");
        field2.setFieldType("INTEGER");
        field2.setIsRequired(false);

        StuField field3 = new StuField();
        field3.setFieldCode("description");
        field3.setFieldType("TEXTAREA");

        StuField field4 = new StuField();
        field4.setFieldCode("gender");
        field4.setFieldType("RADIO");

        String sql = service.generateCreateTableSql(model, Arrays.asList(field1, field2, field3, field4));

        assertThat(sql).contains("CREATE TABLE dyn_test_model");
        assertThat(sql).contains("id UUID PRIMARY KEY");
        assertThat(sql).contains("name VARCHAR(255) NOT NULL");
        assertThat(sql).contains("age INTEGER");
        assertThat(sql).contains("description TEXT");
        assertThat(sql).contains("gender VARCHAR(255)");
        assertThat(sql).contains("created_at TIMESTAMP");
    }

    @Test
    void generateCreateTableSql_shouldThrowExceptionForInvalidName() {
        StuModelHeader model = new StuModelHeader();
        model.setModelId("invalid-name"); // Hyphen is not allowed

        assertThatThrownBy(() -> service.generateCreateTableSql(model, Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid name");
    }

    @Test
    void createTable_shouldExecuteSql() {
        StuModelHeader model = new StuModelHeader();
        model.setModelId("simple_model");

        service.createTable(model, Collections.emptyList());

        verify(jdbcTemplate).execute(anyString());
    }
}
