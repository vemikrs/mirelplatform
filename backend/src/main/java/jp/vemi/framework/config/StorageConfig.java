package jp.vemi.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;

/**
 * ストレージ設定を管理するConfigurationクラス
 * レイヤード・ステンシル管理をサポート
 */
@Component
public class StorageConfig {

    private static final Logger logger = LoggerFactory.getLogger(StorageConfig.class);
    
    // Static initialization flag
    private static boolean initialized = false;

    @Value("${mirel.storage-dir:./data/storage}")
    private String storageDir;

    // -------------------- ProMarker -------------------- //
    @Value("${mirel.promarker.storage-dir:${mirel.storage-dir}/apps/promarker}")
    @lombok.Getter
    private String proMarkerAppDir = "apps/promarker";

    @Value("${mirel.promarker.stencil.user:${mirel.storage-dir}/apps/promarker}/stencil/user")
    private String userStencilDir;

    @Value("${mirel.promarker.stencil.standard:${mirel.storage-dir}/apps/promarker}/stencil/standard")
    private String standardStencilDir;

    @Value("${mirel.promarker.stencil.samples:classpath:/promarker/stencil/samples}")
    private String samplesStencilDir;

    @Value("${mirel.promarker.stencil.auto-deploy-samples:true}")
    private boolean autoDeploySamples;

    private static String configuredStorageDir;
    private static String configuredUserStencilDir;
    private static String configuredStandardStencilDir;
    private static String configuredSamplesStencilDir;
    private static boolean configuredAutoDeploySamples;

    @PostConstruct
    public void init() {
        logger.info("=== StorageConfig PostConstruct Debug ===");
        logger.debug("Raw storageDir from @Value: {}", storageDir);
        logger.debug("Raw ProMarkerAppDir from @Value: {}", proMarkerAppDir);
        logger.debug("Raw userStencilDir from @Value: {}", userStencilDir);
        logger.debug("Raw standardStencilDir from @Value: {}", standardStencilDir);
        logger.debug("Raw samplesStencilDir from @Value: {}", samplesStencilDir);
        logger.debug("Raw autoDeploySamples from @Value: {}", autoDeploySamples);
        
        configuredStorageDir = storageDir;
        configuredUserStencilDir = userStencilDir.replace("${mirel.storage-dir}", storageDir);
        configuredStandardStencilDir = standardStencilDir.replace("${mirel.storage-dir}", storageDir);
        configuredSamplesStencilDir = samplesStencilDir;
        configuredAutoDeploySamples = autoDeploySamples;
        
        initialized = true;  // 初期化完了フラグをセット
        
        logger.info("Processed configuredStorageDir: {}", configuredStorageDir);
        logger.info("Processed configuredUserStencilDir: {}", configuredUserStencilDir);
        logger.info("Processed configuredStandardStencilDir: {}", configuredStandardStencilDir);
        logger.info("Processed configuredSamplesStencilDir: {}", configuredSamplesStencilDir);
        logger.info("Processed configuredAutoDeploySamples: {}", configuredAutoDeploySamples);
    }

    /**
     * 設定されたストレージディレクトリを取得
     * @return ストレージディレクトリパス
     */
    public static String getStorageDir() {
        if (!initialized) {
            logger.warn("StorageConfig not initialized, using default value");
            return "./data/storage";  // フォールバック値
        }
        if (configuredStorageDir == null) {
            logger.warn("StorageConfig.configuredStorageDir is null, using default value");
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
            return "classpath:/promarker/stencil/samples";
        }
        return configuredSamplesStencilDir;
    }

    /**
     * サンプルステンシル自動展開フラグを取得
     * @return 自動展開が有効な場合true
     */
    public static boolean isAutoDeploySamples() {
        if (!initialized) {
            logger.warn("StorageConfig not initialized, auto-deploy-samples defaults to false");
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