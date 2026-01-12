/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.vemi.framework.storage.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * 1回のログ出力ごとに個別のファイルを作成するAppender.
 * <p>
 * R2/ローカル両対応。mirel.storage.type=r2 の場合は R2 にアップロード。
 * ファイル名形式: exception_{yyyy-MM-dd_HH-mm-ss-SSS}_{UUID}.log
 * </p>
 */
public class OneFilePerExceptionAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger log = LoggerFactory.getLogger(OneFilePerExceptionAppender.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private String directory = "logs/exceptions";
    private String r2Prefix = "logs/exceptions/";
    private boolean useR2 = false;

    private Encoder<ILoggingEvent> encoder;
    private StorageService logStorageService;
    private ExecutorService uploadExecutor;

    // Spring ApplicationContext を WeakReference で保持（メモリリーク防止、volatile でスレッドセーフ）
    private static volatile WeakReference<ApplicationContext> applicationContextRef;

    public static void setApplicationContext(ApplicationContext context) {
        applicationContextRef = new WeakReference<>(context);
    }

    private static ApplicationContext getApplicationContext() {
        return applicationContextRef != null ? applicationContextRef.get() : null;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setR2Prefix(String prefix) {
        this.r2Prefix = prefix;
    }

    public void setUseR2(boolean useR2) {
        this.useR2 = useR2;
    }

    @SuppressWarnings("rawtypes")
    public void setEncoder(Encoder encoder) {
        this.encoder = (Encoder<ILoggingEvent>) encoder;
    }

    @Override
    public void start() {
        if (this.encoder == null) {
            addError("No encoder set for the appender named \"" + name + "\".");
            return;
        }

        if (!this.encoder.isStarted()) {
            this.encoder.start();
        }

        // R2 ストレージサービスを取得
        ApplicationContext ctx = getApplicationContext();
        if (ctx != null && useR2) {
            try {
                this.logStorageService = ctx.getBean("logStorageService", StorageService.class);
                this.uploadExecutor = Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "ExceptionLogUploader");
                    t.setDaemon(true);
                    return t;
                });
                log.info("OneFilePerExceptionAppender initialized with R2 storage");
            } catch (Exception e) {
                log.warn("logStorageService not available, falling back to local storage: {}", e.getMessage());
                this.useR2 = false;
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
                if (!uploadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    uploadExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                uploadExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String uuid = UUID.randomUUID().toString();
        String filename = String.format("exception_%s_%s.log", timestamp, uuid);

        byte[] encoded = encoder.encode(eventObject);

        if (useR2 && logStorageService != null) {
            // R2 にアップロード
            uploadExecutor.submit(() -> {
                try {
                    String r2Key = r2Prefix + filename;
                    logStorageService.saveFile(r2Key, encoded);
                    log.debug("Exception log uploaded to R2: {}", r2Key);
                } catch (IOException e) {
                    log.warn("R2 upload failed, falling back to local: {}", e.getMessage());
                    // フォールバック: ローカルに保存
                    saveToLocal(filename, encoded);
                }
            });
        } else {
            // ローカルに保存
            saveToLocal(filename, encoded);
        }
    }

    private void saveToLocal(String filename, byte[] data) {
        Path dirPath = Paths.get(directory);
        Path filePath = dirPath.resolve(filename);

        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Files.write(filePath, data);
        } catch (IOException e) {
            addError("Failed to write log file: " + filePath, e);
        }
    }
}
