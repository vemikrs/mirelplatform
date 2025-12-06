/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会話タイトル生成レスポンス.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTitleResponse {
    
    /**
     * 会話ID.
     */
    private String conversationId;
    
    /**
     * 生成されたタイトル.
     */
    private String title;
    
    /**
     * 成功フラグ.
     */
    private boolean success;
    
    /**
     * エラーメッセージ (失敗時のみ).
     */
    private String errorMessage;
    
    /**
     * 成功レスポンス生成.
     */
    public static GenerateTitleResponse success(String conversationId, String title) {
        return GenerateTitleResponse.builder()
            .conversationId(conversationId)
            .title(title)
            .success(true)
            .build();
    }
    
    /**
     * 失敗レスポンス生成.
     */
    public static GenerateTitleResponse error(String conversationId, String errorMessage) {
        return GenerateTitleResponse.builder()
            .conversationId(conversationId)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
