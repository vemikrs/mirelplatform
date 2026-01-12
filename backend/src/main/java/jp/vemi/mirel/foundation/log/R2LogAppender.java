/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.vemi.framework.storage.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

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

    // Spring ApplicationContext を WeakReference で保持（メモリリーク防止、volatile でスレッドセーフ）
    private static volatile WeakReference<ApplicationContext> applicationContextRef;

    public static void setApplicationContext(ApplicationContext context) {
        applicationContextRef = new WeakReference<>(context);
    }

    private static ApplicationContext getApplicationContext() {
        return applicationContextRef != null ? applicationContextRef.get() : null;
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
        ApplicationContext ctx = getApplicationContext();
        if (ctx != null && uploadEnabled) {
            try {
                this.logStorageService = ctx.getBean("logStorageService", StorageService.class);
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

                // アップロード成功後、ローカルファイルを削除
                if (Files.deleteIfExists(rotatedFile.toPath())) {
                    log.debug("Deleted local log file after R2 upload: {}", rotatedFile.getName());
                }
            } catch (IOException e) {
                log.error("Failed to upload log file to R2: {}", rotatedFile.getName(), e);
            }
        });
    }
}
