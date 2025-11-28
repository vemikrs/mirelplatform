/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.framework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.FeatureStatus;
import jp.vemi.mirel.foundation.abst.dao.entity.FeatureFlag.LicenseResolveStrategy;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.entity.User;


/**
 * CSVファイルからテストデータを読み込むユーティリティ
 */
public class CsvTestDataLoader {
    
    private static final Logger log = LoggerFactory.getLogger(CsvTestDataLoader.class);
    
    /**
     * CSVファイルを読み込んでMapのリストに変換
     * 
     * @param resourcePath クラスパス上のリソースパス (例: "testdata/system-users.csv")
     * @return 各行をMapとして表現したリスト（ヘッダー行がキーとなる）
     */
    public static List<Map<String, String>> loadCsv(String resourcePath) {
        List<Map<String, String>> result = new ArrayList<>();
        
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("CSV file not found: {}", resourcePath);
                return result;
            }
            
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                
                // ヘッダー行を読み込み
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    log.warn("CSV file is empty: {}", resourcePath);
                    return result;
                }
                
                String[] headers = headerLine.split(",");
                
                // データ行を読み込み
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.trim().isEmpty()) {
                        continue; // 空行をスキップ
                    }
                    
                    String[] values = line.split(",", -1); // -1で末尾の空文字も保持
                    if (values.length != headers.length) {
                        log.warn("CSV line {} has {} columns but expected {}. Skipping.", 
                                lineNumber, values.length, headers.length);
                        continue;
                    }
                    
                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        row.put(headers[i].trim(), values[i].trim());
                    }
                    result.add(row);
                }
                
                log.info("Loaded {} rows from {}", result.size(), resourcePath);
            }
        } catch (IOException e) {
            log.error("Failed to load CSV: {}", resourcePath, e);
        }
        
        return result;
    }
    
    /**
     * SystemUserエンティティのリストを生成
     * 
     * @param passwordEncoder パスワードエンコーダー
     * @return SystemUserのリスト
     */
    public static List<SystemUser> loadSystemUsers(PasswordEncoder passwordEncoder) {
        List<Map<String, String>> csvData = loadCsv("testdata/system-users.csv");
        List<SystemUser> systemUsers = new ArrayList<>();
        
        for (Map<String, String> row : csvData) {
            try {
                SystemUser user = new SystemUser();
                user.setId(UUID.fromString(row.get("id")));
                user.setUsername(row.get("username"));
                user.setEmail(row.get("email"));
                user.setPasswordHash(passwordEncoder.encode(row.get("password")));
                user.setEmailVerified(Boolean.parseBoolean(row.get("emailVerified")));
                user.setIsActive(Boolean.parseBoolean(row.get("isActive")));
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                
                systemUsers.add(user);
            } catch (Exception e) {
                log.error("Failed to parse SystemUser row: {}", row, e);
            }
        }
        
        return systemUsers;
    }
    
    /**
     * Userエンティティのリストを生成
     * 
     * @return Userのリスト
     */
    public static List<User> loadUsers() {
        List<Map<String, String>> csvData = loadCsv("testdata/users.csv");
        List<User> users = new ArrayList<>();
        Instant now = Instant.now();
        
        for (Map<String, String> row : csvData) {
            try {
                User user = new User();
                user.setUserId(row.get("userId"));
                user.setSystemUserId(UUID.fromString(row.get("systemUserId")));
                user.setTenantId(row.get("tenantId"));
                user.setUsername(row.get("username"));
                user.setEmail(row.get("email"));
                user.setDisplayName(row.get("displayName"));
                user.setFirstName(row.get("firstName"));
                user.setLastName(row.get("lastName"));
                user.setIsActive(Boolean.parseBoolean(row.get("isActive")));
                user.setEmailVerified(Boolean.parseBoolean(row.get("emailVerified")));
                user.setRoles(row.get("roles")); // roles カラムを読み込み
                user.setLastLoginAt(now);
                
                users.add(user);
            } catch (Exception e) {
                log.error("Failed to parse User row: {}", row, e);
            }
        }
        
        return users;
    }
    
    /**
     * FeatureFlagエンティティのリストを生成
     * 
     * @return FeatureFlagのリスト
     */
    public static List<FeatureFlag> loadFeatureFlags() {
        List<Map<String, String>> csvData = loadCsv("testdata/feature-flags.csv");
        List<FeatureFlag> featureFlags = new ArrayList<>();
        
        for (Map<String, String> row : csvData) {
            try {
                FeatureFlag flag = new FeatureFlag();
                flag.setId(row.get("id"));
                flag.setFeatureKey(row.get("feature_key"));
                flag.setFeatureName(row.get("feature_name"));
                flag.setDescription(row.get("description"));
                flag.setApplicationId(row.get("application_id"));
                flag.setStatus(parseFeatureStatus(row.get("status")));
                flag.setInDevelopment(Boolean.parseBoolean(row.get("in_development")));
                flag.setRequiredLicenseTier(parseLicenseTier(row.get("required_license_tier")));
                flag.setEnabledByDefault(Boolean.parseBoolean(row.get("enabled_by_default")));
                flag.setRolloutPercentage(parseInteger(row.get("rollout_percentage"), 100));
                flag.setLicenseResolveStrategy(parseLicenseResolveStrategy(row.get("license_resolve_strategy")));
                flag.setEnabledForUserIds(parseJsonArrayOrNull(row.get("enabled_for_user_ids")));
                flag.setEnabledForTenantIds(parseJsonArrayOrNull(row.get("enabled_for_tenant_ids")));
                flag.setDisabledForUserIds(parseJsonArrayOrNull(row.get("disabled_for_user_ids")));
                flag.setDisabledForTenantIds(parseJsonArrayOrNull(row.get("disabled_for_tenant_ids")));
                flag.setTargetSegments(parseJsonArrayOrNull(row.get("target_segments")));
                flag.setMetadata(parseJsonArrayOrNull(row.get("metadata")));
                flag.setDeleteFlag(false);
                
                featureFlags.add(flag);
            } catch (Exception e) {
                log.error("Failed to parse FeatureFlag row: {}", row, e);
            }
        }
        
        return featureFlags;
    }
    
    /**
     * FeatureStatus を解析
     */
    private static FeatureStatus parseFeatureStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FeatureStatus.STABLE;
        }
        try {
            return FeatureStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown FeatureStatus: {}, defaulting to STABLE", value);
            return FeatureStatus.STABLE;
        }
    }
    
    /**
     * LicenseTier を解析（空の場合はnull）
     */
    private static LicenseTier parseLicenseTier(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LicenseTier.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown LicenseTier: {}, defaulting to null", value);
            return null;
        }
    }
    
    /**
     * LicenseResolveStrategy を解析
     */
    private static LicenseResolveStrategy parseLicenseResolveStrategy(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LicenseResolveStrategy.TENANT_PRIORITY;
        }
        try {
            return LicenseResolveStrategy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown LicenseResolveStrategy: {}, defaulting to TENANT_PRIORITY", value);
            return LicenseResolveStrategy.TENANT_PRIORITY;
        }
    }
    
    /**
     * 整数を解析（デフォルト値あり）
     */
    private static Integer parseInteger(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * JSON配列文字列を解析（空の場合はnull）
     */
    private static String parseJsonArrayOrNull(String value) {
        if (value == null || value.trim().isEmpty() || "[]".equals(value.trim())) {
            return null;
        }
        return value.trim();
    }
}
