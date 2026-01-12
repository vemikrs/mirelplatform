/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.google.cloud.storage.Storage;

import jp.vemi.framework.storage.gcs.GcsStorageService;

/**
 * ストレージサービスの自動構成。
 * <p>
 * mirel.storage.type プロパティに応じて、
 * Local, S3(R2), GCS のいずれかの StorageService を Bean として登録します。
 * </p>
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(StorageAutoConfiguration.class);

    /**
     * ローカルストレージサービス Bean。
     * mirel.storage.type が "local" または未設定の場合に有効。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "local", matchIfMissing = true)
    public StorageService localStorageService(StorageProperties props) {
        logger.info("Configuring LocalStorageService with baseDir: {}", props.getLocal().getBaseDir());
        return new LocalStorageService(props.getLocal().getBaseDir());
    }

    /**
     * ローカル環境用ログストレージサービス Bean。
     * mirel.storage.type が "local" または未設定の場合に有効。
     */
    @Bean(name = "logStorageService")
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "local", matchIfMissing = true)
    public StorageService localLogStorageService(StorageProperties props) {
        String logDir = props.getLocal().getBaseDir() + "/logs";
        logger.info("Configuring LocalLogStorageService with baseDir: {}", logDir);
        return new LocalStorageService(logDir);
    }

    /**
     * AWS S3 互換 (R2) ストレージサービス Bean。
     * mirel.storage.type が "r2" (または "s3") の場合に有効。
     * 現状は互換性維持のため "r2" を判定値とする。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "r2")
    public StorageService s3StorageService(StorageProperties props) {
        logger.info("Configuring S3StorageService (R2 Compatible) - bucket: {}, prefix: {}",
                props.getR2().getBucket(), props.getR2().getStoragePrefix());
        return new S3StorageService(props.getR2());
    }

    /**
     * ログ専用の S3 (R2) ストレージサービス Bean。
     */
    @Bean(name = "logStorageService")
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "r2")
    public StorageService s3LogStorageService(StorageProperties props) {
        logger.info("Configuring S3LogStorageService (R2 Compatible) - bucket: {}, prefix: {}",
                props.getR2().getBucket(), props.getR2().getLogsPrefix());
        return new S3StorageService(props.getR2(), props.getR2().getLogsPrefix());
    }

    /**
     * Google Cloud Storage (GCS) サービス Bean。
     * mirel.storage.type が "gcs" の場合に有効。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "gcs")
    public StorageService gcsStorageService(StorageProperties props, Storage storage) {
        logger.info("Configuring GcsStorageService - bucket: {}, prefix: {}",
                props.getGcs().getBucket(), props.getGcs().getStoragePrefix());
        return new GcsStorageService(storage, props.getGcs().getBucket(), props.getGcs().getStoragePrefix());
    }

    /**
     * ログ専用の GCS ストレージサービス Bean。
     * 基本的にコンソール出力(Cloud Logging)推奨だが、明示的にファイル保存が必要なケース用。
     * GCSの場合は同一バケット・プレフィックス違いで運用する想定。
     */
    @Bean(name = "logStorageService")
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "gcs")
    public StorageService gcsLogStorageService(StorageProperties props, Storage storage) {
        String logPrefix = "logs/"; // デフォルト値。必要ならプロパティ化
        logger.info("Configuring GcsLogStorageService - bucket: {}, prefix: {}",
                props.getGcs().getBucket(), logPrefix);
        return new GcsStorageService(storage, props.getGcs().getBucket(), logPrefix);
    }
}
