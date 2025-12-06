/**
 * Mira Chat Panel Component
 * 
 * サイドパネル形式のチャットUI
 */
import { useEffect, useRef } from 'react';
import { cn, Button, ScrollArea } from '@mirel/ui';
import { X, MessageSquarePlus, Trash2, Bot } from 'lucide-react';
import { useMira } from '@/hooks/useMira';
import { MiraChatMessage } from './MiraChatMessage';
import { MiraChatInput } from './MiraChatInput';

interface MiraChatPanelProps {
  className?: string;
}

export function MiraChatPanel({ className }: MiraChatPanelProps) {
  const {
    isOpen,
    closePanel,
    sendMessage,
    isLoading,
    error,
    clearError,
    messages,
    activeConversation,
    clearConversation,
    newConversation,
  } = useMira();
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  if (!isOpen) {
    return null;
  }
  
  return (
    <div
      className={cn(
        'fixed right-0 top-0 h-full w-[400px] max-w-full',
        'bg-background border-l shadow-lg z-50',
        'flex flex-col',
        className
      )}
    >
      {/* ヘッダー */}
      <div className="flex items-center justify-between p-3 border-b">
        <div className="flex items-center gap-2">
          <Bot className="w-5 h-5 text-primary" />
          <h2 className="font-semibold">Mira</h2>
          {activeConversation && (
            <span className="text-xs text-muted-foreground">
              ({activeConversation.mode})
            </span>
          )}
        </div>
        <div className="flex items-center gap-1">
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
            title="閉じる"
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
        onSend={sendMessage}
        isLoading={isLoading}
        placeholder="メッセージを入力... (Enter で送信)"
      />
    </div>
  );
}
