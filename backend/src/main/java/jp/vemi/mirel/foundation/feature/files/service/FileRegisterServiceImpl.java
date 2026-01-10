/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.foundation.feature.files.service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import jp.vemi.framework.util.DateUtil;
import jp.vemi.framework.util.FileUtil;
import jp.vemi.framework.util.StorageUtil;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;

/**
 * {@link FileRegisterService} の具象です。 .<br/>
 */
@Service
@Transactional
public class FileRegisterServiceImpl implements FileRegisterService {

    @Autowired
    protected FileManagementRepository fileManagementRepository;

    protected static final String ATCH_FILE_NAME = "__file";

    @Override
    public Pair<String, String> register(MultipartFile multipartFile) {
        String temporaryUuid = UUID.randomUUID().toString();
        // セキュリティ強化: 適切なパーミッション設定で一時ディレクトリを作成
        try {
            java.nio.file.Path tempDir;
            try {
                // POSIX対応環境では厳格なパーミッションを設定
                tempDir = java.nio.file.Files.createTempDirectory("ProMarker-" + temporaryUuid,
                        java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                                java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")));
            } catch (UnsupportedOperationException e) {
                // Windows等のPOSIX非対応環境ではパーミッション指定なしで作成
                tempDir = java.nio.file.Files.createTempDirectory("ProMarker-" + temporaryUuid);
            }
            File temporary = tempDir.toFile();
            FileUtil.transfer(multipartFile, temporary, ATCH_FILE_NAME);
            return register(temporary, false, multipartFile.getOriginalFilename());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to create temporary directory", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<String, String> register(File srcFile, boolean isZip) {
        return register(srcFile, isZip, null);
    }

    /**
     * {@inheritDoc}
     * 新しいトランザクションで実行して楽観ロック競合を回避
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<String, String> register(File srcFile, boolean isZip, String fileName) {

        String uuid = UUID.randomUUID().toString();

        String dest = getSaveDir(uuid);
        File destFile = new File(dest);

        if (destFile.exists()) {
            // error... files exists already...
        }

        if (isZip) {
            if (false == FileUtil.zip(srcFile, dest, ATCH_FILE_NAME)) {
                // error... failed archive file...
            }
            fileName = srcFile.getName() + ".zip";
        } else {
            FileUtil.copy(srcFile, destFile);
        }

        if (StringUtils.isEmpty(fileName)) {
            fileName = ATCH_FILE_NAME;
        }

        // create entity.
        FileManagement fileManagement = new FileManagement();
        fileManagement.fileId = uuid;
        fileManagement.fileName = fileName;
        Path destPath = Paths.get(dest).resolve(ATCH_FILE_NAME);
        fileManagement.filePath = destPath.toString();
        fileManagement.expireDate = DateUtils.addDays(new Date(), defaultExpireTerms());

        // @Versionフィールドはnullのままにして、JPA/Hibernateの自動管理に委ねる
        // 手動初期化はHibernateの新規/既存判別を混乱させる

        // 画像圧縮処理
        // ファイル名から拡張子を取得し、画像かどうかを簡易判定
        if (isImageFile(fileName) && destFile.length() > 2 * 1024 * 1024) { // 2MB以上の場合
            try {
                // 一時ファイルとして圧縮
                File compressed = new File(dest + ".compressed");
                net.coobird.thumbnailator.Thumbnails.of(destFile)
                        .scale(1.0) // サイズ変更なし（必要に応じて .size(1920, 1920) 等）
                        .outputQuality(0.8) // 品質 0.8
                        .toFile(compressed);

                // 圧縮後の方が小さければ採用
                if (compressed.length() < destFile.length()) {
                    if (destFile.delete()) {
                        compressed.renameTo(destFile);
                    }
                } else {
                    compressed.delete();
                }
            } catch (Exception e) {
                // 圧縮失敗時はログを出して元ファイルを維持
                // log.warn("Image compression failed", e);
                e.printStackTrace();
            }
        }

        FileManagement saved = fileManagementRepository.save(fileManagement);
        if (null == saved) {
            throw new RuntimeException("Failed to save FileManagement entity");
        }
        return Pair.of(uuid, fileName);
    }

    private boolean isImageFile(String fileName) {
        if (fileName == null)
            return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".bmp");
    }

    /**
     * save
     */
    protected String getSaveDir(String uuid) {

        // validate.
        Assert.notNull(uuid, "uuid must not be null.");

        // y&m
        Date date = new Date();
        String y = DateUtil.toString(date, "yy");
        String m = DateUtil.toString(date, "MM");

        // concatenate.
        Path basePath = Paths.get(StorageUtil.getBaseDir());
        Path fullPath = basePath.resolve(defaultAppDir()).resolve(y).resolve(m).resolve(uuid);
        return fullPath.toString();

    }

    // TODO: Config化
    protected String defaultAppDir() {
        return "foundation/filemanagement";
    }

    // TODO: Config化
    protected int defaultExpireTerms() {
        return 3;
    }
}
