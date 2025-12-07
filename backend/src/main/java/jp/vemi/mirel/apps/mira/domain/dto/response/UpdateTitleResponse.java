/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会話タイトル更新レスポンス.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTitleResponse {
    
    /**
     * 会話ID.
     */
    private String conversationId;
    
    /**
     * 更新後のタイトル.
     */
    private String title;
    
    /**
     * 成功フラグ.
     */
    private boolean success;
    
    /**
     * エラーメッセージ（失敗時）.
     */
    private String errorMessage;
    
    /**
     * 成功レスポンス生成.
     */
    public static UpdateTitleResponse success(String conversationId, String title) {
        return UpdateTitleResponse.builder()
            .conversationId(conversationId)
            .title(title)
            .success(true)
            .build();
    }
    
    /**
     * エラーレスポンス生成.
     */
    public static UpdateTitleResponse error(String conversationId, String errorMessage) {
        return UpdateTitleResponse.builder()
            .conversationId(conversationId)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
