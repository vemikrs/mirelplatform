/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira 監査ログエンティティ.
 * 
 * <p>Mira のすべての AI 操作を監査目的で記録します。</p>
 */
@Entity
@Table(name = "mir_mira_audit_log", indexes = {
    @Index(name = "idx_mir_mira_audit_tenant_created", columnList = "tenantId, createdAt"),
    @Index(name = "idx_mir_mira_audit_user_created", columnList = "userId, createdAt"),
    @Index(name = "idx_mir_mira_audit_conversation", columnList = "conversationId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraAuditLog {

    /** 監査ログID（UUID） */
    @Id
    @Column(length = 36)
    private String id;

    /** テナントID */
    @Column(length = 36, nullable = false)
    private String tenantId;

    /** ユーザID */
    @Column(length = 36, nullable = false)
    private String userId;

    /** 会話セッションID */
    @Column(length = 36, nullable = false)
    private String conversationId;

    /** メッセージID（紐付け可能な場合） */
    @Column(length = 36)
    private String messageId;

    /** アクション種別 */
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private AuditAction action;

    /** 実行時のモード */
    @Column(length = 30)
    private String mode;

    /** アプリケーションID */
    @Column(length = 50)
    private String appId;

    /** 画面ID */
    @Column(length = 100)
    private String screenId;

    /** プロンプト文字数 */
    @Column
    private Integer promptLength;

    /** 応答文字数 */
    @Column
    private Integer responseLength;

    /** プロンプトのハッシュ値（重複検知用） */
    @Column(length = 64)
    private String promptHash;

    /** 使用したモデル名 */
    @Column(length = 100)
    private String usedModel;

    /** 入力トークン数 */
    @Column
    private Integer inputTokens;

    /** 出力トークン数 */
    @Column
    private Integer outputTokens;

    /** 合計トークン数 */
    @Column
    private Integer totalTokens;

    /** レイテンシ（ミリ秒） */
    @Column
    private Integer latencyMs;

    /** ステータス */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AuditStatus status;

    /** エラー発生時のコード */
    @Column(length = 50)
    private String errorCode;

    /** クライアントIPアドレス */
    @Column(length = 45)
    private String ipAddress;

    /** クライアントUserAgent */
    @Column(length = 255)
    private String userAgent;

    /** 記録日時 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** アクション種別 */
    public enum AuditAction {
        /** チャット */
        CHAT,
        /** コンテキストヘルプ */
        CONTEXT_HELP,
        /** エラー解析 */
        ERROR_ANALYZE,
        /** モード切替 */
        MODE_SWITCH,
        /** コンテキストスナップショット */
        CONTEXT_SNAPSHOT,
        
        // セキュリティイベント
        /** プロンプトインジェクション検出 */
        PROMPT_INJECTION_DETECTED,
        /** プロンプトインジェクションブロック */
        PROMPT_INJECTION_BLOCKED,
        /** 認可拒否 */
        AUTHORIZATION_DENIED,
        /** クロステナントアクセス試行 */
        CROSS_TENANT_ATTEMPT,
        
        // データイベント
        /** 機密データアクセス */
        SENSITIVE_DATA_ACCESS,
        /** データエクスポート要求 */
        DATA_EXPORT_REQUESTED,
        /** PII検出 */
        PII_DETECTED,
        
        // レート制限
        /** レート制限超過 */
        RATE_LIMIT_EXCEEDED,
        /** クォータ枯渇 */
        QUOTA_EXHAUSTED,
        
        // フィルタリング
        /** 出力フィルタリング */
        OUTPUT_FILTERED,
        
        // システムイベント
        /** サーキットブレーカーオープン */
        CIRCUIT_BREAKER_OPENED,
        /** サーキットブレーカークローズ */
        CIRCUIT_BREAKER_CLOSED,
        /** フォールバック有効化 */
        FALLBACK_ACTIVATED,
        
        // コンテキスト管理
        /** コンテキスト更新 */
        CONTEXT_UPDATE
    }

    /** 監査ステータス */
    public enum AuditStatus {
        /** 成功 */
        SUCCESS,
        /** エラー */
        ERROR,
        /** タイムアウト */
        TIMEOUT
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
