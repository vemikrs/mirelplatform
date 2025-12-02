/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.framework.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component("dataSeeder")
@Order(10)
public class DataSeeder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    private static DataLoader getDataLoader() {
        return context.getBean(DataLoader.class);
    }

    public static void initializeDefaultTenant() {
        // This might still be needed if it's not in CSV, or can be moved to CSV?
        // The previous implementation had logic to check/create default tenant with
        // specific JWK URI.
        // If we move this to CSV (mir_tenant_system_master.csv), we can remove this
        // method or make it call dataLoader.
        // For now, let's keep the specific logic if it's complex, or try to move it.
        // The user said "load system data, sample data".
        // Let's assume we should use DataLoader for everything possible.
        // But `initializeDefaultTenant` had logic for `TenantSystemMaster` which is a
        // bit special (key/value).
        // Let's leave it as is for now, or move it to CSV if possible.
        // Actually, `TenantSystemMaster` is just a table. I can create
        // `mir_tenant_system_master.csv`.
        getDataLoader().loadSystemData();
    }

    /**
     * SaaS認証テストデータを初期化 (開発環境のみ)
     */
    public static void initializeSaasTestData() {
        // Already handled by loadSystemData called above?
        // Or we can separate them.
        // The user asked for "resources/db/data/system" and "sample".
        // initializeDefaultTenant is likely system data.
        // initializeSaasTestData is also system data.
        // initializeSchemaSampleData is sample data.

        // Let's consolidate.
        // But DatabaseInitializer calls them separately.
        // We can make them idempotent.
        getDataLoader().loadSystemData();
    }

    /**
     * スキーマサンプルデータを初期化 (CSVから読み込み)
     */
    public static void initializeSchemaSampleData() {
        getDataLoader().loadSampleData();
    }
}