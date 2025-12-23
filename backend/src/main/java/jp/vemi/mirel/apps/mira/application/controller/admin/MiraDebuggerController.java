/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller.admin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.vemi.mirel.apps.mira.application.dto.MiraKnowledgeSearchRequest;
import jp.vemi.mirel.apps.mira.application.dto.debugger.MiraDebuggerAnalytics;
import jp.vemi.mirel.apps.mira.application.dto.debugger.MiraDebuggerStats;
import jp.vemi.mirel.apps.mira.domain.service.MiraDebuggerService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mira/debugger")
@RequiredArgsConstructor
@Tag(name = "Mira Index Debugger", description = "検索インデックスデバッグ・分析用API")
public class MiraDebuggerController {

    private final MiraDebuggerService debuggerService;
    private final jp.vemi.mirel.apps.mira.domain.service.MiraSettingService settingService;

    @GetMapping("/stats")
    @Operation(summary = "インデックス統計取得", description = "インデックスの健全性チェックのための統計情報を取得します。")
    public MiraDebuggerStats getStats(Authentication authentication) {
        checkAdmin(authentication);
        return debuggerService.getStats();
    }

    @PostMapping("/analyze")
    @Operation(summary = "検索プロセス分析", description = "検索クエリがどのように処理され、どの段階でドキュメントが除外されたかを詳細に分析します。")
    public MiraDebuggerAnalytics analyze(
            @RequestBody MiraKnowledgeSearchRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        checkAdmin(authentication);

        String tenantId = request.getTargetTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = jwt.getClaimAsString("tenant_id");
        }
        if (tenantId == null) {
            tenantId = "default";
        }

        String userId = request.getTargetUserId();
        if (userId == null || userId.isEmpty()) {
            userId = jwt.getSubject();
        }

        String scopeStr = request.getScope() != null ? request.getScope().name() : "USER";
        double threshold = request.getThreshold() != null ? request.getThreshold() : 0.0;
        int topK = request.getTopK() != null ? request.getTopK() : 50;

        return debuggerService.analyze(request.getQuery(), scopeStr, tenantId, userId, threshold, topK);
    }

    @PostMapping("/reindex")
    @Operation(summary = "Re-index Scope", description = "指定されたスコープの文書を再インデックスします。")
    public java.util.Map<String, String> reindex(
            @RequestBody MiraKnowledgeSearchRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        checkAdmin(authentication);

        String tenantId = request.getTargetTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = jwt.getClaimAsString("tenant_id");
        }
        if (tenantId == null) {
            tenantId = "default";
        }

        String userId = request.getTargetUserId();
        if (userId == null || userId.isEmpty()) {
            userId = jwt.getSubject();
        }

        String scopeStr = request.getScope() != null ? request.getScope().name() : "USER";

        String message = debuggerService.reindexScope(scopeStr, tenantId, userId);
        return java.util.Map.of("message", message);
    }

    @PostMapping("/settings")
    @Operation(summary = "チューニング設定保存", description = "デバッガで調整したパラメータ(TopK, Threshold)を保存します。")
    public java.util.Map<String, String> saveSettings(
            @RequestBody MiraKnowledgeSearchRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        checkAdmin(authentication);

        String tenantId = request.getTargetTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = jwt.getClaimAsString("tenant_id");
        }
        if (tenantId == null) {
            tenantId = "default";
        }

        // Determine scope to save to
        // If Scope is SYSTEM -> Save System Setting
        // If Scope is TENANT or USER -> Save Tenant Setting (User specific setting is
        // not yet supported in DB, implies org-wide optimization)

        String scopeStr = request.getScope() != null ? request.getScope().name() : "USER";

        if ("SYSTEM".equals(scopeStr)) {
            if (request.getThreshold() != null) {
                settingService.saveSystemSetting(
                        jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_VECTOR_SEARCH_THRESHOLD,
                        String.valueOf(request.getThreshold()));
            }
            if (request.getTopK() != null) {
                settingService.saveSystemSetting(
                        jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_VECTOR_SEARCH_TOP_K,
                        String.valueOf(request.getTopK()));
            }
        } else {
            // Tenant/User
            if (request.getThreshold() != null) {
                settingService.saveTenantSetting(tenantId,
                        jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_VECTOR_SEARCH_THRESHOLD,
                        String.valueOf(request.getThreshold()));
            }
            if (request.getTopK() != null) {
                settingService.saveTenantSetting(tenantId,
                        jp.vemi.mirel.apps.mira.domain.service.MiraSettingService.KEY_VECTOR_SEARCH_TOP_K,
                        String.valueOf(request.getTopK()));
            }
        }

        return java.util.Map.of("message", "Retrieval settings saved for scope: " + scopeStr);
    }

    @PostMapping("/documents/list")
    @Operation(summary = "ドキュメント一覧取得", description = "指定されたスコープの登録済みドキュメント一覧を取得します。")
    public java.util.List<jp.vemi.mirel.apps.mira.application.dto.MiraKnowledgeDocumentDto> listDocuments(
            @RequestBody MiraKnowledgeSearchRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        checkAdmin(authentication);

        String tenantId = request.getTargetTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = jwt.getClaimAsString("tenant_id");
        }
        if (tenantId == null) {
            tenantId = "default";
        }

        String userId = request.getTargetUserId();
        if (userId == null || userId.isEmpty()) {
            userId = jwt.getSubject();
        }

        String scopeStr = request.getScope() != null ? request.getScope().name() : "USER";

        return debuggerService.getDocuments(scopeStr, tenantId, userId);
    }

    @PostMapping("/documents/delete")
    @Operation(summary = "ドキュメント削除", description = "指定されたドキュメントを削除します。")
    public java.util.Map<String, String> deleteDocument(
            @RequestBody java.util.Map<String, String> request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        checkAdmin(authentication);
        String fileId = request.get("fileId");
        if (fileId == null || fileId.isEmpty()) {
            throw new IllegalArgumentException("fileId is required");
        }

        debuggerService.deleteDocument(fileId);
        return java.util.Map.of("message", "Document deleted: " + fileId);
    }

    private void checkAdmin(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Debugger requires ADMIN role");
        }
    }
}
