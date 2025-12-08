/**
 * Mira AI Assistant Store
 * 
 * チャット会話状態をZustandで管理
 */
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { 
  getConversationList, 
  getConversation,
  type ChatResponse, 
  type MiraMode, 
  type ChatContext 
} from '@/lib/api/mira';

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
  title?: string; // AI生成または初期メッセージから自動生成
  messages: MiraMessage[];
  context?: ChatContext;
  createdAt: Date;
  updatedAt: Date;
  isLoaded?: boolean; // 詳細（メッセージ）が読み込まれているか
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
  
  // ページング状態
  hasMore: boolean;
  currentPage: number;

  // メッセージ編集状態
  editingMessageId: string | null;
  editingMessageContent: string | null;
  
  // アクション
  togglePanel: () => void;
  openPanel: () => void;
  closePanel: () => void;
  toggleFullscreen: () => void;
  setFullscreen: (fullscreen: boolean) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  
  // 会話管理
  fetchConversations: (page?: number, size?: number) => Promise<void>;
  loadConversation: (conversationId: string) => Promise<void>;
  startConversation: (mode?: MiraMode, context?: ChatContext) => string;
  setActiveConversation: (conversationId: string | null) => void;
  updateConversationTitle: (conversationId: string, title: string) => void;
  addUserMessage: (conversationId: string, content: string) => void;
  addAssistantMessage: (conversationId: string, response: ChatResponse) => void;
  updateConversationContext: (conversationId: string, context: ChatContext) => void;
  clearConversation: (conversationId: string) => void;
  deleteConversation: (conversationId: string) => void;
  
  // メッセージ編集
  startEditMessage: (conversationId: string, messageId: string) => void;
  cancelEditMessage: () => void;
  resendEditedMessage: (conversationId: string, messageId: string) => void;
  
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
      hasMore: false,
      currentPage: 0,
      
      // メッセージ編集初期状態
      editingMessageId: null,
      editingMessageContent: null,
      
      // パネル制御
      togglePanel: () => set((state) => ({ isOpen: !state.isOpen })),
      openPanel: () => set({ isOpen: true }),
      closePanel: () => set({ isOpen: false, isFullscreen: false }),
      toggleFullscreen: () => set((state) => ({ isFullscreen: !state.isFullscreen, isOpen: true })),
      setFullscreen: (fullscreen) => set({ isFullscreen: fullscreen, isOpen: true }),
      
      // ローディング・エラー
      setLoading: (loading) => set({ isLoading: loading }),
      setError: (error) => set({ error }),
      
      // 会話取得（リスト）
      fetchConversations: async (page = 0, size = 20) => {
        set({ isLoading: true });
        try {
          const response = await getConversationList({ page, size });
          
          set((state) => {
            const newConversations = { ...state.conversations };
            
            response.conversations.forEach((c) => {
              // 既存の会話があれば維持（詳細情報があるかもしれないので）
              // ただし、タイトルなどは更新しても良い
              const existing = newConversations[c.id];
              
              if (existing) {
                newConversations[c.id] = {
                  ...existing,
                  title: c.title,
                  updatedAt: new Date(c.lastActivityAt),
                  mode: c.mode,
                };
              } else {
                newConversations[c.id] = {
                  id: c.id,
                  mode: c.mode,
                  title: c.title,
                  messages: [],
                  createdAt: new Date(c.createdAt),
                  updatedAt: new Date(c.lastActivityAt),
                  isLoaded: false, // 詳細未取得
                };
              }
            });

            return {
              conversations: newConversations,
              hasMore: page < response.totalPages - 1,
              currentPage: page,
              isLoading: false,
            };
          });
        } catch (error) {
          set({ 
            error: error instanceof Error ? error.message : '会話履歴の取得に失敗しました',
            isLoading: false 
          });
        }
      },

      // 会話詳細取得
      loadConversation: async (conversationId) => {
        // 既にロード済みならスキップするロジックを入れても良いが、最新化のため毎回呼ぶ
        set({ isLoading: true });
        try {
          const response = await getConversation(conversationId);
          
          set((state) => {
            const messages: MiraMessage[] = response.messages.map(m => ({
              id: m.id,
              role: m.role as 'user' | 'assistant',
              content: m.content,
              contentType: m.contentType as 'text' | 'markdown' | 'html',
              timestamp: new Date(m.createdAt),
            }));

            const updatedConversation: MiraConversation = {
              id: response.id,
              mode: response.mode,
              title: response.title,
              messages,
              createdAt: new Date(response.createdAt),
              updatedAt: new Date(response.lastActivityAt),
              isLoaded: true,
            };

            return {
              conversations: {
                ...state.conversations,
                [response.id]: updatedConversation,
              },
              isLoading: false,
            };
          });
        } catch (error) {
          set({ 
             error: error instanceof Error ? error.message : '会話詳細の取得に失敗しました',
             isLoading: false
          });
        }
      },

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
          isLoaded: true, // 新規作成なのでロード済み扱い
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
      
      setActiveConversation: (conversationId) => {
        set({ activeConversationId: conversationId });
        
        // 未ロードならロードする
        if (conversationId) {
          const state = get();
          const conversation = state.conversations[conversationId];
          if (conversation && !conversation.isLoaded) {
            state.loadConversation(conversationId);
          }
        }
      },
      
      updateConversationTitle: (conversationId, title) => {
        set((state) => {
          const conversation = state.conversations[conversationId];
          if (!conversation) return state;
          
          return {
            conversations: {
              ...state.conversations,
              [conversationId]: {
                ...conversation,
                title,
                updatedAt: new Date(),
              },
            },
          };
        });
      },
      
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
      
      // メッセージ編集
      startEditMessage: (conversationId, messageId) => {
        const conversation = get().conversations[conversationId];
        if (!conversation) return;
        
        const message = conversation.messages.find(m => m.id === messageId);
        if (!message || message.role !== 'user') return;
        
        set({
          editingMessageId: messageId,
          editingMessageContent: message.content,
          activeConversationId: conversationId,
        });
      },
      
      cancelEditMessage: () => {
        set({
          editingMessageId: null,
          editingMessageContent: null,
        });
      },
      
      resendEditedMessage: (conversationId, messageId) => {
        const conversation = get().conversations[conversationId];
        if (!conversation) return;
        
        // 編集点以降のメッセージを削除
        const messageIndex = conversation.messages.findIndex(m => m.id === messageId);
        if (messageIndex === -1) return;
        
        const updatedMessages = conversation.messages.slice(0, messageIndex);
        
        set((state) => ({
          conversations: {
            ...state.conversations,
            [conversationId]: {
              ...conversation,
              messages: updatedMessages,
              updatedAt: new Date(),
            },
          },
          editingMessageId: null,
          editingMessageContent: null,
        }));
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
