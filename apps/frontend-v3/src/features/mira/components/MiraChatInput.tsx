/**
 * Mira Chat Input Component
 * 
 * メッセージ入力フォーム + @メンション風モード選択
 */
import { useState, useRef, useEffect, type KeyboardEvent } from 'react';
import { Button, cn } from '@mirel/ui';
import { Send, Loader2, ChevronDown, HelpCircle, AlertTriangle, Paintbrush2, Workflow, MessageSquare } from 'lucide-react';

type MiraMode = 'GENERAL_CHAT' | 'CONTEXT_HELP' | 'ERROR_ANALYZE' | 'STUDIO_AGENT' | 'WORKFLOW_AGENT';

/** モード定義 */
const MODES: { mode: MiraMode; label: string; shortLabel: string; icon: typeof MessageSquare; color: string }[] = [
  { mode: 'GENERAL_CHAT', label: 'General', shortLabel: 'General', icon: MessageSquare, color: 'text-foreground' },
  { mode: 'CONTEXT_HELP', label: 'Help - 機能の説明', shortLabel: 'Help', icon: HelpCircle, color: 'text-blue-500' },
  { mode: 'ERROR_ANALYZE', label: 'Error - エラー分析', shortLabel: 'Error', icon: AlertTriangle, color: 'text-red-500' },
  { mode: 'STUDIO_AGENT', label: 'Studio - モデル設計', shortLabel: 'Studio', icon: Paintbrush2, color: 'text-purple-500' },
  { mode: 'WORKFLOW_AGENT', label: 'Workflow - フロー設計', shortLabel: 'Workflow', icon: Workflow, color: 'text-green-500' },
];

interface MiraChatInputProps {
  onSend: (message: string, mode?: MiraMode) => void;
  isLoading?: boolean;
  disabled?: boolean;
  placeholder?: string;
  className?: string;
  showShortcuts?: boolean;
  initialMode?: MiraMode;
}

export function MiraChatInput({
  onSend,
  isLoading = false,
  disabled = false,
  placeholder = 'メッセージを入力...',
  className,
  initialMode = 'GENERAL_CHAT',
}: MiraChatInputProps) {
  const [message, setMessage] = useState('');
  const [selectedMode, setSelectedMode] = useState<MiraMode>(initialMode);
  const [showModeMenu, setShowModeMenu] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  
  // 自動リサイズ
  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 150)}px`;
    }
  }, [message]);
  
  // メニュー外クリックで閉じる
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowModeMenu(false);
      }
    };
    if (showModeMenu) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [showModeMenu]);
  
  const handleSend = () => {
    const trimmed = message.trim();
    if (trimmed && !isLoading && !disabled) {
      onSend(trimmed, selectedMode);
      setMessage('');
    }
  };
  
  const handleModeSelect = (mode: MiraMode) => {
    setSelectedMode(mode);
    setShowModeMenu(false);
    textareaRef.current?.focus();
  };
  
  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    // Ctrl/Cmd + Enter で送信
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      handleSend();
    }
    // Enter のみは送信（Shift + Enter は改行）
    if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
      handleSend();
    }
    // @ でモードメニューを開く
    if (e.key === '@' && message === '') {
      e.preventDefault();
      setShowModeMenu(true);
    }
    // Escape でメニューを閉じる
    if (e.key === 'Escape' && showModeMenu) {
      setShowModeMenu(false);
    }
  };
  
  const currentModeConfig = MODES.find(m => m.mode === selectedMode) ?? MODES[0];
  const CurrentIcon = currentModeConfig.icon;
  
  return (
    <div className={cn('', className)}>
      {/* メッセージ入力エリア */}
      <div className="relative px-3 py-2">
        {/* モード選択メニュー */}
        {showModeMenu && (
          <div 
            ref={menuRef}
            className="absolute bottom-full left-3 mb-2 w-56 py-1 bg-popover border rounded-lg shadow-lg z-10"
          >
            <p className="px-3 py-1.5 text-xs font-medium text-muted-foreground">
              モードを選択
            </p>
            {MODES.map(({ mode, label, icon: Icon, color }) => (
              <button
                key={mode}
                onClick={() => handleModeSelect(mode)}
                className={cn(
                  "w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted transition-colors",
                  selectedMode === mode && "bg-muted"
                )}
              >
                <Icon className={cn("w-4 h-4", color)} />
                <span>{label}</span>
              </button>
            ))}
          </div>
        )}
        
        <div className="flex items-end gap-2">
          {/* モード選択ボタン */}
          <button
            onClick={() => setShowModeMenu(!showModeMenu)}
            className={cn(
              "flex items-center gap-1 px-2 py-1.5 text-xs rounded-md border",
              "hover:bg-muted transition-colors shrink-0",
              currentModeConfig.color
            )}
            title="モードを変更 (@)"
          >
            <CurrentIcon className="w-3.5 h-3.5" />
            <span>{currentModeConfig.shortLabel}</span>
            <ChevronDown className="w-3 h-3 opacity-50" />
          </button>
          
          {/* テキストエリア */}
          <textarea
            ref={textareaRef}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            disabled={isLoading || disabled}
            rows={1}
            className={cn(
              'flex-1 resize-none rounded-md border border-input bg-transparent px-3 py-2',
              'text-sm placeholder:text-muted-foreground',
              'focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring',
              'disabled:cursor-not-allowed disabled:opacity-50',
              'overflow-hidden'
            )}
          />
          
          {/* 送信ボタン */}
          <Button
            onClick={handleSend}
            disabled={!message.trim() || isLoading || disabled}
            size="icon"
            className="shrink-0"
          >
            {isLoading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </Button>
        </div>
        
        {/* ヒント表示 */}
        <p className="text-[10px] text-muted-foreground mt-1.5 text-center">
          Enter で送信 • Shift+Enter で改行 • @ でモード選択
        </p>
      </div>
    </div>
  );
}
