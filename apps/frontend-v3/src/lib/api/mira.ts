/**
 * Mira AI Assistant API Client
 * 
 * チャット・コンテキストヘルプ・エラー分析機能のAPI呼び出し
 */
import { apiClient, createApiRequest } from './client';
import type { ApiResponse } from './types';

// ========================================
// Types
// ========================================

/**
 * チャットリクエスト
 */
export interface ChatRequest {
  conversationId?: string;
  mode?: MiraMode;
  context?: ChatContext;
  message: {
    content: string;
  };
}

/**
 * チャットコンテキスト
 */
export interface ChatContext {
  appId?: string;
  screenId?: string;
  systemRole?: string;
  appRole?: string;
  payload?: Record<string, unknown>;
}

/**
 * Mira動作モード
 */
export type MiraMode = 
  | 'GENERAL_CHAT'
  | 'CONTEXT_HELP'
  | 'ERROR_ANALYZE'
  | 'STUDIO_AGENT'
  | 'WORKFLOW_AGENT';

/**
 * チャットレスポンス
 */
export interface ChatResponse {
  conversationId: string;
  messageId?: string;
  mode: string;
  assistantMessage: {
    content: string;
    contentType: 'markdown' | 'text' | 'html';
  };
  metadata?: {
    provider?: string;
    model?: string;
    latencyMs?: number;
    promptTokens?: number;
    completionTokens?: number;
  };
}

/**
 * コンテキストスナップショットリクエスト
 */
export interface ContextSnapshotRequest {
  appId: string;
  screenId: string;
  systemRole?: string;
  appRole?: string;
  payload?: Record<string, unknown>;
}

/**
 * コンテキストスナップショットレスポンス
 */
export interface ContextSnapshotResponse {
  snapshotId: string;
  createdAt: string;
}

/**
 * エラーレポートリクエスト
 */
export interface ErrorReportRequest {
  source: string;
  code?: string;
  message: string;
  detail?: string;
  context?: {
    appId?: string;
    screenId?: string;
    payload?: Record<string, unknown>;
  };
}

/**
 * Mira APIレスポンスラッパー
 */
export interface MiraChatApiResponse {
  data: ChatResponse | null;
  errors: string[];
}

export interface MiraSnapshotApiResponse {
  data: ContextSnapshotResponse | null;
  errors: string[];
}

// ========================================
// API Functions
// ========================================

/**
 * チャットメッセージ送信
 */
export async function sendChatMessage(request: ChatRequest): Promise<ChatResponse> {
  const response = await apiClient.post<MiraChatApiResponse>(
    '/apps/mira/api/chat',
    { model: request }
  );
  
  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }
  
  if (!response.data.data) {
    throw new Error('レスポンスデータがありません');
  }
  
  return response.data.data;
}

/**
 * コンテキストスナップショット保存
 */
export async function saveContextSnapshot(
  request: ContextSnapshotRequest
): Promise<ContextSnapshotResponse> {
  const response = await apiClient.post<MiraSnapshotApiResponse>(
    '/apps/mira/api/context-snapshot',
    { model: request }
  );
  
  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }
  
  if (!response.data.data) {
    throw new Error('スナップショット保存に失敗しました');
  }
  
  return response.data.data;
}

/**
 * エラー分析リクエスト
 */
export async function analyzeError(request: ErrorReportRequest): Promise<ChatResponse> {
  const response = await apiClient.post<MiraChatApiResponse>(
    '/apps/mira/api/error-report',
    { model: request }
  );
  
  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }
  
  if (!response.data.data) {
    throw new Error('エラー分析に失敗しました');
  }
  
  return response.data.data;
}

/**
 * 会話クリア
 */
export async function clearConversation(conversationId: string): Promise<void> {
  await apiClient.delete(`/apps/mira/api/conversation/${conversationId}`);
}

/**
 * 会話ステータス取得
 */
export async function getConversationStatus(
  conversationId: string
): Promise<{ conversationId: string; active: boolean }> {
  const response = await apiClient.get<{ conversationId: string; active: boolean }>(
    `/apps/mira/api/conversation/${conversationId}/status`
  );
  return response.data;
}

/**
 * ヘルスチェック
 */
export async function checkMiraHealth(): Promise<{
  status: string;
  service: string;
  timestamp: string;
}> {
  const response = await apiClient.get<{
    status: string;
    service: string;
    timestamp: string;
  }>('/apps/mira/api/health');
  return response.data;
}
