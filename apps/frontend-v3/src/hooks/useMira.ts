/**
 * Mira AI Assistant Hooks
 * 
 * TanStack Query + Zustand を組み合わせたカスタムフック
 */
import { useMutation, useQuery } from '@tanstack/react-query';
import { useMiraStore } from '@/stores/miraStore';
import {
  sendChatMessage,
  analyzeError,
  saveContextSnapshot,
  clearConversation as clearConversationApi,
  checkMiraHealth,
  type ChatRequest,
  type ErrorReportRequest,
  type ContextSnapshotRequest,
  type MiraMode,
  type ChatContext,
} from '@/lib/api/mira';

// ========================================
// Chat Hook
// ========================================

/**
 * チャット送信フック
 */
export function useMiraChat() {
  const {
    isLoading,
    setLoading,
    setError,
    activeConversationId,
    startConversation,
    addUserMessage,
    addAssistantMessage,
    getActiveConversation,
  } = useMiraStore();
  
  const mutation = useMutation({
    mutationFn: sendChatMessage,
    onMutate: () => {
      setLoading(true);
      setError(null);
    },
    onSuccess: (response) => {
      const conversationId = activeConversationId || response.conversationId;
      addAssistantMessage(conversationId, response);
    },
    onError: (error: Error) => {
      setError(error.message);
    },
    onSettled: () => {
      setLoading(false);
    },
  });
  
  const sendMessage = async (
    content: string,
    options?: {
      mode?: MiraMode;
      context?: ChatContext;
    }
  ) => {
    // 会話がなければ新規作成
    let conversationId = activeConversationId;
    if (!conversationId) {
      conversationId = startConversation(options?.mode, options?.context);
    }
    
    // ユーザーメッセージを追加
    addUserMessage(conversationId, content);
    
    // APIリクエスト
    const request: ChatRequest = {
      conversationId: conversationId !== activeConversationId ? undefined : conversationId,
      mode: options?.mode,
      context: options?.context,
      message: { content },
    };
    
    return mutation.mutateAsync(request);
  };
  
  return {
    sendMessage,
    isLoading: mutation.isPending || isLoading,
    error: mutation.error,
    reset: mutation.reset,
  };
}

// ========================================
// Error Analysis Hook
// ========================================

/**
 * エラー分析フック
 */
export function useMiraErrorAnalysis() {
  const { setLoading, setError, openPanel } = useMiraStore();
  
  const mutation = useMutation({
    mutationFn: analyzeError,
    onMutate: () => {
      setLoading(true);
      setError(null);
      openPanel(); // エラー分析時はパネルを開く
    },
    onError: (error: Error) => {
      setError(error.message);
    },
    onSettled: () => {
      setLoading(false);
    },
  });
  
  const analyze = async (request: ErrorReportRequest) => {
    return mutation.mutateAsync(request);
  };
  
  return {
    analyze,
    isLoading: mutation.isPending,
    error: mutation.error,
    data: mutation.data,
  };
}

// ========================================
// Context Snapshot Hook
// ========================================

/**
 * コンテキストスナップショットフック
 */
export function useMiraContextSnapshot() {
  const mutation = useMutation({
    mutationFn: saveContextSnapshot,
  });
  
  const save = async (request: ContextSnapshotRequest) => {
    return mutation.mutateAsync(request);
  };
  
  return {
    save,
    isLoading: mutation.isPending,
    error: mutation.error,
  };
}

// ========================================
// Conversation Management Hook
// ========================================

/**
 * 会話管理フック
 */
export function useMiraConversation() {
  const {
    activeConversationId,
    conversations,
    setActiveConversation,
    clearConversation: clearLocal,
    deleteConversation,
    getActiveConversation,
    getConversationMessages,
  } = useMiraStore();
  
  const clearMutation = useMutation({
    mutationFn: clearConversationApi,
    onSuccess: (_, conversationId) => {
      clearLocal(conversationId);
    },
  });
  
  const clear = async (conversationId?: string) => {
    const id = conversationId || activeConversationId;
    if (id) {
      await clearMutation.mutateAsync(id);
    }
  };
  
  const remove = (conversationId?: string) => {
    const id = conversationId || activeConversationId;
    if (id) {
      deleteConversation(id);
    }
  };
  
  return {
    activeConversationId,
    conversations: Object.values(conversations),
    activeConversation: getActiveConversation(),
    messages: activeConversationId 
      ? getConversationMessages(activeConversationId) 
      : [],
    setActive: setActiveConversation,
    clear,
    remove,
    isClearLoading: clearMutation.isPending,
  };
}

// ========================================
// Health Check Hook
// ========================================

/**
 * Mira ヘルスチェックフック
 */
export function useMiraHealth() {
  return useQuery({
    queryKey: ['mira', 'health'],
    queryFn: checkMiraHealth,
    refetchInterval: 60000, // 1分ごと
    retry: 1,
  });
}

// ========================================
// Panel Control Hook
// ========================================

/**
 * パネル制御フック
 */
export function useMiraPanel() {
  const { isOpen, togglePanel, openPanel, closePanel, error, setError } = useMiraStore();
  
  return {
    isOpen,
    toggle: togglePanel,
    open: openPanel,
    close: closePanel,
    error,
    clearError: () => setError(null),
  };
}

// ========================================
// Combined Hook
// ========================================

/**
 * Mira 統合フック
 * 
 * 全機能を一括で提供
 */
export function useMira() {
  const chat = useMiraChat();
  const conversation = useMiraConversation();
  const panel = useMiraPanel();
  const errorAnalysis = useMiraErrorAnalysis();
  const contextSnapshot = useMiraContextSnapshot();
  
  return {
    // パネル
    isOpen: panel.isOpen,
    togglePanel: panel.toggle,
    openPanel: panel.open,
    closePanel: panel.close,
    
    // チャット
    sendMessage: chat.sendMessage,
    isLoading: chat.isLoading,
    error: panel.error,
    clearError: panel.clearError,
    
    // 会話
    messages: conversation.messages,
    activeConversation: conversation.activeConversation,
    conversations: conversation.conversations,
    clearConversation: conversation.clear,
    newConversation: () => conversation.setActive(null),
    
    // エラー分析
    analyzeError: errorAnalysis.analyze,
    
    // コンテキスト
    saveContext: contextSnapshot.save,
  };
}
