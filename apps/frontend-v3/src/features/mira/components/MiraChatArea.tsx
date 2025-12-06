/**
 * Mira チャットエリアコンポーネント
 * 
 * メッセージ表示とメッセージ入力フォームを提供
 */
import { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import { cn, Button, ScrollArea, Input } from '@mirel/ui';
import { 
  Bot, 
  User, 
  Send, 
  Sparkles, 
  Loader2,
  Copy,
  Check,
} from 'lucide-react';
import type { MiraConversation, MiraMessage } from '@/stores/miraStore';

interface MiraChatAreaProps {
  conversation: MiraConversation | null;
  isLoading: boolean;
  onSendMessage: (message: string) => void;
  onRetry?: (messageId: string) => void;
  className?: string;
}

export function MiraChatArea({
  conversation,
  isLoading,
  onSendMessage,
  onRetry,
  className,
}: MiraChatAreaProps) {
  const [input, setInput] = useState('');
  const scrollRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  
  const messages = useMemo(() => conversation?.messages ?? [], [conversation?.messages]);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);
  
  // 初期フォーカス
  useEffect(() => {
    inputRef.current?.focus();
  }, []);
  
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;
    
    onSendMessage(input.trim());
    setInput('');
  };
  
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
      handleSubmit(e);
    }
  };
  
  return (
    <div className={cn("flex flex-col", className)}>
      {/* メッセージエリア */}
      <ScrollArea className="flex-1" ref={scrollRef}>
        <div className="p-4 space-y-4 max-w-3xl mx-auto">
          {messages.length === 0 ? (
            <EmptyState />
          ) : (
            messages.map((message) => (
              <MessageBubble 
                key={message.id} 
                message={message}
                onRetry={onRetry}
              />
            ))
          )}
          
          {/* ローディング表示 */}
          {isLoading && <LoadingIndicator />}
        </div>
      </ScrollArea>
      
      {/* 入力フォーム */}
      <div className="border-t p-4">
        <form onSubmit={handleSubmit} className="max-w-3xl mx-auto">
          <div className="flex gap-2">
            <Input
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Mira に質問する..."
              className="flex-1"
              disabled={isLoading}
            />
            <Button type="submit" disabled={!input.trim() || isLoading}>
              {isLoading ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <Send className="w-4 h-4" />
              )}
            </Button>
          </div>
          <p className="text-xs text-muted-foreground mt-2 text-center">
            ⌘ + Enter で送信
          </p>
        </form>
      </div>
    </div>
  );
}

/** 空状態表示 */
function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mb-4">
        <Sparkles className="w-8 h-8 text-primary" />
      </div>
      <h3 className="text-lg font-medium mb-2">Mira へようこそ</h3>
      <p className="text-muted-foreground max-w-sm">
        mirelplatform の AI アシスタントです。
        質問やヘルプが必要なことを何でも聞いてください。
      </p>
    </div>
  );
}

/** メッセージバブル */
interface MessageBubbleProps {
  message: MiraMessage;
  onRetry?: (messageId: string) => void;
}

function MessageBubble({ message }: MessageBubbleProps) {
  const [copied, setCopied] = useState(false);
  const isUser = message.role === 'user';
  
  const handleCopy = useCallback(async () => {
    await navigator.clipboard.writeText(message.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }, [message.content]);
  
  return (
    <div 
      className={cn(
        "flex gap-3",
        isUser && "flex-row-reverse"
      )}
    >
      {/* アバター */}
      <div 
        className={cn(
          "w-8 h-8 rounded-full flex items-center justify-center shrink-0",
          isUser 
            ? "bg-primary text-primary-foreground" 
            : "bg-muted"
        )}
      >
        {isUser ? (
          <User className="w-4 h-4" />
        ) : (
          <Bot className="w-4 h-4" />
        )}
      </div>
      
      {/* メッセージ本文 */}
      <div 
        className={cn(
          "flex-1 max-w-[80%] group",
          isUser && "text-right"
        )}
      >
        <div 
          className={cn(
            "inline-block p-3 rounded-lg text-sm",
            isUser 
              ? "bg-primary text-primary-foreground rounded-br-none" 
              : "bg-muted rounded-bl-none"
          )}
        >
          <div className="whitespace-pre-wrap break-word">
            {message.content}
          </div>
        </div>
        
        {/* アクションボタン */}
        {!isUser && (
          <div className="mt-1 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
            <Button
              variant="ghost"
              size="icon"
              className="w-6 h-6"
              onClick={handleCopy}
              title="コピー"
            >
              {copied ? (
                <Check className="w-3.5 h-3.5 text-green-500" />
              ) : (
                <Copy className="w-3.5 h-3.5" />
              )}
            </Button>
          </div>
        )}
        
        {/* タイムスタンプ */}
        <div className="text-xs text-muted-foreground mt-1">
          {formatTimestamp(message.timestamp)}
        </div>
      </div>
    </div>
  );
}

/** ローディングインジケーター */
function LoadingIndicator() {
  return (
    <div className="flex gap-3">
      <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center">
        <Bot className="w-4 h-4" />
      </div>
      <div className="flex-1">
        <div className="inline-block p-3 rounded-lg bg-muted rounded-bl-none">
          <div className="flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-sm text-muted-foreground">考え中...</span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ユーティリティ関数
function formatTimestamp(date: Date | string): string {
  const d = new Date(date);
  return d.toLocaleTimeString('ja-JP', { 
    hour: '2-digit', 
    minute: '2-digit' 
  });
}
