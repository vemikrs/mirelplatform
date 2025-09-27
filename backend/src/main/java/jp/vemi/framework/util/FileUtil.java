package jp.vemi.framework.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;

/**
 * File management utility class.
 */
public class FileUtil {

    final protected static int BUF_SIZE_DEFAULT = 1024;
    final protected static int BUF_SIZE_ZIP = 2048;
    final protected static String ZIP_CHARSET_DEFAULT = "UTF-8";

    /**
     * default constructor.
     */
    private FileUtil() {
    }

    /**
     * ファイル取得.<br/>
     * 
     * @param in
     * @return
     */
    public static List<File> getFiles(File in) {
        return getFiles(in, new String[0]);
    }

    public static List<File> getFiles(File in, String... ignoreKeywords) {
        if (null == in) {
            return Lists.newArrayList();
        }

        if (in.isFile()) {
            return Lists.newArrayList(in);
        }

        File[] files = in.listFiles();
        if (null == files) {
            return Lists.newArrayList();
        }

        List<File> result = Lists.newArrayList();
        for (File f : files) {
            if (f.isDirectory()) {
                result.addAll(getFiles(f, ignoreKeywords));
            } else {
                result.add(f);
            }
        }
        return result;
    }

    /**
     *
     * @param file
     *            ファイル
     * @param str
     *            文字列
     */
    public static void writeStringToFile(File file, String str) {

        File parentDir = file.getParentFile();
        try {
            Files.createDirectories(parentDir.toPath());
        } catch (IOException e) {
            throw new MirelSystemException(e);
        }
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            out.write(str);
            out.newLine();
        } catch (IOException e) {
            throw new MirelSystemException(e);
        }
    }

    public static String convertExtension(String path, String newExtension) {
        if (StringUtils.isEmpty(path))
            return StringUtils.EMPTY;
        int extIdx = path.lastIndexOf('.');
        if (0 < extIdx)
            path = path.substring(0, extIdx);
        return path + '.' + newExtension;
    }

    public static void transfer(MultipartFile src, File destDir, String fileName) {
        Assert.notNull(src, "src file must not be null.");
        Assert.notNull(destDir, "destDir file must not be null.");
        Assert.hasText(fileName, "fileName file must not be null.");

        if (false == destDir.exists()) {
            destDir.mkdirs();
        }

        File targetFile = new File(destDir.getPath() + "/" + fileName);

        try {
            src.transferTo(targetFile);
        } catch (IllegalStateException | IOException e) {
            throw new MirelApplicationException(e);
        }
    }

    public static void copy(File src, File dest) {
        Assert.notNull(src, "src file must not be null.");
        Assert.notNull(dest, "dest file must not be null.");

        if (false == src.exists()) {
            throw new IllegalArgumentException("src file is not exists.");
        }

        if (src.isFile()) {

            // i/o fileIO
            InputStream in;
            OutputStream out;
            try {
                in = new FileInputStream(src);
                out = new FileOutputStream(dest);

                // copy
                byte[] buf = new byte[BUF_SIZE_DEFAULT];
                while (in.read(buf) != -1) {
                    out.write(buf);
                }

                // flush
                out.flush();
            } catch (FileNotFoundException e) {
                throw new MirelSystemException(e);
            } catch (IOException e) {
                throw new MirelSystemException(e);
            }

            // close
            CloseableUtil.close(in);
            CloseableUtil.close(out);
        } else {
            // directory

            if (false == dest.exists()) {
                dest.mkdirs();
            }

            for (File file : src.listFiles()) {
                String absPath = file.getName();
                File destFile = new File(dest.getPath() + "/" + absPath);
                copy(file, destFile);
            }

        }
    }

    public static boolean zip(File src, String destPath, String defaultFileName) {
        return zip(src, new File(destPath), defaultFileName);
    }

    public static boolean zip(File src, File dest, String defaultFileName) {

        Assert.notNull(src, "src file must not be null.");
        Assert.notNull(dest, "dest file must not be null.");

        List<File> srcItems = getFiles(src);

        if (false == dest.exists()) {
            dest.mkdirs();
        }

        String fileName;
        if (StringUtils.isEmpty(defaultFileName)) {
            Path zipPath = dest.toPath().resolve(src.getName() + ".zip");
            fileName = zipPath.toString();
        } else {
            Path zipPath = dest.toPath().resolve(defaultFileName);
            fileName = zipPath.toString();
        }

        byte[] buf = new byte[BUF_SIZE_ZIP];

        try (OutputStream os = new FileOutputStream(fileName)) {

            ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(
                    os, BUF_SIZE_ZIP), Charset.forName(ZIP_CHARSET_DEFAULT));

            for (File file : srcItems) {

                zip.putNextEntry(new ZipEntry(file.getAbsolutePath().replace(
                        src.getAbsolutePath() + File.separator, "")));

                try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {

                    int len = 0;
                    while ((len = in.read(buf)) != -1) {
                        zip.write(buf, 0, len);
                    }

                    CloseableUtil.close(in);

                }

            }

            CloseableUtil.close(zip);

        } catch (IOException e) {
            throw new MirelApplicationException(e);
        }

        return true;
    }

    /**
     * Search files recursively by filename.
     * 
     * @param filePath
     * @param fileName
     * @return list of hitted filePath.
     */
    public static List<String> findByFileName(String filePath, String fileName) {

        List<File> files = getFiles(new File(filePath));

        List<String> targetFileNames = Lists.newArrayList();
        for (File file : files) {
            if (file.getName().equals(fileName)) {
                targetFileNames.add(file.getAbsolutePath());
            }
        }
        return targetFileNames;
    }

}
