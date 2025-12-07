/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Mira RBAC アダプター.
 * 
 * <p>mirelplatform のロールベースアクセス制御と連携し、
 * Mira 固有の権限チェックを行います。</p>
 */
@Slf4j
@Component
public class MiraRbacAdapter {
    
    /** Mira 機能を利用可能なシステムロール */
    private static final Set<String> MIRA_ALLOWED_SYSTEM_ROLES = Set.of(
        "SYSTEM_ADMIN",
        "ADMIN",
        "POWER_USER",
        "STANDARD_USER",
        "VIEWER"
    );
    
    /** Studio Agent を利用可能なロール */
    private static final Set<String> STUDIO_AGENT_ROLES = Set.of(
        "SYSTEM_ADMIN",
        "ADMIN",
        "DEVELOPER",
        "POWER_USER"
    );
    
    /** 監査ログエクスポートが可能なロール */
    private static final Set<String> AUDIT_EXPORT_ROLES = Set.of(
        "SYSTEM_ADMIN",
        "ADMIN",
        "AUDITOR"
    );
    
    /**
     * ユーザーが Mira を利用可能かチェック.
     *
     * @param systemRole システムロール
     * @param tenantId テナントID
     * @return 利用可否
     */
    public boolean canUseMira(String systemRole, String tenantId) {
        if (systemRole == null) {
            log.warn("systemRole が null: tenantId={}", tenantId);
            return false;
        }
        
        // テナント単位での Mira 有効化チェック（将来拡張）
        // 現時点では全テナントで有効
        
        return MIRA_ALLOWED_SYSTEM_ROLES.contains(systemRole.toUpperCase());
    }
    
    /**
     * 特定の Mira モードを利用可能かチェック.
     *
     * @param mode Mira モード
     * @param systemRole システムロール
     * @param appRole アプリロール
     * @return 利用可否
     */
    public boolean canUseMode(MiraMode mode, String systemRole, String appRole) {
        if (mode == null) {
            return false;
        }
        
        return switch (mode) {
            case STUDIO_AGENT -> isStudioAgentAllowed(systemRole, appRole);
            case WORKFLOW_AGENT -> isWorkflowAgentAllowed(systemRole, appRole);
            case ERROR_ANALYZE -> true; // 全ユーザー利用可
            case CONTEXT_HELP -> true;   // 全ユーザー利用可
            case GENERAL_CHAT -> true;   // 全ユーザー利用可
        };
    }
    
    /**
     * 監査ログのエクスポートが可能かチェック.
     *
     * @param systemRole システムロール
     * @return 可否
     */
    public boolean canExportAuditLog(String systemRole) {
        if (systemRole == null) {
            return false;
        }
        return AUDIT_EXPORT_ROLES.contains(systemRole.toUpperCase());
    }
    
    /**
     * 会話履歴の全件取得が可能かチェック（管理者向け）.
     *
     * @param systemRole システムロール
     * @return 可否
     */
    public boolean canViewAllConversations(String systemRole) {
        if (systemRole == null) {
            return false;
        }
        String upper = systemRole.toUpperCase();
        return "SYSTEM_ADMIN".equals(upper) || "ADMIN".equals(upper);
    }
    
    /**
     * 他ユーザーの会話を閲覧可能かチェック.
     *
     * @param systemRole システムロール
     * @param requestUserId リクエストユーザーID
     * @param targetUserId 対象ユーザーID
     * @return 可否
     */
    public boolean canViewUserConversation(
            String systemRole, 
            String requestUserId, 
            String targetUserId) {
        
        // 自分の会話は常に閲覧可
        if (requestUserId != null && requestUserId.equals(targetUserId)) {
            return true;
        }
        
        // 管理者は他ユーザーの会話も閲覧可
        return canViewAllConversations(systemRole);
    }
    
    // ========================================
    // Private Methods
    // ========================================
    
    private boolean isStudioAgentAllowed(String systemRole, String appRole) {
        if (systemRole != null && STUDIO_AGENT_ROLES.contains(systemRole.toUpperCase())) {
            return true;
        }
        if (appRole != null) {
            String upper = appRole.toUpperCase();
            return upper.contains("DEVELOPER") || upper.contains("ADMIN");
        }
        return false;
    }
    
    private boolean isWorkflowAgentAllowed(String systemRole, String appRole) {
        // ワークフロー機能を使用するユーザーは利用可
        // 現時点では全ユーザーに開放
        return systemRole != null || appRole != null;
    }
}
