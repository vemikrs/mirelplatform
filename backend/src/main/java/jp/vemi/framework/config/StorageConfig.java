package jp.vemi.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * ストレージ設定を管理するConfigurationクラス
 */
@Component
public class StorageConfig {

    @Value("${mirel.storage-dir:./data/storage}")
    private String storageDir;

    private static String configuredStorageDir;

    @PostConstruct
    public void init() {
        configuredStorageDir = storageDir;
    }

    /**
     * 設定されたストレージディレクトリを取得
     * @return ストレージディレクトリパス
     */
    public static String getStorageDir() {
        return configuredStorageDir;
    }

    /**
     * インスタンス経由でのアクセス（テスト等で利用）
     * @return ストレージディレクトリパス
     */
    public String getInstanceStorageDir() {
        return storageDir;
    }
}