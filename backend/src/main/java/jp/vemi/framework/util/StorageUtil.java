package jp.vemi.framework.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import jp.vemi.framework.config.StorageConfig;
import jp.vemi.framework.exeption.MirelSystemException;

/**
 * Strorageに関するユーティリティクラスです。<br/>
 * 設定値はStorageConfigから取得します。
 *
 * @author mirelplaftofm
 *
 */
public class StorageUtil {

    /**
     * private constructor to prevent instantiation
     */
    private StorageUtil() {
    }

    public static String getBaseDir() {
        return StorageConfig.getStorageDir();
    }

    /**
     * ストレージ基準ディレクトリ配下で安全にパスを解決します。
     * - 先頭のスラッシュはストレージ相対とみなして除去
     * - 正規化してベースディレクトリ配下であることを検証
     *
     * @param storagePath ストレージ相対パス（先頭に/が付いていても可）
     * @return ベースディレクトリ配下の正規化済み Path
     * @throws IllegalArgumentException ベースディレクトリ外へ逸脱する場合
     */
    private static Path resolveWithinBase(String storagePath) {
        String sp = storagePath == null ? "" : storagePath;
        // Windowsの区切りやバックスラッシュを防止
        sp = sp.replace('\\', '/');
        // 先頭の/はストレージ相対と見なして除去
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
     * @param path
     * @return
     */
    public static String parseToCanonicalPath(String path) {
        return resolveWithinBase(path).toString();
    }

    /**
     * getFile.<br/>
     * @param storagePath パス
     * @return ファイル
     */
    public static File getFile(String storagePath) {
        return resolveWithinBase(storagePath).toFile();
    }
    /**
     * 配下ファイルの取得.<br/>
     * @param storagePath パス
     * @return {@link File} のリスト
     */
    public static List<String> getFiles(String storagePath) {
        if (StringUtils.isEmpty(storagePath))
            return Lists.newArrayList();

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

    public static URL getResource(String storagePath) {
        try {
            Path resolved = resolveWithinBase(storagePath);
            return resolved.toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
