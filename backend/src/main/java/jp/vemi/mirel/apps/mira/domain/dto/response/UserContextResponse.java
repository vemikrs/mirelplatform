/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ユーザーコンテキストレスポンス.
 */
@Schema(description = "ユーザーコンテキストレスポンス")
public record UserContextResponse(
    @Schema(description = "専門用語コンテキスト")
    String terminology,
    @Schema(description = "回答スタイルコンテキスト")
    String style,
    @Schema(description = "ワークフローコンテキスト")
    String workflow
) {}
