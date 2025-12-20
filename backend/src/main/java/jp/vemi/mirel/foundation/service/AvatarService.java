/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Avatar Service - ユーザーアバター画像の管理
 * 
 * アバター画像のダウンロード、保存、URL生成を担当します。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarService {

    @Value("${mirel.storage-dir:./data/storage}")
    private String storageDir;

    @Value("${server.servlet.context-path:/mipla2}")
    private String contextPath;

    private static final String AVATARS_DIR = "avatars";
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * アバター画像をダウンロードして保存します。
     * 
     * @param avatarUrl
     *            GitHub等のアバター画像URL
     * @param userId
     *            ユーザーID
     * @return 保存されたアバター画像の相対URL (例: /api/users/{userId}/avatar)
     */
    public String downloadAndSaveAvatar(String avatarUrl, UUID userId) {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            log.warn("Avatar URL is null or empty for user: {}", userId);
            return null;
        }

        try {
            // アバター画像をダウンロード
            byte[] imageBytes = restTemplate.getForObject(avatarUrl, byte[].class);

            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("Failed to download avatar image from: {}", avatarUrl);
                return null;
            }

            // ファイルサイズチェック
            if (imageBytes.length > MAX_AVATAR_SIZE) {
                log.warn("Avatar image too large: {} bytes (max: {})", imageBytes.length, MAX_AVATAR_SIZE);
                return null;
            }

            // 保存先ディレクトリを作成
            Path avatarsPath = Paths.get(storageDir, AVATARS_DIR);
            if (!Files.exists(avatarsPath)) {
                Files.createDirectories(avatarsPath);
                log.info("Created avatars directory: {}", avatarsPath);
            }

            // ファイル名生成（ユーザーID + 拡張子）
            String extension = extractExtension(avatarUrl);
            String fileName = userId.toString() + extension;
            Path filePath = avatarsPath.resolve(fileName);

            // ファイル保存
            Files.write(filePath, imageBytes);
            log.info("Saved avatar image: {} ({} bytes)", filePath, imageBytes.length);

            // APIエンドポイントURLを返却
            return "/api/users/" + userId + "/avatar";

        } catch (IOException e) {
            log.error("Failed to save avatar image for user: {}", userId, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while downloading avatar: {}", avatarUrl, e);
            return null;
        }
    }

    /**
     * アバター画像を取得します。
     * 
     * @param userId
     *            ユーザーID
     * @return アバター画像のバイト配列、存在しない場合はnull
     */
    public byte[] getAvatar(UUID userId) {
        try {
            Path avatarsPath = Paths.get(storageDir, AVATARS_DIR);

            // 複数の拡張子を試す
            for (String ext : new String[] { ".jpg", ".png", ".gif", ".jpeg" }) {
                Path filePath = avatarsPath.resolve(userId.toString() + ext);
                if (Files.exists(filePath)) {
                    return Files.readAllBytes(filePath);
                }
            }

            log.debug("Avatar not found for user: {}", userId);
            return null;

        } catch (IOException e) {
            log.error("Failed to read avatar image for user: {}", userId, e);
            return null;
        }
    }

    /**
     * アバター画像を削除します。
     * 
     * @param userId
     *            ユーザーID
     */
    public void deleteAvatar(UUID userId) {
        try {
            Path avatarsPath = Paths.get(storageDir, AVATARS_DIR);

            // 複数の拡張子を試す
            for (String ext : new String[] { ".jpg", ".png", ".gif", ".jpeg", ".webp" }) {
                Path filePath = avatarsPath.resolve(userId.toString() + ext);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted avatar image: {}", filePath);
                }
            }

        } catch (IOException e) {
            log.error("Failed to delete avatar image for user: {}", userId, e);
        }
    }

    /**
     * バイト配列からアバター画像を保存します。
     * 
     * @param imageBytes
     *            画像バイト配列
     * @param userId
     *            ユーザーID
     * @param extension
     *            ファイル拡張子（例: ".jpg"）
     * @return 保存されたアバター画像の相対URL
     */
    public String saveAvatarFromBytes(byte[] imageBytes, UUID userId, String extension) {
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("Image bytes is null or empty for user: {}", userId);
            return null;
        }

        try {
            // ファイルサイズチェック
            if (imageBytes.length > MAX_AVATAR_SIZE) {
                log.warn("Avatar image too large: {} bytes (max: {})", imageBytes.length, MAX_AVATAR_SIZE);
                return null;
            }

            // 保存先ディレクトリを作成
            Path avatarsPath = Paths.get(storageDir, AVATARS_DIR);
            if (!Files.exists(avatarsPath)) {
                Files.createDirectories(avatarsPath);
                log.info("Created avatars directory: {}", avatarsPath);
            }

            // 既存のアバターを削除
            deleteAvatar(userId);

            // ファイル名生成（ユーザーID + 拡張子）
            String fileName = userId.toString() + extension;
            Path filePath = avatarsPath.resolve(fileName);

            // ファイル保存
            Files.write(filePath, imageBytes);
            log.info("Saved avatar image: {} ({} bytes)", filePath, imageBytes.length);

            // APIエンドポイントURLを返却
            return "/api/users/" + userId + "/avatar";

        } catch (IOException e) {
            log.error("Failed to save avatar image for user: {}", userId, e);
            return null;
        }
    }

    /**
     * URLから拡張子を抽出します。
     * 
     * @param url
     *            画像URL
     * @return 拡張子（例: ".jpg"）、抽出できない場合は ".jpg"
     */
    private String extractExtension(String url) {
        if (url == null || url.isEmpty()) {
            return ".jpg";
        }

        // クエリパラメータを除去
        String urlWithoutQuery = url.split("\\?")[0];

        // 拡張子を抽出
        int lastDotIndex = urlWithoutQuery.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < urlWithoutQuery.length() - 1) {
            String ext = urlWithoutQuery.substring(lastDotIndex);
            // 有効な画像拡張子かチェック
            if (ext.matches("\\.(jpg|jpeg|png|gif)")) {
                return ext;
            }
        }

        // デフォルト拡張子
        return ".jpg";
    }

    /**
     * デフォルトアバター画像のURLを取得します。
     * 
     * @return デフォルトアバターのURL
     */
    public String getDefaultAvatarUrl() {
        return "/assets/default-avatar.png";
    }
}
