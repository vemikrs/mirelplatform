/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会話タイトル更新リクエスト.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTitleRequest {
    
    /**
     * 会話ID.
     */
    @NotBlank(message = "会話IDは必須です")
    private String conversationId;
    
    /**
     * 新しいタイトル.
     */
    @NotBlank(message = "タイトルは必須です")
    private String title;
}
