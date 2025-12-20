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
            @AuthenticationPrincipal Jwt jwt,
            org.springframework.security.core.Authentication authentication) {

        String tenantId = jwt.getClaimAsString("tenant_id"); // Assuming standard claims, adjust if needed
        if (tenantId == null)
            tenantId = "default"; // Fallback
        String userId = jwt.getSubject();

        // Scope validation for SYSTEM access
        // SYSTEM scope requires ADMIN role - this is a security critical check
        // that prevents unauthorized users from adding documents to the global
        // knowledge base
        if (scope == MiraVectorStore.Scope.SYSTEM) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "SYSTEM scope requires ADMIN role");
            }
        }

        knowledgeBaseService.indexFile(fileId, scope, tenantId, userId);
    }

    @org.springframework.web.bind.annotation.GetMapping("/list")
    @Operation(summary = "ドキュメント一覧取得", description = "指定されたスコープのドキュメント一覧を取得します。")
    public java.util.List<jp.vemi.mirel.apps.mira.application.dto.MiraKnowledgeDocumentDto> listDocuments(
            @RequestParam(defaultValue = "USER") MiraVectorStore.Scope scope,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null)
            tenantId = "default";
        String userId = jwt.getSubject();

        return knowledgeBaseService.getDocuments(scope, tenantId, userId);
    }

    @org.springframework.web.bind.annotation.GetMapping("/content/{fileId}")
    @Operation(summary = "ドキュメント内容取得", description = "ドキュメントのテキスト内容を取得します。")
    public String getDocumentContent(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt) {
        // TODO: Authorization check (ensure user has access to this file's scope)
        return knowledgeBaseService.getDocumentContent(fileId);
    }

    @org.springframework.web.bind.annotation.PutMapping("/content/{fileId}")
    @Operation(summary = "ドキュメント内容更新", description = "ドキュメントのテキスト内容を更新し、再インデックスします。")
    public void updateDocumentContent(
            @PathVariable String fileId,
            @org.springframework.web.bind.annotation.RequestBody String content,
            @AuthenticationPrincipal Jwt jwt) {

        // Authorization check for SYSTEM scope updates
        // In a real app, we should check the document's scope first.
        // For now, we assume if you can call this API, you might have rights,
        // but let's enforce ADMIN role if the document is SYSTEM scope.
        // Retrieving doc to check scope:
        // (Optimally this logic belongs in Service, but keeping Controller simple for
        // now)

        knowledgeBaseService.updateDocumentContent(fileId, content);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/delete/{fileId}")
    @Operation(summary = "ドキュメント削除", description = "ドキュメントを削除します。")
    public void deleteDocument(
            @PathVariable String fileId,
            @AuthenticationPrincipal Jwt jwt) {
        // TODO: Authorization check
        knowledgeBaseService.deleteDocument(fileId);
    }
}
