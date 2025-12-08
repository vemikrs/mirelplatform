/**
 * Mira AI Assistant Hooks
 * 
 * TanStack Query + Zustand を組み合わせたカスタムフック
 */
import { useCallback } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useMiraStore } from '@/stores/miraStore';
import {
  sendChatMessage,
  analyzeError,
  saveContextSnapshot,
  clearConversation as clearConversationApi,
  checkMiraHealth,
  generateConversationTitle,
  updateConversationTitle as updateConversationTitleApi,
  type ChatRequest,
  type ErrorReportRequest,
  type ContextSnapshotRequest,
  type MiraMode,
  type ChatContext,
  type MessageConfig,
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
    conversations,
    startConversation,
    addUserMessage,
    addAssistantMessage,
    updateConversationTitle,
  } = useMiraStore();
  
  const mutation = useMutation({
    mutationFn: sendChatMessage,
    onMutate: () => {
      setLoading(true);
      setError(null);
    },
    onSuccess: async (response) => {
      const conversationId = activeConversationId || response.conversationId;
      addAssistantMessage(conversationId, response);
      
      // タイトル未設定の会話で最初の応答後にタイトル生成
      const conversation = conversations[conversationId];
      if (conversation && !conversation.title && conversation.messages.length >= 2) {
        try {
          // 最初の2-4メッセージを使用
          const messagesToUse = conversation.messages.slice(0, 4).map(m => ({
            role: m.role as 'user' | 'assistant',
            content: m.content,
          }));
          
          const titleResponse = await generateConversationTitle({
            conversationId,
            messages: messagesToUse,
          });
          
          if (titleResponse.success && titleResponse.title) {
            updateConversationTitle(conversationId, titleResponse.title);
          }
        } catch (error) {
          // タイトル生成失敗はサイレントに無視
          console.warn('タイトル生成失敗:', error);
        }
      }
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
      messageConfig?: MessageConfig;
      forceProvider?: string;
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
      context: {
        ...options?.context,
        messageConfig: options?.messageConfig,
      },
      message: { content },
      forceProvider: options?.forceProvider,
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
    fetchConversations,
    loadConversation,
    hasMore,
    currentPage,
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

  // Initial fetch
  // コンポーネントマウント時に一度だけ実行
  /* useEffect(() => {
    // 既にデータがあれば取得しない、等の制御も可能だが、
    // 同期のために取得を推奨
    fetchConversations(0);
   }, []); */
  // NOTE: useMiraConversationが複数箇所で呼ばれると多重リクエストになる可能性があるため、
  // ここでの自動取得は避けるか、Store側で制御が必要。
  // 一旦、明示的に呼び出すか、上位コンポーネント(MiraPage)で制御する方針とする。
  
  return {
    activeConversationId,
    conversations: Object.values(conversations).sort((a, b) => 
      new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    ),
    activeConversation: getActiveConversation(),
    messages: activeConversationId 
      ? getConversationMessages(activeConversationId) 
      : [],
    setActive: setActiveConversation,
    clear,
    remove,
    isClearLoading: clearMutation.isPending,
    fetchConversations,
    loadConversation,
    hasMore,
    currentPage,
  };
}

// ========================================
// Title Update Hook
// ========================================

/**
 * タイトル更新フック
 */
export function useMiraTitleUpdate() {
  const { updateConversationTitle } = useMiraStore();
  
  const mutation = useMutation({
    mutationFn: updateConversationTitleApi,
    onSuccess: (response) => {
      if (response.success) {
        updateConversationTitle(response.conversationId, response.title);
      }
    },
  });
  
  const updateTitle = async (conversationId: string, title: string) => {
    await mutation.mutateAsync({ conversationId, title });
  };
  
  return {
    updateTitle,
    isUpdating: mutation.isPending,
    error: mutation.error,
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
  const titleUpdate = useMiraTitleUpdate();
  const {
    editingMessageId,
    editingMessageContent,
    startEditMessage,
    cancelEditMessage,
    resendEditedMessage,
  } = useMiraStore();
  
  const loadMoreConversations = useCallback(async () => {
    if (conversation.hasMore) {
      await conversation.fetchConversations(conversation.currentPage + 1);
    }
  }, [conversation.hasMore, conversation.currentPage, conversation.fetchConversations]);

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
    
    // タイトル更新
    updateConversationTitle: titleUpdate.updateTitle,
    isUpdatingTitle: titleUpdate.isUpdating,
    
    // メッセージ編集
    editingMessageId,
    editingMessageContent,
    startEditMessage,
    cancelEditMessage,
    resendEditedMessage,
    
    // エラー分析
    analyzeError: errorAnalysis.analyze,
    
    // コンテキスト
    saveContext: contextSnapshot.save,

    // ページング・同期
    fetchConversations: conversation.fetchConversations,
    loadConversation: conversation.loadConversation,
    hasMore: conversation.hasMore,
    currentPage: conversation.currentPage,
    loadMoreConversations,
  };
}
