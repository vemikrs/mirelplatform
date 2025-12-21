/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.vemi.mirel.apps.mira.application.dto.playground.PlaygroundChatRequest;
import jp.vemi.mirel.apps.mira.application.dto.playground.PlaygroundChatResponse;
import jp.vemi.mirel.apps.mira.domain.service.MiraChatService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mira/admin/playground")
@RequiredArgsConstructor
@Tag(name = "Mira AI Playground", description = "AIプレイグラウンド (Admin Only)")
@PreAuthorize("hasRole('ADMIN')")
public class MiraPlaygroundController {

    private final MiraChatService chatService;

    @GetMapping("/models")
    @Operation(summary = "利用可能モデル一覧取得", description = "利用可能なAIモデルの一覧を取得します。")
    public List<String> listModels() {
        // TODO: Retrieve from registry or config
        return List.of("gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro", "gpt-4o", "gpt-4-turbo",
                "claude-3-opus");
    }

    @PostMapping("/chat")
    @Operation(summary = "プレイグラウンドチャット実行", description = "詳細なパラメータを指定してチャットを実行します。")
    public PlaygroundChatResponse playgroundChat(
            @RequestBody PlaygroundChatRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null)
            tenantId = "default";
        String userId = jwt.getSubject();

        // Admin privilege is checked by @PreAuthorize

        return chatService.executePlaygroundChat(request, tenantId, userId);
    }
}
