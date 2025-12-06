/**
 * Mira Chat Message Component
 * 
 * チャットメッセージの表示コンポーネント
 */
import { cn } from '@mirel/ui';
import { Bot, User } from 'lucide-react';
import type { MiraMessage } from '@/stores/miraStore';
import { MiraMarkdown } from './MiraMarkdown';

interface MiraChatMessageProps {
  message: MiraMessage;
  className?: string;
}

export function MiraChatMessage({ message, className }: MiraChatMessageProps) {
  const isUser = message.role === 'user';
  
  return (
    <div
      className={cn(
        'flex gap-3 p-3',
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
          'flex-1 max-w-[80%] rounded-lg p-3',
          isUser 
            ? 'bg-primary text-primary-foreground' 
            : 'bg-muted'
        )}
      >
        {message.contentType === 'markdown' ? (
          <MiraMarkdown content={message.content} />
        ) : (
          <p className="whitespace-pre-wrap text-sm">{message.content}</p>
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
