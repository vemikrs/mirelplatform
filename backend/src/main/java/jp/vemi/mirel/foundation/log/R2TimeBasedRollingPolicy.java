/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.log;

import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.helper.CompressionMode;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * R2 へのログアップロードを統合した TimeBasedRollingPolicy。
 * <p>
 * ファイルローテーション完了後に、自動的にローテーションされたファイルを
 * R2 にアップロードします。
 * </p>
 */
public class R2TimeBasedRollingPolicy<E> extends TimeBasedRollingPolicy<E> {

    private static final Logger log = LoggerFactory.getLogger(R2TimeBasedRollingPolicy.class);

    private R2LogAppender r2LogAppender;

    /**
     * R2LogAppender を設定します。
     * Logback XML から r2LogAppender プロパティで参照を設定できます。
     */
    public void setR2LogAppender(R2LogAppender appender) {
        this.r2LogAppender = appender;
    }

    @Override
    public void start() {
        super.start();

        // 親 Appender を自動検出
        if (r2LogAppender == null && getContext() != null) {
            // context から R2LogAppender を探す
            log.debug("R2LogAppender not explicitly set, will use parent appender if available");
        }
    }

    @Override
    public void rollover() throws ch.qos.logback.core.rolling.RolloverFailure {
        // 親クラスのロールオーバー処理
        super.rollover();

        // R2 へのアップロード処理
        uploadToR2();
    }

    private void uploadToR2() {
        // R2LogAppender が設定されていない場合はスキップ
        if (r2LogAppender == null) {
            log.trace("R2LogAppender not configured, skipping R2 upload");
            return;
        }

        try {
            // ローテーションされたファイルを取得
            String elapsedPeriodsFileName = getTimeBasedFileNamingAndTriggeringPolicy()
                    .getElapsedPeriodsFileName();

            if (elapsedPeriodsFileName != null) {
                File rotatedFile = new File(elapsedPeriodsFileName);

                // 圧縮されている場合は圧縮ファイルを使用
                if (getCompressionMode() != CompressionMode.NONE) {
                    String compressedFileName = elapsedPeriodsFileName + "." +
                            getCompressionMode().toString().toLowerCase();
                    File compressedFile = new File(compressedFileName);
                    if (compressedFile.exists()) {
                        rotatedFile = compressedFile;
                    }
                }

                if (rotatedFile.exists()) {
                    r2LogAppender.uploadRotatedFile(rotatedFile);
                    log.debug("Triggered R2 upload for rotated file: {}", rotatedFile.getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to trigger R2 upload after rollover", e);
        }
    }
}
