/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.TriggeringPolicy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.vemi.framework.storage.StorageService;
import jp.vemi.framework.storage.R2StorageService;
import jp.vemi.framework.storage.StorageProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * R2 へログをアップロードする Logback Appender。
 * <p>
 * ローカルファイルに一時書き込み後、ローテーション時に R2 へアップロードします。
 * StorageService (logStorageService Bean) を使用します。
 * </p>
 */
public class R2LogAppender extends RollingFileAppender<ILoggingEvent> {

    private static final Logger log = LoggerFactory.getLogger(R2LogAppender.class);

    private StorageService logStorageService;
    private String r2Prefix = "logs/";
    private boolean uploadEnabled = true;
    private ExecutorService uploadExecutor;

    // Spring ApplicationContext を静的に保持（Logback は Spring 管理外のため）
    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    public void setR2Prefix(String prefix) {
        this.r2Prefix = prefix;
    }

    public void setUploadEnabled(boolean enabled) {
        this.uploadEnabled = enabled;
    }

    @Override
    public void start() {
        // StorageService を取得（Spring コンテキストから）
        if (applicationContext != null && uploadEnabled) {
            try {
                this.logStorageService = applicationContext.getBean("logStorageService", StorageService.class);
                this.uploadExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "R2LogUploader");
                    t.setDaemon(true);
                    return t;
                });
                log.info("R2LogAppender initialized with logStorageService");
            } catch (Exception e) {
                log.warn("logStorageService not available, R2 upload disabled: {}", e.getMessage());
                this.uploadEnabled = false;
            }
        }

        super.start();
    }

    @Override
    public void stop() {
        super.stop();

        if (uploadExecutor != null) {
            uploadExecutor.shutdown();
            try {
                if (!uploadExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    uploadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                uploadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * ローテーション後のファイルを R2 にアップロードします。
     * このメソッドは RollingPolicy から呼び出されます。
     */
    public void uploadRotatedFile(File rotatedFile) {
        if (!uploadEnabled || logStorageService == null || rotatedFile == null || !rotatedFile.exists()) {
            return;
        }

        uploadExecutor.submit(() -> {
            try {
                String r2Key = r2Prefix + rotatedFile.getName();
                byte[] data = Files.readAllBytes(rotatedFile.toPath());
                logStorageService.saveFile(r2Key, data);
                log.info("Uploaded log file to R2: {}", r2Key);

                // アップロード成功後、ローカルファイルを削除（オプション）
                // Files.deleteIfExists(rotatedFile.toPath());
            } catch (IOException e) {
                log.error("Failed to upload log file to R2: {}", rotatedFile.getName(), e);
            }
        });
    }
}
