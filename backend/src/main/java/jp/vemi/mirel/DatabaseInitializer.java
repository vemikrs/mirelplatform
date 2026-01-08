/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import jp.vemi.framework.util.DataSeeder;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration.MigrationType;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemMigration.MigrationStatus;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemMigrationRepository;

import java.time.LocalDateTime;

/**
 * データベース初期化設定.
 * 
 * 起動時のデータシーディングを制御します。
 * 初期化モード（ALWAYS/ONCE/NEVER）とサンプルデータ投入フラグで動作を制御。
 */
@Configuration
@Order(20)
@DependsOn("dataSeeder")
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final String MIGRATION_ID_SYSTEM_SEED = "DB_SEED_SYSTEM_V1";
    private static final String MIGRATION_ID_SAMPLE_SEED = "DB_SEED_SAMPLE_V1";

    @Autowired
    private SystemMigrationRepository migrationRepository;

    /**
     * 初期化モード
     * - ALWAYS: 毎回実行
     * - ONCE: 初回のみ実行（デフォルト）
     * - NEVER: 実行しない
     */
    @Value("${mirel.database.initialize-mode:ONCE}")
    private String initializeMode;

    /**
     * サンプルデータ投入フラグ
     * 開発環境ではtrue、本番環境ではfalse
     */
    @Value("${mirel.database.seed-sample-data:false}")
    private boolean seedSampleData;

    @Bean
    public ApplicationRunner databaseInitializer() {
        return args -> {
            logger.info("Database initialization started. mode={}, seedSampleData={}",
                    initializeMode, seedSampleData);

            if ("NEVER".equalsIgnoreCase(initializeMode)) {
                logger.info("Database initialization skipped (mode=NEVER)");
                return;
            }

            boolean alwaysMode = "ALWAYS".equalsIgnoreCase(initializeMode);

            // システムデータのシーディング
            if (alwaysMode || shouldExecute(MIGRATION_ID_SYSTEM_SEED)) {
                logger.info("Executing system data seeding...");
                try {
                    DataSeeder.initializeSystemData();
                    markMigrationComplete(MIGRATION_ID_SYSTEM_SEED, MigrationType.SEED, "System data seeded");
                    logger.info("System data seeding completed");
                } catch (Exception e) {
                    logger.error("System data seeding failed", e);
                    markMigrationFailed(MIGRATION_ID_SYSTEM_SEED, MigrationType.SEED, e.getMessage());
                    throw e;
                }
            } else {
                logger.info("System data seeding skipped (already completed)");
            }

            // サンプルデータのシーディング
            if (seedSampleData) {
                if (alwaysMode || shouldExecute(MIGRATION_ID_SAMPLE_SEED)) {
                    logger.info("Executing sample data seeding...");
                    try {
                        DataSeeder.initializeSampleData();
                        markMigrationComplete(MIGRATION_ID_SAMPLE_SEED, MigrationType.SEED, "Sample data seeded");
                        logger.info("Sample data seeding completed");
                    } catch (Exception e) {
                        logger.error("Sample data seeding failed", e);
                        markMigrationFailed(MIGRATION_ID_SAMPLE_SEED, MigrationType.SEED, e.getMessage());
                        throw e;
                    }
                } else {
                    logger.info("Sample data seeding skipped (already completed)");
                }
            } else {
                logger.info("Sample data seeding disabled (seedSampleData=false)");
            }

            logger.info("Database initialization completed");
        };
    }

    /**
     * マイグレーションを実行すべきかどうかを判定
     */
    private boolean shouldExecute(String migrationId) {
        try {
            return !migrationRepository.isCompleted(migrationId);
        } catch (Exception e) {
            // テーブルが存在しない場合（初回起動）は実行する
            logger.warn("Migration check failed (table may not exist yet): {}", e.getMessage());
            return true;
        }
    }

    /**
     * マイグレーション完了を記録
     */
    private void markMigrationComplete(String id, MigrationType type, String details) {
        try {
            SystemMigration migration = new SystemMigration();
            migration.setId(id);
            migration.setMigrationType(type);
            migration.setVersion("1.0.0");
            migration.setStatus(MigrationStatus.COMPLETED);
            migration.setAppliedAt(LocalDateTime.now());
            migration.setAppliedBy("SYSTEM");
            migration.setDetails(details);
            migrationRepository.save(migration);
        } catch (Exception e) {
            logger.warn("Failed to record migration completion: {}", e.getMessage());
        }
    }

    /**
     * マイグレーション失敗を記録
     */
    private void markMigrationFailed(String id, MigrationType type, String errorMessage) {
        try {
            SystemMigration migration = new SystemMigration();
            migration.setId(id);
            migration.setMigrationType(type);
            migration.setVersion("1.0.0");
            migration.setStatus(MigrationStatus.FAILED);
            migration.setAppliedAt(LocalDateTime.now());
            migration.setAppliedBy("SYSTEM");
            migration.setDetails("Error: " + errorMessage);
            migrationRepository.save(migration);
        } catch (Exception e) {
            logger.warn("Failed to record migration failure: {}", e.getMessage());
        }
    }
}
