/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.exception.MiraErrorCode;
import jp.vemi.mirel.apps.mira.domain.exception.MiraException;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira プロンプトオーケストレーションサービス.
 * <p>
 * 3レイヤーアーキテクチャ（Identity / State / Governance）に基づいて
 * System Prompt を動的に構築する。
 * </p>
 * 
 * <p><b>レイヤー構成:</b></p>
 * <ol>
 *   <li><b>Identity Layer</b> (静的): Miraの役割・トーン&マナー定義</li>
 *   <li><b>State Layer</b> (動的): アプリケーション状態のJSON注入</li>
 *   <li><b>Governance Layer</b> (動的): ロケール・権限に応じた制約</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraPromptService {

    private static final String PROMPTS_BASE_PATH = "classpath:prompts/";
    
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final MiraAiProperties properties;
    private final MiraContextLayerService contextLayerService;

    /**
     * 3レイヤーを統合して System Prompt を生成.
     *
     * @param context Mira コンテキスト
     * @return 統合された System Prompt
     */
    public String buildSystemPrompt(MiraContext context) {
        if (log.isDebugEnabled()) {
            log.debug("[MiraPromptService] Building system prompt: mode={}, locale={}, screenId={}",
                    context.getMode(), context.getLocale(), context.getScreenId());
        }

        StringBuilder builder = new StringBuilder();

        // Layer 1: Identity (Static)
        String identity = loadTemplate("identity/mira-identity.md");
        builder.append(identity);
        builder.append("\n\n");

        // Layer 2: State (Dynamic JSON Injection)
        builder.append("# Context Data (JSON Injection)\n\n");
        builder.append("<context>\n");
        builder.append(buildStateContextJson(context));
        builder.append("\n</context>\n\n");
        builder.append("""
                Analyze the JSON above to understand:
                - User's current screen and context
                - User's role and permissions
                - Any selected objects or recent actions
                
                """);

        // Layer 3: Governance (Dynamic Rules)
        String locale = context.getLocale() != null ? context.getLocale() : "ja";
        builder.append(loadTemplate("governance/locale-" + locale + ".md"));
        builder.append("\n\n");
        builder.append(loadTemplate("governance/terminology.md"));
        builder.append("\n\n");

        // Mode-specific instructions
        String modeTemplate = getModeTemplateFile(context.getMode());
        builder.append(loadTemplate("modes/" + modeTemplate));
        builder.append("\n\n");

        // Hierarchical context addition (DB-sourced)
        String hierarchicalContext = contextLayerService.buildContextPromptAddition(
                context.getTenantId(),
                context.getOrganizationId(),
                context.getUserId());
        if (hierarchicalContext != null && !hierarchicalContext.isEmpty()) {
            builder.append(hierarchicalContext);
        }

        // Response format reminder
        builder.append("# Response Format\n");
        if ("ja".equals(locale)) {
            builder.append("- Respond in Japanese (日本語)\n");
        } else {
            builder.append("- Respond in English\n");
        }
        builder.append("- Use Markdown formatting for structure\n");
        builder.append("- Include code examples when relevant\n");
        builder.append("- Keep responses focused and actionable\n");

        String result = builder.toString();
        
        if (log.isTraceEnabled()) {
            log.trace("[MiraPromptService] Built system prompt: {} chars", result.length());
        }

        return result;
    }

    /**
     * ChatRequest から MiraContext を構築.
     *
     * @param request チャットリクエスト
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param mode 動作モード
     * @return MiraContext
     */
    public MiraContext buildContext(
            ChatRequest request, 
            String tenantId, 
            String userId, 
            MiraMode mode) {
        
        ChatRequest.Context ctx = request.getContext();
        
        StateContext.StateContextBuilder stateBuilder = StateContext.builder()
                .tenantId(tenantId)
                .userId(userId);
        
        if (ctx != null) {
            stateBuilder
                    .screenId(ctx.getScreenId())
                    .systemRole(ctx.getSystemRole())
                    .appRole(ctx.getAppRole())
                    .locale(ctx.getLocale() != null ? ctx.getLocale() : "ja");
            
            if (ctx.getPayload() != null) {
                stateBuilder.selectedEntity((String) ctx.getPayload().get("selectedEntity"));
                @SuppressWarnings("unchecked")
                List<String> actions = (List<String>) ctx.getPayload().get("recentActions");
                stateBuilder.recentActions(actions);
            }
        }
        
        return MiraContext.builder()
                .tenantId(tenantId)
                .userId(userId)
                .mode(mode)
                .locale(ctx != null && ctx.getLocale() != null ? ctx.getLocale() : "ja")
                .screenId(ctx != null ? ctx.getScreenId() : null)
                .appRole(ctx != null ? ctx.getAppRole() : null)
                .systemRole(ctx != null ? ctx.getSystemRole() : null)
                .stateContext(stateBuilder.build())
                .build();
    }

    /**
     * State Context を JSON 文字列に変換.
     */
    private String buildStateContextJson(MiraContext context) {
        try {
            StateContext state = context.getStateContext();
            if (state == null) {
                state = StateContext.builder()
                        .tenantId(context.getTenantId())
                        .userId(context.getUserId())
                        .screenId(context.getScreenId())
                        .systemRole(context.getSystemRole())
                        .appRole(context.getAppRole())
                        .locale(context.getLocale())
                        .build();
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(state);
        } catch (JsonProcessingException e) {
            log.warn("[MiraPromptService] Failed to serialize state context to JSON", e);
            return "{}";
        }
    }

    /**
     * モードからテンプレートファイル名を取得.
     */
    private String getModeTemplateFile(MiraMode mode) {
        return switch (mode) {
            case GENERAL_CHAT -> "general-chat.md";
            case CONTEXT_HELP -> "context-help.md";
            case ERROR_ANALYZE -> "error-analyze.md";
            case STUDIO_AGENT -> "studio-agent.md";
            case WORKFLOW_AGENT -> "workflow-agent.md";
        };
    }

    /**
     * テンプレートファイルを読み込み.
     *
     * @param path prompts/ からの相対パス
     * @return テンプレート内容
     */
    private String loadTemplate(String path) {
        String fullPath = PROMPTS_BASE_PATH + path;
        try {
            Resource resource = resourceLoader.getResource(fullPath);
            if (!resource.exists()) {
                log.warn("[MiraPromptService] Template not found: {}", fullPath);
                return "";
            }
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            if (log.isTraceEnabled()) {
                log.trace("[MiraPromptService] Loaded template: {} ({} bytes)", path, content.length());
            }
            
            return content;
        } catch (IOException e) {
            log.error("[MiraPromptService] Failed to load template: {}", fullPath, e);
            throw new MiraException(MiraErrorCode.TEMPLATE_LOAD_ERROR, path);
        }
    }

    /**
     * Mira コンテキスト.
     */
    @Data
    @Builder
    public static class MiraContext {
        private String tenantId;
        private String organizationId;
        private String userId;
        private String conversationId;
        private MiraMode mode;
        private String locale;
        private String screenId;
        private String systemRole;
        private String appRole;
        private StateContext stateContext;
        private ErrorContext errorContext;
    }

    /**
     * State Layer 用コンテキスト.
     */
    @Data
    @Builder
    public static class StateContext {
        private String tenantId;
        private String userId;
        private String screenId;
        private String systemRole;
        private String appRole;
        private String locale;
        private String selectedEntity;
        private List<String> recentActions;
        private Map<String, Object> customData;
    }

    /**
     * エラーコンテキスト（ERROR_ANALYZE モード用）.
     */
    @Data
    @Builder
    public static class ErrorContext {
        private String source;
        private String code;
        private String message;
        private String stackTrace;
        private String timestamp;
        private Map<String, Object> additionalInfo;
    }
}
