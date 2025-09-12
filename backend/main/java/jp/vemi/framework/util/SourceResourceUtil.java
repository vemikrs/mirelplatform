package jp.vemi.framework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import jakarta.servlet.ServletContext;

import org.springframework.util.Assert;

public class SourceResourceUtil {

    /** クラスファイルディレクトリフルパス */
    public static String DIR_CLASSES = "";
    /** キャラ（半角スペース） */
    private static final char CHAR_HALF_SPACE = ' ';

    /**
     * SQLファイルのフルパスを生成して返却します。
     *
     * @param clazz
     *            対象クラス
     * @param fileName
     *            リソースファイル名
     * @return
     */
    public static <E> String getResourcePath(String fileName, Class<E> clazz) {
        Assert.notNull(clazz, "clazz must not be null.");
        int lastPos = clazz.getName().lastIndexOf('.');
        String pkg = clazz.getName().substring(0, lastPos);
        ;

        String ret = DIR_CLASSES + (pkg + "\\").replaceAll("\\.", "\\\\")
                + fileName;
        return ret;
    }

    public static String readFileAsOneLine(String fileName, Class<?> clazz) {
        return readFile(fileName, clazz, CHAR_HALF_SPACE);
    }

    /**
     * ファイル読込.<br/>
     *
     * @param fileName
     *            ファイル名
     * @param clazz
     *            対象クラス
     * @param lineSeparator
     *            行区切り文字
     * @return ファイル内容
     */
    public static String readFile(String fileName, Class<?> clazz,
            char lineSeparator) {

        String resourcePath = SourceResourceUtil.getResourcePath(fileName, clazz);

        InputStreamReader iReader = null;
        try {
            iReader = newFileReader(resourcePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        BufferedReader reader = new BufferedReader(iReader);

        StringBuilder sbuilder = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sbuilder.append(line);
                sbuilder.append(lineSeparator);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (null != reader)
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        if (sbuilder.length() < 1)
            return null;

        return sbuilder.toString();
    }

    public static synchronized void setDirClasses(ServletContext context) {
        String defaultPath = context.getRealPath("/WEB-INF/classes/");
        if (!defaultPath.endsWith("\\"))
            defaultPath = defaultPath + "\\";
        if (defaultPath != null)
            DIR_CLASSES = defaultPath;
        return;
    }

    public static InputStreamReader newFileReader(String resourcePath)
            throws FileNotFoundException {

        try {
            return new InputStreamReader(new FileInputStream(new File(
                    resourcePath)), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new InputStreamReader(new FileInputStream(new File(
                    resourcePath)));
        }

    }
}
