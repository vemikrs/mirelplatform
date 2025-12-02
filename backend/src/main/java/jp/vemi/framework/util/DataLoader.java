/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.framework.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

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
 * JDBCメタデータを使用してカラム型を取得し、適切な型変換を行います。
 */
@Component
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final CsvMapper csvMapper;
    
    /** テーブルごとのカラム型情報キャッシュ */
    private final Map<String, Map<String, ColumnInfo>> tableColumnCache = new ConcurrentHashMap<>();
    
    /** データロード済みフラグ（重複ロード防止） */
    private volatile boolean systemDataLoaded = false;
    private volatile boolean sampleDataLoaded = false;

    public DataLoader(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.csvMapper = new CsvMapper();
    }

    /**
     * システムデータをロードします (resources/db/data/system/*.csv)
     * 複数回呼ばれても1回のみ実行します。
     */
    @Transactional
    public synchronized void loadSystemData() {
        if (systemDataLoaded) {
            log.debug("System data already loaded, skipping");
            return;
        }
        loadDataFromLocation("classpath:db/data/system/*.csv");
        systemDataLoaded = true;
    }

    /**
     * サンプルデータをロードします (resources/db/data/sample/*.csv)
     * 複数回呼ばれても1回のみ実行します。
     */
    @Transactional
    public synchronized void loadSampleData() {
        if (sampleDataLoaded) {
            log.debug("Sample data already loaded, skipping");
            return;
        }
        loadDataFromLocation("classpath:db/data/sample/*.csv");
        sampleDataLoaded = true;
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
                
                // テーブルが存在するか確認
                if (!tableExists(tableName)) {
                    log.warn("Table {} does not exist, skipping CSV file: {}", tableName, filename);
                    continue;
                }
                
                log.info("Loading data for table: {} from {}", tableName, filename);
                loadCsvToTable(resource, tableName);
            }
        } catch (IOException e) {
            log.error("Failed to load data from location: {}", locationPattern, e);
            throw new RuntimeException("Data loading failed", e);
        }
    }

    /**
     * テーブルが存在するかチェック
     */
    private boolean tableExists(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            // PostgreSQLは小文字で管理するため、両方試す
            try (ResultSet rs = metaData.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.warn("Failed to check table existence: {}", tableName, e);
            return false;
        }
    }

    /**
     * テーブルのカラム情報を取得（キャッシュ付き）
     */
    private Map<String, ColumnInfo> getColumnInfo(String tableName) {
        return tableColumnCache.computeIfAbsent(tableName, this::fetchColumnInfo);
    }

    /**
     * JDBCメタデータからカラム情報を取得
     */
    private Map<String, ColumnInfo> fetchColumnInfo(String tableName) {
        Map<String, ColumnInfo> columnInfoMap = new HashMap<>();
        
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // PostgreSQLは小文字でテーブル名を管理
            String normalizedTableName = tableName.toLowerCase();
            
            try (ResultSet rs = metaData.getColumns(null, null, normalizedTableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME").toLowerCase();
                    int dataType = rs.getInt("DATA_TYPE");
                    String typeName = rs.getString("TYPE_NAME");
                    int nullable = rs.getInt("NULLABLE");
                    
                    columnInfoMap.put(columnName, new ColumnInfo(columnName, dataType, typeName, nullable == DatabaseMetaData.columnNullable));
                    log.trace("Column info: {} - type={} ({}), nullable={}", columnName, dataType, typeName, nullable);
                }
            }
            
            if (columnInfoMap.isEmpty()) {
                log.warn("No columns found for table: {}. The table might not exist or use different case.", tableName);
            }
        } catch (SQLException e) {
            log.error("Failed to fetch column info for table: {}", tableName, e);
            throw new RuntimeException("Failed to fetch column metadata", e);
        }
        
        return columnInfoMap;
    }

    private void loadCsvToTable(Resource resource, String tableName) {
        // 先にカラム情報を取得
        Map<String, ColumnInfo> columnInfo = getColumnInfo(tableName);
        if (columnInfo.isEmpty()) {
            log.warn("Skipping table {} - no column information available", tableName);
            return;
        }
        
        try (InputStream is = resource.getInputStream()) {
            CsvSchema schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withColumnSeparator(',')
                    .withQuoteChar('"')
                    .withEscapeChar('\\');
            MappingIterator<Map<String, String>> it = csvMapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(is);

            int insertedCount = 0;
            int skippedCount = 0;
            
            while (it.hasNext()) {
                Map<String, String> row = it.next();
                if (row.isEmpty()) {
                    continue;
                }
                if (insertRow(tableName, row, columnInfo)) {
                    insertedCount++;
                } else {
                    skippedCount++;
                }
            }
            
            log.info("Loaded {} records into {} (skipped {} duplicates)", insertedCount, tableName, skippedCount);
        } catch (IOException e) {
            log.error("Failed to read CSV file: {}", resource.getFilename(), e);
            throw new RuntimeException("CSV reading failed", e);
        }
    }

    private boolean insertRow(String tableName, Map<String, String> row, Map<String, ColumnInfo> columnInfo) {
        if (row.isEmpty()) {
            return false;
        }

        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (Map.Entry<String, String> entry : row.entrySet()) {
            String col = entry.getKey().toLowerCase();
            String val = entry.getValue();
            
            ColumnInfo info = columnInfo.get(col);
            if (info == null) {
                log.warn("Column {} not found in table {} metadata, skipping", col, tableName);
                continue;
            }
            
            columns.add(col);
            values.add(convertValue(val, info));
            placeholders.add("?");
        }

        if (columns.isEmpty()) {
            log.warn("No valid columns to insert for table {}", tableName);
            return false;
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(", ", columns),
                String.join(", ", placeholders));

        try {
            jdbcTemplate.update(sql, values.toArray());
            return true;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.debug("Skipping duplicate record for table {}", tableName);
            return false;
        } catch (Exception e) {
            log.error("Failed to insert row into {}: columns={}, values={}", tableName, columns, values, e);
            throw e;
        }
    }

    /**
     * カラム型に基づいて値を変換
     */
    private Object convertValue(String value, ColumnInfo columnInfo) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        int sqlType = columnInfo.dataType();
        String typeName = columnInfo.typeName().toLowerCase();

        try {
            return switch (sqlType) {
                // 整数型
                case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> Integer.parseInt(value);
                case Types.BIGINT -> Long.parseLong(value);
                
                // 浮動小数点型
                case Types.FLOAT, Types.REAL -> Float.parseFloat(value);
                case Types.DOUBLE -> Double.parseDouble(value);
                case Types.DECIMAL, Types.NUMERIC -> new BigDecimal(value);
                
                // ブール型
                case Types.BOOLEAN, Types.BIT -> parseBoolean(value);
                
                // 日時型
                case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> parseTimestamp(value);
                case Types.DATE -> java.sql.Date.valueOf(value);
                case Types.TIME, Types.TIME_WITH_TIMEZONE -> java.sql.Time.valueOf(value);
                
                // JSON型 (PostgreSQLのjsonb/json) および UUID型
                case Types.OTHER -> {
                    if ("jsonb".equals(typeName) || "json".equals(typeName)) {
                        // PostgreSQLのJSONB/JSONはPGobjectで渡す
                        yield createPgObject(typeName, value);
                    }
                    if ("uuid".equals(typeName)) {
                        // PostgreSQLのUUID型
                        yield java.util.UUID.fromString(value);
                    }
                    yield value;
                }
                
                // 文字列型、その他
                case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.NVARCHAR, Types.NCHAR, Types.CLOB, Types.NCLOB ->
                    value;
                
                default -> {
                    log.trace("Using string value for unknown SQL type {} ({})", sqlType, typeName);
                    yield value;
                }
            };
        } catch (Exception e) {
            log.warn("Failed to convert value '{}' for column {} (type={}): {}", 
                    value, columnInfo.columnName(), typeName, e.getMessage());
            return value; // フォールバック: 文字列としてそのまま返す
        }
    }

    /**
     * ブール値をパース
     */
    private Boolean parseBoolean(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    /**
     * タイムスタンプをパース（複数フォーマット対応）
     */
    private Timestamp parseTimestamp(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        // ISO 8601 with timezone (e.g., 2023-01-01T00:00:00Z)
        try {
            Instant instant = Instant.parse(value);
            return Timestamp.from(instant);
        } catch (DateTimeParseException ignored) {}
        
        // ISO 8601 with offset (e.g., 2023-01-01T00:00:00+09:00)
        try {
            OffsetDateTime odt = OffsetDateTime.parse(value);
            return Timestamp.from(odt.toInstant());
        } catch (DateTimeParseException ignored) {}
        
        // Without timezone (e.g., 2023-01-01T00:00:00)
        try {
            LocalDateTime ldt = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Timestamp.valueOf(ldt);
        } catch (DateTimeParseException ignored) {}
        
        // Date only (e.g., 2023-01-01)
        try {
            LocalDateTime ldt = LocalDateTime.parse(value + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Timestamp.valueOf(ldt);
        } catch (DateTimeParseException ignored) {}
        
        log.warn("Failed to parse timestamp: {}", value);
        return null;
    }

    /**
     * PostgreSQLのPGobjectを作成（JSON/JSONB用）
     */
    private Object createPgObject(String type, String value) {
        try {
            Class<?> pgObjectClass = Class.forName("org.postgresql.util.PGobject");
            Object pgObject = pgObjectClass.getDeclaredConstructor().newInstance();
            pgObjectClass.getMethod("setType", String.class).invoke(pgObject, type);
            pgObjectClass.getMethod("setValue", String.class).invoke(pgObject, value);
            return pgObject;
        } catch (Exception e) {
            log.debug("PGobject not available, using string value for {}: {}", type, e.getMessage());
            return value;
        }
    }

    /**
     * カラム情報を保持するレコード
     */
    private record ColumnInfo(String columnName, int dataType, String typeName, boolean nullable) {}
}
