/**
 * Mira Chat Message Component
 * 
 * チャットメッセージの表示コンポーネント
 */
import { useState } from 'react';
import { cn, Button } from '@mirel/ui';
import { Bot, User, Copy, Check } from 'lucide-react';
import type { MiraMessage } from '@/stores/miraStore';
import { MiraMarkdown } from './MiraMarkdown';

interface MiraChatMessageProps {
  message: MiraMessage;
  className?: string;
}

export function MiraChatMessage({ message, className }: MiraChatMessageProps) {
  const [copied, setCopied] = useState(false);
  const isUser = message.role === 'user';
  
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('コピーに失敗しました:', err);
    }
  };
  
  return (
    <div
      className={cn(
        'flex gap-3 p-3 group',
        isUser ? 'flex-row-reverse' : 'flex-row',
        className
      )}
    >
      {/* アバター */}
      <div
        className={cn(
          'flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center',
          isUser 
            ? 'bg-primary text-primary-foreground' 
            : 'bg-muted text-muted-foreground'
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
          'flex-1 max-w-[80%] rounded-lg p-3 relative',
          isUser 
            ? 'bg-primary text-primary-foreground' 
            : 'bg-muted'
        )}
      >
        {/* コピーボタン */}
        <Button
          variant="ghost"
          size="icon"
          onClick={handleCopy}
          className={cn(
            'absolute top-1 right-1 w-7 h-7',
            'opacity-0 group-hover:opacity-100 transition-opacity',
            isUser 
              ? 'text-primary-foreground hover:bg-primary-foreground/10' 
              : 'hover:bg-background/50'
          )}
          title="コピー"
        >
          {copied ? (
            <Check className="w-3.5 h-3.5" />
          ) : (
            <Copy className="w-3.5 h-3.5" />
          )}
        </Button>
        
        {message.contentType === 'markdown' ? (
          <MiraMarkdown content={message.content} />
        ) : (
          <p className="whitespace-pre-wrap text-sm pr-8">{message.content}</p>
        )}
        
        {/* メタデータ（アシスタントのみ） */}
        {!isUser && message.metadata && (
          <div className="mt-2 pt-2 border-t border-border/50 text-xs text-muted-foreground">
            {message.metadata.model && (
              <span className="mr-2">{message.metadata.model}</span>
            )}
            {message.metadata.latencyMs && (
              <span>{message.metadata.latencyMs}ms</span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
