/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.bootstrap.service;

import jp.vemi.framework.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Bootstrapトークン管理サービス.
 * 
 * 初回起動時にトークンファイルを生成し、
 * Bootstrap完了後に削除します。
 * 
 * StorageService を使用して GCS/S3/ローカルを透過的にサポートします。
 */
@Service
public class BootstrapTokenService {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapTokenService.class);

    private static final String TOKEN_PATH = "bootstrap/setup-token.txt";

    private final StorageService storageService;

    // メモリキャッシュ（起動中のトークン検証用）
    private String cachedToken = null;

    public BootstrapTokenService(StorageService storageService) {
        System.out.println("[DEBUG] BootstrapTokenService: Constructor called, StorageService type = "
                + (storageService != null ? storageService.getClass().getName() : "null"));
        System.out.flush();
        this.storageService = storageService;
        System.out.println("[DEBUG] BootstrapTokenService: Constructor completed successfully");
        System.out.flush();
    }

    /**
     * 初回起動時にトークンファイルを生成
     * 
     * @return 生成されたトークン（既に完了済みの場合はnull）
     */
    public String generateTokenIfNeeded() {
        // 既にトークンファイルが存在する場合は読み込み
        if (storageService.exists(TOKEN_PATH)) {
            return readTokenFromStorage();
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
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            try (InputStream inputStream = new ByteArrayInputStream(contentBytes)) {
                storageService.saveFile(TOKEN_PATH, inputStream, (long) contentBytes.length);
            }

            // lgtm[java/sensitive-log] - logging fixed success message only, no sensitive
            // data
            logger.info("Bootstrap token file created successfully in storage");
            // Note: Token value intentionally not logged for security

            return token;
        } catch (Exception e) {
            logger.error("Failed to create bootstrap token file in storage", e);
            throw new BootstrapStorageException("Failed to create bootstrap token file", e);
        }
    }

    /**
     * ストレージからトークンを読み込み
     */
    private String readTokenFromStorage() {
        try (InputStream inputStream = storageService.getInputStream(TOKEN_PATH)) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            // "トークン: " の後の値を抽出
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("トークン:")) {
                    cachedToken = line.replace("トークン:", "").trim();
                    return cachedToken;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to read bootstrap token file from storage", e);
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

        // ストレージから再読み込みして比較
        if (storageService.exists(TOKEN_PATH)) {
            String fileToken = readTokenFromStorage();
            return token.equals(fileToken);
        }

        return false;
    }

    /**
     * トークンファイルが存在するか確認
     */
    public boolean tokenFileExists() {
        return storageService.exists(TOKEN_PATH);
    }

    /**
     * トークンファイルを削除
     */
    public void deleteTokenFile() {
        try {
            if (storageService.exists(TOKEN_PATH)) {
                storageService.delete(TOKEN_PATH);
                // lgtm[java/sensitive-log] - logging fixed success message only, no sensitive
                // data
                logger.info("Bootstrap token file deleted successfully from storage");
            }
            cachedToken = null;
        } catch (Exception e) {
            logger.error("Failed to delete bootstrap token file from storage", e);
        }
    }

    /**
     * ストレージ操作のカスタム例外
     */
    public static class BootstrapStorageException extends RuntimeException {
        public BootstrapStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
