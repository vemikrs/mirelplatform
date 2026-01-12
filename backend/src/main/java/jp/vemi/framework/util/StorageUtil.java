package jp.vemi.framework.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import jp.vemi.framework.config.StorageConfig;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.storage.LocalStorageService;
import jp.vemi.framework.storage.StorageService;

/**
 * Strorageに関するユーティリティクラスです。<br/>
 * <p>
 * このクラスは {@link StorageService} へのブリッジとして動作します。
 * クラウド環境（R2）では {@link StorageService} Bean を使用し、
 * ローカル環境では従来通りのファイルシステム操作にフォールバックします。
 * </p>
 *
 * @author mirelplaftofm
 */
public class StorageUtil {

    private static final Logger logger = LoggerFactory.getLogger(StorageUtil.class);

    // WeakReference で保持してメモリリーク防止
    private static WeakReference<ApplicationContext> applicationContextRef;
    private static WeakReference<StorageService> storageServiceRef;

    /**
     * private constructor to prevent instantiation
     */
    private StorageUtil() {
    }

    /**
     * Spring ApplicationContext を設定します。
     * StorageService Bean の取得に使用されます。
     */
    public static void setApplicationContext(ApplicationContext context) {
        applicationContextRef = new WeakReference<>(context);
        storageServiceRef = null; // リセット
    }

    /**
     * StorageService インスタンスを取得します。
     */
    private static StorageService getStorageService() {
        StorageService cached = storageServiceRef != null ? storageServiceRef.get() : null;
        if (cached != null) {
            return cached;
        }

        ApplicationContext ctx = applicationContextRef != null ? applicationContextRef.get() : null;
        if (ctx != null) {
            try {
                StorageService service = ctx.getBean(StorageService.class);
                storageServiceRef = new WeakReference<>(service);
                logger.debug("StorageService Bean obtained: {}", service.getClass().getSimpleName());
                return service;
            } catch (Exception e) {
                logger.debug("StorageService Bean not available, using local fallback");
            }
        }
        return null;
    }

    /**
     * ローカルフォールバック用のベースディレクトリを取得します。
     */
    public static String getBaseDir() {
        StorageService service = getStorageService();
        if (service != null) {
            return service.getBasePath();
        }
        return StorageConfig.getStorageDir();
    }

    /**
     * ストレージ基準ディレクトリ配下で安全にパスを解決します。
     * - 先頭のスラッシュはストレージ相対とみなして除去
     * - 正規化してベースディレクトリ配下であることを検証
     *
     * @param storagePath
     *            ストレージ相対パス（先頭に/が付いていても可）
     * @return ベースディレクトリ配下の正規化済み Path
     * @throws IllegalArgumentException
     *             ベースディレクトリ外へ逸脱する場合
     */
    private static Path resolveWithinBase(String storagePath) {
        String sp = storagePath == null ? "" : storagePath;
        sp = sp.replace('\\', '/');
        if (sp.startsWith("/")) {
            sp = sp.replaceFirst("^/+", "");
        }

        Path base = Paths.get(getBaseDir()).normalize();
        Path resolved = base.resolve(sp).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("Path escapes storage base directory: " + storagePath);
        }
        return resolved;
    }

    /**
     * キャノニキャルパス
     */
    public static String parseToCanonicalPath(String path) {
        return resolveWithinBase(path).toString();
    }

    /**
     * getFile.<br/>
     * 
     * @param storagePath
     *            パス
     * @return ファイル
     */
    public static File getFile(String storagePath) {
        StorageService service = getStorageService();

        // LocalStorageService の場合は直接 File を返せる
        if (service instanceof LocalStorageService) {
            return ((LocalStorageService) service).getFile(storagePath);
        }

        // R2 の場合は一時ファイルにダウンロード
        // 注意: 呼び出し元がファイル使用後に明示的に削除する責任があります
        if (service != null && service.exists(storagePath)) {
            try {
                Path tempFile = Files.createTempFile("storage-", getFileName(storagePath));
                try (InputStream is = service.getInputStream(storagePath)) {
                    Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                // 長時間稼働を想定し deleteOnExit() は使用しない
                // 呼び出し元が使用後に Files.deleteIfExists() で削除すること
                logger.debug("Created temp file for R2 download: {}", tempFile);
                return tempFile.toFile();
            } catch (IOException e) {
                logger.warn("Failed to download from StorageService, falling back to local: {}", e.getMessage());
            }
        }

        // フォールバック: ローカルファイルシステム
        return resolveWithinBase(storagePath).toFile();
    }

    private static String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * 配下ファイルの取得.<br/>
     * 
     * @param storagePath
     *            パス
     * @return {@link File} のリスト
     */
    public static List<String> getFiles(String storagePath) {
        if (StringUtils.isEmpty(storagePath))
            return Lists.newArrayList();

        StorageService service = getStorageService();
        if (service != null) {
            List<String> files = service.listFiles(storagePath);
            if (!files.isEmpty()) {
                // StorageService から返されるパスは storagePath からの相対パス
                // ここでは storagePath を基準にフルパスを構築
                return files.stream()
                        .map(f -> Paths.get(getBaseDir(), storagePath, f).normalize().toString())
                        .collect(Collectors.toList());
            }
        }

        // フォールバック: ローカルファイルシステム
        File file = getFile(storagePath);
        List<File> files = FileUtil.getFiles(file);

        List<String> fileNames = Lists.newArrayList();
        for (File f : files) {
            try {
                fileNames.add(f.getCanonicalPath());
            } catch (IOException e) {
                throw new MirelSystemException(e);
            }
        }

        return fileNames;
    }

    /**
     * ファイルの存在確認
     */
    public static boolean exists(String storagePath) {
        StorageService service = getStorageService();
        if (service != null) {
            return service.exists(storagePath);
        }
        return resolveWithinBase(storagePath).toFile().exists();
    }

    /**
     * InputStream を取得
     */
    public static InputStream getInputStream(String storagePath) throws IOException {
        StorageService service = getStorageService();
        if (service != null) {
            return service.getInputStream(storagePath);
        }
        return Files.newInputStream(resolveWithinBase(storagePath));
    }

    /**
     * ファイルを保存
     */
    public static void saveFile(String storagePath, byte[] data) throws IOException {
        StorageService service = getStorageService();
        if (service != null) {
            service.saveFile(storagePath, data);
            return;
        }
        Path path = resolveWithinBase(storagePath);
        Files.createDirectories(path.getParent());
        Files.write(path, data);
    }

    public static URL getResource(String storagePath) {
        StorageService service = getStorageService();
        if (service != null) {
            URL url = service.getPresignedUrl(storagePath, Duration.ofHours(1));
            if (url != null) {
                return url;
            }
        }

        try {
            Path resolved = resolveWithinBase(storagePath);
            return resolved.toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
