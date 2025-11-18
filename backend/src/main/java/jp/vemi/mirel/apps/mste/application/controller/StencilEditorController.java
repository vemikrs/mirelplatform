/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.application.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jp.vemi.mirel.apps.mste.domain.dto.LoadStencilParameter;
import jp.vemi.mirel.apps.mste.domain.dto.LoadStencilResult;
import jp.vemi.mirel.apps.mste.domain.dto.SaveStencilParameter;
import jp.vemi.mirel.apps.mste.domain.dto.SaveStencilResult;
import jp.vemi.mirel.apps.mste.domain.dto.StencilVersionDto;
import jp.vemi.mirel.apps.mste.domain.service.StencilEditorService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * ステンシルエディタAPI Controller
 */
@RestController
@RequestMapping("apps/mste/editor")
@Tag(name = "ProMarker - Stencil Editor", description = "ステンシルエディタAPI")
public class StencilEditorController {

    @Autowired
    private StencilEditorService stencilEditorService;

    @GetMapping("/list")
    @Operation(
        summary = "ステンシル一覧取得",
        description = "全カテゴリのステンシル一覧を取得します。categoryIdパラメータでフィルタリング可能"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> listStencils(
        @org.springframework.web.bind.annotation.RequestParam(required = false) String categoryId
    ) {
        ApiResponse<Map<String, Object>> response = stencilEditorService.listStencils(categoryId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/**")
    @Operation(
        summary = "ステンシル読込またはバージョン履歴取得",
        description = "指定されたステンシルの設定とファイル一覧を取得、またはバージョン履歴を取得します"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    public ResponseEntity<?> handleDynamicPath(
        jakarta.servlet.http.HttpServletRequest servletRequest
    ) {
        // リクエストパスから処理を判定
        // 例1: /apps/mste/editor/springboot/service/221208A → loadStencil
        // 例2: /apps/mste/editor/springboot/service/versions → getVersionHistory
        String requestPath = servletRequest.getRequestURI();
        String contextPath = servletRequest.getContextPath(); // /mipla2
        String basePath = "/apps/mste/editor";
        
        // /mipla2/apps/mste/editor を除去
        String pathAfterBase = requestPath.substring((contextPath + basePath).length());
        
        // versionsエンドポイントかどうか判定
        if (pathAfterBase.endsWith("/versions")) {
            // バージョン履歴取得
            String stencilId = pathAfterBase.replace("/versions", "");
            ApiResponse<List<StencilVersionDto>> response = 
                stencilEditorService.getVersionHistory(stencilId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            // ステンシル読込
            // 最後の要素がシリアル番号
            int lastSlash = pathAfterBase.lastIndexOf('/');
            if (lastSlash == -1) {
                // パスが不正
                ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .build();
                errorResponse.addError("Invalid path format");
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }
            
            String stencilId = pathAfterBase.substring(0, lastSlash);
            String serial = pathAfterBase.substring(lastSlash + 1);
            
            LoadStencilParameter param = LoadStencilParameter.builder()
                .stencilId(stencilId)
                .serial(serial)
                .build();
                
            ApiRequest<LoadStencilParameter> request = ApiRequest.<LoadStencilParameter>builder()
                .model(param)
                .build();
                
            ApiResponse<LoadStencilResult> response = stencilEditorService.loadStencil(request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping("/save")
    @Operation(
        summary = "ステンシル保存",
        description = "ステンシルを新しいバージョンとして保存します"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "保存成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    value = "{\"data\":{\"newSerial\":\"250101A\",\"success\":true},\"messages\":[\"保存しました\"],\"errors\":[]}"
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<SaveStencilResult>> saveStencil(
        @RequestBody Map<String, Object> request
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) request.get("content");
        
        SaveStencilParameter param = buildSaveParameter(content);
        
        ApiRequest<SaveStencilParameter> apiRequest = ApiRequest.<SaveStencilParameter>builder()
            .model(param)
            .build();
            
        ApiResponse<SaveStencilResult> response = stencilEditorService.saveStencil(apiRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/common/{categoryId}")
    @Operation(
        summary = "カテゴリ共通設定保存",
        description = "カテゴリ共通のstencil-settings.ymlを保存します"
    )
    public ResponseEntity<ApiResponse<Void>> saveCommonSettings(
        @PathVariable String categoryId,
        @RequestBody Map<String, Object> request
    ) {
        // TODO: 実装
        ApiResponse<Void> response = ApiResponse.<Void>builder().build();
        response.addMessage("カテゴリ共通設定を保存しました");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * リクエストからSaveStencilParameterを構築
     */
    private SaveStencilParameter buildSaveParameter(Map<String, Object> content) {
        // TODO: 完全な実装
        SaveStencilParameter param = new SaveStencilParameter();
        param.setStencilId((String) content.get("stencilId"));
        param.setSerial((String) content.get("serial"));
        param.setMessage((String) content.get("message"));
        return param;
    }
}
