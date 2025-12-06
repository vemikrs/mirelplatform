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

    /**
     * システムデータを初期化（CSVから読み込み）
     * DataLoader内で重複ロード防止済み
     */
    public static void initializeSystemData() {
        getDataLoader().loadSystemData();
    }

    /**
     * スキーマサンプルデータを初期化 (CSVから読み込み)
     * DataLoader内で重複ロード防止済み
     */
    public static void initializeSampleData() {
        getDataLoader().loadSampleData();
    }
}