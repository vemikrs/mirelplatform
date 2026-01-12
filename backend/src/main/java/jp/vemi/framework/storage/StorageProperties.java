/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * ストレージ設定プロパティ。
 * <p>
 * application.yml の mirel.storage 配下の設定をバインドします。
 * StorageAutoConfiguration で @EnableConfigurationProperties により Bean 登録されます。
 * </p>
 */
@ConfigurationProperties(prefix = "mirel.storage")
@Getter
@Setter
public class StorageProperties {

    /**
     * ストレージタイプ: "local" または "r2"
     */
    private String type = "local";

    /**
     * ローカルストレージ設定
     */
    private LocalProperties local = new LocalProperties();

    /**
     * R2 ストレージ設定
     */
    private R2Properties r2 = new R2Properties();

    @Getter
    @Setter
    public static class LocalProperties {
        /**
         * ローカルストレージのベースディレクトリ
         */
        private String baseDir = "./data/storage";
    }

    @Getter
    @Setter
    public static class R2Properties {
        /**
         * R2 エンドポイント URL
         */
        private String endpoint;

        /**
         * R2 バケット名
         */
        private String bucket;

        /**
         * ストレージ用プレフィックス
         */
        private String storagePrefix = "storage/";

        /**
         * ログ用プレフィックス
         */
        private String logsPrefix = "logs/";

        /**
         * アクセスキー ID
         */
        private String accessKeyId;

        /**
         * シークレットアクセスキー
         */
        private String secretAccessKey;

        /**
         * リージョン（R2 は auto を使用）
         */
        private String region = "auto";
    }
}
