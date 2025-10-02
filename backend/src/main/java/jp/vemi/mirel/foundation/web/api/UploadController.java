/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jp.vemi.mirel.foundation.feature.files.dto.FileUploadResult;
import jp.vemi.mirel.foundation.feature.files.service.FileRegisterService;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * ファイルアップロードコントローラ.<br/>
 */
@RestController
@Tag(name = "File Management", description = "ファイルのアップロード・ダウンロード管理API")
public class UploadController {

    /** {@link FileRegisterService サービス} */
    @Autowired
    protected FileRegisterService service;

    /**
     * ファイルアップロード. <br/>
     * 
     * @param multipartFile アップロードファイル
     * @return アップロード結果 (UUID、ファイル名)
     */
    @Operation(
        summary = "ファイルアップロード",
        description = "マルチパート形式でファイルをアップロードします。" +
                      "アップロード成功時にファイルIDとファイル名を返却します。" +
                      "このファイルIDは他のAPIで使用できます。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "アップロード成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "ファイルが空です"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "アップロード処理エラー")
    })
    @RequestMapping(path = "commons/upload")
    public ResponseEntity<ApiResponse<FileUploadResult>> index(
            @Parameter(
                description = "アップロードするファイル",
                required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            // return
        }

        Pair<String, String> ret = service.register(multipartFile);
        FileUploadResult fileUploadResult = new FileUploadResult();
        fileUploadResult.uuid = ret.getKey();
        fileUploadResult.fileName = ret.getValue();
        ResponseEntity<ApiResponse<FileUploadResult>> rentity = new ResponseEntity<>(
                new ApiResponse<>(fileUploadResult), HttpStatus.OK);
        return rentity;
    }

}
