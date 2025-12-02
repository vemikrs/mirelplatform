/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.domain.dao.entity.StuField;
import jp.vemi.mirel.apps.studio.domain.dao.repository.StuFieldRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for dynamic entity CRUD operations.
 */
@Service
public class DynamicEntityService {

    private final JdbcTemplate jdbcTemplate;
    private final StuFieldRepository fieldRepository;

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public DynamicEntityService(JdbcTemplate jdbcTemplate, StuFieldRepository fieldRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.fieldRepository = fieldRepository;
    }

    /**
     * Find all records for a model.
     *
     * @param modelId
     *            The model ID
     * @return List of records
     */
    public List<Map<String, Object>> findAll(String modelId) {
        validateName(modelId);
        String tableName = "dyn_" + modelId;
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Find a record by ID.
     *
     * @param modelId
     *            The model ID
     * @param id
     *            The record ID
     * @return The record, or null if not found
     */
    public Map<String, Object> findById(String modelId, String id) {
        validateName(modelId);
        String tableName = "dyn_" + modelId;
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        try {
            return jdbcTemplate.queryForMap(sql, UUID.fromString(id));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Insert a new record.
     *
     * @param modelId
     *            The model ID
     * @param data
     *            The data to insert
     */
    @Transactional
    public void insert(String modelId, Map<String, Object> data) {
        validateName(modelId);
        String tableName = "dyn_" + modelId;

        List<StuField> fields = fieldRepository.findByModelIdOrderBySortOrder(modelId);
        Map<String, StuField> fieldMap = fields.stream()
                .collect(Collectors.toMap(StuField::getFieldCode, f -> f));

        validateData(data, fieldMap);

        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (id, created_at, updated_at");
        StringBuilder values = new StringBuilder("VALUES (?, ?, ?");
        List<Object> params = new ArrayList<>();

        UUID id = UUID.randomUUID();
        params.add(id);
        params.add(LocalDateTime.now());
        params.add(LocalDateTime.now());

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (fieldMap.containsKey(key)) {
                validateName(key);
                sql.append(", ").append(key);
                values.append(", ?");
                params.add(entry.getValue());
            }
        }

        sql.append(") ").append(values).append(")");

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * Update an existing record.
     *
     * @param modelId
     *            The model ID
     * @param id
     *            The record ID
     * @param data
     *            The data to update
     */
    @Transactional
    public void update(String modelId, String id, Map<String, Object> data) {
        validateName(modelId);
        String tableName = "dyn_" + modelId;

        List<StuField> fields = fieldRepository.findByModelIdOrderBySortOrder(modelId);
        Map<String, StuField> fieldMap = fields.stream()
                .collect(Collectors.toMap(StuField::getFieldCode, f -> f));

        validateData(data, fieldMap);

        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET updated_at = ?");
        List<Object> params = new ArrayList<>();
        params.add(LocalDateTime.now());

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (fieldMap.containsKey(key)) {
                validateName(key);
                sql.append(", ").append(key).append(" = ?");
                params.add(entry.getValue());
            }
        }

        sql.append(" WHERE id = ?");
        params.add(UUID.fromString(id));

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

    /**
     * Delete a record.
     *
     * @param modelId
     *            The model ID
     * @param id
     *            The record ID
     */
    @Transactional
    public void delete(String modelId, String id) {
        validateName(modelId);
        String tableName = "dyn_" + modelId;
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        jdbcTemplate.update(sql, UUID.fromString(id));
    }

    private void validateData(Map<String, Object> data, Map<String, StuField> fieldMap) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            StuField field = fieldMap.get(key);

            if (field == null)
                continue; // Ignore unknown fields or handle as error

            if (Boolean.TRUE.equals(field.getIsRequired()) && (value == null || value.toString().isEmpty())) {
                throw new IllegalArgumentException("Field " + field.getFieldName() + " is required");
            }

            if (value != null) {
                String strValue = value.toString();

                if ("NUMBER".equals(field.getFieldType())) {
                    try {
                        double numValue = Double.parseDouble(strValue);
                        if (field.getMinValue() != null && numValue < field.getMinValue()) {
                            throw new IllegalArgumentException(
                                    "Field " + field.getFieldName() + " must be >= " + field.getMinValue());
                        }
                        if (field.getMaxValue() != null && numValue > field.getMaxValue()) {
                            throw new IllegalArgumentException(
                                    "Field " + field.getFieldName() + " must be <= " + field.getMaxValue());
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Field " + field.getFieldName() + " must be a number");
                    }
                } else if ("STRING".equals(field.getFieldType()) || "TEXT".equals(field.getFieldType())) {
                    if (field.getMinLength() != null && strValue.length() < field.getMinLength()) {
                        throw new IllegalArgumentException("Field " + field.getFieldName() + " must be at least "
                                + field.getMinLength() + " characters");
                    }
                    if (field.getMaxLength() != null && strValue.length() > field.getMaxLength()) {
                        throw new IllegalArgumentException("Field " + field.getFieldName() + " must be at most "
                                + field.getMaxLength() + " characters");
                    }
                    if (field.getValidationRegex() != null && !field.getValidationRegex().isEmpty()) {
                        if (!Pattern.matches(field.getValidationRegex(), strValue)) {
                            throw new IllegalArgumentException("Field " + field.getFieldName() + " format is invalid");
                        }
                    }
                }
            }
        }
    }

    private void validateName(String name) {
        if (name == null || !VALID_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
    }
}
