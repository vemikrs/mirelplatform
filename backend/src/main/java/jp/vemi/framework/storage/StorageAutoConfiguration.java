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

/**
 * ストレージサービスの自動構成。
 * <p>
 * mirel.storage.type プロパティに応じて、
 * LocalStorageService または R2StorageService を Bean として登録します。
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
     * R2 ストレージサービス Bean。
     * mirel.storage.type が "r2" の場合に有効。
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "r2")
    public StorageService r2StorageService(StorageProperties props) {
        logger.info("Configuring R2StorageService - bucket: {}, prefix: {}",
                props.getR2().getBucket(), props.getR2().getStoragePrefix());
        return new R2StorageService(props.getR2());
    }

    /**
     * ログ専用の R2 ストレージサービス Bean。
     * mirel.storage.type が "r2" の場合に有効。
     */
    @Bean(name = "logStorageService")
    @ConditionalOnProperty(name = "mirel.storage.type", havingValue = "r2")
    public StorageService r2LogStorageService(StorageProperties props) {
        logger.info("Configuring R2LogStorageService - bucket: {}, prefix: {}",
                props.getR2().getBucket(), props.getR2().getLogsPrefix());
        return new R2StorageService(props.getR2(), props.getR2().getLogsPrefix());
    }
}
