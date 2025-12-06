/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ErrorReportRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * プロンプトビルダー.
 * 
 * <p>テンプレートとコンテキスト情報からAIリクエスト用のプロンプトを組み立てます。</p>
 */
@Slf4j
@Component
public class PromptBuilder {
    
    /** Mustache プレースホルダーパターン */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");
    
    /** デフォルト最大履歴メッセージ数 */
    private static final int DEFAULT_MAX_HISTORY = 10;
    
    /**
     * チャットリクエストからAIリクエストを構築.
     *
     * @param request チャットリクエスト
     * @param mode 動作モード
     * @param conversationHistory 会話履歴（古い順）
     * @return AIリクエスト
     */
    public AiRequest buildChatRequest(
            ChatRequest request,
            MiraMode mode,
            List<AiRequest.Message> conversationHistory) {
        
        PromptTemplate template = PromptTemplate.fromMode(mode);
        Map<String, String> variables = buildContextVariables(request);
        String systemPrompt = renderTemplate(template.getSystemPrompt(), variables);
        
        List<AiRequest.Message> messages = new ArrayList<>();
        messages.add(AiRequest.Message.system(systemPrompt));
        
        // 会話履歴を追加（最新N件）
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            int startIndex = Math.max(0, conversationHistory.size() - DEFAULT_MAX_HISTORY);
            messages.addAll(conversationHistory.subList(startIndex, conversationHistory.size()));
        }
        
        // ユーザーメッセージを追加
        messages.add(AiRequest.Message.user(request.getMessage().getContent()));
        
        return AiRequest.builder()
                .messages(messages)
                .temperature(getTemperatureForMode(mode))
                .maxTokens(getMaxTokensForMode(mode))
                .build();
    }
    
    /**
     * エラーレポートからAIリクエストを構築.
     *
     * @param request エラーレポートリクエスト
     * @return AIリクエスト
     */
    public AiRequest buildErrorAnalyzeRequest(ErrorReportRequest request) {
        PromptTemplate template = PromptTemplate.ERROR_ANALYZE;
        Map<String, String> variables = buildErrorVariables(request);
        String systemPrompt = renderTemplate(template.getSystemPrompt(), variables);
        
        // エラー詳細をユーザーメッセージとして構成
        String userMessage = buildErrorUserMessage(request);
        
        List<AiRequest.Message> messages = List.of(
            AiRequest.Message.system(systemPrompt),
            AiRequest.Message.user(userMessage)
        );
        
        return AiRequest.builder()
                .messages(messages)
                .temperature(0.3) // エラー分析は低めの温度
                .maxTokens(1000)
                .build();
    }
    
    /**
     * コンテキストヘルプ用AIリクエストを構築.
     *
     * @param request チャットリクエスト
     * @return AIリクエスト
     */
    public AiRequest buildContextHelpRequest(ChatRequest request) {
        return buildChatRequest(request, MiraMode.CONTEXT_HELP, null);
    }
    
    // ========================================
    // Private Methods
    // ========================================
    
    /**
     * チャットリクエストからテンプレート変数を構築.
     */
    private Map<String, String> buildContextVariables(ChatRequest request) {
        Map<String, String> variables = new HashMap<>();
        
        if (request.getContext() != null) {
            ChatRequest.Context ctx = request.getContext();
            variables.put("appId", nullSafe(ctx.getAppId()));
            variables.put("screenId", nullSafe(ctx.getScreenId()));
            variables.put("systemRole", nullSafe(ctx.getSystemRole()));
            variables.put("appRole", nullSafe(ctx.getAppRole()));
            
            // Studio モジュール情報（payload から抽出）
            if (ctx.getPayload() != null) {
                variables.put("studioModule", 
                    nullSafe((String) ctx.getPayload().get("studioModule")));
                variables.put("targetEntity", 
                    nullSafe((String) ctx.getPayload().get("targetEntity")));
                variables.put("processId", 
                    nullSafe((String) ctx.getPayload().get("processId")));
                variables.put("currentStep", 
                    nullSafe((String) ctx.getPayload().get("currentStep")));
                variables.put("workflowStatus", 
                    nullSafe((String) ctx.getPayload().get("workflowStatus")));
            }
        }
        
        return variables;
    }
    
    /**
     * エラーレポートからテンプレート変数を構築.
     */
    private Map<String, String> buildErrorVariables(ErrorReportRequest request) {
        Map<String, String> variables = new HashMap<>();
        
        variables.put("errorSource", nullSafe(request.getSource()));
        variables.put("errorCode", nullSafe(request.getCode()));
        variables.put("errorMessage", nullSafe(request.getMessage()));
        variables.put("errorDetail", nullSafe(request.getDetail()));
        
        if (request.getContext() != null) {
            variables.put("appId", nullSafe(request.getContext().getAppId()));
            variables.put("screenId", nullSafe(request.getContext().getScreenId()));
        }
        
        return variables;
    }
    
    /**
     * エラーレポートからユーザーメッセージを構築.
     */
    private String buildErrorUserMessage(ErrorReportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下のエラーが発生しました。原因と解決策を教えてください。\n\n");
        sb.append("```\n");
        sb.append("ソース: ").append(nullSafe(request.getSource())).append("\n");
        sb.append("コード: ").append(nullSafe(request.getCode())).append("\n");
        sb.append("メッセージ: ").append(nullSafe(request.getMessage())).append("\n");
        if (request.getDetail() != null && !request.getDetail().isEmpty()) {
            sb.append("詳細:\n").append(request.getDetail()).append("\n");
        }
        sb.append("```");
        return sb.toString();
    }
    
    /**
     * テンプレートにプレースホルダーを適用.
     */
    private String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * モードに応じた温度パラメータを取得.
     */
    private double getTemperatureForMode(MiraMode mode) {
        return switch (mode) {
            case ERROR_ANALYZE -> 0.3;  // 正確な分析
            case CONTEXT_HELP -> 0.5;   // バランス
            case STUDIO_AGENT -> 0.4;   // 技術的正確性
            case WORKFLOW_AGENT -> 0.4; // 業務的正確性
            case GENERAL_CHAT -> 0.7;   // 自然な会話
        };
    }
    
    /**
     * モードに応じた最大トークン数を取得.
     */
    private int getMaxTokensForMode(MiraMode mode) {
        return switch (mode) {
            case ERROR_ANALYZE -> 1000;
            case CONTEXT_HELP -> 800;
            case STUDIO_AGENT -> 1500;
            case WORKFLOW_AGENT -> 800;
            case GENERAL_CHAT -> 1000;
        };
    }
    
    /**
     * null安全な文字列変換.
     */
    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
