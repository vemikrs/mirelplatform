/**
 * Mira Chat Panel Component
 * 
 * サイドパネル形式のチャットUI
 */
import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { cn, Button, ScrollArea } from '@mirel/ui';
import { X, MessageSquarePlus, Trash2, Bot, ExternalLink } from 'lucide-react';
import { useMira, useMiraPanel } from '@/hooks/useMira';
import { useMiraStore } from '@/stores/miraStore';
import { MiraChatMessage } from './MiraChatMessage';
import { MiraChatInput } from './MiraChatInput';
import type { MiraMode } from '@/lib/api/mira';

interface MiraChatPanelProps {
  className?: string;
}

export function MiraChatPanel({ className }: MiraChatPanelProps) {
  const navigate = useNavigate();
  const {
    sendMessage,
    isLoading,
    error,
    clearError,
    messages,
    activeConversation,
    clearConversation,
    newConversation,
  } = useMira();
  
  const { isOpen, close: closePanel } = useMiraPanel();
  const togglePanel = useMiraStore((state) => state.togglePanel);
  const activeConversationId = useMiraStore((state) => state.activeConversationId);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // Ctrl+Shift+M でパネル開閉
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'M') {
        e.preventDefault();
        togglePanel();
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [togglePanel]);
  
  const handleSend = (message: string, mode?: MiraMode) => {
    sendMessage(message, { mode });
  };
  
  // 専用画面で開く
  const handleOpenInFullPage = () => {
    closePanel();
    // 会話IDがあればそれを開く、なければMiraトップへ
    if (activeConversationId) {
      navigate(`/mira?conversation=${activeConversationId}`);
    } else {
      navigate('/mira');
    }
  };
  
  if (!isOpen) {
    return null;
  }
  
  // モード表示用のバッジラベル
  const getModeLabel = (mode?: string) => {
    const labels: Record<string, string> = {
      general_chat: 'General',
      context_help: 'Help',
      error_analyze: 'Error',
      studio_agent: 'Studio',
      workflow_agent: 'Workflow',
      GENERAL_CHAT: 'General',
      CONTEXT_HELP: 'Help',
      ERROR_ANALYZE: 'Error',
      STUDIO_AGENT: 'Studio',
      WORKFLOW_AGENT: 'Workflow',
    };
    return mode ? labels[mode] || mode : null;
  };
  
  return (
    <div
      className={cn(
        'fixed right-0 top-0 h-full w-[400px] max-w-full',
        'bg-background border-l shadow-lg z-50',
        'flex flex-col',
        className
      )}
      role="dialog"
      aria-label="Mira AI アシスタント"
    >
      {/* ヘッダー */}
      <div className="flex items-center justify-between p-3 border-b">
        <div className="flex items-center gap-2">
          <Bot className="w-5 h-5 text-primary" />
          <h2 className="font-semibold">Mira</h2>
          {activeConversation?.mode && (
            <span className="px-2 py-0.5 text-xs rounded-full bg-primary/10 text-primary font-medium">
              {getModeLabel(activeConversation.mode)}
            </span>
          )}
        </div>
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            onClick={handleOpenInFullPage}
            title="専用画面で開く"
          >
            <ExternalLink className="w-4 h-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={newConversation}
            title="新しい会話"
          >
            <MessageSquarePlus className="w-4 h-4" />
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
          <Button
            variant="ghost"
            size="icon"
            onClick={closePanel}
            title="閉じる (Ctrl+Shift+M)"
          >
            <X className="w-4 h-4" />
          </Button>
        </div>
      </div>
      
      {/* エラー表示 */}
      {error && (
        <div className="p-3 bg-destructive/10 text-destructive text-sm flex items-center justify-between">
          <span>{error}</span>
          <Button
            variant="ghost"
            size="sm"
            onClick={clearError}
          >
            閉じる
          </Button>
        </div>
      )}
      
      {/* メッセージエリア */}
      <ScrollArea className="flex-1">
        <div className="p-2">
          {messages.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-muted-foreground">
              <Bot className="w-12 h-12 mb-4 opacity-50" />
              <p className="text-center">
                こんにちは！<br />
                何かお手伝いできることはありますか？
              </p>
              <p className="text-xs mt-2 text-center opacity-70">
                ショートカットボタンから<br />
                質問を始めることもできます
              </p>
            </div>
          ) : (
            <>
              {messages.map((message) => (
                <MiraChatMessage key={message.id} message={message} />
              ))}
              <div ref={messagesEndRef} />
            </>
          )}
        </div>
      </ScrollArea>
      
      {/* 入力エリア */}
      <MiraChatInput
        onSend={handleSend}
        isLoading={isLoading}
        placeholder="メッセージを入力... (Enter で送信)"
        showShortcuts={true}
      />
    </div>
  );
}
