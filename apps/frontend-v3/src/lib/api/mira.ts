/**
 * Mira AI Assistant API Client
 * 
 * チャット・コンテキストヘルプ・エラー分析機能のAPI呼び出し
 */
import { apiClient } from './client';

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
  forceProvider?: string;
  /** 強制モデル指定 */
  forceModel?: string;
  /** Web検索を有効化 */
  webSearchEnabled?: boolean;
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
  /** メッセージ送信設定 */
  messageConfig?: MessageConfig;
}

/**
 * メッセージ送信設定
 */
export interface MessageConfig {
  historyScope?: 'auto' | 'recent' | 'none';
  recentCount?: number;
  contextOverrides?: Record<string, { enabled: boolean; priority: number }>;
  additionalPresets?: string[];
  temporaryContext?: string;
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

/**
 * タイトル生成リクエスト
 */
export interface GenerateTitleRequest {
  conversationId: string;
  messages: Array<{
    role: 'user' | 'assistant';
    content: string;
  }>;
}

/**
 * タイトル生成レスポンス
 */
export interface GenerateTitleResponse {
  conversationId: string;
  title: string;
  success: boolean;
  errorMessage?: string;
}

export interface MiraTitleApiResponse {
  data: GenerateTitleResponse | null;
  errors: string[];
}

/**
 * エクスポートデータレスポンス
 */
export interface ExportDataResponse {
  metadata: {
    exportedAt: string;
    userId: string;
    tenantId: string;
    conversationCount: number;
    totalMessageCount: number;
    version: string;
  };
  conversations: Array<{
    conversationId: string;
    title: string | null;
    mode: string | null;
    createdAt: string;
    lastActivityAt: string;
    messages: Array<{
      messageId: string;
      senderType: string;
      content: string;
      contentType: string;
      createdAt: string;
      metadata: Record<string, unknown>;
    }>;
  }>;
  userContext: {
    terminology: string;
    style: string;
    workflow: string;
    additionalContexts: Record<string, string>;
  };
}

export interface MiraExportApiResponse {
  data: ExportDataResponse | null;
  errors: string[];
}

/**
 * タイトル更新リクエスト
 */
export interface UpdateTitleRequest {
  conversationId: string;
  title: string;
}

/**
 * タイトル更新レスポンス
 */
export interface UpdateTitleResponse {
  conversationId: string;
  title: string;
  success: boolean;
  errorMessage?: string;
}

export interface MiraUpdateTitleApiResponse {
  data: UpdateTitleResponse | null;
  errors: string[];
}

/**
 * 設定推奨リクエスト
 */
export interface SuggestConfigRequest {
  messageContent: string;
}

export interface MiraSuggestConfigApiResponse {
  data: MessageConfig | null;
  errors: string[];
}

/**
 * 会話一覧レスポンス
 */
export interface ConversationListResponse {
  conversations: Array<{
    id: string;
    title: string;
    mode: MiraMode;
    createdAt: string;
    lastActivityAt: string;
  }>;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MiraConversationListApiResponse {
  data: ConversationListResponse | null;
  errors: string[];
}

/**
 * 会話詳細レスポンス
 */
export interface ConversationDetailResponse {
  id: string;
  title: string;
  mode: MiraMode;
  createdAt: string;
  lastActivityAt: string;
  messages: Array<{
    id: string;
    role: string;
    content: string;
    contentType: string;
    createdAt: string;
  }>;
}

export interface MiraConversationDetailApiResponse {
  data: ConversationDetailResponse | null;
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

/**
 * 会話タイトル生成
 */
export async function generateConversationTitle(
  request: GenerateTitleRequest
): Promise<GenerateTitleResponse> {
  const response = await apiClient.post<MiraTitleApiResponse>(
    '/apps/mira/api/conversation/generate-title',
    { model: request }
  );
  
  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }
  
  if (!response.data.data) {
    throw new Error('タイトル生成に失敗しました');
  }
  
