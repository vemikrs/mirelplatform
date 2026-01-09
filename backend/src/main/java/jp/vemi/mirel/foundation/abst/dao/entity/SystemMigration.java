/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * システムマイグレーション状態管理エンティティ.
 * 
 * Bootstrap、データシーディング、スキーママイグレーション、
 * データマイグレーションなどの実行状態を永続化します。
 * 2回目以降の起動時に再実行を防止するために使用します。
 */
@Entity
@Table(name = "mir_system_migration")
@Getter
@Setter
public class SystemMigration {

    /**
     * マイグレーションID
     * 例: "BOOTSTRAP_V1", "DB_SEED_SYSTEM_V1", "DB_SEED_SAMPLE_V1"
     */
    @Id
    @Column(name = "id", length = 100)
    private String id;

    /**
     * マイグレーションタイプ
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "migration_type", nullable = false, length = 50)
    private MigrationType migrationType;

    /**
     * バージョン番号
     * 例: "1.0.0"
     */
    @Column(name = "version", length = 20)
    private String version;

    /**
     * ステータス
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MigrationStatus status;

    /**
     * 適用日時
     */
    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    /**
     * 適用者（システムまたはユーザーID）
     */
    @Column(name = "applied_by", length = 255)
    private String appliedBy;

    /**
     * 詳細情報（JSON形式）
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /**
     * マイグレーションタイプ
     */
    public enum MigrationType {
        /** 初期セットアップ（Bootstrap管理者作成等） */
        BOOTSTRAP,
        /** データシーディング */
        SEED,
        /** スキーママイグレーション */
        SCHEMA,
        /** データマイグレーション */
        DATA_MIGRATION
    }

    /**
     * マイグレーションステータス
     */
    public enum MigrationStatus {
        /** 保留中 */
        PENDING,
        /** 完了 */
        COMPLETED,
        /** 失敗 */
        FAILED
    }
}
