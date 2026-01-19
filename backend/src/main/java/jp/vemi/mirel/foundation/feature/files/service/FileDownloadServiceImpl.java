/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.feature.files.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import groovy.lang.Tuple3;
import jp.vemi.framework.storage.StorageService;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import jp.vemi.mirel.foundation.feature.files.dto.FileDownloadParameter;
import jp.vemi.mirel.foundation.feature.files.dto.FileDownloadResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link FileDownloadService} の具象です。 .<br/>
 */
@Slf4j
@Service
public class FileDownloadServiceImpl implements FileDownloadService {

    /** {@link FileManagementRepository} */
    @Autowired
    protected FileManagementRepository fileManagementRepository;

    @Autowired
    protected StorageService storageService;

    protected Date getToday() {
        return new Date();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<FileDownloadResult> invoke(ApiRequest<FileDownloadParameter> parameter) {

        ApiResponse<FileDownloadResult> resp = new ApiResponse<>();
        resp.setData(new FileDownloadResult());

        final Date today = getToday();
        final List<FileManagement> fmItems = Lists.newArrayList();

        parameter.getModel().getFileIds().forEach(fileId -> {

            FileManagement item;
            try {
                item = fileManagementRepository.findById(fileId).get();
            } catch (NoSuchElementException e) {
                log.warn("File not found: fileId={}", fileId, e);
                resp.addError("ファイルが見つかりません。ファイル管理ID：" + fileId);
                return;
            }

            // file record is null.
            if (null == item) {
                resp.addError("ファイルが見つかりません。ファイル管理ID：" + fileId);
                return;
            }

            // file expired.
            if (null != item.getExpireDate() && today.after(item.getExpireDate())) {
                resp.addError("ファイルは有効期間を過ぎました。ファイル名：" + item.fileName);
                return;
            }

            // file deleted.
            if (item.getDeleteFlag()) {
                resp.addError("ファイルは既に削除されています。ファイル名：" + item.fileName);
                return;
            }

            // ok.
            fmItems.add(item);
        });

        // file not found.
        if (fmItems.isEmpty()) {
            resp.addError("取得可能なファイルがありませんでした。");
            return resp;
        }

        // transfer to File model
        fmItems.forEach(item -> {

            String storagePath = item.getFilePath();

            // StorageService経由でファイル存在確認
            if (!storageService.exists(storagePath)) {
                log.warn("File not found in storage: path={}", storagePath);
                resp.addError("ファイルがストレージから取得できませんでした。ファイル名：" + item.fileName);
                return;
            }

            // ok. パスは論理パスとして保持（実際のダウンロードはDownloadControllerでStorageService経由）
            Path path = Paths.get(storagePath);
            resp.getData().paths.add(new Tuple3<String, String, Path>(item.getFileId(), item.getFileName(), path));
        });

        return resp;
    }

}
