/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest.MessageConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira プリセット推奨サービス.
 * <p>
 * ユーザーの入力メッセージに基づいて、最適なコンテキスト設定(MessageConfig)を提案します。
 * </p>
 */
@Slf4j
@Service
public class MiraPresetSuggestionService {

    /**
     * メッセージ内容からAI推奨設定を生成.
     * 
     * @param messageContent
     *            ユーザー入力メッセージ
     * @return 推奨メッセージ設定
     */
    public MessageConfig suggestConfig(String messageContent) {
        // TODO: In Phase 2, integrate with a lightweight LLM (e.g., GPT-4o-mini)
        // to generate suggestions based on intent classification.

        // For Phase 1 (MVP), we use simple heuristic rules.

        if (messageContent == null) {
            return MessageConfig.builder().build();
        }

        String lowerContent = messageContent.toLowerCase();

        // Example Rule 1: Code review request
        if (lowerContent.contains("レビュー") || lowerContent.contains("review") || lowerContent.contains("fix")) {
            return MessageConfig.builder()
                    .historyScope("recent")
                    .recentCount(5)
                    .temporaryContext("あなたはシニアエンジニアです。コードの品質、セキュリティ、可読性の観点から厳格にレビューしてください。")
                    .build();
        }

        // Example Rule 2: Summary request
        if (lowerContent.contains("要約") || lowerContent.contains("summary") || lowerContent.contains("tl;dr")) {
            return MessageConfig.builder()
                    .historyScope("auto")
                    .temporaryContext("簡潔に要約してください。箇条書きを使用してください。")
                    .build();
        }

        // Default: no specific suggestion
        return MessageConfig.builder()
                .historyScope("auto")
                .build();
    }
}
