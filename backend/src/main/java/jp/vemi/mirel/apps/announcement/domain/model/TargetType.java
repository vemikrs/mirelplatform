package jp.vemi.mirel.apps.announcement.domain.model;

public enum TargetType {
    ALL, // 全ユーザー
    TENANT, // 特定テナント
    ROLE, // 特定ロール（ADMIN等）
    USER, // 特定ユーザー
    ORGANIZATION // 特定組織
}
