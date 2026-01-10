/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.ContextOverride;
import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.MessageConfig;

// Actually, MiraChatService likely uses a domain model for Message. Let's check MiraChatService context later or assume a generic Message type for now, 
// using generic Object or just List<?> for the signature in plan, but let's see. 
// The plan used `List<Message>`. I'll check `MiraChatService` imports if needed, but for now I'll use a placeholder or generic.
// Actually, better to check `MiraChatService` to see what `Message` class it uses.
// For now I will implement the context merging part which returns String or Map.

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jp.vemi.framework.util.SanitizeUtil;

/**
 * Mira コンテキストマージサービス.
 * <p>
 * MessageConfigに基づいてコンテキストをマージ・調整する主要ロジックを提供します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraContextMergeService {

    private final MiraContextLayerService contextLayerService;

    /**
     * messageConfigに基づいてコンテキストをマージして最終的なプロンプト用テキストを生成.
     * 
     * @param tenantId
     *            テナントID
     * @param organizationId
     *            組織ID
     * @param userId
     *            ユーザーID
     * @param messageConfig
     *            メッセージ設定
     * @return マージされたコンテキスト文字列
     */
    public String buildFinalContextPrompt(
            String tenantId,
            String organizationId,
            String userId,
            MessageConfig messageConfig) {

        // 1. 基本コンテキスト取得
        Map<String, String> baseContext = contextLayerService.buildMergedContext(tenantId, organizationId, userId);

        if (messageConfig == null) {
            return formatContextMap(baseContext);
        }

        // 2. contextOverridesを適用
        Map<String, String> filteredContext = applyOverrides(
                baseContext, messageConfig.getContextOverrides());

        // 3. additionalPresetsを追加 (TBD: implementation when preset logic is ready)
        // String withPresets = mergePresets(filteredContext,
        // messageConfig.getAdditionalPresets());

        // 4. temporaryContextを最後にマージ（最優先）
        StringBuilder sb = new StringBuilder();
        sb.append(formatContextMap(filteredContext));

        if (messageConfig.getTemporaryContext() != null && !messageConfig.getTemporaryContext().isEmpty()) {
            sb.append("\n\n## Temporary Context (High Priority Override)\n");
            sb.append(
                    "IMPORTANT: The following instruction overrides all previous persona, style, and behavior instructions including System and User Contexts.\n");
            sb.append(messageConfig.getTemporaryContext());
            sb.append("\n");
        }

        return sb.toString();
    }

    private Map<String, String> applyOverrides(
            Map<String, String> baseContext,
            Map<String, ContextOverride> overrides) {

        if (overrides == null || overrides.isEmpty()) {
            return new HashMap<>(baseContext);
        }

        Map<String, String> result = new HashMap<>(baseContext);

        overrides.forEach((category, override) -> {
            if (Boolean.FALSE.equals(override.getEnabled())) {
                result.remove(category);
                log.debug("Context category '{}' disabled by override", SanitizeUtil.forLog(category));
            }
            // Priority handling (if we had multiple layers here we would re-sort,
            // but here we only have the final merged map.
            // So logical priority changes might need to be verified at the layer fetching
            // stage if strict.
            // For now, we assume this service operates on the flattened result,
            // so priority only affects if we were re-merging source layers.
            // Since we receive the merged map, we can't easily "boost" priority vs other
            // layers
            // unless we re-fetch.
            // However, typical usage of priority here is 'low' priority might mean
            // "append at the very end so it has less weight" or similar in prompt
            // engineering,
            // but standard 'system' prompt usually comes first.
            // For this implementation, we just respect 'enabled' flag.
        });

        return result;
    }

    private String formatContextMap(Map<String, String> contextMap) {
        if (contextMap.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n# Additional Context\n\n");
        contextMap.forEach((category, content) -> {
            sb.append("## ").append(category).append("\n");
            sb.append(content).append("\n\n");
        });
        return sb.toString();
    }

    // Note: History filtering will be handled in MiraChatService or a separate
    // method here
    // depending on where the history list is managed.
}
