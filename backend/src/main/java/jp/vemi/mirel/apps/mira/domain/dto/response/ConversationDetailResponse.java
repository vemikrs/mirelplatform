/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraConversation.ConversationMode;

/**
 * 会話詳細レスポンス DTO.
 */
@Schema(description = "Mira 会話詳細レスポンス")
public record ConversationDetailResponse(
        @Schema(description = "会話ID") String id,
        @Schema(description = "タイトル") String title,
        @Schema(description = "モード") ConversationMode mode,
        @Schema(description = "作成日時") LocalDateTime createdAt,
        @Schema(description = "最終アクティビティ日時") LocalDateTime lastActivityAt,
        @Schema(description = "メッセージリスト") List<MessageDetail> messages) {

    /**
     * メッセージ詳細.
     */
    @Schema(description = "メッセージ詳細")
    public record MessageDetail(
            @Schema(description = "メッセージID") String id,
            @Schema(description = "ロール (user/assistant)") String role,
            @Schema(description = "本文") String content,
            @Schema(description = "コンテンツタイプ") String contentType,
            @Schema(description = "送信日時") LocalDateTime createdAt) {
    }
}
