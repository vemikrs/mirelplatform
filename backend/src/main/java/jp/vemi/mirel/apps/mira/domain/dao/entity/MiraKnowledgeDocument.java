/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mira ナレッジドキュメント管理エンティティ.
 * <p>
 * FileManagementと紐づき、スコープや所有者情報を管理します。
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mir_mira_knowledge_document")
public class MiraKnowledgeDocument {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /** 紐づくファイルID (FileManagement.fileId) */
    @Column(name = "file_id", nullable = false)
    private String fileId;

    /** スコープ */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MiraVectorStore.Scope scope;

    /** テナントID */
    @Column(name = "tenant_id")
    private String tenantId;

    /** ユーザーID */
    @Column(name = "user_id")
    private String userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
