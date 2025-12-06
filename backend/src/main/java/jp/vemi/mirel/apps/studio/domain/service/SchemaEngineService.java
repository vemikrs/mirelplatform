package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModelHeader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for generating and executing DDL.
 */
@Service
public class SchemaEngineService {

    private final JdbcTemplate jdbcTemplate;

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public SchemaEngineService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Create a table for the given model.
     *
     * @param model
     *            The model definition
     * @param fields
     *            The field definitions
     */
    @Transactional
    public void createTable(StuModelHeader model, List<StuModel> fields) {
        String sql = generateCreateTableSql(model, fields);
        jdbcTemplate.execute(sql);
    }

    /**
     * Generate CREATE TABLE SQL.
     *
     * @param model
     *            The model definition
     * @param fields
     *            The field definitions
     * @return The SQL string
     */
    public String generateCreateTableSql(StuModelHeader model, List<StuModel> fields) {
        validateName(model.getModelId());

        // Table name prefix "dyn_" to distinguish from system tables
        String tableName = "dyn_" + model.getModelId();

        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName).append(" (");

        // Default primary key
        sql.append("id UUID PRIMARY KEY, ");

        for (StuModel field : fields) {
            validateName(field.getFieldName());
            sql.append(field.getFieldName()).append(" ");
            sql.append(mapToSqlType(field.getDataType()));

            if (Boolean.TRUE.equals(field.getIsRequired())) {
                sql.append(" NOT NULL");
            }

            sql.append(", ");
        }

        // System columns
        sql.append("created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, ");
        sql.append("updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");

        sql.append(")");

        return sql.toString();
    }

    private String mapToSqlType(String studioType) {
        if (studioType == null)
            return "VARCHAR(255)";
        return switch (studioType) {
            case "STRING" -> "VARCHAR(255)";
            case "TEXT" -> "TEXT";
            case "TEXTAREA" -> "TEXT";
            case "RADIO" -> "VARCHAR(255)";
            case "NUMBER" -> "NUMERIC";
            case "INTEGER" -> "INTEGER";
            case "BOOLEAN" -> "BOOLEAN";
            case "DATE" -> "DATE";
            case "DATETIME" -> "TIMESTAMP";
            case "JSON" -> "JSONB";
            default -> "VARCHAR(255)";
        };
    }

    private void validateName(String name) {
        if (name == null || !VALID_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
    }
}
