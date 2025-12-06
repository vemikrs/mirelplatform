/**
 * Mira Chat Input Component
 * 
 * メッセージ入力フォーム
 */
import { useState, useRef, useEffect, type KeyboardEvent } from 'react';
import { Button, cn } from '@mirel/ui';
import { Send, Loader2 } from 'lucide-react';

interface MiraChatInputProps {
  onSend: (message: string) => void;
  isLoading?: boolean;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
}

export function MiraChatInput({
  onSend,
  isLoading = false,
  disabled = false,
  placeholder = 'メッセージを入力...',
  className,
}: MiraChatInputProps) {
  const [message, setMessage] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  
  // 自動リサイズ
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [message]);
  
  const handleSend = () => {
    const trimmed = message.trim();
    if (trimmed && !isLoading && !disabled) {
      onSend(trimmed);
      setMessage('');
    }
  };
  
  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    // Ctrl/Cmd + Enter で送信
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      handleSend();
    }
    // Shift + Enter は改行（デフォルト動作）
    // Enter のみは送信
    if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
      handleSend();
    }
  };
  
  return (
    <div className={cn('flex gap-2 p-3 border-t bg-background', className)}>
      <textarea
        ref={textareaRef}
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={isLoading || disabled}
        rows={1}
        className={cn(
          'flex-1 resize-none rounded-md border border-input bg-background px-3 py-2',
          'text-sm placeholder:text-muted-foreground',
          'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
          'disabled:cursor-not-allowed disabled:opacity-50'
        )}
      />
      <Button
        onClick={handleSend}
        disabled={!message.trim() || isLoading || disabled}
        size="icon"
        className="flex-shrink-0"
      >
        {isLoading ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : (
          <Send className="w-4 h-4" />
        )}
      </Button>
    </div>
  );
}
