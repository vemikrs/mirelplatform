/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1回のログ出力ごとに個別のファイルを作成するAppender.
 * <p>
 * ローカル開発環境でのデバッグ用。
 * ファイル名形式: exception_{yyyy-MM-dd_HH-mm-ss-SSS}_{UUID}.log
 * </p>
 */
public class OneFilePerExceptionAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger log = LoggerFactory.getLogger(OneFilePerExceptionAppender.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    private String directory = "logs/exceptions";

    // 後方互換性のため残すが使用しない
    private boolean useR2 = false;

    private Encoder<ILoggingEvent> encoder;

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setUseR2(boolean useR2) {
        if (useR2) {
            log.warn(
                    "OneFilePerExceptionAppender: 'useR2' is set to true but R2 upload is no longer supported. Logs will be saved locally only.");
        }
        this.useR2 = useR2;
    }

    public void setR2Prefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            log.warn("OneFilePerExceptionAppender: 'r2Prefix' property ('{}') is ignored. R2 upload is not supported.",
                    prefix);
        }
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

        super.start();
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

        saveToLocal(filename, encoded);
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
            log.error("OneFilePerExceptionAppender: Failed to write log file: {}", filePath, e);
        }
    }
}
