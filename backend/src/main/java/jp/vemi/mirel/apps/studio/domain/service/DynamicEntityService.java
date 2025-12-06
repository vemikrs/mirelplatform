/*
 * Copyright(c) 2025 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.studio.domain.service;

import jp.vemi.mirel.apps.studio.modeler.domain.entity.StuModel;
import jp.vemi.mirel.apps.studio.modeler.domain.repository.StuModelRepository;
import jp.vemi.mirel.foundation.feature.TenantContext;
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
    private final StuModelRepository fieldRepository;

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    public DynamicEntityService(JdbcTemplate jdbcTemplate, StuModelRepository fieldRepository) {
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
        validateModelId(modelId);
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
        validateModelId(modelId);
        String tableName = "dyn_" + modelId;
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        try {
            return jdbcTemplate.queryForMap(sql, id);
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
        validateModelId(modelId);
        String tableName = "dyn_" + modelId;

        List<StuModel> fields = fieldRepository.findByPk_ModelIdAndTenantId(modelId, TenantContext.getTenantId());
        // TODO: Ensure fields are sorted by sort order if needed, or rely on fetch
        // order if consistent

        Map<String, StuModel> fieldMap = fields.stream()
                .collect(Collectors.toMap(StuModel::getFieldName, f -> f));

        validateData(data, fieldMap);

        StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (id, created_at, updated_at");
        StringBuilder values = new StringBuilder("VALUES (?, ?, ?");
        List<Object> params = new ArrayList<>();

        String id = data.containsKey("id") ? data.get("id").toString() : UUID.randomUUID().toString();
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
        validateModelId(modelId);
        String tableName = "dyn_" + modelId;

        List<StuModel> fields = fieldRepository.findByPk_ModelIdAndTenantId(modelId, TenantContext.getTenantId());
        Map<String, StuModel> fieldMap = fields.stream()
                .collect(Collectors.toMap(StuModel::getFieldName, f -> f));

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
        params.add(id);

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
        validateModelId(modelId);
        String tableName = "dyn_" + modelId;
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private void validateData(Map<String, Object> data, Map<String, StuModel> fieldMap) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            StuModel field = fieldMap.get(key);

            if (field == null)
                continue; // Ignore unknown fields or handle as error

            if (Boolean.TRUE.equals(field.getIsRequired()) && (value == null || value.toString().isEmpty())) {
                throw new IllegalArgumentException("Field " + field.getFieldName() + " is required");
            }

            if (value != null) {
                String strValue = value.toString();

                if ("NUMBER".equals(field.getDataType())) {
                    try {
                        double numValue = Double.parseDouble(strValue);
                        if (field.getMinValue() != null && numValue < field.getMinValue().doubleValue()) {
                            throw new IllegalArgumentException(
                                    "Field " + field.getFieldName() + " must be >= " + field.getMinValue());
                        }
                        if (field.getMaxValue() != null && numValue > field.getMaxValue().doubleValue()) {
                            throw new IllegalArgumentException(
                                    "Field " + field.getFieldName() + " must be <= " + field.getMaxValue());
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Field " + field.getFieldName() + " must be a number");
                    }
                } else if ("STRING".equals(field.getDataType()) || "TEXT".equals(field.getDataType())) {
                    if (field.getMinLength() != null && strValue.length() < field.getMinLength()) {
                        throw new IllegalArgumentException("Field " + field.getFieldName() + " must be at least "
                                + field.getMinLength() + " characters");
                    }
                    if (field.getMaxLength() != null && strValue.length() > field.getMaxLength()) {
                        throw new IllegalArgumentException("Field " + field.getFieldName() + " must be at most "
                                + field.getMaxLength() + " characters");
                    }
                    if (field.getRegexPattern() != null && !field.getRegexPattern().isEmpty()) {
                        if (!Pattern.matches(field.getRegexPattern(), strValue)) {
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

    private void validateModelId(String modelId) {
        validateName(modelId);
        if (!fieldRepository.existsByPk_ModelIdAndTenantId(modelId, TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Model not found: " + modelId);
        }
    }

    /**
     * Export data as CSV.
     *
     * @param modelId
     *            The model ID
     * @return CSV content as byte array
     */
    public byte[] exportCsv(String modelId) {
        List<Map<String, Object>> data = findAll(modelId);
        List<StuModel> fields = fieldRepository.findByPk_ModelIdAndTenantId(modelId, TenantContext.getTenantId());

        if (fields.isEmpty()) {
            return new byte[0];
        }

        com.fasterxml.jackson.dataformat.csv.CsvMapper mapper = new com.fasterxml.jackson.dataformat.csv.CsvMapper();
        com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder schemaBuilder = com.fasterxml.jackson.dataformat.csv.CsvSchema
                .builder();

        // Add columns based on fields
        for (StuModel field : fields) {
            schemaBuilder.addColumn(field.getFieldName());
        }

        // Add id column if not present (usually good to have for updates, but maybe
        // optional for export)
        // For now, let's stick to defined fields + id
        schemaBuilder.addColumn("id");

        com.fasterxml.jackson.dataformat.csv.CsvSchema schema = schemaBuilder.build().withHeader();

        try {
            return mapper.writer(schema).writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }
    }

    /**
     * Import data from CSV.
     *
     * @param modelId
     *            The model ID
     * @param csvData
     *            CSV content
     */
    @Transactional
    public void importCsv(String modelId, byte[] csvData) {
        List<StuModel> fields = fieldRepository.findByPk_ModelIdAndTenantId(modelId, TenantContext.getTenantId());
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Model has no fields defined");
        }

        com.fasterxml.jackson.dataformat.csv.CsvMapper mapper = new com.fasterxml.jackson.dataformat.csv.CsvMapper();
        com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder schemaBuilder = com.fasterxml.jackson.dataformat.csv.CsvSchema
                .builder();

        for (StuModel field : fields) {
            schemaBuilder.addColumn(field.getFieldName());
        }
        schemaBuilder.addColumn("id"); // Optional ID for updates

        com.fasterxml.jackson.dataformat.csv.CsvSchema schema = schemaBuilder.build().withHeader();

        try {
            com.fasterxml.jackson.databind.MappingIterator<Map<String, Object>> it = mapper
                    .readerFor(Map.class)
                    .with(schema)
                    .readValues(csvData);

            while (it.hasNext()) {
                Map<String, Object> row = it.next();
                // If ID exists and is valid, update. Otherwise insert.
                String idStr = (String) row.get("id");
                if (idStr != null && !idStr.isEmpty()) {
                    // Try to find existing
                    if (findById(modelId, idStr) != null) {
                        update(modelId, idStr, row);
                    } else {
                        // ID provided but not found, treat as new insert with that ID?
                        // Or just insert as new. Let's insert as new but we need to handle the ID.
                        // For simplicity, let's assume import is mostly for new data or bulk updates
                        // where ID matches.
                        // If ID is present but not found, we can insert it with that ID if we modify
                        // insert() to accept ID.
                        // Current insert() generates ID. Let's just ignore ID if not found and create
                        // new.
                        insert(modelId, row);
                    }
                } else {
                    insert(modelId, row);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV", e);
        }
    }
}
