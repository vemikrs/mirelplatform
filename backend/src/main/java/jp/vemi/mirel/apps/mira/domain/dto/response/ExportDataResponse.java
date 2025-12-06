/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Mira エクスポートデータレスポンス.
 * <p>
 * ユーザー単位の会話履歴とユーザーコンテキストを含むエクスポートデータ。
 * </p>
 */
@Schema(description = "Mira エクスポートデータレスポンス")
public record ExportDataResponse(
    @Schema(description = "エクスポートメタデータ")
    ExportMetadata metadata,
    
    @Schema(description = "会話履歴リスト")
    List<ConversationExport> conversations,
    
    @Schema(description = "ユーザーコンテキスト")
    UserContextExport userContext
) {
    /**
     * エクスポートメタデータ.
     */
    @Schema(description = "エクスポートメタデータ")
    public record ExportMetadata(
        @Schema(description = "エクスポート日時")
        Instant exportedAt,
        
        @Schema(description = "ユーザーID")
        String userId,
        
        @Schema(description = "テナントID")
        String tenantId,
        
        @Schema(description = "会話数")
        int conversationCount,
        
        @Schema(description = "総メッセージ数")
        int totalMessageCount,
        
        @Schema(description = "エクスポート形式バージョン")
        String version
    ) {}
    
    /**
     * 会話エクスポート.
     */
    @Schema(description = "会話エクスポート")
    public record ConversationExport(
        @Schema(description = "会話ID")
        String conversationId,
        
        @Schema(description = "会話タイトル")
        String title,
        
        @Schema(description = "モード")
        String mode,
        
        @Schema(description = "作成日時")
        Instant createdAt,
        
        @Schema(description = "最終更新日時")
        Instant lastActivityAt,
        
        @Schema(description = "メッセージリスト")
        List<MessageExport> messages
    ) {}
    
    /**
     * メッセージエクスポート.
     */
    @Schema(description = "メッセージエクスポート")
    public record MessageExport(
        @Schema(description = "メッセージID")
        String messageId,
        
        @Schema(description = "送信者タイプ (USER/ASSISTANT/SYSTEM)")
        String senderType,
        
        @Schema(description = "コンテンツ")
        String content,
        
        @Schema(description = "コンテンツタイプ")
        String contentType,
        
        @Schema(description = "作成日時")
        Instant createdAt,
        
        @Schema(description = "メタデータ（モデル名、トークン数など）")
        Map<String, Object> metadata
    ) {}
    
    /**
     * ユーザーコンテキストエクスポート.
     */
    @Schema(description = "ユーザーコンテキストエクスポート")
    public record UserContextExport(
        @Schema(description = "専門用語コンテキスト")
        String terminology,
        
        @Schema(description = "回答スタイルコンテキスト")
        String style,
        
        @Schema(description = "ワークフローコンテキスト")
        String workflow,
        
        @Schema(description = "その他のコンテキスト（カテゴリ別）")
        Map<String, String> additionalContexts
    ) {}
}
