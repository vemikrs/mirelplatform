/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.foundation.feature.files.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import jp.vemi.framework.storage.StorageService;
import jp.vemi.framework.util.DateUtil;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;

/**
 * {@link FileRegisterService} の具象です。
 * <p>
 * StorageService を使用してローカル/R2 両対応でファイルを保存します。
 * </p>
 */
@Service
@Transactional
public class FileRegisterServiceImpl implements FileRegisterService {

    private static final Logger logger = LoggerFactory.getLogger(FileRegisterServiceImpl.class);

    @Autowired
    protected FileManagementRepository fileManagementRepository;

    @Autowired
    protected StorageService storageService;

    protected static final String ATCH_FILE_NAME = "__file";

    @Override
    public Pair<String, String> register(MultipartFile multipartFile) {
        String temporaryUuid = UUID.randomUUID().toString();
        try {
            // 一時ファイルを作成してMultipartFileの内容を保存
            Path tempDir;
            try {
                tempDir = Files.createTempDirectory("ProMarker-" + temporaryUuid,
                        java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                                java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
            } catch (UnsupportedOperationException e) {
                tempDir = Files.createTempDirectory("ProMarker-" + temporaryUuid);
            }

            Path tempFile = tempDir.resolve(ATCH_FILE_NAME);
            multipartFile.transferTo(tempFile.toFile());

            return register(tempFile.toFile(), false, multipartFile.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary directory", e);
        }
    }

    @Override
    public Pair<String, String> register(File srcFile, boolean isZip) {
        return register(srcFile, isZip, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<String, String> register(File srcFile, boolean isZip, String fileName) {
        String uuid = UUID.randomUUID().toString();
        String relativePath = getRelativePath(uuid);
        String storagePath = relativePath + "/" + ATCH_FILE_NAME;

        try {
            byte[] fileData;

            if (isZip) {
                fileData = createZipArchive(srcFile);
                fileName = srcFile.getName() + ".zip";
            } else {
                fileData = readFileBytes(srcFile);
            }

            if (StringUtils.isEmpty(fileName)) {
                fileName = ATCH_FILE_NAME;
            }

            // 画像圧縮処理（2MB以上の画像）
            if (isImageFile(fileName) && fileData.length > 2 * 1024 * 1024) {
                byte[] compressed = compressImage(fileData);
                if (compressed != null && compressed.length < fileData.length) {
                    fileData = compressed;
                    logger.debug("Image compressed: {} -> {} bytes", srcFile.length(), fileData.length);
                }
            }

            // StorageService 経由で保存
            storageService.saveFile(storagePath, fileData);
            logger.debug("File saved to storage: {}", storagePath);

            // エンティティ作成
            FileManagement fileManagement = new FileManagement();
            fileManagement.fileId = uuid;
            fileManagement.fileName = fileName;
            fileManagement.filePath = storagePath;
            fileManagement.expireDate = DateUtils.addDays(new Date(), defaultExpireTerms());

            FileManagement saved = fileManagementRepository.save(fileManagement);
            if (saved == null) {
                throw new RuntimeException("Failed to save FileManagement entity");
            }

            return Pair.of(uuid, fileName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + srcFile.getName(), e);
        }
    }

    private byte[] readFileBytes(File file) throws IOException {
        if (file.isDirectory()) {
            // ディレクトリの場合は全ファイルを ZIP 化
            return createZipArchive(file);
        }
        return Files.readAllBytes(file.toPath());
    }

    private byte[] createZipArchive(File source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            if (source.isDirectory()) {
                addDirectoryToZip(zos, source, source.getName());
            } else {
                addFileToZip(zos, source, source.getName());
            }
        }
        return baos.toByteArray();
    }

    private void addDirectoryToZip(ZipOutputStream zos, File dir, String basePath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            logger.warn("Unable to list files in directory (may not be a directory or I/O error occurred): {}",
                    dir.getPath());
            return;
        }

        for (File file : files) {
            String entryPath = basePath + "/" + file.getName();
            if (file.isDirectory()) {
                addDirectoryToZip(zos, file, entryPath);
            } else {
                addFileToZip(zos, file, entryPath);
            }
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        zos.closeEntry();
    }

    /** 画像圧縮品質（0.0-1.0、1.0が最高品質） */
    private static final double IMAGE_COMPRESSION_QUALITY = 0.8;

    private byte[] compressImage(byte[] imageData) {
        Path tempInput = null;
        Path tempOutput = null;
        try {
            // 一時ファイルに書き出して Thumbnailator で圧縮
            tempInput = Files.createTempFile("img-", ".tmp");
            tempOutput = Files.createTempFile("img-compressed-", ".tmp");

            Files.write(tempInput, imageData);

            net.coobird.thumbnailator.Thumbnails.of(tempInput.toFile())
                    .scale(1.0)
                    .outputQuality(IMAGE_COMPRESSION_QUALITY)
                    .toFile(tempOutput.toFile());

            return Files.readAllBytes(tempOutput);
        } catch (Exception e) {
            logger.warn("Image compression failed, returning original data", e);
            // 圧縮失敗時は元データを返す（null ではなく）
            return imageData;
        } finally {
            // 一時ファイルを確実にクリーンアップ
            try {
                if (tempInput != null)
                    Files.deleteIfExists(tempInput);
            } catch (IOException ignored) {
            }
            try {
                if (tempOutput != null)
                    Files.deleteIfExists(tempOutput);
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null)
            return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".bmp");
    }

    protected String getRelativePath(String uuid) {
        Assert.notNull(uuid, "uuid must not be null.");

        Date date = new Date();
        String y = DateUtil.toString(date, "yy");
        String m = DateUtil.toString(date, "MM");

        return defaultAppDir() + "/" + y + "/" + m + "/" + uuid;
    }

    protected String defaultAppDir() {
        return "foundation/filemanagement";
    }

    protected int defaultExpireTerms() {
        return 3;
    }
}
