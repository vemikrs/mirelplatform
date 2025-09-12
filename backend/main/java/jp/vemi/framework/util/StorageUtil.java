package jp.vemi.framework.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;

import jp.vemi.framework.exeption.MirelSystemException;

/**
 * Strorageに関するユーティリティクラスです。<br/>
 *
 * @author mirelplaftofm
 *
 */
public class StorageUtil {

    protected static String baseDir;
    static {
        final String defaultBaseDir = "C:\\data\\m2\\storage";
        String envMirelStoragePath = "";
        if (StringUtils.isEmpty(envMirelStoragePath)) {
            baseDir = defaultBaseDir;
        } else {
            baseDir = envMirelStoragePath;
        }
}

    public static String getBaseDir() {
        return baseDir;
    }

    /**
     * キャノニキャルパス
     * @param path
     * @return
     */
    public static String parseToCanonicalPath(String path) {
        return StringUtils.join(baseDir, path);
    }

    /**
     * getFile.<br/>
     * @param storagePath パス
     * @return ファイル
     */
    public static File getFile(String storagePath) {
        return new File(baseDir + "\\" + storagePath);
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
        URL url;
        try {
            url = new URL(baseDir + "\\" + storagePath);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        return url;
    }
}
