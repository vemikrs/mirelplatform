/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.storage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ローカルファイルシステム用のストレージサービス実装。
 */
public class LocalStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path baseDir;

    public LocalStorageService(String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        logger.info("LocalStorageService initialized with baseDir: {}", this.baseDir);
    }

    /**
     * パスを解決し、ベースディレクトリ外への逸脱を防止します。
     */
    private Path resolveSecurePath(String path) {
        String sanitized = path == null ? "" : path.replace('\\', '/');
        if (sanitized.startsWith("/")) {
            sanitized = sanitized.replaceFirst("^/+", "");
        }
        Path resolved = baseDir.resolve(sanitized).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("Path escapes storage base directory: " + path);
        }
        return resolved;
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        Path filePath = resolveSecurePath(path);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + path);
        }
        return new FileInputStream(filePath.toFile());
    }

    @Override
    public void saveFile(String path, InputStream data, long contentLength) throws IOException {
        Path filePath = resolveSecurePath(path);
        Files.createDirectories(filePath.getParent());
        Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
        logger.debug("Saved file to: {}", filePath);
    }

    @Override
    public void saveFile(String path, byte[] data) throws IOException {
        saveFile(path, new ByteArrayInputStream(data), data.length);
    }

    @Override
    public List<String> listFiles(String prefix) {
        Path dirPath = resolveSecurePath(prefix);
        List<String> results = new ArrayList<>();

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return results;
        }

        try (Stream<Path> stream = Files.walk(dirPath)) {
            stream.filter(Files::isRegularFile)
                    .forEach(p -> {
                        String relativePath = baseDir.relativize(p).toString().replace('\\', '/');
                        results.add(relativePath);
                    });
        } catch (IOException e) {
            logger.error("Failed to list files in: {}", prefix, e);
        }
        return results;
    }

    @Override
    public boolean exists(String path) {
        Path filePath = resolveSecurePath(path);
        return Files.exists(filePath);
    }

    @Override
    public void delete(String path) throws IOException {
        Path filePath = resolveSecurePath(path);
        Files.deleteIfExists(filePath);
        logger.debug("Deleted file: {}", filePath);
    }

    @Override
    public URL getPresignedUrl(String path, Duration expiry) {
        try {
            Path filePath = resolveSecurePath(path);
            return filePath.toUri().toURL();
        } catch (Exception e) {
            logger.error("Failed to generate URL for: {}", path, e);
            return null;
        }
    }

    @Override
    public String getBasePath() {
        return baseDir.toString();
    }

    /**
     * ファイルオブジェクトを直接取得します（後方互換性のため）。
     *
     * @param path
     *            ストレージ相対パス
     * @return File オブジェクト
     */
    public File getFile(String path) {
        return resolveSecurePath(path).toFile();
    }
}
