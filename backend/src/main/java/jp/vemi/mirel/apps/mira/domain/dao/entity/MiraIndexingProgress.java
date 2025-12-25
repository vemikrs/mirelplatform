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
 * Mira インデックス処理進捗エンティティ.
 * <p>
 * 非同期インデックス処理の進捗状況を追跡します。
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mir_mira_indexing_progress")
public class MiraIndexingProgress {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    /** 対象ファイルID */
    @Column(name = "file_id", nullable = false)
    private String fileId;

    /** 処理ステータス */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IndexingStatus status;

    /** エラーメッセージ（失敗時） */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** 処理対象チャンク数 */
    @Column(name = "total_chunks")
    private Integer totalChunks;

    /** 処理済みチャンク数 */
    @Column(name = "processed_chunks")
    private Integer processedChunks;

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

    /**
     * インデックス処理ステータス.
     */
    public enum IndexingStatus {
        /** 待機中 */
        PENDING,
        /** 処理中 */
        PROCESSING,
        /** 完了 */
        COMPLETED,
        /** 失敗 */
        FAILED
    }
}
