/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dao.repository;

import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration.MigrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * システムマイグレーションリポジトリ.
 */
@Repository
public interface SystemMigrationRepository extends JpaRepository<SystemMigration, String> {

    /**
     * マイグレーションタイプで検索
     */
    List<SystemMigration> findByMigrationType(MigrationType migrationType);

    /**
     * IDとステータスで存在確認
     */
    boolean existsByIdAndStatus(String id, SystemMigration.MigrationStatus status);

    /**
     * 完了済みマイグレーションの存在確認
     */
    default boolean isCompleted(String id) {
        return existsByIdAndStatus(id, SystemMigration.MigrationStatus.COMPLETED);
    }
}
