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
import jp.vemi.mirel.apps.mira.domain.dto.response.ChatResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ContextSnapshotResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.GenerateTitleResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.UserContextResponse;
import jp.vemi.mirel.apps.mira.domain.service.MiraAuditService;
import jp.vemi.mirel.apps.mira.domain.service.MiraChatService;
import jp.vemi.mirel.apps.mira.domain.service.MiraContextLayerService;
import jp.vemi.mirel.apps.mira.domain.service.MiraRbacAdapter;
import jp.vemi.mirel.apps.mira.domain.service.MiraTenantContextManager;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    
    // ========================================
    // Chat Endpoints
    // ========================================
    
    @PostMapping("/chat")
    @Operation(
        summary = "チャットメッセージ送信",
        description = "AI アシスタントにメッセージを送信し、応答を取得します。" +
                      "会話IDを指定することで、会話を継続できます。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MiraChatApiResponse.class),
                examples = @ExampleObject(
                    name = "成功例",
                    value = """
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
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "リクエストエラー"),
        @ApiResponse(responseCode = "401", description = "認証エラー"),
        @ApiResponse(responseCode = "403", description = "権限エラー"),
        @ApiResponse(responseCode = "500", description = "サーバーエラー")
    })
    public ResponseEntity<MiraChatApiResponse> chat(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "チャットリクエスト",
            required = true,
            content = @Content(
                schema = @Schema(implementation = ApiRequest.class),
                examples = @ExampleObject(
                    value = """
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
                    """
                )
            )
        )
        @RequestBody ApiRequest<ChatRequest> request) {
        
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
                e.getMessage()
            );
            return ResponseEntity.internalServerError()
                .body(MiraChatApiResponse.error("チャット処理中にエラーが発生しました"));
        }
    }
    
    // ========================================
    // Context Snapshot Endpoints
    // ========================================
    
    @PostMapping("/context-snapshot")
    @Operation(
        summary = "コンテキストスナップショット保存",
        description = "現在の画面コンテキスト情報を保存します。" +
                      "コンテキストヘルプ機能で使用されます。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MiraSnapshotApiResponse.class)
            )
        ),
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
    @Operation(
        summary = "エラー分析",
        description = "発生したエラーを AI に分析させ、解決策を提案します。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MiraChatApiResponse.class),
                examples = @ExampleObject(
                    name = "成功例",
                    value = """
                    {
                      "data": {
                        "conversationId": "...",
                        "mode": "ERROR_ANALYZE",
                        "assistantMessage": {
                          "content": "## エラー原因\\n..."
                        }
                      }
                    }
                    """
                )
            )
        )
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
    @Operation(
        summary = "会話クリア",
        description = "指定した会話をクリア（終了）します。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "404", description = "会話が見つからない")
    })
    public ResponseEntity<Map<String, Object>> clearConversation(
        @Parameter(description = "会話ID", required = true)
        @PathVariable String conversationId) {
        
        try {
            String tenantId = tenantContextManager.getCurrentTenantId();
            String userId = tenantContextManager.getCurrentUserId();
            
            chatService.clearConversation(conversationId, tenantId, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "会話をクリアしました"
            ));
            
        } catch (Exception e) {
            log.error("会話クリアエラー: conversationId={}", conversationId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "success", false,
                    "message", "会話のクリアに失敗しました"
                ));
        }
    }
    
    @GetMapping("/conversation/{conversationId}/status")
    @Operation(
        summary = "会話ステータス取得",
        description = "指定した会話のステータスを取得します。"
    )
    public ResponseEntity<Map<String, Object>> getConversationStatus(
        @Parameter(description = "会話ID", required = true)
        @PathVariable String conversationId) {
        
        String tenantId = tenantContextManager.getCurrentTenantId();
        String userId = tenantContextManager.getCurrentUserId();
        
        boolean active = chatService.isConversationActive(conversationId, tenantId, userId);
        
        return ResponseEntity.ok(Map.of(
            "conversationId", conversationId,
            "active", active
        ));
    }
    
    // ========================================
    // Title Generation Endpoints
    // ========================================
    
    @PostMapping("/conversation/generate-title")
    @Operation(
        summary = "会話タイトル生成",
        description = "会話内容を AI が分析し、適切なタイトルを生成します。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MiraTitleApiResponse.class),
                examples = @ExampleObject(
                    name = "成功例",
                    value = """
                    {
                      "data": {
                        "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                        "title": "ステンシル作成の質問",
                        "success": true
                      },
                      "errors": []
                    }
                    """
                )
            )
        ),
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
    
    // ========================================
    // User Context Endpoints
    // ========================================
    
    @GetMapping("/user-context")
    @Operation(
        summary = "ユーザーコンテキスト取得",
        description = "現在のユーザーのAIコンテキスト設定を取得します。"
    )
    @ApiResponse(
        responseCode = "200",
        description = "成功",
        content = @Content(mediaType = "application/json")
    )
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
                contexts.getOrDefault("workflow", "")
            );
            
            return ResponseEntity.ok(MiraUserContextApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("ユーザーコンテキスト取得エラー", e);
            return ResponseEntity.internalServerError()
                .body(MiraUserContextApiResponse.error("コンテキストの取得中にエラーが発生しました"));
        }
    }
    
    @PutMapping("/user-context")
    @Operation(
        summary = "ユーザーコンテキスト更新",
        description = "現在のユーザーのAIコンテキスト設定を更新します。"
    )
    @ApiResponse(
        responseCode = "200",
        description = "成功",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<MiraUserContextApiResponse> updateUserContext(
            @RequestBody UserContextRequest request) {
        try {
            String userId = tenantContextManager.getCurrentUserId();
            
            log.info("[MiraApi] updateUserContext: userId={}", userId);
            
            // 各カテゴリを保存（upsert）
            contextLayerService.saveOrUpdateUserContext(userId, "terminology", request.terminology());
            contextLayerService.saveOrUpdateUserContext(userId, "style", request.style());
            contextLayerService.saveOrUpdateUserContext(userId, "workflow", request.workflow());
            
            // 監査ログ
            auditService.logContextUpdate(userId, "USER_CONTEXT_UPDATED");
            
            return ResponseEntity.ok(MiraUserContextApiResponse.success(
                new UserContextResponse(
                    request.terminology(),
                    request.style(),
                    request.workflow()
                )
            ));
            
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
    @Operation(
        summary = "ヘルスチェック",
        description = "Mira AI サービスの稼働状態を確認します。"
    )
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "mira-ai-assistant",
            "timestamp", java.time.Instant.now().toString()
        ));
    }
    
    // ========================================
    // Response Wrapper Classes
    // ========================================
    
    /**
     * チャット API レスポンス.
     */
    @Schema(description = "Mira チャット API レスポンス")
    public record MiraChatApiResponse(
        @Schema(description = "レスポンスデータ")
        ChatResponse data,
        @Schema(description = "エラーメッセージリスト")
        java.util.List<String> errors
    ) {
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
        @Schema(description = "レスポンスデータ")
        ContextSnapshotResponse data,
        @Schema(description = "エラーメッセージリスト")
        java.util.List<String> errors
    ) {
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
        @Schema(description = "レスポンスデータ")
        GenerateTitleResponse data,
        @Schema(description = "エラーメッセージリスト")
        java.util.List<String> errors
    ) {
        public static MiraTitleApiResponse success(GenerateTitleResponse data) {
            return new MiraTitleApiResponse(data, java.util.List.of());
        }
        
        public static MiraTitleApiResponse error(String message) {
            return new MiraTitleApiResponse(null, java.util.List.of(message));
        }
    }
    
    /**
     * ユーザーコンテキスト API レスポンス.
     */
    @Schema(description = "Mira ユーザーコンテキスト API レスポンス")
    public record MiraUserContextApiResponse(
        @Schema(description = "レスポンスデータ")
        UserContextResponse data,
        @Schema(description = "エラーメッセージリスト")
        java.util.List<String> errors
    ) {
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
        @Schema(description = "専門用語コンテキスト")
        String terminology,
        @Schema(description = "回答スタイルコンテキスト")
        String style,
        @Schema(description = "ワークフローコンテキスト")
        String workflow
    ) {}
}
