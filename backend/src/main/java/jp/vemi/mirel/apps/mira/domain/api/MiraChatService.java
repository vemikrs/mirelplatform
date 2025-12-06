/*
 * Copyright 2025 Vemi Inc. All rights reserved.
 */
package jp.vemi.mirel.apps.mira.domain.api;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ContextSnapshotRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ErrorReportRequest;
import jp.vemi.mirel.apps.mira.domain.dto.response.ChatResponse;
import jp.vemi.mirel.apps.mira.domain.dto.response.ContextSnapshotResponse;

/**
 * Mira チャットサービスインターフェース.
 * 
 * <p>AI アシスタント機能のコアサービスを定義します。</p>
 */
public interface MiraChatService {
    
    /**
     * チャットメッセージを処理.
     *
     * @param request チャットリクエスト
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @return チャット応答
     */
    ChatResponse chat(ChatRequest request, String tenantId, String userId);
    
    /**
     * コンテキストスナップショットを保存.
     *
     * @param request スナップショットリクエスト
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @return スナップショット応答
     */
    ContextSnapshotResponse saveContextSnapshot(
            ContextSnapshotRequest request, 
            String tenantId, 
            String userId);
    
    /**
     * エラーを分析.
     *
     * @param request エラーレポートリクエスト
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @return AI によるエラー分析結果
     */
    ChatResponse analyzeError(ErrorReportRequest request, String tenantId, String userId);
    
    /**
     * 会話履歴をクリア.
     *
     * @param conversationId 会話ID
     * @param tenantId テナントID
     * @param userId ユーザーID
     */
    void clearConversation(String conversationId, String tenantId, String userId);
    
    /**
     * 会話のステータスを取得.
     *
     * @param conversationId 会話ID
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @return 会話が存在しアクティブな場合true
     */
    boolean isConversationActive(String conversationId, String tenantId, String userId);
}
