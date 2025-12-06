/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * ポリシーエンフォーサー.
 * 
 * <p>ロールベースのアクセス制御と応答フィルタリングを行います。</p>
 */
@Slf4j
@Component
public class PolicyEnforcer {
    
    /** 機密情報パターン */
    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
        Pattern.compile("(?i)(password|passwd|pwd)\\s*[:=]\\s*\\S+"),
        Pattern.compile("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*\\S+"),
        Pattern.compile("(?i)(secret|token)\\s*[:=]\\s*\\S+"),
        Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+"),
        Pattern.compile("(?i)(jdbc|mysql|postgres|oracle)://[^\\s]+")
    );
    
    /** 禁止キーワード（システム操作系） - 大文字で統一 */
    private static final Set<String> PROHIBITED_KEYWORDS = Set.of(
        "DROP TABLE",
        "DELETE FROM",
        "TRUNCATE",
        "RM -RF",
        "SHUTDOWN",
        "FORMAT"
    );
    
    /** 管理者専用機能 */
    private static final Set<String> ADMIN_ONLY_FEATURES = Set.of(
        "system_config",
        "user_management",
        "audit_log_export",
        "backup_restore"
    );
    
    /**
     * リクエストの事前検証.
     *
     * @param request チャットリクエスト
     * @return 検証結果
     */
    public ValidationResult validateRequest(ChatRequest request) {
        // 入力検証
        if (request.getMessage() == null || request.getMessage().getContent() == null) {
            return ValidationResult.error("メッセージが空です");
        }
        
        String content = request.getMessage().getContent();
        
        // 禁止キーワードチェック
        for (String keyword : PROHIBITED_KEYWORDS) {
            if (content.toUpperCase().contains(keyword)) {
                log.warn("禁止キーワード検出: {}", keyword);
                return ValidationResult.error("セキュリティ上の理由により、この操作は許可されていません");
            }
        }
        
        // 長さ制限
        if (content.length() > 10000) {
            return ValidationResult.error("メッセージが長すぎます（最大10000文字）");
        }
        
        return ValidationResult.ok();
    }
    
    /**
     * 機能アクセス権限チェック.
     *
     * @param feature 機能ID
     * @param systemRole システムロール
     * @return アクセス可否
     */
    public boolean canAccessFeature(String feature, String systemRole) {
        if (ADMIN_ONLY_FEATURES.contains(feature)) {
            return "ADMIN".equalsIgnoreCase(systemRole) 
                || "SYSTEM_ADMIN".equalsIgnoreCase(systemRole);
        }
        return true;
    }
    
    /**
     * Mira モードへのアクセス権限チェック.
     *
     * @param mode Miraモード
     * @param systemRole システムロール
     * @param appRole アプリロール
     * @return アクセス可否
     */
    public boolean canAccessMode(MiraMode mode, String systemRole, String appRole) {
        // STUDIO_AGENT は開発者ロール以上
        if (mode == MiraMode.STUDIO_AGENT) {
            return isDevRole(systemRole) || isDevRole(appRole);
        }
        
        // その他のモードは全ユーザーアクセス可
        return true;
    }
    
    /**
     * AI応答の事後フィルタリング.
     *
     * @param response AI応答テキスト
     * @param systemRole システムロール
     * @return フィルタリング後の応答
     */
    public String filterResponse(String response, String systemRole) {
        if (response == null) {
            return null;
        }
        
        String filtered = response;
        
        // 機密情報のマスキング
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            filtered = pattern.matcher(filtered).replaceAll("[REDACTED]");
        }
        
        // 非管理者には詳細なシステム情報を隠す
        if (!isAdminRole(systemRole)) {
            filtered = maskSystemDetails(filtered);
        }
        
        return filtered;
    }
    
    /**
     * コンテキスト情報のサニタイズ.
     *
     * @param payload 元のペイロード
     * @return サニタイズ後のペイロード（機密フィールド除去）
     */
    public java.util.Map<String, Object> sanitizePayload(java.util.Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        
        java.util.Map<String, Object> sanitized = new java.util.HashMap<>(payload);
        
        // 機密フィールドを除去
        Set<String> sensitiveKeys = Set.of(
            "password", "secret", "token", "apiKey", "credential",
            "privateKey", "connectionString"
        );
        
        sensitiveKeys.forEach(key -> {
            sanitized.remove(key);
            sanitized.remove(key.toLowerCase());
            sanitized.remove(toSnakeCase(key));
        });
        
        return sanitized;
    }
    
    // ========================================
    // Private Methods
    // ========================================
    
    private boolean isAdminRole(String role) {
        if (role == null) return false;
        String upper = role.toUpperCase();
        return upper.contains("ADMIN") || upper.equals("SYSTEM");
    }
    
    private boolean isDevRole(String role) {
        if (role == null) return false;
        String upper = role.toUpperCase();
        return upper.contains("ADMIN") || upper.contains("DEV") 
            || upper.contains("DEVELOPER") || upper.equals("SYSTEM");
    }
    
    private String maskSystemDetails(String text) {
        // IPアドレスのマスキング
        text = text.replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP]");
        // ポート番号を含むホスト情報のマスキング
        text = text.replaceAll("localhost:\\d+", "[HOST]");
        return text;
    }
    
    private String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    
    // ========================================
    // Inner Classes
    // ========================================
    
    /**
     * 検証結果.
     */
    public record ValidationResult(boolean valid, String errorMessage) {
        
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}
