/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import lombok.Setter;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 1回のログ出力ごとに個別のファイルを作成するAppender.
 * <p>
 * ファイル名形式: exception_{yyyy-MM-dd_HH-mm-ss-SSS}_{UUID}.log
 * </p>
 */
public class OneFilePerExceptionAppender extends AppenderBase<ILoggingEvent> {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    @Setter
    private String directory = "logs/exceptions";

    @Setter
    private Encoder<ILoggingEvent> encoder;

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null) {
            return;
        }

        // ファイル名を生成
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String uuid = UUID.randomUUID().toString();
        String filename = String.format("exception_%s_%s.log", timestamp, uuid);

        Path dirPath = Paths.get(directory);
        Path filePath = dirPath.resolve(filename);

        try {
            // ディレクトリ作成
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // ログ内容をエンコード
            byte[] encoded = encoder.encode(eventObject);

            // ファイル書き込み
            Files.write(filePath, encoded);

        } catch (IOException e) {
            addError("Failed to write log file: " + filePath, e);
        }
    }
}
