/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.application.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Hidden;
import jp.vemi.mirel.apps.mste.domain.api.GenerateApi;
import jp.vemi.mirel.apps.mste.domain.api.MsteApi;
import jp.vemi.mirel.apps.mste.domain.api.ReloadStencilMasterApi;
import jp.vemi.mirel.apps.mste.domain.api.SuggestApi;
import jp.vemi.mirel.apps.mste.domain.api.UploadStencilApi;
import jp.vemi.mirel.apps.mste.domain.service.GenerateService;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

@RestController
@RequestMapping("apps/mste/api")
@Tag(name = "ProMarker (MSTE)", description = "Master Stencil Template Engine - テンプレート管理・コード生成API")
public class ApiController {

    /** {@link GenerateService} */
    @Autowired
    protected GenerateService generateService;

    @Autowired
    private SuggestApi suggestApi;
    
    @Autowired
    private GenerateApi generateApi;
    
    @Autowired
    private ReloadStencilMasterApi reloadStencilMasterApi;
    
    @Autowired
    private UploadStencilApi uploadStencilApi;

    @Autowired()
    private Map<String, MsteApi> apis;

    // ===== 新エンドポイント (OpenAPI対応) =====

    @PostMapping("/suggest")
    @Operation(
        summary = "ステンシル候補取得",
        description = "カテゴリ・ステンシル・シリアル番号のドロップダウン候補を取得します。" +
                      "「*」を指定すると全件取得、具体的な値を指定するとフィルタリングされます。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "成功例",
                    value = "{\"data\":{\"model\":{\"fltStrStencilCategory\":{\"items\":[],\"selected\":\"\"}}},\"messages\":[],\"errors\":[]}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "リクエストエラー")
    })
    public ResponseEntity<ApiResponse<?>> suggest(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "検索条件",
            required = true,
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    value = "{\"content\":{\"stencilCategoy\":\"*\",\"stencilCanonicalName\":\"*\",\"serialNo\":\"*\"}}"
                )
            )
        )
        @RequestBody Map<String, Object> request) {
        
        return executeApi(suggestApi, request, "suggestApi");
    }

    @PostMapping("/generate")
    @Operation(
        summary = "コード生成",
        description = "選択したステンシルテンプレートからソースコードを生成します。" +
                      "パラメータはステンシル定義に基づいて動的に変化します。" +
                      "\n\n**レスポンス構造**: 生成されたファイル群はZIPアーカイブされ、そのZIPファイルのIDと論理名が返されます。" +
                      "\n- `files`: Pair<String, String>のリストで、JSONではオブジェクト配列として表現されます" +
                      "\n- オブジェクトのキー: ZIPファイルのファイル管理ID (UUID形式)" +
                      "\n- オブジェクトの値: ZIPファイルの論理名（例: `hello-world-250913A.zip`）" +
                      "\n- ZIP内には生成された複数のファイル（.java, .xml等）がアーカイブされています" +
                      "\n\n**関連API**:" +
                      "\n- `/commons/upload`: ファイル型パラメータの事前アップロード" +
                      "\n- `/commons/dlsite/{fileId}` または `/commons/download`: ZIPファイルのダウンロード"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "成功 - ZIPファイルのIDと論理名を返却",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "成功例",
                    value = "{\n  \"data\": {\n    \"files\": [{\n      \"abc123-def456-...\": \"hello-world-250913A.zip\"\n    }]\n  },\n  \"messages\": [],\n  \"errors\": []\n}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "パラメータエラー"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "生成処理エラー")
    })
    public ResponseEntity<ApiResponse<?>> generate(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "生成に必要なパラメータ（content直下に指定）",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Map.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Request例",
                    value = "{\n  \"content\": {\n    \"stencilCategoy\": \"/samples\",\n    \"stencilCanonicalName\": \"/samples/hello-world\",\n    \"serialNo\": \"250913A\",\n    \"message\": \"Hello\"\n  }\n}"
                )
            )
        )
        @RequestBody Map<String, Object> request) {
        
        return executeApi(generateApi, request, "generateApi");
    }

    @PostMapping("/reloadStencilMaster")
    @Operation(
        summary = "ステンシルマスタ再読込",
        description = "クラスパスとデータベースからステンシル定義を再読み込みします。" +
                      " 新しいステンシルを追加した後に実行してください。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "再読込成功",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "成功例",
                    value = "{\n  \"data\": null,\n  \"messages\": [\"Reloaded successfully\"],\n  \"errors\": []\n}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "読込エラー")
    })
    public ResponseEntity<ApiResponse<?>> reloadStencilMaster(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "再読込要求（contentは空でOK）",
            required = false,
            content = @io.swagger.v3.oas.annotations.media.Content(
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Map.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Request例",
                    value = "{\n  \"content\": {}\n}"
                )
            )
        )
        @RequestBody Map<String, Object> request) {
        
        return executeApi(reloadStencilMasterApi, request, "reloadStencilMasterApi");
    }

    @PostMapping("/uploadStencil")
    @Hidden
    @Operation(
        summary = "カスタムステンシルアップロード",
        description = "ユーザー定義のステンシルテンプレートを登録します。" +
                      " フロー: (1) '/commons/upload' にZIP等をアップロード→fileId取得 (2) 本APIにfileId等を渡して取り込み。" +
                      " 現状は暫定仕様で、次PRで確定予定です。"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "登録成功",
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "成功例",
                    value = "{\n  \"data\": {},\n  \"messages\": [\"Registered\"],\n  \"errors\": []\n}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "ファイル形式エラー")
    })
    public ResponseEntity<ApiResponse<?>> uploadStencil(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "暫定スキーマ: content.zipFileId（必須）, content.categoryId, content.overwrite",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = Map.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Request例",
                    value = "{\n  \"content\": {\n    \"zipFileId\": \"abc123\",\n    \"categoryId\": \"/user\",\n    \"overwrite\": true\n  }\n}"
                )
            )
        )
        @RequestBody Map<String, Object> request) {
        
        return executeApi(uploadStencilApi, request, "uploadStencilApi");
    }

    // ===== 共通実行ロジック =====
    
    private ResponseEntity<ApiResponse<?>> executeApi(
        MsteApi api, Map<String, Object> request, String apiName) {
        
        System.out.println("API: " + apiName + ", Request: " + request);
        
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

        if (false == apis.containsKey(apiName)) {
            return new ResponseEntity<>(ApiResponse.builder().errors(
                    Lists.newArrayList(apiName + " api not found.")).build(),
                    HttpStatus.OK);
        }

        MsteApi api = apis.get(apiName);
        return executeApi(api, request, apiName);
    }

    // ===== テスト/開発用エンドポイント =====
    
    @Hidden  // Swagger UIに表示しない
    @RequestMapping("/gen")
    public Map<String, Object> gen() {

        Object sresult = generateService.invoke(null);
        System.out.println(sresult);

        Map<String, Object> map = newHashMap();

        Map<String, Object> child = newHashMap();
        child.put("key", "child1");

        List<Map<String, Object>> params = new ArrayList<>();

        {
            Map<String, Object> param = newHashMap();
            param.put("key", "param1");
            params.add(param);
        }

        {
            Map<String, Object> param = newHashMap();
            param.put("key", "param2");
            params.add(param);
        }

        map.put("child", child);
        map.put("params", params);

        return map;
    }

    protected static Map<String, Object> newHashMap() {
        return new LinkedHashMap<>();
    }

}
