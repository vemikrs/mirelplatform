/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
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
import io.swagger.v3.oas.annotations.tags.Tag;

import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;

/**
 * Mira添付ファイル取得コントローラ.
 */
@RestController
@RequestMapping("/apps/mira/api/files")
@Tag(name = "Mira File", description = "Mira添付ファイルへのアクセス")
public class MiraFileController {

    @Autowired
    private FileManagementRepository fileManagementRepository;

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

        Path path = Paths.get(fileInfo.getFilePath());
        File file = path.toFile();

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        // MIMEタイプ判定
        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(path);
        } catch (IOException e) {
            // ignore
        }

        // 補正: probeContentTypeがnullを返す場合や不正確な場合のフォールバック（簡易）
        if (contentType == null) {
            String fileName = fileInfo.getFileName().toLowerCase();
            if (fileName.endsWith(".png"))
                contentType = "image/png";
            else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
                contentType = "image/jpeg";
            else if (fileName.endsWith(".gif"))
                contentType = "image/gif";
            else if (fileName.endsWith(".svg"))
                contentType = "image/svg+xml";
            else if (fileName.endsWith(".pdf"))
                contentType = "application/pdf";
            else if (fileName.endsWith(".txt"))
                contentType = "text/plain";
            else if (fileName.endsWith(".json"))
                contentType = "application/json";
            else if (fileName.endsWith(".html"))
                contentType = "text/html";
            else if (fileName.endsWith(".css"))
                contentType = "text/css";
            else if (fileName.endsWith(".js"))
                contentType = "application/javascript";
            else
                contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.getFileName() + "\"")
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS)) // 長めのキャッシュ
                .body(resource);
    }
}
