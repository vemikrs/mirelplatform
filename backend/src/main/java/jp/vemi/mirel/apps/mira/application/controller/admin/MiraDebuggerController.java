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
        if (tenantId == null)
            tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null)
            tenantId = "default";

        String userId = request.getTargetUserId();
        if (userId == null)
            userId = jwt.getSubject();

        String scopeStr = request.getScope() != null ? request.getScope().name() : "USER";
        double threshold = request.getThreshold() != null ? request.getThreshold() : 0.0;

        return debuggerService.analyze(request.getQuery(), scopeStr, tenantId, userId, threshold);
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
