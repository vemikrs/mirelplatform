package jp.vemi.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * ストレージ設定を管理するConfigurationクラス
 * レイヤード・ステンシル管理をサポート
 */
@Component
public class StorageConfig {
    
    // Static initialization flag
    private static boolean initialized = false;

    @Value("${mirel.storage-dir:./data/storage}")
    private String storageDir;
    
    @Value("${mirel.stencil.layers.user:${mirel.storage-dir}/apps/promarker/stencil/user}")
    private String userStencilDir;
    
    @Value("${mirel.stencil.layers.standard:${mirel.storage-dir}/apps/promarker/stencil/standard}")
    private String standardStencilDir;
    
    @Value("${mirel.stencil.layers.samples:classpath:/stencil-samples}")
    private String samplesStencilDir;
    
    @Value("${mirel.stencil.auto-deploy-samples:true}")
    private boolean autoDeploySamples;

    private static String configuredStorageDir;
    private static String configuredUserStencilDir;
    private static String configuredStandardStencilDir;
    private static String configuredSamplesStencilDir;
    private static boolean configuredAutoDeploySamples;

    @PostConstruct
    public void init() {
        System.out.println("=== StorageConfig PostConstruct Debug ===");
        System.out.println("Raw storageDir from @Value: " + storageDir);
        System.out.println("Raw userStencilDir from @Value: " + userStencilDir);
        System.out.println("Raw standardStencilDir from @Value: " + standardStencilDir);
        System.out.println("Raw samplesStencilDir from @Value: " + samplesStencilDir);
        System.out.println("Raw autoDeploySamples from @Value: " + autoDeploySamples);
        
        configuredStorageDir = storageDir;
        configuredUserStencilDir = userStencilDir.replace("${mirel.storage-dir}", storageDir);
        configuredStandardStencilDir = standardStencilDir.replace("${mirel.storage-dir}", storageDir);
        configuredSamplesStencilDir = samplesStencilDir;
        configuredAutoDeploySamples = autoDeploySamples;
        
        initialized = true;  // 初期化完了フラグをセット
        
        System.out.println("Processed configuredStorageDir: " + configuredStorageDir);
        System.out.println("Processed configuredUserStencilDir: " + configuredUserStencilDir);
        System.out.println("Processed configuredStandardStencilDir: " + configuredStandardStencilDir);
        System.out.println("Processed configuredSamplesStencilDir: " + configuredSamplesStencilDir);
        System.out.println("Processed configuredAutoDeploySamples: " + configuredAutoDeploySamples);
    }

    /**
     * 設定されたストレージディレクトリを取得
     * @return ストレージディレクトリパス
     */
    public static String getStorageDir() {
        if (!initialized) {
            System.out.println("WARNING: StorageConfig not initialized, using default value");
            return "./data/storage";  // フォールバック値
        }
        if (configuredStorageDir == null) {
            System.out.println("WARNING: StorageConfig.configuredStorageDir is null, using default value");
            return "./data/storage";  // フォールバック値
        }
        return configuredStorageDir;
    }

    /**
     * ユーザー独自ステンシルディレクトリを取得
     * @return ユーザーステンシルディレクトリパス
     */
    public static String getUserStencilDir() {
        if (configuredUserStencilDir == null) {
            return getStorageDir() + "/user";
        }
        return configuredUserStencilDir;
    }

    /**
     * 標準ステンシルディレクトリを取得  
     * @return 標準ステンシルディレクトリパス
     */
    public static String getStandardStencilDir() {
        if (configuredStandardStencilDir == null) {
            return getStorageDir() + "/standard";
        }
        return configuredStandardStencilDir;
    }

    /**
     * サンプルステンシルディレクトリを取得
     * @return サンプルステンシルディレクトリパス（classpath:形式含む）
     */
    public static String getSamplesStencilDir() {
        if (configuredSamplesStencilDir == null) {
            return "classpath:/stencil-samples";
        }
        return configuredSamplesStencilDir;
    }

    /**
     * サンプルステンシル自動展開フラグを取得
     * @return 自動展開が有効な場合true
     */
    public static boolean isAutoDeploySamples() {
        if (!initialized) {
            System.out.println("WARNING: StorageConfig not initialized, auto-deploy-samples defaults to false");
            return false;  // 初期化されていない場合はfalse
        }
        return configuredAutoDeploySamples;  // booleanのデフォルトはfalseなのでnullチェック不要
    }

    /**
     * インスタンス経由でのアクセス（テスト等で利用）
     * @return ストレージディレクトリパス
     */
    public String getInstanceStorageDir() {
        return storageDir;
    }
}