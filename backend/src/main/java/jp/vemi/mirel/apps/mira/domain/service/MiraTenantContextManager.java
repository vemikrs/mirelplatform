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
 * <p>
 * リクエストスコープでテナント情報とユーザー情報を管理します。
 * mirelplatform の認証・認可基盤と連携します。
 * </p>
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

    @org.springframework.beans.factory.annotation.Autowired
    private jp.vemi.mirel.foundation.context.ExecutionContext executionContext;

    /**
     * 現在のテナントIDを取得.
     *
     * @return テナントID
     */
    public String getCurrentTenantId() {
        // 1. Request Attribute (Legacy override)
        String tenantId = getRequestAttribute(ATTR_TENANT_ID);
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }

        // 2. ExecutionContext
        tenantId = executionContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }

        return DEFAULT_TENANT_ID;
    }

    /**
     * 現在のユーザーIDを取得.
     *
     * @return ユーザーID
     * @throws IllegalStateException
     *             認証済みユーザーが存在しない場合
     */
    public String getCurrentUserId() {
        // 1. Request Attribute (Legacy override)
        String userId = getRequestAttribute(ATTR_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // 2. ExecutionContext
        userId = executionContext.getCurrentUserId();

        // 3. Validation - Prevent Anonymous Access
        if (userId != null && !userId.isEmpty() && !"anonymous".equalsIgnoreCase(userId)) {
            return userId;
        }

        // Fix for data leakage: Do NOT return "anonymous" default.
        // Throw exception to prevent saving data to shared anonymous account.
        throw new IllegalStateException("Mira requires an authenticated user. Anonymous access is not allowed.");
    }

    /**
     * 現在のシステムロールを取得.
     *
     * @return システムロール
     */
    public String getCurrentSystemRole() {
        String role = getRequestAttribute(ATTR_SYSTEM_ROLE);

        if (role == null || role.isEmpty()) {
            // ExecutionContext doesn't expose roles directly in simple getter yet,
            // but we can extract from security context if needed.
            // For now, fallback to default if not set.
            // TODO: Enhance ExecutionContext to expose system roles or extract from tokens
            return "STANDARD_USER";
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
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @param systemRole
     *            システムロール
     * @param appRole
     *            アプリロール
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
        try {
            String userId = getCurrentUserId();
            return userId != null && !userId.isEmpty();
        } catch (IllegalStateException e) {
            return false;
        }
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

    // Removed unused extract* methods as we delegate to ExecutionContext or threw
    // exception
}
