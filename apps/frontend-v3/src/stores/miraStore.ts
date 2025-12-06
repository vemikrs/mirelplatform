/**
 * Mira AI Assistant Store
 * 
 * チャット会話状態をZustandで管理
 */
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { ChatResponse, MiraMode, ChatContext } from '@/lib/api/mira';

// ========================================
// Types
// ========================================

/**
 * メッセージ
 */
export interface MiraMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  contentType: 'text' | 'markdown' | 'html';
  timestamp: Date;
  metadata?: {
    provider?: string;
    model?: string;
    latencyMs?: number;
  };
}

/**
 * 会話
 */
export interface MiraConversation {
  id: string;
  mode: MiraMode;
  messages: MiraMessage[];
  context?: ChatContext;
  createdAt: Date;
  updatedAt: Date;
}

/**
 * Mira Store State
 */
interface MiraState {
  // 状態
  isOpen: boolean;
  isFullscreen: boolean;
  isLoading: boolean;
  activeConversationId: string | null;
  conversations: Record<string, MiraConversation>;
  error: string | null;
  
  // アクション
  togglePanel: () => void;
  openPanel: () => void;
  closePanel: () => void;
  toggleFullscreen: () => void;
  setFullscreen: (fullscreen: boolean) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  
  // 会話管理
  startConversation: (mode?: MiraMode, context?: ChatContext) => string;
  setActiveConversation: (conversationId: string | null) => void;
  addUserMessage: (conversationId: string, content: string) => void;
  addAssistantMessage: (conversationId: string, response: ChatResponse) => void;
  updateConversationContext: (conversationId: string, context: ChatContext) => void;
  clearConversation: (conversationId: string) => void;
  deleteConversation: (conversationId: string) => void;
  
  // ヘルパー
  getActiveConversation: () => MiraConversation | null;
  getConversationMessages: (conversationId: string) => MiraMessage[];
}

// ========================================
// Store
// ========================================

export const useMiraStore = create<MiraState>()(
  persist(
    (set, get) => ({
      // 初期状態
      isOpen: false,
      isFullscreen: false,
      isLoading: false,
      activeConversationId: null,
      conversations: {},
      error: null,
      
      // パネル制御
      togglePanel: () => set((state) => ({ isOpen: !state.isOpen })),
      openPanel: () => set({ isOpen: true }),
      closePanel: () => set({ isOpen: false, isFullscreen: false }),
      toggleFullscreen: () => set((state) => ({ isFullscreen: !state.isFullscreen, isOpen: true })),
      setFullscreen: (fullscreen) => set({ isFullscreen: fullscreen, isOpen: true }),
      
      // ローディング・エラー
      setLoading: (loading) => set({ isLoading: loading }),
      setError: (error) => set({ error }),
      
      // 会話管理
      startConversation: (mode = 'GENERAL_CHAT', context) => {
        const id = crypto.randomUUID();
        const now = new Date();
        
        const conversation: MiraConversation = {
          id,
          mode,
          messages: [],
          context,
          createdAt: now,
          updatedAt: now,
        };
        
        set((state) => ({
          conversations: {
            ...state.conversations,
            [id]: conversation,
          },
          activeConversationId: id,
        }));
        
        return id;
      },
      
      setActiveConversation: (conversationId) => 
        set({ activeConversationId: conversationId }),
      
      addUserMessage: (conversationId, content) => {
        const message: MiraMessage = {
          id: crypto.randomUUID(),
          role: 'user',
          content,
          contentType: 'text',
          timestamp: new Date(),
        };
        
        set((state) => {
          const conversation = state.conversations[conversationId];
          if (!conversation) return state;
          
          return {
            conversations: {
              ...state.conversations,
              [conversationId]: {
                ...conversation,
                messages: [...conversation.messages, message],
                updatedAt: new Date(),
              },
            },
          };
        });
      },
      
      addAssistantMessage: (conversationId, response) => {
        const message: MiraMessage = {
          id: response.messageId || crypto.randomUUID(),
          role: 'assistant',
          content: response.assistantMessage.content,
          contentType: response.assistantMessage.contentType,
          timestamp: new Date(),
          metadata: response.metadata,
        };
        
        set((state) => {
          const conversation = state.conversations[conversationId];
          if (!conversation) return state;
          
          // サーバーからの会話IDで更新
          const updatedId = response.conversationId || conversationId;
          
          const updatedConversation: MiraConversation = {
            ...conversation,
            id: updatedId,
            messages: [...conversation.messages, message],
            updatedAt: new Date(),
          };
          
          // 会話IDが変わった場合は古いIDを削除
          const newConversations = { ...state.conversations };
          if (updatedId !== conversationId) {
            delete newConversations[conversationId];
          }
          newConversations[updatedId] = updatedConversation;
          
          return {
            conversations: newConversations,
            activeConversationId: updatedId,
          };
        });
      },
      
      updateConversationContext: (conversationId, context) => {
        set((state) => {
          const conversation = state.conversations[conversationId];
          if (!conversation) return state;
          
          return {
            conversations: {
              ...state.conversations,
              [conversationId]: {
                ...conversation,
                context: { ...conversation.context, ...context },
                updatedAt: new Date(),
              },
            },
          };
        });
      },
      
      clearConversation: (conversationId) => {
        set((state) => {
          const conversation = state.conversations[conversationId];
          if (!conversation) return state;
          
          return {
            conversations: {
              ...state.conversations,
              [conversationId]: {
                ...conversation,
                messages: [],
                updatedAt: new Date(),
              },
            },
          };
        });
      },
      
      deleteConversation: (conversationId) => {
        set((state) => {
          const newConversations = { ...state.conversations };
          delete newConversations[conversationId];
          
          return {
            conversations: newConversations,
            activeConversationId: 
              state.activeConversationId === conversationId 
                ? null 
                : state.activeConversationId,
          };
        });
      },
      
      // ヘルパー
      getActiveConversation: () => {
        const { activeConversationId, conversations } = get();
        if (!activeConversationId) return null;
        return conversations[activeConversationId] || null;
      },
      
      getConversationMessages: (conversationId) => {
        const conversation = get().conversations[conversationId];
        return conversation?.messages || [];
      },
    }),
    {
      name: 'mira-store',
      partialize: (state) => ({
        // パネル状態と会話のみ永続化（isFullscreenは永続化しない）
        isOpen: state.isOpen,
        conversations: state.conversations,
        activeConversationId: state.activeConversationId,
      }),
    }
  )
);
