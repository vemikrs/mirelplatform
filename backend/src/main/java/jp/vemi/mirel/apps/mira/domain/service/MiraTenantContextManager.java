/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * Mira テナントコンテキストマネージャー.
 * 
 * <p>リクエストスコープでテナント情報とユーザー情報を管理します。
 * mirelplatform の認証・認可基盤と連携します。</p>
 */
@Slf4j
@Component
public class MiraTenantContextManager {
    
    /** リクエスト属性キー: テナントID */
    private static final String ATTR_TENANT_ID = "mira.tenantId";
    
    /** リクエスト属性キー: ユーザーID */
    private static final String ATTR_USER_ID = "mira.userId";
    
    /** リクエスト属性キー: システムロール */
    private static final String ATTR_SYSTEM_ROLE = "mira.systemRole";
    
    /** リクエスト属性キー: アプリロール */
    private static final String ATTR_APP_ROLE = "mira.appRole";
    
    /** デフォルトテナントID（シングルテナントモード用） */
    private static final String DEFAULT_TENANT_ID = "default";
    
    /** デフォルトユーザーID（匿名アクセス用） */
    private static final String DEFAULT_USER_ID = "anonymous";
    
    /**
     * 現在のテナントIDを取得.
     *
     * @return テナントID
     */
    public String getCurrentTenantId() {
        String tenantId = getRequestAttribute(ATTR_TENANT_ID);
        
        if (tenantId == null || tenantId.isEmpty()) {
            // 認証コンテキストから取得を試みる（将来拡張）
            tenantId = extractTenantIdFromSecurityContext();
        }
        
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }
    
    /**
     * 現在のユーザーIDを取得.
     *
     * @return ユーザーID
     */
    public String getCurrentUserId() {
        String userId = getRequestAttribute(ATTR_USER_ID);
        
        if (userId == null || userId.isEmpty()) {
            // 認証コンテキストから取得を試みる（将来拡張）
            userId = extractUserIdFromSecurityContext();
        }
        
        return userId != null ? userId : DEFAULT_USER_ID;
    }
    
    /**
     * 現在のシステムロールを取得.
     *
     * @return システムロール
     */
    public String getCurrentSystemRole() {
        String role = getRequestAttribute(ATTR_SYSTEM_ROLE);
        
        if (role == null || role.isEmpty()) {
            role = extractSystemRoleFromSecurityContext();
        }
        
        return role;
    }
    
    /**
     * 現在のアプリロールを取得.
     *
     * @return アプリロール
     */
    public String getCurrentAppRole() {
        return getRequestAttribute(ATTR_APP_ROLE);
    }
    
    /**
     * テナントコンテキストを設定.
     *
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param systemRole システムロール
     * @param appRole アプリロール
     */
    public void setContext(String tenantId, String userId, String systemRole, String appRole) {
        setRequestAttribute(ATTR_TENANT_ID, tenantId);
        setRequestAttribute(ATTR_USER_ID, userId);
        setRequestAttribute(ATTR_SYSTEM_ROLE, systemRole);
        setRequestAttribute(ATTR_APP_ROLE, appRole);
        
        log.debug("テナントコンテキスト設定: tenantId={}, userId={}, systemRole={}", 
            tenantId, userId, systemRole);
    }
    
    /**
     * テナントコンテキストをクリア.
     */
    public void clearContext() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                attrs.removeAttribute(ATTR_TENANT_ID, RequestAttributes.SCOPE_REQUEST);
                attrs.removeAttribute(ATTR_USER_ID, RequestAttributes.SCOPE_REQUEST);
                attrs.removeAttribute(ATTR_SYSTEM_ROLE, RequestAttributes.SCOPE_REQUEST);
                attrs.removeAttribute(ATTR_APP_ROLE, RequestAttributes.SCOPE_REQUEST);
            }
        } catch (Exception e) {
            log.debug("コンテキストクリア中の例外（無視可）: {}", e.getMessage());
        }
    }
    
    /**
     * 現在のコンテキストが有効かチェック.
     *
     * @return 有効な場合true
     */
    public boolean hasValidContext() {
        String tenantId = getCurrentTenantId();
        String userId = getCurrentUserId();
        
        return tenantId != null && !tenantId.isEmpty()
            && userId != null && !userId.isEmpty();
    }
    
    // ========================================
    // Private Methods
    // ========================================
    
    private String getRequestAttribute(String name) {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object value = attrs.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
                return value != null ? value.toString() : null;
            }
        } catch (Exception e) {
            log.debug("リクエスト属性取得失敗: {}", name);
        }
        return null;
    }
    
    private void setRequestAttribute(String name, String value) {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null && value != null) {
                attrs.setAttribute(name, value, RequestAttributes.SCOPE_REQUEST);
            }
        } catch (Exception e) {
            log.debug("リクエスト属性設定失敗: {}", name);
        }
    }
    
    /**
     * セキュリティコンテキストからテナントIDを抽出.
     * 将来的にはJWT/Spring Securityと連携。
     */
    private String extractTenantIdFromSecurityContext() {
        // TODO: Spring Security 連携時に実装
        // SecurityContext からテナント情報を抽出
        return null;
    }
    
    /**
     * セキュリティコンテキストからユーザーIDを抽出.
     */
    private String extractUserIdFromSecurityContext() {
        // TODO: Spring Security 連携時に実装
        // Authentication.getName() などから取得
        return null;
    }
    
    /**
     * セキュリティコンテキストからシステムロールを抽出.
     */
    private String extractSystemRoleFromSecurityContext() {
        // TODO: Spring Security 連携時に実装
        // GrantedAuthority から最上位ロールを取得
        return "STANDARD_USER"; // デフォルト
    }
}
