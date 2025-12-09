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
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira コンテキストレイヤーエンティティ.
 * 
 * <p>
 * 階層コンテキスト（System/Tenant/Organization/User）を管理します。
 * </p>
 * 
 * <h3>階層構造</h3>
 * 
 * <pre>
 * System Context (グローバル共通)
 *     ↓
 * Tenant Context (テナント固有)
 *     ↓
 * Organization Context (組織固有)
 *     ↓
 * User Context (ユーザー固有)
 * </pre>
 */
@Entity
@Table(name = "mir_mira_context_layer", indexes = {
        @Index(name = "idx_mir_mira_ctx_scope", columnList = "scope, scopeId"),
        @Index(name = "idx_mir_mira_ctx_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraContextLayer {

    /** コンテキストレイヤーID（UUID） */
    @Id
    @Column(length = 36)
    private String id;

    /** スコープ */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContextScope scope;

    /** スコープID（SYSTEM の場合は null） */
    @Column(length = 36)
    private String scopeId;

    /** カテゴリ（terminology, workflow_patterns, style 等） */
    @Column(length = 100, nullable = false)
    private String category;

    /** コンテンツ（Markdown または JSON） */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 優先度（高いほど後から適用される） */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /** 有効フラグ */
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    /** 作成日時 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 更新日時 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** コンテキストスコープ */
    public enum ContextScope {
        /** システム全体（全テナント共通） */
        SYSTEM,
        /** テナント固有 */
        TENANT,
        /** 組織固有 */
        ORGANIZATION,
        /** ユーザー固有 */
        USER
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