  return response.data.data;
}

/**
 * ユーザーデータエクスポート
 */
export async function exportUserData(): Promise<ExportDataResponse> {
  const response = await apiClient.get<MiraExportApiResponse>(
    '/apps/mira/api/export'
  );
  
  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }
  
  if (!response.data.data) {
    throw new Error('エクスポートに失敗しました');
  }
  
  return response.data.data;
}

/**
 * 会話タイトル更新
 */
export async function updateConversationTitle(
  request: UpdateTitleRequest
): Promise<UpdateTitleResponse> {
  const response = await apiClient.put<MiraUpdateTitleApiResponse>(
    '/apps/mira/api/conversation/update-title',
    { model: request }
  );
  
  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }
  
  if (!response.data.data) {
    throw new Error('タイトル更新に失敗しました');
  }
  
  return response.data.data;
}

/**
 * 設定推奨取得
 */
export async function suggestConfig(
  request: SuggestConfigRequest
): Promise<MessageConfig> {
  const response = await apiClient.post<MiraSuggestConfigApiResponse>(
    '/apps/mira/api/suggest-config',
    { model: request }
  );
  
  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }
  
  if (!response.data.data) {
    throw new Error('推奨設定の取得に失敗しました');
  }
  
  return response.data.data;
}


/**
 * 会話一覧取得
 */
export async function getConversationList(params?: {
  page?: number;
  size?: number;
}): Promise<ConversationListResponse> {
  const response = await apiClient.get<MiraConversationListApiResponse>(
    '/apps/mira/api/conversations',
    { params }
  );

  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }

  if (!response.data.data) {
    throw new Error('会話一覧の取得に失敗しました');
  }

  return response.data.data;
}

/**
 * 会話詳細取得
 */
export async function getConversation(
  conversationId: string
): Promise<ConversationDetailResponse> {
  const response = await apiClient.get<MiraConversationDetailApiResponse>(
    `/apps/mira/api/conversations/${conversationId}`
  );

  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }

  if (!response.data.data) {
    throw new Error('会話詳細の取得に失敗しました');
  }

  return response.data.data;
}

/**
 * 会話タイトル再生成
 */
export async function regenerateConversationTitle(
  conversationId: string
): Promise<GenerateTitleResponse> {
  const response = await apiClient.post<MiraTitleApiResponse>(
    `/apps/mira/api/conversation/${conversationId}/regenerate-title`
  );

  if (response.data.errors?.length > 0) {
    throw new Error(response.data.errors[0]);
  }

  if (!response.data.data) {
    throw new Error('タイトルの再生成に失敗しました');
  }

  return response.data.data;
}

// ========================================
// Model Management (Phase 3)
// ========================================

/**
 * モデル情報
 */
export interface ModelInfo {
  id: string;
  provider: string;
  modelName: string;
  displayName: string;
  description?: string;
  capabilities: string[];
  maxTokens?: number;
  contextWindow?: number;
  isActive: boolean;
  isRecommended: boolean;
  isExperimental: boolean;
}

/**
 * プロバイダ情報
 */
export interface ProviderInfo {
  name: string;
  displayName: string;
  available: boolean;
}

/**
 * プロバイダ一覧取得（管理者向け）
 */
export async function getProviders(): Promise<ProviderInfo[]> {
  const response = await apiClient.get<ProviderInfo[]>(
    '/apps/mira/api/admin/providers'
  );
  return response.data || [];
}

/**
 * モデル一覧取得（管理者向け）
 */
export async function getModels(provider?: string): Promise<ModelInfo[]> {
  const params = provider ? { provider } : {};
  const response = await apiClient.get<ModelInfo[]>(
    '/apps/mira/api/admin/models',
    { params }
  );
  return response.data || [];
}

/**
 * 利用可能モデル取得（ユーザー向け）
 */
export async function getAvailableModels(): Promise<ModelInfo[]> {
  const response = await apiClient.get<ModelInfo[]>(
    '/apps/mira/api/available-models'
  );
  return response.data || [];
}
