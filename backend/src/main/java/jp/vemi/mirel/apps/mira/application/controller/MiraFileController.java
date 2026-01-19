/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jp.vemi.framework.util.SanitizeUtil;
import io.swagger.v3.oas.annotations.tags.Tag;

import jp.vemi.framework.storage.StorageService;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira添付ファイル取得コントローラ.
 */
@Slf4j
@RestController
@RequestMapping("/apps/mira/api/files")
@Tag(name = "Mira File", description = "Mira添付ファイルへのアクセス")
public class MiraFileController {

    @Autowired
    private FileManagementRepository fileManagementRepository;

    @Autowired
    private StorageService storageService;

    @Operation(summary = "添付ファイル取得", description = "ファイルIDを指定して添付ファイルを取得します。適切なContent-Typeを返します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "404", description = "ファイルが見つかりません"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileId) {

        FileManagement fileInfo;
        try {
            fileInfo = fileManagementRepository.findById(fileId).orElseThrow();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }

        // ストレージ相対パスを取得
        String storagePath = fileInfo.getFilePath();

        // ストレージ上の存在確認
        if (!storageService.exists(storagePath)) {
            log.warn("File not found in storage: fileId={}, path={}", SanitizeUtil.forLog(fileId),
                    SanitizeUtil.forLog(storagePath));
            return ResponseEntity.notFound().build();
        }

        try {
            InputStream inputStream = storageService.getInputStream(storagePath);
            Resource resource = new InputStreamResource(inputStream);

            // MIMEタイプ判定（ファイル名から）
            String contentType = determineContentType(fileInfo.getFileName());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.getFileName() + "\"")
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS)) // 長めのキャッシュ
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to read file from storage: fileId={}, path={}", SanitizeUtil.forLog(fileId),
                    SanitizeUtil.forLog(storagePath), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ファイル名からMIMEタイプを判定します。
     */
    private String determineContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".png"))
            return "image/png";
        else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg"))
            return "image/jpeg";
        else if (lowerName.endsWith(".gif"))
            return "image/gif";
        else if (lowerName.endsWith(".webp"))
            return "image/webp";
        else if (lowerName.endsWith(".svg"))
            return "image/svg+xml";
        else if (lowerName.endsWith(".pdf"))
            return "application/pdf";
        else if (lowerName.endsWith(".txt"))
            return "text/plain";
        else if (lowerName.endsWith(".json"))
            return "application/json";
        else if (lowerName.endsWith(".html"))
            return "text/html";
        else if (lowerName.endsWith(".css"))
            return "text/css";
        else if (lowerName.endsWith(".js"))
            return "application/javascript";
        else
            return "application/octet-stream";
    }
}
