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
  Input,
} from '@mirel/ui';
import { 
  Sparkles,
  Edit2,
  Check,
  X,
  PanelLeft,
} from 'lucide-react';
import { useOs } from '@/lib/hooks/useOs';
import { useMira } from '@/hooks/useMira';
import { useMiraStore } from '@/stores/miraStore';
import { exportUserData, type MiraMode, type MessageConfig } from '@/lib/api/mira';
import { MiraMenu } from '../components/MiraMenu';
import { MiraChatMessage } from '../components/MiraChatMessage';
import { MiraChatInput } from '../components/MiraChatInput';
import { MiraConversationList } from '../components/MiraConversationList';
import { MiraKeyboardShortcuts } from '../components/MiraKeyboardShortcuts';
import { MiraUserContextEditor } from '../components/MiraUserContextEditor';
import { MiraDeleteConfirmDialog } from '../components/MiraDeleteConfirmDialog';

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
  } = useMira();
  
  const setActiveConversation = useMiraStore((state) => state.setActiveConversation);
  const deleteConversation = useMiraStore((state) => state.deleteConversation);
  const storedConversations = useMiraStore((state) => state.conversations);
  
  // サイドバーの表示状態
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  
  // キーボードショートカットオーバーレイの状態
  const [showKeyboardShortcuts, setShowKeyboardShortcuts] = useState(false);
  
  // 会話クリア確認ダイアログの状態
  const [showClearConfirm, setShowClearConfirm] = useState(false);
  
  // タイトル編集状態
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editedTitle, setEditedTitle] = useState('');
  
  // エクスポート処理状態
  const [isExporting, setIsExporting] = useState(false);
  
  // メッセージ編集確認ダイアログの状態
  const [showEditConfirm, setShowEditConfirm] = useState(false);
  const [pendingEditMessageId, setPendingEditMessageId] = useState<string | null>(null);
  const [affectedMessagesCount, setAffectedMessagesCount] = useState(0);

  // コンテキストエディタの表示状態
  const [isContextEditorOpen, setIsContextEditorOpen] = useState(false);
  
  // URLクエリパラメータから会話IDを取得して開く
  useEffect(() => {
    const conversationId = searchParams.get('conversation');
    if (conversationId && storedConversations[conversationId]) {
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
  
  // 連続キー入力の追跡（gg, ge用）
  const lastKeyRef = useRef<{ key: string; time: number }>({ key: '', time: 0 });
  
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
  
  // グローバルキーボードショートカット
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // テキストエリアにフォーカスがある場合は無視
      const target = e.target as HTMLElement;
      if (target.tagName === 'TEXTAREA' || target.tagName === 'INPUT') {
        return;
      }
      
      const now = Date.now();
      
      // ? でショートカット一覧を表示
      if (e.key === '?' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        setShowKeyboardShortcuts(true);
        return;
      }
      
      // ⌘/Ctrl + H でサイドバーを切り替え
      if (e.key === 'h' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        setIsSidebarOpen((prev) => !prev);
        return;
      }
      
      // ⌘/Ctrl + N で新規会話
      if (e.key === 'n' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        handleNewConversation();
        return;
      }
      
      // n で入力欄にフォーカス（Ctrl/Cmdなし）
      if (e.key === 'n' && !e.ctrlKey && !e.metaKey && !e.altKey) {
        e.preventDefault();
        // MiraChatInput内のtextareaにフォーカス
        const textarea = document.querySelector('.mira-chat-input textarea') as HTMLTextAreaElement;
        textarea?.focus();
        return;
      }
      
      // j で次のメッセージへ
      if (e.key === 'j' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        if (messages.length > 0) {
          const newIndex = Math.min(selectedMessageIndex + 1, messages.length - 1);
          setSelectedMessageIndex(newIndex);
          scrollToMessage(newIndex);
        }
        lastKeyRef.current = { key: 'j', time: now };
        return;
      }
      
      // k で前のメッセージへ
      if (e.key === 'k' && !e.ctrlKey && !e.metaKey) {
        e.preventDefault();
        if (messages.length > 0) {
          const newIndex = Math.max(selectedMessageIndex - 1, 0);
          setSelectedMessageIndex(newIndex);
          scrollToMessage(newIndex);
        }
        lastKeyRef.current = { key: 'k', time: now };
        return;
      }
      
      // g + g で最初のメッセージへ（500ms以内の連続押下）
      if (e.key === 'g' && !e.ctrlKey && !e.metaKey) {
        if (lastKeyRef.current.key === 'g' && now - lastKeyRef.current.time < 500) {
          e.preventDefault();
          setSelectedMessageIndex(0);
          scrollToMessage(0);
          lastKeyRef.current = { key: '', time: 0 };
        } else {
          lastKeyRef.current = { key: 'g', time: now };
        }
        return;
      }
      
      // g + e で最後のメッセージへ（500ms以内）
      if (e.key === 'e' && !e.ctrlKey && !e.metaKey) {
        if (lastKeyRef.current.key === 'g' && now - lastKeyRef.current.time < 500) {
          e.preventDefault();
          const lastIndex = messages.length - 1;
          setSelectedMessageIndex(lastIndex);
          scrollToMessage(lastIndex);
          lastKeyRef.current = { key: '', time: 0 };
        } else {
          // 単独の e キーでメッセージ編集（選択中のメッセージがユーザーメッセージの場合）
          if (selectedMessageIndex >= 0 && messages[selectedMessageIndex]?.role === 'user') {
            e.preventDefault();
            handleEditMessage(messages[selectedMessageIndex].id);
          }
          lastKeyRef.current = { key: 'e', time: now };
        }
        return;
      }
      
      // c で選択中メッセージをコピー
      if (e.key === 'c' && !e.ctrlKey && !e.metaKey && selectedMessageIndex >= 0) {
        e.preventDefault();
        const msg = messages[selectedMessageIndex];
        if (msg) {
          navigator.clipboard.writeText(msg.content);
        }
        return;
      }
      
      // Escape でサイドバーを閉じる / メッセージ選択解除
      if (e.key === 'Escape') {
        if (selectedMessageIndex >= 0) {
          setSelectedMessageIndex(-1);
        }
        return;
      }
    };
    
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isSidebarOpen, handleNewConversation, messages, selectedMessageIndex, scrollToMessage, handleEditMessage]);
  
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
  
  // タイトル編集開始
  const handleStartEditTitle = useCallback(() => {
    if (activeConversation) {
      setEditedTitle(activeConversation.title || getConversationSummary());
      setIsEditingTitle(true);
    }
  }, [activeConversation, getConversationSummary]);
  
  // タイトル編集保存
  const handleSaveTitle = useCallback(async () => {
    if (activeConversation && editedTitle.trim()) {
      try {
        await updateConversationTitle(activeConversation.id, editedTitle.trim());
        setIsEditingTitle(false);
      } catch (error) {
        console.error('タイトル更新エラー:', error);
      }
    }
  }, [activeConversation, editedTitle, updateConversationTitle]);
  
  // タイトル編集キャンセル
  const handleCancelEditTitle = useCallback(() => {
    setIsEditingTitle(false);
    setEditedTitle('');
  }, []);
  
  const { metaKey } = useOs();

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
            onClose={() => setIsSidebarOpen(false)}
          />
        </div>
      )}
      
      {/* メインエリア: チャット（1カラム中央配置） */}
      <div className="flex-1 flex flex-col min-w-0">

        {/* チャットヘッダー */}
        <div className="flex items-center justify-between p-3 border-b bg-surface">
          <div className="flex items-center gap-3 flex-1 min-w-0">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsSidebarOpen((prev) => !prev)}
              title={`サイドバーを切替 (${metaKey}+H)`}
            >
              <PanelLeft className="w-5 h-5" />
            </Button>
            <div className="flex items-center gap-2 flex-1 min-w-0">
              <Sparkles className="w-4 h-4 text-primary shrink-0" />
              {isEditingTitle ? (
                <div className="flex items-center gap-2 flex-1 min-w-0">
                  <Input
                    value={editedTitle}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditedTitle(e.target.value)}
                    onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
                      if (e.key === 'Enter') {
                        handleSaveTitle();
                      } else if (e.key === 'Escape') {
                        handleCancelEditTitle();
                      }
                    }}
                    className="h-8 text-sm flex-1"
                    autoFocus
                    disabled={isUpdatingTitle}
                  />
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={handleSaveTitle}
                    disabled={isUpdatingTitle}
                    title="保存 (Enter)"
                    className="h-8 w-8"
                  >
                    <Check className="w-4 h-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={handleCancelEditTitle}
                    disabled={isUpdatingTitle}
                    title="キャンセル (Esc)"
                    className="h-8 w-8"
                  >
                    <X className="w-4 h-4" />
                  </Button>
                </div>
              ) : (
                <>
                  <h2 className="font-medium text-sm truncate">
                    {getConversationSummary()}
                  </h2>
                  {activeConversation && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={handleStartEditTitle}
                      title="タイトルを編集"
                      className="h-6 w-6 shrink-0"
                    >
                      <Edit2 className="w-3 h-3" />
                    </Button>
                  )}
                </>
              )}
              {activeConversation?.mode && activeConversation.mode !== 'GENERAL_CHAT' && (
                <span className="text-xs px-1.5 py-0.5 rounded bg-primary/10 text-primary shrink-0">
                  {getModeLabel(activeConversation.mode)}
                </span>
              )}
            </div>
          </div>
          <div>
            <MiraMenu 
              onNewConversation={handleNewConversation}
              onOpenContextEditor={() => setIsContextEditorOpen(true)}
              onOpenShortcuts={() => setShowKeyboardShortcuts(true)}
              onExport={handleExport}
              onClearConversation={() => setShowClearConfirm(true)}
              isExporting={isExporting}
              hasMessages={messages.length > 0}
            />
          </div>
        </div>
        
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
