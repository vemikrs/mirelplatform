/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation.ConversationMode;

/**
 * 会話一覧レスポンス DTO.
 */
@Schema(description = "Mira 会話一覧レスポンス")
public record ConversationListResponse(
        @Schema(description = "会話リスト") List<ConversationSummary> conversations,
        @Schema(description = "現在のページ番号 (0-indexed)") int page,
        @Schema(description = "1ページあたりの件数") int size,
        @Schema(description = "全件数") long totalElements,
        @Schema(description = "全ページ数") int totalPages) {

    /**
     * 会話サマリー.
     */
    @Schema(description = "会話サマリー")
    public record ConversationSummary(
            @Schema(description = "会話ID") String id,
            @Schema(description = "タイトル") String title,
            @Schema(description = "モード") ConversationMode mode,
            @Schema(description = "作成日時") LocalDateTime createdAt,
            @Schema(description = "最終アクティビティ日時") LocalDateTime lastActivityAt) {
    }
}
