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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira 会話セッションエンティティ.
 * 
 * <p>チャット会話のセッション情報を管理します。</p>
 */
@Entity
@Table(name = "mir_mira_conversation", indexes = {
    @Index(name = "idx_mir_mira_conv_tenant_user", columnList = "tenantId, userId"),
    @Index(name = "idx_mir_mira_conv_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraConversation {

    /** 会話セッションID（UUID） */
    @Id
    @Column(length = 36)
    private String id;

    /** テナントID */
    @Column(length = 36, nullable = false)
    private String tenantId;

    /** ユーザID */
    @Column(length = 36, nullable = false)
    private String userId;

    /** モード */
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ConversationMode mode;

    /** タイトル（会話の要約） */
    @Column(length = 255)
    private String title;

    /** ステータス */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    /** セッション開始日時 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 最終アクティビティ日時 */
    @Column(nullable = false)
    private LocalDateTime lastActivityAt;

    /** 会話モード種別 */
    public enum ConversationMode {
        /** 汎用チャット */
        GENERAL_CHAT,
        /** コンテキストヘルプ */
        CONTEXT_HELP,
        /** エラー解析 */
        ERROR_ANALYZE,
        /** Studio エージェント */
        STUDIO_AGENT,
        /** Workflow エージェント */
        WORKFLOW_AGENT
    }

    /** 会話ステータス */
    public enum ConversationStatus {
        /** アクティブ */
        ACTIVE,
        /** クローズ */
        CLOSED,
        /** アーカイブ */
        ARCHIVED
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.lastActivityAt == null) {
            this.lastActivityAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * 最終アクティビティを現在時刻に更新.
     */
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }
}
