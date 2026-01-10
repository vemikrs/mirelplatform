/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.application.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PutMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ContextSnapshotRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ErrorReportRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.GenerateTitleRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.UpdateTitleRequest;
import jp.vemi.mirel.apps.mira.domain.dto.response.ChatResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ContextSnapshotResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ExportDataResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.GenerateTitleResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.UpdateTitleResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.UserContextResponse;
import jp.vemi.mirel.apps.mira.domain.service.MiraAuditService;
import jp.vemi.mirel.apps.mira.domain.service.MiraChatService;
import jp.vemi.mirel.apps.mira.domain.service.MiraContextLayerService;
import jp.vemi.mirel.apps.mira.domain.service.MiraExportService;
import jp.vemi.mirel.apps.mira.domain.service.MiraRbacAdapter;
import jp.vemi.mirel.apps.mira.domain.service.MiraTenantContextManager;
import jp.vemi.mirel.apps.mira.domain.service.MiraPresetSuggestionService;
import jp.vemi.mirel.apps.mira.domain.dto.request.SuggestConfigRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.MessageConfig;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jp.vemi.framework.util.SanitizeUtil;

/**
 * Mira AI アシスタント API コントローラー.
 */
@Slf4j
@RestController
@RequestMapping("apps/mira/api")
@RequiredArgsConstructor
@Tag(name = "Mira AI Assistant", description = "AI アシスタント機能を提供するAPI")
public class MiraApiController {

    private final MiraChatService chatService;
    private final MiraContextLayerService contextLayerService;
    private final MiraRbacAdapter rbacAdapter;
    private final MiraAuditService auditService;
    private final MiraTenantContextManager tenantContextManager;
    private final MiraExportService exportService;
    private final MiraPresetSuggestionService presetSuggestionService;
    private final jp.vemi.mirel.apps.mira.domain.service.ModelSelectionService modelSelectionService;
    private final jp.vemi.mirel.apps.mira.domain.service.MiraSettingService settingService;

    // ========================================
    // Chat Endpoints
    // ========================================

    @PostMapping("/chat")
    @Operation(summary = "チャットメッセージ送信", description = "AI アシスタントにメッセージを送信し、応答を取得します。" +
            "会話IDを指定することで、会話を継続できます。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraChatApiResponse.class), examples = @ExampleObject(name = "成功例", value = """
                    {
                      "data": {
                        "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                        "messageId": "550e8400-e29b-41d4-a716-446655440001",
                        "mode": "GENERAL_CHAT",
                        "assistantMessage": {
                          "content": "こんにちは！何かお手伝いできることはありますか？",
                          "contentType": "markdown"
                        },
                        "metadata": {
                          "provider": "azure-openai",
                          "model": "gpt-4o",
                          "latencyMs": 1234
                        }
                      },
                      "errors": []
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "リクエストエラー"),
            @ApiResponse(responseCode = "401", description = "認証エラー"),
            @ApiResponse(responseCode = "403", description = "権限エラー"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraChatApiResponse> chat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "チャットリクエスト", required = true, content = @Content(schema = @Schema(implementation = ApiRequest.class), examples = @ExampleObject(value = """
                    {
                      "model": {
                        "conversationId": null,
                        "mode": "GENERAL_CHAT",
                        "context": {
                          "appId": "promarker",
                          "screenId": "stencil-editor"
                        },
                        "message": {
                          "content": "こんにちは、ステンシルの作成方法を教えてください"
                        }
                      }
                    }
                    """))) @RequestBody ApiRequest<ChatRequest> request) {

        try {
            // コンテキスト取得
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();
            String systemRole = tenantContextManager.getCurrentSystemRole();

            // RBAC チェック
            if (!rbacAdapter.canUseMira(systemRole, tenantId)) {
                log.warn("Mira アクセス拒否: tenantId={}, userId={}, role={}",
                        tenantId, userId, systemRole);
                return ResponseEntity.status(403)
                        .body(MiraChatApiResponse.error("Mira の利用権限がありません"));
            }

            // チャット処理
            ChatRequest chatRequest = request.getModel();
            ChatResponse response = chatService.chat(chatRequest, tenantId, userId);

            return ResponseEntity.ok(MiraChatApiResponse.success(response));

        } catch (Exception e) {
            log.error("チャット処理エラー", e);
            auditService.logApiError(
                    tenantContextManager.getCurrentTenantId(),
                    tenantContextManager.getCurrentUserId(),
                    "chat",
                    e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MiraChatApiResponse.error("チャット処理中にエラーが発生しました"));
        }
    }

