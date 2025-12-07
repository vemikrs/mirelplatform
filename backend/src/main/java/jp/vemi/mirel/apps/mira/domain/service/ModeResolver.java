/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * モード解決サービス.
 * 
 * <p>リクエストの内容からAI動作モードを判定します。</p>
 */
@Slf4j
@Component
public class ModeResolver {

    // 意図推定パターン (ReDoS 対策: .* を .*? に変更し、文字数制限を追加)
    private static final Map<MiraMode, Pattern[]> INTENT_PATTERNS = Map.of(
            MiraMode.CONTEXT_HELP, new Pattern[] {
                    Pattern.compile("この画面.{0,50}?(?:説明|教え|何)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?:how|what).{0,50}?(?:screen|page)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?:使い方|操作方法|機能)", Pattern.CASE_INSENSITIVE)
            },
            MiraMode.ERROR_ANALYZE, new Pattern[] {
                    Pattern.compile("(?:エラー|問題|障害|失敗)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?:error|fail|problem|issue)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?:動かない|うまくいかない|できない)", Pattern.CASE_INSENSITIVE)
            },
            MiraMode.STUDIO_AGENT, new Pattern[] {
                    Pattern.compile("(?:モデル|エンティティ|フィールド).{0,30}?(?:作成|追加|編集)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?:ステンシル|テンプレート).{0,30}?(?:生成|作成)", Pattern.CASE_INSENSITIVE)
            },
            MiraMode.WORKFLOW_AGENT, new Pattern[] {
                    Pattern.compile("(?:ワークフロー|フロー|自動化)", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("(?:workflow|automation|process)", Pattern.CASE_INSENSITIVE)
            }
    );

    /**
     * モードを解決.
     * 
     * @param request チャットリクエスト
     * @return 解決されたモード
     */
    public MiraMode resolve(ChatRequest request) {
        // 明示的なモード指定があれば優先
        if (request.getMode() != null && !request.getMode().isEmpty()) {
            MiraMode explicitMode = MiraMode.fromCode(request.getMode());
            log.debug("Explicit mode specified: {}", explicitMode);
            return explicitMode;
        }

        // コンテキスト情報に基づくモード推定
        if (request.getContext() != null) {
            MiraMode contextMode = resolveFromContext(request.getContext());
            if (contextMode != null) {
                log.debug("Mode resolved from context: {}", contextMode);
                return contextMode;
            }
        }

        // メッセージ内容から意図推定
        if (request.getMessage() != null && request.getMessage().getContent() != null) {
            MiraMode intentMode = resolveFromIntent(request.getMessage().getContent());
            if (intentMode != null) {
                log.debug("Mode resolved from intent: {}", intentMode);
                return intentMode;
            }
        }

        // デフォルトは汎用チャット
        log.debug("Using default mode: GENERAL_CHAT");
        return MiraMode.GENERAL_CHAT;
    }

    /**
     * コンテキスト情報からモードを推定.
     */
    private MiraMode resolveFromContext(ChatRequest.Context context) {
        String appId = context.getAppId();
        if (appId == null) {
            return null;
        }

        return switch (appId.toLowerCase()) {
            case "studio" -> MiraMode.STUDIO_AGENT;
            case "workflow" -> MiraMode.WORKFLOW_AGENT;
            default -> null;
        };
    }

    /**
     * メッセージ内容から意図を推定.
     */
    private MiraMode resolveFromIntent(String message) {
        for (Map.Entry<MiraMode, Pattern[]> entry : INTENT_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(message).find()) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
