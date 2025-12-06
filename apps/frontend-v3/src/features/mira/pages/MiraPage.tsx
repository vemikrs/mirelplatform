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
import { Button, ScrollArea } from '@mirel/ui';
import { 
  Sparkles,
  Trash2,
  Menu,
  Plus,
  Keyboard,
} from 'lucide-react';
import { useMira } from '@/hooks/useMira';
import { useMiraStore } from '@/stores/miraStore';
import type { MiraMode } from '@/lib/api/mira';
import { MiraChatMessage } from '../components/MiraChatMessage';
import { MiraChatInput } from '../components/MiraChatInput';
import { MiraConversationList } from '../components/MiraConversationList';
import { MiraKeyboardShortcuts } from '../components/MiraKeyboardShortcuts';
import { MiraUserContextEditor } from '../components/MiraUserContextEditor';

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
  } = useMira();
  
  const setActiveConversation = useMiraStore((state) => state.setActiveConversation);
  const deleteConversation = useMiraStore((state) => state.deleteConversation);
  const storedConversations = useMiraStore((state) => state.conversations);
  
  // ドロワーの開閉状態
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  
  // キーボードショートカットオーバーレイの状態
  const [showKeyboardShortcuts, setShowKeyboardShortcuts] = useState(false);
  
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
    setIsDrawerOpen(false);
  }, [newConversation]);
  
  // 選択中メッセージへスクロール
  const scrollToMessage = useCallback((index: number) => {
    if (messages[index]) {
      const messageEl = messageRefs.current.get(messages[index].id);
      messageEl?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [messages]);
  
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
      
      // ⌘/Ctrl + H で会話履歴を開く
      if (e.key === 'h' && (e.ctrlKey || e.metaKey)) {
        e.preventDefault();
        setIsDrawerOpen((prev) => !prev);
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
      
      // Escape でドロワーを閉じる / メッセージ選択解除
      if (e.key === 'Escape') {
        if (isDrawerOpen) {
          setIsDrawerOpen(false);
        } else if (selectedMessageIndex >= 0) {
          setSelectedMessageIndex(-1);
        }
        return;
      }
    };
    
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isDrawerOpen, handleNewConversation, messages, selectedMessageIndex, scrollToMessage]);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // メッセージ送信ハンドラ
  const handleSend = useCallback((message: string, mode?: MiraMode) => {
    sendMessage(message, { mode });
  }, [sendMessage]);
  
  // 会話選択ハンドラ
  const handleSelectConversation = useCallback((conversationId: string) => {
    setActiveConversation(conversationId);
    setIsDrawerOpen(false); // 選択後にドロワーを閉じる
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
  
  return (
    <div className="h-[calc(100vh-6rem)] flex relative overflow-hidden">
      {/* 左ドロワー: 会話履歴（Mira表示内に制限） */}
      {isDrawerOpen && (
        <div 
          className="absolute inset-0 bg-black/20 z-10"
          onClick={() => setIsDrawerOpen(false)}
        />
      )}
      <div 
        className={`
          absolute left-0 top-0 bottom-0 z-20
          transform transition-transform duration-300 ease-in-out
          ${isDrawerOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
      >
        <MiraConversationList
          conversations={conversations}
          activeConversationId={activeConversation?.id ?? null}
          onSelect={handleSelectConversation}
          onDelete={handleDeleteConversation}
          onNewConversation={handleNewConversation}
          onClose={() => setIsDrawerOpen(false)}
        />
      </div>
      
      {/* メインエリア: チャット（1カラム中央配置） */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* チャットヘッダー */}
        <div className="flex items-center justify-between p-3 border-b bg-surface">
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setIsDrawerOpen(true)}
              title="会話履歴を開く (⌘+H)"
            >
              <Menu className="w-5 h-5" />
            </Button>
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-primary" />
              <h2 className="font-medium text-sm">
                {getConversationSummary()}
              </h2>
              {activeConversation?.mode && activeConversation.mode !== 'GENERAL_CHAT' && (
                <span className="text-xs px-1.5 py-0.5 rounded bg-primary/10 text-primary">
                  {getModeLabel(activeConversation.mode)}
                </span>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1">
            <MiraUserContextEditor />
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setShowKeyboardShortcuts(true)}
              title="キーボードショートカット (?)"
            >
              <Keyboard className="w-4 h-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={handleNewConversation}
              title="新しい会話 (⌘+N)"
            >
              <Plus className="w-4 h-4" />
            </Button>
            {messages.length > 0 && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => clearConversation()}
                title="会話をクリア"
              >
                <Trash2 className="w-4 h-4" />
              </Button>
            )}
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
                    <MiraChatMessage message={message} />
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
            />
          </div>
        </div>
      </div>
      
      {/* キーボードショートカットオーバーレイ */}
      <MiraKeyboardShortcuts
        isOpen={showKeyboardShortcuts}
        onClose={() => setShowKeyboardShortcuts(false)}
      />
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
