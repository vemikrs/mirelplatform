/**
 * Mira Chat Input Component
 * 
 * メッセージ入力フォーム + ショートカットボタン
 */
import { useState, useRef, useEffect, type KeyboardEvent } from 'react';
import { Button, cn } from '@mirel/ui';
import { Send, Loader2, ChevronDown, ChevronUp, HelpCircle, AlertCircle, Settings, Wrench, GitBranch } from 'lucide-react';

/** ショートカットボタンの定義 */
const SHORTCUT_BUTTONS = [
  { id: 'help', label: 'この画面の説明', icon: HelpCircle, mode: 'CONTEXT_HELP' as const },
  { id: 'error', label: 'エラーを説明', icon: AlertCircle, mode: 'ERROR_ANALYZE' as const },
  { id: 'settings', label: '設定手順を教えて', icon: Settings, mode: 'CONTEXT_HELP' as const },
  { id: 'studio', label: 'Studio 設計相談', icon: Wrench, mode: 'STUDIO_AGENT' as const },
  { id: 'workflow', label: 'Workflow 設計相談', icon: GitBranch, mode: 'WORKFLOW_AGENT' as const },
] as const;

type MiraMode = 'GENERAL_CHAT' | 'CONTEXT_HELP' | 'ERROR_ANALYZE' | 'STUDIO_AGENT' | 'WORKFLOW_AGENT';

interface MiraChatInputProps {
  onSend: (message: string, mode?: MiraMode) => void;
  isLoading?: boolean;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
  showShortcuts?: boolean;
}

export function MiraChatInput({
  onSend,
  isLoading = false,
  disabled = false,
  placeholder = 'メッセージを入力...',
  className,
  showShortcuts = true,
}: MiraChatInputProps) {
  const [message, setMessage] = useState('');
  const [shortcutsExpanded, setShortcutsExpanded] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  
  // 自動リサイズ
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [message]);
  
  const handleSend = (overrideMessage?: string, mode?: MiraMode) => {
    const trimmed = (overrideMessage ?? message).trim();
    if (trimmed && !isLoading && !disabled) {
      onSend(trimmed, mode);
      setMessage('');
    }
  };
  
  const handleShortcut = (shortcut: typeof SHORTCUT_BUTTONS[number]) => {
    // ショートカットボタンに対応するメッセージを送信
    const shortcutMessages: Record<string, string> = {
      help: 'この画面について説明してください',
      error: '発生したエラーについて説明してください',
      settings: 'この機能の設定手順を教えてください',
      studio: 'Studio での設計について相談させてください',
      workflow: 'ワークフローの設計について相談させてください',
    };
    handleSend(shortcutMessages[shortcut.id], shortcut.mode);
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
    <div className={cn('border-t bg-background', className)}>
      {/* ショートカットボタン */}
      {showShortcuts && (
        <div className="px-3 pt-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShortcutsExpanded(!shortcutsExpanded)}
            className="h-6 px-2 text-xs text-muted-foreground"
          >
            ショートカット
            {shortcutsExpanded ? (
              <ChevronUp className="ml-1 w-3 h-3" />
            ) : (
              <ChevronDown className="ml-1 w-3 h-3" />
            )}
          </Button>
          {shortcutsExpanded && (
            <div className="flex flex-wrap gap-1 mt-1 pb-1">
              {SHORTCUT_BUTTONS.map((shortcut) => (
                <Button
                  key={shortcut.id}
                  variant="outline"
                  size="sm"
                  onClick={() => handleShortcut(shortcut)}
                  disabled={isLoading || disabled}
                  className="h-7 px-2 text-xs"
                >
                  <shortcut.icon className="w-3 h-3 mr-1" />
                  {shortcut.label}
                </Button>
              ))}
            </div>
          )}
        </div>
      )}
      
      {/* メッセージ入力エリア */}
      <div className="flex gap-2 p-3">
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
          onClick={() => handleSend()}
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
    </div>
  );
}
