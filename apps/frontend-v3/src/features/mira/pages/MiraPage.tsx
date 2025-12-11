/**
 * Mira AI Assistant - 専用ページ
 * 
 * チャット履歴の一覧と汎用AIチャット機能を提供する専用画面。
 * シンプルな1カラムレイアウト + スライドインドロワー:
 * - 左ドロワー: 会話履歴リスト（トグル表示）
 * - 中央: チャットエリア（メイン）
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Button,
  ScrollArea,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  toast,
} from '@mirel/ui';
import { 
  Sparkles,
} from 'lucide-react';
import { useMira } from '@/hooks/useMira';
import { useMiraStore } from '@/stores/miraStore';
import { exportUserData, type MiraMode, type MessageConfig } from '@/lib/api/mira';
import { MiraHeader } from '../components/MiraHeader';
import { MiraChatMessage } from '../components/MiraChatMessage';
import { MiraChatInput, type MiraChatInputHandle } from '../components/MiraChatInput';
import { MiraConversationList } from '../components/MiraConversationList';
import { MiraKeyboardShortcuts } from '../components/MiraKeyboardShortcuts';
import { MiraUserContextEditor } from '../components/MiraUserContextEditor';
import { MiraDeleteConfirmDialog } from '../components/MiraDeleteConfirmDialog';
import { useMiraShortcuts } from '../hooks/useMiraShortcuts';

export function MiraPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const {
    sendMessage,
    isLoading,
    error,
    clearError,
    messages,
    activeConversation,
    conversations,
    clearConversation,
    newConversation,
    editingMessageId,
    editingMessageContent,
    startEditMessage,
    cancelEditMessage,
    resendEditedMessage,
    updateConversationTitle,
    isUpdatingTitle,
    loadMoreConversations,
    hasMore,
    fetchConversations,
    regenerateTitle,
  } = useMira();
  
  const setActiveConversation = useMiraStore((state) => state.setActiveConversation);
  const deleteConversation = useMiraStore((state) => state.deleteConversation);
  const storedConversations = useMiraStore((state) => state.conversations);
  
  // サイドバーの表示状態（localStorageから復元、デフォルトは非表示）
  const [isSidebarOpen, setIsSidebarOpen] = useState(() => {
    const saved = localStorage.getItem('mira-sidebar-open');
    return saved !== null ? saved === 'true' : false;
  });

  // サイドバー状態の変更をlocalStorageに保存
  useEffect(() => {
    localStorage.setItem('mira-sidebar-open', String(isSidebarOpen));
  }, [isSidebarOpen]);
  
  // キーボードショートカットオーバーレイの状態
  const [showKeyboardShortcuts, setShowKeyboardShortcuts] = useState(false);
  
  // 会話クリア確認ダイアログの状態
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  
  // エクスポート処理状態
  const [isExporting, setIsExporting] = useState(false);
  
  // メッセージ編集確認ダイアログの状態
  const [showEditConfirm, setShowEditConfirm] = useState(false);
  const [pendingEditMessageId, setPendingEditMessageId] = useState<string | null>(null);
  const [affectedMessagesCount, setAffectedMessagesCount] = useState(0);

  // コンテキストエディタの表示状態
  const [isContextEditorOpen, setIsContextEditorOpen] = useState(false);
  
  // 初期ロード：会話履歴を取得
  useEffect(() => {
    fetchConversations(0);
  }, [fetchConversations]);
  
  // URLクエリパラメータから会話IDを取得して開く、または新規チャットを開始する
  useEffect(() => {
    const conversationId = searchParams.get('conversation');
    const isNew = searchParams.get('new') === 'true';

    if (isNew) {
      // 明示的に新規チャットを開始
      setActiveConversation(null);
      setSearchParams({}, { replace: true });
    } else if (conversationId && storedConversations[conversationId]) {
      setActiveConversation(conversationId);
      // URLパラメータをクリア（履歴を汚さないため）
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, setSearchParams, storedConversations, setActiveConversation]);
  
  // 現在のモードはアクティブな会話から取得
  const currentMode = activeConversation?.mode ?? 'GENERAL_CHAT';
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messageRefs = useRef<Map<string, HTMLDivElement>>(new Map());
  
  // 現在選択中のメッセージインデックス（j/kナビゲーション用）
  const [selectedMessageIndex, setSelectedMessageIndex] = useState(-1);

  // 検索入力への参照
  const searchInputRef = useRef<HTMLInputElement>(null);
  
  // チャット入力への参照
  const chatInputRef = useRef<MiraChatInputHandle>(null);

  // 新規会話作成ハンドラ（useEffectより前に定義）
  const handleNewConversation = useCallback(() => {
    newConversation();
  }, [newConversation]);
  
  // エクスポート処理ハンドラ
  const handleExport = useCallback(async () => {
    try {
      setIsExporting(true);
      const data = await exportUserData();
      
      // JSON形式でダウンロード
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `mira-export-${new Date().toISOString().split('T')[0]}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      
      toast({
        title: 'エクスポート完了',
        description: `${data.metadata.conversationCount}件の会話をエクスポートしました`,
      });
    } catch (error) {
      console.error('エクスポートエラー:', error);
      toast({
        title: 'エクスポート失敗',
        description: error instanceof Error ? error.message : 'エクスポート中にエラーが発生しました',
        variant: 'destructive',
      });
    } finally {
      setIsExporting(false);
    }
  }, []);
  
  // 選択中メッセージへスクロール
  const scrollToMessage = useCallback((index: number) => {
    if (messages[index]) {
      const messageEl = messageRefs.current.get(messages[index].id);
      messageEl?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [messages]);
  
  // メッセージ編集ハンドラ（useEffectより前に定義）
  const handleEditMessage = useCallback((messageId: string) => {
    if (!activeConversation?.id) return;
    
    // 編集点以降のメッセージ数をカウント
    const messageIndex = messages.findIndex(m => m.id === messageId);
    if (messageIndex === -1) return;
    
    const affectedCount = messages.length - messageIndex - 1;
    
    if (affectedCount > 0) {
      // 以降にメッセージがある場合は警告ダイアログを表示
      setPendingEditMessageId(messageId);
      setAffectedMessagesCount(affectedCount);
      setShowEditConfirm(true);
    } else {
      // 最新メッセージの場合は直ちに編集モードへ
      startEditMessage(activeConversation.id, messageId);
    }
  }, [activeConversation, messages, startEditMessage]);
  
  // キーボードショートカット（カスタムフックに委譲）
  useMiraShortcuts({
    isSidebarOpen,
    setIsSidebarOpen,
    handleNewConversation,
    messages,
    selectedMessageIndex,
    setSelectedMessageIndex,
    scrollToMessage,
    handleEditMessage,
    setShowKeyboardShortcuts,
    chatInputRef,
    searchInputRef,
  });
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // メッセージ送信ハンドラ
  const handleSend = useCallback((message: string, mode?: MiraMode, config?: MessageConfig) => {
    if (editingMessageId && activeConversation?.id) {
      // 編集モードでの再送信
      resendEditedMessage(activeConversation.id, editingMessageId);
      // 編集後のメッセージで新規送信
      sendMessage(message, { mode, messageConfig: config });
    } else {
      // 通常の送信
      sendMessage(message, { mode, messageConfig: config });
    }
  }, [sendMessage, editingMessageId, activeConversation, resendEditedMessage]);
  
  // 編集確認ハンドラ
  const handleConfirmEdit = useCallback(() => {
    if (activeConversation?.id && pendingEditMessageId) {
      startEditMessage(activeConversation.id, pendingEditMessageId);
    }
    setShowEditConfirm(false);
    setPendingEditMessageId(null);
  }, [activeConversation, pendingEditMessageId, startEditMessage]);
  
  const handleCancelConfirmEdit = useCallback(() => {
    setShowEditConfirm(false);
    setPendingEditMessageId(null);
  }, []);
  
  // 会話選択ハンドラ
  const handleSelectConversation = useCallback((conversationId: string) => {
    setActiveConversation(conversationId);
  }, [setActiveConversation]);
  
  // 会話削除ハンドラ
  const handleDeleteConversation = useCallback((conversationId: string) => {
    deleteConversation(conversationId);
  }, [deleteConversation]);
  
  // 会話切り替え時にフォーカス
  useEffect(() => {
    if (activeConversation?.id) {
       // 少し遅延を入れてレンダリング完了後にフォーカス
       setTimeout(() => {
         chatInputRef.current?.focus();
       }, 50);
    }
  }, [activeConversation?.id]); // IDが変わったときのみ発火
  
  // 会話の要約を取得（タイトル優先、なければ最初のメッセージ）
  const getConversationSummary = useCallback(() => {
    if (!activeConversation) return 'Mira';
    // タイトルがあればそれを使用
    if (activeConversation.title) {
      return activeConversation.title.length > 40
        ? activeConversation.title.substring(0, 40) + '...'
        : activeConversation.title;
    }
    // なければ最初のユーザーメッセージ
    const firstUserMessage = activeConversation.messages.find((m) => m.role === 'user');
    if (firstUserMessage) {
      return firstUserMessage.content.length > 40
        ? firstUserMessage.content.substring(0, 40) + '...'
        : firstUserMessage.content;
    }
    return '新しい会話';
  }, [activeConversation]);
  
  // モード表示用のバッジラベル
  const getModeLabel = (mode?: string) => {
    const labels: Record<string, string> = {
      GENERAL_CHAT: 'General',
      CONTEXT_HELP: 'Help',
      ERROR_ANALYZE: 'Error',
      STUDIO_AGENT: 'Studio',
      WORKFLOW_AGENT: 'Workflow',
    };
    return mode ? labels[mode] || mode : null;
  };

  return (
    <div className="h-[calc(100vh-6rem)] flex relative overflow-hidden">
      {/* 左サイドバー: 会話履歴 */}
      {isSidebarOpen && (
        <div className="h-full shrink-0">
          <MiraConversationList
            conversations={conversations}
            activeConversationId={activeConversation?.id ?? null}
            onSelect={handleSelectConversation}
            onDelete={handleDeleteConversation}
            onNewConversation={handleNewConversation}

            searchInputRef={searchInputRef}
            hasMore={hasMore}
            onLoadMore={loadMoreConversations}
          />
        </div>
      )}
      
      {/* メインエリア: チャット（1カラム中央配置） */}
      <div className="flex-1 flex flex-col min-w-0">

        {/* チャットヘッダー */}
        <MiraHeader
          isSidebarOpen={isSidebarOpen}
          onToggleSidebar={() => setIsSidebarOpen((prev) => !prev)}
          activeConversation={activeConversation}
          getConversationSummary={getConversationSummary}
          getModeLabel={getModeLabel}
          onNewConversation={handleNewConversation}
          onOpenContextEditor={() => setIsContextEditorOpen(true)}
          onOpenShortcuts={() => setShowKeyboardShortcuts(true)}
          onExport={handleExport}
          onClearConversation={() => setShowClearConfirm(true)}
          onUpdateTitle={updateConversationTitle}
          onRegenerateTitle={() => activeConversation ? regenerateTitle(activeConversation.id) : Promise.resolve()}
          isExporting={isExporting}
          isUpdatingTitle={isUpdatingTitle}
          hasMessages={messages.length > 0}
        />
        
        {/* エラー表示 */}
        {error && (
          <div className="p-3 bg-destructive/10 text-destructive text-sm flex items-center justify-between">
            <span>{error}</span>
            <Button variant="ghost" size="sm" onClick={clearError}>
              閉じる
            </Button>
          </div>
        )}
        
        {/* メッセージエリア */}
        <ScrollArea className="flex-1">
          <div className="p-4 max-w-3xl mx-auto">
            {messages.length === 0 ? (
              <MiraEmptyState onSendMessage={handleSend} currentMode={currentMode} />
            ) : (
              <>
                {messages.map((message, index) => (
                  <div
                    key={message.id}
                    ref={(el) => {
                      if (el) messageRefs.current.set(message.id, el);
                    }}
                    className={selectedMessageIndex === index ? 'ring-2 ring-primary/50 rounded-lg' : ''}
                  >
                    <MiraChatMessage message={message} onEdit={handleEditMessage} />
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </>
            )}
          </div>
        </ScrollArea>
        
        {/* 入力エリア */}
        <div className="border-t bg-surface">
          <div className="max-w-3xl mx-auto">
            <MiraChatInput
              ref={chatInputRef}
              onSend={handleSend}
              isLoading={isLoading}
              placeholder="メッセージを入力... (Enter で送信)"
              className="mira-chat-input"
              autoFocus
              editingMessageId={editingMessageId || undefined}
              editingMessageContent={editingMessageContent || undefined}
              onCancelEdit={cancelEditMessage}
            />
          </div>
        </div>
      </div>
      
      {/* キーボードショートカットオーバーレイ */}
      <MiraKeyboardShortcuts
        isOpen={showKeyboardShortcuts}
        onClose={() => setShowKeyboardShortcuts(false)}
      />
      
      {/* ユーザーコンテキスト設定ダイアログ */}
      <MiraUserContextEditor 
        open={isContextEditorOpen} 
        onOpenChange={setIsContextEditorOpen}
        showTrigger={false}
      />
      
      {/* 会話クリア確認ダイアログ */}
      <MiraDeleteConfirmDialog
        isOpen={showClearConfirm}
        onClose={() => setShowClearConfirm(false)}
        onConfirm={() => {
          clearConversation();
          toast({
            title: '成功',
            description: '会話をクリアしました',
          });
        }}
        title="この会話をクリアしますか?"
        description="すべてのメッセージが削除されます。この操作は取り消せません。"
        confirmLabel="クリア"
      />
      
      {/* メッセージ編集確認ダイアログ */}
      <Dialog open={showEditConfirm} onOpenChange={setShowEditConfirm}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>メッセージを編集</DialogTitle>
            <DialogDescription>
              このメッセージを編集すると、以降の会話履歴（{affectedMessagesCount}件）が削除されます。
              <br />
              よろしいですか？
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={handleCancelConfirmEdit}>
              キャンセル
            </Button>
            <Button onClick={handleConfirmEdit}>
              削除して再送信
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

/** 空状態表示コンポーネント */
interface MiraEmptyStateProps {
  onSendMessage: (message: string) => void;
  currentMode: MiraMode;
}

function MiraEmptyState({ onSendMessage }: MiraEmptyStateProps) {
  const suggestions = [
    'このエラーの原因を教えて',
    'コードをレビューして',
    'ドキュメントを書いて',
    'アーキテクチャを相談したい',
  ];
  
  return (
    <div className="flex flex-col items-center justify-center h-[60vh] text-center">
      <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mb-6">
        <Sparkles className="w-8 h-8 text-primary" />
      </div>
      <h2 className="text-xl font-medium mb-2">Mira</h2>
      <p className="text-muted-foreground mb-8">
        何を手伝いましょうか？
      </p>
      
      {/* シンプルなサジェストリンク */}
      <div className="flex flex-col gap-2 text-sm">
        <p className="text-xs text-muted-foreground mb-1">よく使われる質問:</p>
        {suggestions.map((suggestion) => (
          <button
            key={suggestion}
            onClick={() => onSendMessage(suggestion)}
            className="text-primary hover:underline"
          >
            • {suggestion}
          </button>
        ))}
      </div>
    </div>
  );
}