    @PostMapping("/suggest-config")
    @Operation(summary = "AI推奨設定を取得")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraSuggestConfigApiResponse.class)))
    })
    public ResponseEntity<MiraSuggestConfigApiResponse> suggestConfig(
            @RequestBody ApiRequest<SuggestConfigRequest> request) {
        String messageContent = request.getModel().getMessageContent();
        MessageConfig suggestion = presetSuggestionService.suggestConfig(messageContent);

        return ResponseEntity.ok(MiraSuggestConfigApiResponse.success(suggestion));
    }

    // ========================================
    // Context Snapshot Endpoints
    // ========================================

    @PostMapping("/context-snapshot")
    @Operation(summary = "コンテキストスナップショット保存", description = "現在の画面コンテキスト情報を保存します。" +
            "コンテキストヘルプ機能で使用されます。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraSnapshotApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "リクエストエラー")
    })
    public ResponseEntity<MiraSnapshotApiResponse> saveContextSnapshot(
            @RequestBody ApiRequest<ContextSnapshotRequest> request) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();

            ContextSnapshotRequest snapshotRequest = request.getModel();
            ContextSnapshotResponse response = chatService.saveContextSnapshot(
                    snapshotRequest, tenantId, userId);

            return ResponseEntity.ok(MiraSnapshotApiResponse.success(response));

        } catch (Exception e) {
            log.error("スナップショット保存エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraSnapshotApiResponse.error("スナップショット保存中にエラーが発生しました"));
        }
    }

    // ========================================
    // Error Report Endpoints
    // ========================================

    @PostMapping("/error-report")
    @Operation(summary = "エラー分析", description = "発生したエラーを AI に分析させ、解決策を提案します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraChatApiResponse.class), examples = @ExampleObject(name = "成功例", value = """
                    {
                      "data": {
                        "conversationId": "...",
                        "mode": "ERROR_ANALYZE",
                        "assistantMessage": {
                          "content": "## エラー原因\\n..."
                        }
                      }
                    }
                    """)))
    })
    public ResponseEntity<MiraChatApiResponse> analyzeError(
            @RequestBody ApiRequest<ErrorReportRequest> request) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();

            ErrorReportRequest errorRequest = request.getModel();
            ChatResponse response = chatService.analyzeError(errorRequest, tenantId, userId);

            return ResponseEntity.ok(MiraChatApiResponse.success(response));

        } catch (Exception e) {
            log.error("エラー分析処理エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraChatApiResponse.error("エラー分析中に問題が発生しました"));
        }
    }

    // ========================================
    // Conversation Management Endpoints
    // ========================================

    @DeleteMapping("/conversation/{conversationId}")
    @Operation(summary = "会話クリア", description = "指定した会話をクリア（終了）します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "404", description = "会話が見つからない")
    })
    public ResponseEntity<Map<String, Object>> clearConversation(
            @Parameter(description = "会話ID", required = true) @PathVariable String conversationId) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();

            chatService.clearConversation(conversationId, tenantId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "会話をクリアしました"));

        } catch (Exception e) {
            log.error("会話クリアエラー: conversationId={}", SanitizeUtil.forLog(conversationId), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "会話のクリアに失敗しました"));
        }
    }

    @GetMapping("/conversation/{conversationId}/status")
    @Operation(summary = "会話ステータス取得", description = "指定した会話のステータスを取得します。")
    public ResponseEntity<Map<String, Object>> getConversationStatus(
            @Parameter(description = "会話ID", required = true) @PathVariable String conversationId) {

        String tenantId = tenantContextManager.getCurrentTenantId();
        String userId = tenantContextManager.getCurrentUserId();

        boolean active = chatService.isConversationActive(conversationId, tenantId, userId);

        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "active", active));
    }

    @GetMapping("/conversations")
    @Operation(summary = "会話一覧取得", description = "会話履歴のページネーション一覧を取得します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = jp.vemi.mirel.apps.mira.domain.dto.response.ConversationListResponse.class))),
            @ApiResponse(responseCode = "403", description = "権限エラー"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraConversationListApiResponse> listConversations(
            @Parameter(description = "ページ番号 (0-indexed)") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @Parameter(description = "1ページあたりの件数") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();
            String systemRole = tenantContextManager.getCurrentSystemRole();

            // RBAC チェック
            if (!rbacAdapter.canUseMira(systemRole, tenantId)) {
                return ResponseEntity.status(403)
                        .body(MiraConversationListApiResponse.error("Mira の利用権限がありません"));
            }

            var response = chatService.listConversations(
                    tenantId, userId,
                    org.springframework.data.domain.PageRequest.of(page, size));

            return ResponseEntity.ok(MiraConversationListApiResponse.success(response));

        } catch (Exception e) {
            log.error("会話一覧取得エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraConversationListApiResponse.error("会話一覧の取得中にエラーが発生しました"));
        }
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "会話詳細取得", description = "会話の詳細（メッセージ履歴を含む）を取得します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = jp.vemi.mirel.apps.mira.domain.dto.response.ConversationDetailResponse.class))),
            @ApiResponse(responseCode = "403", description = "権限エラー"),
            @ApiResponse(responseCode = "404", description = "会話が見つからない"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraConversationDetailApiResponse> getConversation(
            @Parameter(description = "会話ID") @PathVariable String conversationId) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();
            String systemRole = tenantContextManager.getCurrentSystemRole();

            // RBAC チェック
            if (!rbacAdapter.canUseMira(systemRole, tenantId)) {
                return ResponseEntity.status(403)
                        .body(MiraConversationDetailApiResponse.error("Mira の利用権限がありません"));
            }

            var response = chatService.getConversation(conversationId, tenantId, userId);
            return ResponseEntity.ok(MiraConversationDetailApiResponse.success(response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(MiraConversationDetailApiResponse.error("会話が見つかりません"));
        } catch (Exception e) {
            log.error("会話詳細取得エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraConversationDetailApiResponse.error("会話詳細の取得中にエラーが発生しました"));
        }
    }

    // ========================================
    // Title Generation Endpoints
    // ========================================

    @PostMapping("/conversation/generate-title")
    @Operation(summary = "会話タイトル生成", description = "会話内容を AI が分析し、適切なタイトルを生成します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraTitleApiResponse.class), examples = @ExampleObject(name = "成功例", value = """
                    {
                      "data": {
                        "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                        "title": "ステンシル作成の質問",
                        "success": true
                      },
                      "errors": []
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "リクエストエラー"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraTitleApiResponse> generateTitle(
            @RequestBody ApiRequest<GenerateTitleRequest> request) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();

            GenerateTitleRequest titleRequest = request.getModel();
            GenerateTitleResponse response = chatService.generateTitle(titleRequest, tenantId, userId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(MiraTitleApiResponse.success(response));
            } else {
                return ResponseEntity.ok(MiraTitleApiResponse.error(response.getErrorMessage()));
            }

        } catch (Exception e) {
            log.error("タイトル生成処理エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraTitleApiResponse.error("タイトル生成中にエラーが発生しました"));
        }
    }

    @PostMapping("/conversation/{conversationId}/regenerate-title")
    @Operation(summary = "会話タイトル再生成", description = "会話の履歴に基づいてタイトルを再生成します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraTitleApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "権限エラー"),
            @ApiResponse(responseCode = "404", description = "会話が見つからない"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraTitleApiResponse> regenerateTitle(
            @Parameter(description = "会話ID") @PathVariable String conversationId) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();
            String systemRole = tenantContextManager.getCurrentSystemRole();

            // RBAC チェック
            if (!rbacAdapter.canUseMira(systemRole, tenantId)) {
                return ResponseEntity.status(403)
                        .body(MiraTitleApiResponse.error("Mira の利用権限がありません"));
            }

            GenerateTitleResponse response = chatService.regenerateTitle(conversationId, tenantId, userId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(MiraTitleApiResponse.success(response));
            } else {
                return ResponseEntity.ok(MiraTitleApiResponse.error(response.getErrorMessage()));
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(MiraTitleApiResponse.error("会話が見つかりません"));
        } catch (Exception e) {
            log.error("タイトル再生成処理エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraTitleApiResponse.error("タイトル再生成中にエラーが発生しました"));
        }
    }

    @PutMapping("/conversation/update-title")
    @Operation(summary = "会話タイトル更新", description = "会話のタイトルを手動で更新します。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraUpdateTitleApiResponse.class), examples = @ExampleObject(name = "成功例", value = """
                    {
                      "data": {
                        "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                        "title": "新しいタイトル",
                        "success": true
                      },
                      "errors": []
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "リクエストエラー"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraUpdateTitleApiResponse> updateTitle(
            @RequestBody ApiRequest<UpdateTitleRequest> request) {

        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();

            UpdateTitleRequest titleRequest = request.getModel();
            UpdateTitleResponse response = chatService.updateTitle(titleRequest, tenantId, userId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(MiraUpdateTitleApiResponse.success(response));
            } else {
                return ResponseEntity.ok(MiraUpdateTitleApiResponse.error(response.getErrorMessage()));
            }

        } catch (Exception e) {
            log.error("タイトル更新処理エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraUpdateTitleApiResponse.error("タイトル更新中にエラーが発生しました"));
        }
    }

    // ========================================
    // User Context Endpoints
    // ========================================

    @GetMapping("/user-context")
    @Operation(summary = "ユーザーコンテキスト取得", description = "現在のユーザーのAIコンテキスト設定を取得します。")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json"))
    public ResponseEntity<MiraUserContextApiResponse> getUserContext() {
        try {
            String userId = tenantContextManager.getCurrentUserId();
            String tenantId = tenantContextManager.getCurrentTenantId();
            // organizationId は現時点ではnull（将来的に追加）
            String organizationId = null;

            log.debug("[MiraApi] getUserContext: userId={}", userId);

            // ユーザースコープのコンテキストを取得
            var contexts = contextLayerService.buildMergedContext(tenantId, organizationId, userId);

            UserContextResponse response = new UserContextResponse(
                    contexts.getOrDefault("terminology", ""),
                    contexts.getOrDefault("style", ""),
                    contexts.getOrDefault("workflow", ""),
                    contexts.getOrDefault("tavilyApiKey", ""));

            return ResponseEntity.ok(MiraUserContextApiResponse.success(response));

        } catch (Exception e) {
            log.error("ユーザーコンテキスト取得エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraUserContextApiResponse.error("コンテキストの取得中にエラーが発生しました"));
        }
    }

    @PutMapping("/user-context")
    @Operation(summary = "ユーザーコンテキスト更新", description = "現在のユーザーのAIコンテキスト設定を更新します。")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json"))
    public ResponseEntity<MiraUserContextApiResponse> updateUserContext(
            @RequestBody UserContextRequest request) {
        try {
            String userId = tenantContextManager.getCurrentUserId();

            log.info("[MiraApi] updateUserContext: userId={}", userId);

            // 各カテゴリを保存（upsert）
            contextLayerService.saveOrUpdateUserContext(userId, "terminology", request.terminology());
            contextLayerService.saveOrUpdateUserContext(userId, "style", request.style());
            contextLayerService.saveOrUpdateUserContext(userId, "workflow", request.workflow());
            contextLayerService.saveOrUpdateUserContext(userId, "tavilyApiKey", request.tavilyApiKey()); // Save Tavily
                                                                                                         // Key

            // 監査ログ
            auditService.logContextUpdate(userId, "USER_CONTEXT_UPDATED");

            return ResponseEntity.ok(MiraUserContextApiResponse.success(
                    new UserContextResponse(
                            request.terminology(),
                            request.style(),
                            request.workflow(),
                            request.tavilyApiKey())));

        } catch (Exception e) {
            log.error("ユーザーコンテキスト更新エラー", e);
            return ResponseEntity.internalServerError()
                    .body(MiraUserContextApiResponse.error("コンテキストの更新中にエラーが発生しました"));
        }
    }

    // ========================================
    // Health Check
    // ========================================

    @GetMapping("/health")
    @Operation(summary = "ヘルスチェック", description = "Mira AI サービスの稼働状態を確認します。")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "mira-ai-assistant",
                "timestamp", java.time.Instant.now().toString()));
    }

    // ========================================
    // Export Endpoints
    // ========================================

    @GetMapping("/export")
    @Operation(summary = "ユーザーデータエクスポート", description = "現在のユーザーの会話履歴とユーザーコンテキストをすべてエクスポートします。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MiraExportApiResponse.class), examples = @ExampleObject(name = "成功例", value = """
                    {
                      "data": {
                        "metadata": {
                          "exportedAt": "2025-12-07T10:00:00Z",
                          "userId": "user123",
                          "tenantId": "tenant1",
                          "conversationCount": 5,
                          "totalMessageCount": 42,
                          "version": "1.0"
                        },
                        "conversations": [...],
                        "userContext": {
                          "terminology": "...",
                          "style": "...",
                          "workflow": "..."
                        }
                      },
                      "errors": []
                    }
                    """))),
            @ApiResponse(responseCode = "401", description = "認証エラー"),
            @ApiResponse(responseCode = "403", description = "権限エラー"),
            @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraExportApiResponse> exportUserData() {
        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();
            String systemRole = tenantContextManager.getCurrentSystemRole();

            // RBAC チェック
            if (!rbacAdapter.canUseMira(systemRole, tenantId)) {
                log.warn("Mira エクスポートアクセス拒否: tenantId={}, userId={}, role={}",
                        tenantId, userId, systemRole);
                return ResponseEntity.status(403)
                        .body(MiraExportApiResponse.error("Mira の利用権限がありません"));
            }

            log.info("[MiraApi] Starting export for user: userId={}", userId);

            // エクスポート実行
            ExportDataResponse exportData = exportService.exportUserData(tenantId, userId);

            log.info("[MiraApi] Export completed: userId={}, conversations={}, messages={}",
                    userId, exportData.metadata().conversationCount(), exportData.metadata().totalMessageCount());

            return ResponseEntity.ok(MiraExportApiResponse.success(exportData));

        } catch (Exception e) {
            log.error("エクスポート処理エラー", e);
            auditService.logApiError(
                    tenantContextManager.getCurrentTenantId(),
                    tenantContextManager.getCurrentUserId(),
                    "export",
                    e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(MiraExportApiResponse.error("エクスポート処理中にエラーが発生しました"));
        }
    }

    // ========================================
    // Model Selection (Phase 3)
    // ========================================

    /**
     * 利用可能なモデル一覧取得（ユーザー向け）.
     */
    @GetMapping("/available-models")
    @Operation(summary = "利用可能なモデル一覧取得", description = "現在のプロバイダで利用可能なモデル一覧を取得します")
    @ApiResponse(responseCode = "200", description = "成功")
    public ResponseEntity<java.util.List<jp.vemi.mirel.apps.mira.domain.dao.entity.MiraModelRegistry>> getAvailableModels() {
        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String provider = settingService.getAiProvider(tenantId);

            return ResponseEntity.ok(modelSelectionService.getAvailableModels(provider));
        } catch (Exception e) {
            log.error("利用可能モデル取得エラー", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================================
    // Response Wrapper Classes
    // ========================================

    /**
     * チャット API レスポンス.
     */
    @Schema(description = "Mira チャット API レスポンス")
    public record MiraChatApiResponse(
            @Schema(description = "レスポンスデータ") ChatResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraChatApiResponse success(ChatResponse data) {
            return new MiraChatApiResponse(data, java.util.List.of());
        }

        public static MiraChatApiResponse error(String message) {
            return new MiraChatApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * スナップショット API レスポンス.
     */
    @Schema(description = "Mira スナップショット API レスポンス")
    public record MiraSnapshotApiResponse(
            @Schema(description = "レスポンスデータ") ContextSnapshotResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraSnapshotApiResponse success(ContextSnapshotResponse data) {
            return new MiraSnapshotApiResponse(data, java.util.List.of());
        }

        public static MiraSnapshotApiResponse error(String message) {
            return new MiraSnapshotApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * タイトル生成 API レスポンス.
     */
    @Schema(description = "Mira タイトル生成 API レスポンス")
    public record MiraTitleApiResponse(
            @Schema(description = "レスポンスデータ") GenerateTitleResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraTitleApiResponse success(GenerateTitleResponse data) {
            return new MiraTitleApiResponse(data, java.util.List.of());
        }

        public static MiraTitleApiResponse error(String message) {
            return new MiraTitleApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * タイトル更新 API レスポンス.
     */
    @Schema(description = "Mira タイトル更新 API レスポンス")
    public record MiraUpdateTitleApiResponse(
            @Schema(description = "レスポンスデータ") UpdateTitleResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraUpdateTitleApiResponse success(UpdateTitleResponse data) {
            return new MiraUpdateTitleApiResponse(data, java.util.List.of());
        }

        public static MiraUpdateTitleApiResponse error(String message) {
            return new MiraUpdateTitleApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * ユーザーコンテキスト API レスポンス.
     */
    @Schema(description = "Mira ユーザーコンテキスト API レスポンス")
    public record MiraUserContextApiResponse(
            @Schema(description = "レスポンスデータ") UserContextResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraUserContextApiResponse success(UserContextResponse data) {
            return new MiraUserContextApiResponse(data, java.util.List.of());
        }

        public static MiraUserContextApiResponse error(String message) {
            return new MiraUserContextApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * ユーザーコンテキスト更新リクエスト.
     */
    @Schema(description = "ユーザーコンテキスト更新リクエスト")
    public record UserContextRequest(
            @Schema(description = "専門用語コンテキスト") String terminology,
            @Schema(description = "回答スタイルコンテキスト") String style,
            @Schema(description = "ワークフローコンテキスト") String workflow,
            @Schema(description = "Tavily API Key (Integration)") String tavilyApiKey) {
    }

    /**
     * エクスポート API レスポンス.
     */
    @Schema(description = "Mira エクスポート API レスポンス")
    public record MiraExportApiResponse(
            @Schema(description = "レスポンスデータ") ExportDataResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraExportApiResponse success(ExportDataResponse data) {
            return new MiraExportApiResponse(data, java.util.List.of());
        }

        public static MiraExportApiResponse error(String message) {
            return new MiraExportApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * 推奨設定 API レスポンス.
     */
    @Schema(description = "Mira 推奨設定 API レスポンス")
    public record MiraSuggestConfigApiResponse(
            @Schema(description = "レスポンスデータ") MessageConfig data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraSuggestConfigApiResponse success(MessageConfig data) {
            return new MiraSuggestConfigApiResponse(data, java.util.List.of());
        }

        public static MiraSuggestConfigApiResponse error(String message) {
            return new MiraSuggestConfigApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * 会話一覧 API レスポンス.
     */
    @Schema(description = "Mira 会話一覧 API レスポンス")
    public record MiraConversationListApiResponse(
            @Schema(description = "レスポンスデータ") jp.vemi.mirel.apps.mira.domain.dto.response.ConversationListResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraConversationListApiResponse success(
                jp.vemi.mirel.apps.mira.domain.dto.response.ConversationListResponse data) {
            return new MiraConversationListApiResponse(data, java.util.List.of());
        }

        public static MiraConversationListApiResponse error(String message) {
            return new MiraConversationListApiResponse(null, java.util.List.of(message));
        }
    }

    /**
     * 会話詳細 API レスポンス.
     */
    @Schema(description = "Mira 会話詳細 API レスポンス")
    public record MiraConversationDetailApiResponse(
            @Schema(description = "レスポンスデータ") jp.vemi.mirel.apps.mira.domain.dto.response.ConversationDetailResponse data,
            @Schema(description = "エラーメッセージリスト") java.util.List<String> errors) {
        public static MiraConversationDetailApiResponse success(
                jp.vemi.mirel.apps.mira.domain.dto.response.ConversationDetailResponse data) {
            return new MiraConversationDetailApiResponse(data, java.util.List.of());
        }

        public static MiraConversationDetailApiResponse error(String message) {
            return new MiraConversationDetailApiResponse(null, java.util.List.of(message));
        }
    }
}
