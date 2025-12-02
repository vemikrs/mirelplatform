/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * 汎用CSVデータローダー.
 * resources/db/data 配下のCSVファイルを読み込み、ファイル名をテーブル名としてデータをINSERTします。
 */
@Component
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private final JdbcTemplate jdbcTemplate;
    private final CsvMapper csvMapper;

    public DataLoader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.csvMapper = new CsvMapper();
    }

    /**
     * システムデータをロードします (resources/db/data/system/*.csv)
     */
    @Transactional
    public void loadSystemData() {
        loadDataFromLocation("classpath:db/data/system/*.csv");
    }

    /**
     * サンプルデータをロードします (resources/db/data/sample/*.csv)
     */
    @Transactional
    public void loadSampleData() {
        loadDataFromLocation("classpath:db/data/sample/*.csv");
    }

    private void loadDataFromLocation(String locationPattern) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(locationPattern);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(".csv")) {
                    continue;
                }

                String tableName = filename.replace(".csv", "");
                log.info("Loading data for table: {} from {}", tableName, filename);
                loadCsvToTable(resource, tableName);
            }
        } catch (IOException e) {
            log.error("Failed to load data from location: {}", locationPattern, e);
            throw new RuntimeException("Data loading failed", e);
        }
    }

    private void loadCsvToTable(Resource resource, String tableName) {
        try (InputStream is = resource.getInputStream()) {
            CsvSchema schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnSeparator(',')
                    .withQuoteChar('"')
                    .withEscapeChar('\\');
            MappingIterator<Map<String, String>> it = csvMapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(is);

            while (it.hasNext()) {
                Map<String, String> row = it.next();
                if (row.isEmpty()) {
                    continue;
                }
                insertRow(tableName, row);
            }
        } catch (IOException e) {
            log.error("Failed to read CSV file: {}", resource.getFilename(), e);
            throw new RuntimeException("CSV reading failed", e);
        }
    }

    private void insertRow(String tableName, Map<String, String> row) {
        if (row.isEmpty()) {
            return;
        }

        // Check if record exists (assuming 'id' column exists and is the primary key,
        // or composite key handling needed?)
        // For simplicity and idempotency, we can use INSERT ON CONFLICT DO NOTHING
        // (Postgres) or MERGE (H2).
        // However, standard JDBC doesn't support generic upsert easily without knowing
        // PK.
        // Given the requirement is "sample data", we can check existence if we know the
        // PK.
        // But we don't know the PK generically.
        // Strategy: Try INSERT, ignore duplicate key errors? Or just simple INSERT and
        // let it fail if exists?
        // Better: Construct a SELECT count(*) query using all columns? No, that's too
        // heavy.
        // Let's assume for now we just INSERT and catch duplicate key exception, or
        // check if table is empty?
        // The previous implementation checked `existsById`.

        // Since we want to be generic, maybe we can try to find a column named 'id' or
        // 'user_id' etc.
        // Or, we can just construct the INSERT statement.

        List<String> columns = new ArrayList<>(row.keySet());
        List<Object> values = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (String col : columns) {
            String val = row.get(col);
            // Handle nulls or special types if needed. For now, pass as String (JDBC driver
            // usually handles conversion)
            // Exception: boolean
            if ("true".equalsIgnoreCase(val) || "false".equalsIgnoreCase(val)) {
                values.add(Boolean.parseBoolean(val));
            } else if (val == null || val.isEmpty()) {
                values.add(null);
            } else {
                values.add(val);
            }
            placeholders.add("?");
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(", ", columns),
                String.join(", ", placeholders));

        try {
            jdbcTemplate.update(sql, values.toArray());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.debug("Skipping duplicate record for table {}: {}", tableName, row);
        } catch (Exception e) {
            log.error("Failed to insert row into {}: {}", tableName, row, e);
            // Don't throw, just log and continue? Or fail fast?
            // For sample data, maybe fail fast is better to detect schema mismatches.
            throw e;
        }
    }
}
