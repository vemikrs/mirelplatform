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
                user.setLastLoginAt(now);
                
                users.add(user);
            } catch (Exception e) {
                log.error("Failed to parse User row: {}", row, e);
            }
        }
        
        return users;
    }
}
