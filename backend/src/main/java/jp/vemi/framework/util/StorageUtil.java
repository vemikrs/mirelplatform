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
     * キャノニキャルパス
     * @param path
     * @return
     */
    public static String parseToCanonicalPath(String path) {
        return Paths.get(getBaseDir()).resolve(path).toString();
    }

    /**
     * getFile.<br/>
     * @param storagePath パス
     * @return ファイル
     */
    public static File getFile(String storagePath) {
        Path resolved = Paths.get(getBaseDir()).resolve(storagePath);
        return resolved.toFile();
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
            Path resolved = Paths.get(getBaseDir()).resolve(storagePath);
            return resolved.toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
