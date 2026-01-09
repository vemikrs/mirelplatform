/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.bootstrap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

/**
 * Bootstrapトークン管理サービス.
 * 
 * 初回起動時にトークンファイルを生成し、
 * Bootstrap完了後に削除します。
 */
@Service
public class BootstrapTokenService {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapTokenService.class);

    private static final String TOKEN_DIR = "bootstrap";
    private static final String TOKEN_FILE = "setup-token.txt";

    @Value("${mirel.storage-dir:./data/storage}")
    private String storageDir;

    // メモリキャッシュ（起動中のトークン検証用）
    private String cachedToken = null;

    /**
     * トークンファイルのパスを取得
     */
    private Path getTokenPath() {
        return Paths.get(storageDir, TOKEN_DIR, TOKEN_FILE);
    }

    /**
     * 初回起動時にトークンファイルを生成
     * 
     * @return 生成されたトークン（既に完了済みの場合はnull）
     */
    public String generateTokenIfNeeded() {
        Path tokenPath = getTokenPath();

        // 既にトークンファイルが存在する場合は読み込み
        if (Files.exists(tokenPath)) {
            return readTokenFromFile(tokenPath);
        }

        // 新規トークン生成
        String token = UUID.randomUUID().toString();
        cachedToken = token;

        String content = """
                ========================================
                MirelPlatform 初期セットアップトークン
                ========================================

                以下のトークンを使用して初期管理者を作成してください。

                トークン: %s

                セットアップ方法:
                1. ブラウザで {base-url}/bootstrap にアクセス
                2. または以下のAPIを呼び出し:
                   POST /api/bootstrap/admin

                ※このファイルはセットアップ完了後に自動削除されます
                ※このファイルにアクセスできる人のみが初期セットアップ可能です
                ========================================
                """.formatted(token);

        try {
            Files.createDirectories(tokenPath.getParent());
            Files.writeString(tokenPath, content);

            // Linuxの場合、ファイル権限を600に設定
            try {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(tokenPath, perms);
            } catch (UnsupportedOperationException e) {
                // Windows等POSIX非対応OSでは無視
                logger.debug("POSIX file permissions not supported on this OS");
            }

            // lgtm[java/sensitive-log] - logging fixed success message only, no sensitive
            // data
            logger.info("Bootstrap token file created successfully");
            // Note: Token value intentionally not logged for security

            return token;
        } catch (IOException e) {
            logger.error("Failed to create bootstrap token file", e);
            return null;
        }
    }

    /**
     * トークンファイルからトークンを読み込み
     */
    private String readTokenFromFile(Path tokenPath) {
        try {
            String content = Files.readString(tokenPath);
            // "トークン: " の後の値を抽出
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("トークン:")) {
                    cachedToken = line.replace("トークン:", "").trim();
                    return cachedToken;
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read bootstrap token file", e);
        }
        return null;
    }

    /**
     * トークンを検証
     * 
     * @param token
     *            検証するトークン
     * @return 有効な場合true
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        // メモリキャッシュと比較
        if (cachedToken != null && cachedToken.equals(token)) {
            return true;
        }

        // ファイルから再読み込みして比較
        Path tokenPath = getTokenPath();
        if (Files.exists(tokenPath)) {
            String fileToken = readTokenFromFile(tokenPath);
            return token.equals(fileToken);
        }

        return false;
    }

    /**
     * トークンファイルが存在するか確認
     */
    public boolean tokenFileExists() {
        return Files.exists(getTokenPath());
    }

    /**
     * トークンファイルを削除
     */
    public void deleteTokenFile() {
        Path tokenPath = getTokenPath();
        try {
            if (Files.exists(tokenPath)) {
                Files.delete(tokenPath);
                // lgtm[java/sensitive-log] - logging fixed success message only, no sensitive
                // data
                logger.info("Bootstrap token file deleted successfully");
            }
            cachedToken = null;
        } catch (IOException e) {
            logger.error("Failed to delete bootstrap token file", e);
        }
    }
}
