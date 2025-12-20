/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;
import jp.vemi.mirel.apps.mira.domain.service.MiraKnowledgeBaseService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mira/knowledge")
@RequiredArgsConstructor
@Tag(name = "Mira Knowledge Base", description = "ナレッジベース管理API")
public class MiraKnowledgeBaseController {

    private final MiraKnowledgeBaseService knowledgeBaseService;

    @PostMapping("/index/{fileId}")
    @Operation(summary = "ドキュメントのインデックス作成", description = "指定されたファイルをナレッジベースに登録します。")
    public void indexFile(
            @PathVariable String fileId,
            @RequestParam(defaultValue = "USER") MiraVectorStore.Scope scope,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id"); // Assuming standard claims, adjust if needed
        if (tenantId == null)
            tenantId = "default"; // Fallback
        String userId = jwt.getSubject();

        // Scope validation
        if (scope == MiraVectorStore.Scope.SYSTEM) {
            java.util.Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            @SuppressWarnings("unchecked")
            java.util.List<String> roles = (realmAccess != null) ? (java.util.List<String>) realmAccess.get("roles")
                    : null;
            if (roles == null || !roles.contains("ADMIN")) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "SYSTEM scope requires ADMIN role");
            }
        }

        knowledgeBaseService.indexFile(fileId, scope, tenantId, userId);
    }
}
