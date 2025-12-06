/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会話タイトル生成リクエスト.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTitleRequest {
    
    /**
     * 会話ID.
     */
    @NotBlank(message = "会話IDは必須です")
    private String conversationId;
    
    /**
     * タイトル生成に使用するメッセージリスト.
     * フロントエンドから最初の数メッセージを送信。
     */
    @NotEmpty(message = "メッセージは最低1件必要です")
    private List<MessageSummary> messages;
    
    /**
     * メッセージサマリー.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageSummary {
        private String role;  // "user" or "assistant"
        private String content;
    }
}
