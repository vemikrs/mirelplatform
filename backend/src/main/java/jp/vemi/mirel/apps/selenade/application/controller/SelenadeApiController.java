/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.application.controller;

import java.util.Map;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jp.vemi.mirel.apps.selenade.domain.api.RunTestApi;
import jp.vemi.mirel.apps.selenade.domain.api.SelenadeApi;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

@RestController
@RequestMapping("apps/arr/api")
@Tag(name = "Selenade Test Automation", description = "Webテスト自動化API (開発中)")
public class SelenadeApiController {

    @Autowired(required = false)
    private RunTestApi runTestApi;

    @Autowired(required = false)
    private Map<String, SelenadeApi> apis;

    // ===== 新エンドポイント (OpenAPI対応) =====

    @PostMapping("/runTest")
    @Operation(
        summary = "Webテスト実行",
        description = "Selenideを使用してWebテストを自動実行します。" +
                      "テスト定義に基づいてブラウザ操作を自動化し、結果を返却します。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "テスト実行完了",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "パラメータエラー"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "テスト実行エラー")
    })
    public ResponseEntity<ApiResponse<?>> runTest(
        @RequestBody Map<String, Object> request) {
        
        if (runTestApi == null) {
            return new ResponseEntity<>(ApiResponse.builder().errors(
                    Lists.newArrayList("RunTestApi is not available (under development)")).build(),
                    HttpStatus.OK);
        }
        
        return executeApi(runTestApi, request, "runTestApi");
    }

    // ===== 共通実行ロジック =====
    
    private ResponseEntity<ApiResponse<?>> executeApi(
        SelenadeApi api, Map<String, Object> request, String apiName) {
        
        System.out.println("Selenade API: " + apiName + ", Request: " + request);
        
        try {
            ApiResponse<?> body = api.service(request);
            System.out.println("Response: " + body);
            return new ResponseEntity<>(body, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(
                ApiResponse.builder()
                    .errors(Lists.newArrayList(e.getLocalizedMessage()))
                    .build(),
                HttpStatus.OK
            );
        }
    }

    // ===== 後方互換性維持 (削除予定) =====
    
    @Deprecated
    @Hidden
    @RequestMapping("/{path}")
    public ResponseEntity<ApiResponse<?>> index(@RequestBody Map<String, Object> request,
            @PathVariable String path) {

        String apiName = path + "Api";

        if (apis == null || false == apis.containsKey(apiName)) {
            return new ResponseEntity<>(ApiResponse.builder().errors(
                    Lists.newArrayList(apiName + " api not found.")).build(),
                    HttpStatus.OK);
        }

        SelenadeApi api = apis.get(apiName);
        return executeApi(api, request, apiName);
    }

}
