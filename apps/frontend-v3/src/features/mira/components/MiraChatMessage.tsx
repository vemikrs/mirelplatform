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
      
      {/* コンテンツエリア */}
      {isUser ? (
        <>
          {/* ユーザーフキダシ */}
          <div
            className={cn(
              'rounded-lg relative',
              compact ? 'max-w-[85%] p-2 text-sm' : 'max-w-[80%] p-3',
              'bg-primary text-primary-foreground'
            )}
          >
            <p className={cn( 'whitespace-pre-wrap', compact ? 'text-xs' : 'text-sm' )}>
              {message.content}
            </p>
          </div>

          {/* ユーザーアクション（左側に表示 = flex-row-reverseの末尾） */}
          <div className="flex flex-col justify-end opacity-0 group-hover:opacity-100 transition-opacity px-1">
             <div className="flex items-center gap-1">
              {onEdit && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => onEdit(message.id)}
                  className="w-8 h-8 text-muted-foreground hover:text-foreground hover:bg-muted"
                  title="編集して再送信"
                >
                  <Edit className="w-4 h-4" />
                </Button>
              )}
               <Button
                 variant="ghost"
                 size="icon"
                 onClick={handleCopy}
                 className="w-8 h-8 text-muted-foreground hover:text-foreground hover:bg-muted"
                 title="コピー"
               >
                 {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
               </Button>
             </div>
          </div>
        </>
      ) : (
        /* AIレスポンス（全幅 + アクション下部） */
        <div className="flex flex-col flex-1 min-w-0">
          <div
            className={cn(
              'rounded-lg',
              compact ? 'p-2 text-sm' : 'p-3',
              'bg-muted w-full'
            )}
          >
            {message.contentType === 'markdown' ? (
              <MiraMarkdown content={message.content} compact={compact} />
            ) : (
              <p className={cn(
                'whitespace-pre-wrap',
                compact ? 'text-xs' : 'text-sm'
              )}>
                {message.content}
              </p>
            )}
            
            {/* メタデータ */}
            {!compact && message.metadata && (
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

          {/* AIアクション（下部に表示） */}
          <div className="flex items-center gap-1 mt-1 opacity-0 group-hover:opacity-100 transition-opacity px-1">
            <Button
              variant="ghost"
              size="icon"
              onClick={handleCopy}
              className="w-8 h-8 text-muted-foreground hover:text-foreground"
              title="コピー"
            >
              {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
