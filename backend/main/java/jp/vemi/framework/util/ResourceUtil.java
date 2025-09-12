package jp.vemi.framework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import jp.vemi.framework.exeption.MirelSystemException;

import org.apache.commons.lang3.StringUtils;

/**
 * リソースに関するユーティリティクラスです。
 *
 * @author mirelplatform
 *
 */
public class ResourceUtil {

    public static String getResourceAsString(InputStreamReader isReader) {
        BufferedReader reader = new BufferedReader(isReader);
        StringBuilder sbuilder = new StringBuilder();

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sbuilder.append(line);
                sbuilder.append("\r\n"); // 改行
            }
        } catch (IOException e) {
            throw new MirelSystemException(e);
        }

        CloseableUtil.close(reader);

        if (sbuilder.length() < 1)
            return StringUtils.EMPTY;

        return sbuilder.toString();

    }

    /**
     * リソースを文字列として読み込みます。<br/>
     *
     * @param path パス
     * @return リソースの文字列
     */
    public static String getResourceAsString(String path) {
        return getResourceAsString(newFileReader(path));
    }

    /**
     * リソースを文字列として読み込みます。<br/>
     *
     * @param path パス
     * @return リソースの文字列
     */
    public static String getResourceAsString(File file) {
        return getResourceAsString(newFileReader(file));
    }

    /**
     * {@link InputStreamReader} のインスタンスを生成します。.<br/>
     *
     * @param path パス
     * @return {@link InputStreamReader} のインスタンス
     * @throws FileNotFoundException
     */
    public static InputStreamReader newFileReader(String path) {
        try {
            return newFileReader(new File(path));
        } catch (Throwable e) {
            throw new MirelSystemException(e);
        }
    }

    /**
     * {@link InputStreamReader} のインスタンスを生成します。.<br/>
     *
     * @param url {@link URL}
     * @return {@link InputStreamReader} のインスタンス
     * @throws FileNotFoundException
     */
    public static InputStreamReader newFileReader(URL url) {
        return newFileReader(url.getFile());
    }

    /**
     * {@link InputStreamReader} のインスタンスを生成します。.<br/>
     *
     * @param url {@link URL}
     * @return {@link InputStreamReader} のインスタンス
     * @throws FileNotFoundException
     */
    public static InputStreamReader newFileReader(File file) {
        try {
            return new InputStreamReader(new FileInputStream(file), "UTF-8");
        } catch (Throwable e) {
            throw new MirelSystemException(e);
        }
    }

}
