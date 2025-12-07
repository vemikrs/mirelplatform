/**
 * Mira Chat Message Component
 * 
 * チャットメッセージの表示コンポーネント
 */
import { useState } from 'react';
import { cn, Button } from '@mirel/ui';
import { Bot, User, Copy, Check, Edit } from 'lucide-react';
import type { MiraMessage } from '@/stores/miraStore';
import { MiraMarkdown } from './MiraMarkdown';

interface MiraChatMessageProps {
  message: MiraMessage;
  className?: string;
  /** コンパクト表示（ポップアップウィンドウ用） */
  compact?: boolean;
  /** メッセージ編集コールバック */
  onEdit?: (messageId: string) => void;
}

export function MiraChatMessage({ message, className, compact = false, onEdit }: MiraChatMessageProps) {
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
        'flex gap-2 group',
        compact ? 'p-2' : 'gap-3 p-3',
        isUser ? 'flex-row-reverse' : 'flex-row',
        className
      )}
    >
      {/* アバター */}
      <div
        className={cn(
          'shrink-0 rounded-full flex items-center justify-center',
          compact ? 'w-6 h-6' : 'w-8 h-8',
          isUser 
            ? 'bg-primary text-primary-foreground' 
            : 'bg-muted text-muted-foreground'
        )}
      >
        {isUser ? (
          <User className={compact ? 'w-3 h-3' : 'w-4 h-4'} />
        ) : (
          <Bot className={compact ? 'w-3 h-3' : 'w-4 h-4'} />
        )}
      </div>
      
      {/* メッセージ本文 */}
      <div
        className={cn(
          'flex-1 rounded-lg relative',
          compact ? 'max-w-[85%] p-2 text-sm' : 'max-w-[80%] p-3',
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
            'absolute top-1',
            compact ? 'w-5 h-5' : 'w-7 h-7',
            'opacity-0 group-hover:opacity-100 transition-opacity',
            isUser 
              ? 'text-primary-foreground hover:bg-primary-foreground/10 right-8' 
              : 'hover:bg-background/50 right-1'
          )}
          title="コピー"
        >
          {copied ? (
            <Check className={compact ? 'w-2.5 h-2.5' : 'w-3.5 h-3.5'} />
          ) : (
            <Copy className={compact ? 'w-2.5 h-2.5' : 'w-3.5 h-3.5'} />
          )}
        </Button>
        
        {/* 編集ボタン（ユーザーメッセージのみ） */}
        {isUser && onEdit && (
          <Button
            variant="ghost"
            size="icon"
            onClick={() => onEdit(message.id)}
            className={cn(
              'absolute top-1 right-1',
              compact ? 'w-5 h-5' : 'w-7 h-7',
              'opacity-0 group-hover:opacity-100 transition-opacity',
              'text-primary-foreground hover:bg-primary-foreground/10'
            )}
            title="編集して再送信"
          >
            <Edit className={compact ? 'w-2.5 h-2.5' : 'w-3.5 h-3.5'} />
          </Button>
        )}
        
        {message.contentType === 'markdown' ? (
          <MiraMarkdown content={message.content} compact={compact} />
        ) : (
          <p className={cn(
            'whitespace-pre-wrap pr-6',
            compact ? 'text-xs' : 'text-sm pr-8'
          )}>
            {message.content}
          </p>
        )}
        
        {/* メタデータ（アシスタントのみ、コンパクト時は非表示） */}
        {!isUser && !compact && message.metadata && (
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
