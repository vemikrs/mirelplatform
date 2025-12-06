/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.model;

/**
 * 承認者タイプ.
 */
public enum ApproverType {
    DIRECT_MANAGER, // 直属上長
    DEPARTMENT_HEAD, // 部門長
    DIVISION_HEAD, // 本部長
    SECTION_HEAD, // 課長
    SPECIFIC_USER, // 特定ユーザー
    ROLE_BASED // ロールベース（経理部長など）
}
