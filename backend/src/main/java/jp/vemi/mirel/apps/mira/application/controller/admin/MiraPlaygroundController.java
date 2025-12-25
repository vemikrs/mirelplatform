/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.controller.admin;

import java.util.List;

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
import jp.vemi.mirel.apps.mira.application.dto.playground.PlaygroundConfigResponse;
import jp.vemi.mirel.apps.mira.domain.service.MiraChatService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mira/admin/playground")
@RequiredArgsConstructor
@Tag(name = "Mira AI Playground", description = "AIプレイグラウンド (Admin Only)")
@PreAuthorize("hasRole('ADMIN')")
public class MiraPlaygroundController {

    private final MiraChatService chatService;
    private final jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties miraAiProperties;
    private final jp.vemi.mirel.apps.mira.domain.dao.repository.MiraModelRegistryRepository modelRegistryRepository;

    @GetMapping("/models")
    @Operation(summary = "利用可能モデル一覧取得", description = "利用可能なAIモデルの一覧を取得します。")
    public List<String> getAvailableModels() {
        return modelRegistryRepository.findByIsActiveTrue().stream()
                .map(jp.vemi.mirel.apps.mira.domain.dao.entity.MiraModelRegistry::getModelName)
                .toList();
    }

    @GetMapping("/config")
    @Operation(summary = "プレイグラウンド設定取得", description = "利用可能なモデルやデフォルトパラメータ定義を取得します。")
    public PlaygroundConfigResponse getConfig() {

        List<PlaygroundConfigResponse.ModelOption> models = modelRegistryRepository.findByIsActiveTrue().stream()
                .map(entity -> PlaygroundConfigResponse.ModelOption.builder()
                        .id(entity.getModelName())
                        .name(entity.getDisplayName())
                        .provider(entity.getProvider())
                        .build())
                .toList();

        // Setup defaults based on properties (fallback to hardcoded if not set)
        Double defaultTemp = 0.7;
        if (miraAiProperties.getVertexAi() != null && miraAiProperties.getVertexAi().getTemperature() != null) {
            defaultTemp = miraAiProperties.getVertexAi().getTemperature();
        }

        return PlaygroundConfigResponse.builder()
                .models(models)
                .defaultParams(PlaygroundConfigResponse.DefaultParams.builder()
                        .temperature(defaultTemp)
                        .topP(0.95)
                        .topK(40)
                        .maxTokens(4096)
                        // Retrieve default system prompt from shared definition
                        .systemPrompt(
                                jp.vemi.mirel.apps.mira.domain.service.PromptTemplate.GENERAL_CHAT.getSystemPrompt())
                        .build())
                .build();
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
