/**
 * Mira Chat Input Component
 * 
 * メッセージ入力フォーム + @メンション風モード選択 + 入力履歴
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

// 入力履歴の最大保持数
const MAX_HISTORY = 50;

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
  
  // 入力履歴管理
  const [inputHistory, setInputHistory] = useState<string[]>(() => {
    try {
      const saved = localStorage.getItem('mira-input-history');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });
  const [historyIndex, setHistoryIndex] = useState(-1);
  const [tempMessage, setTempMessage] = useState(''); // 履歴ナビ前のメッセージを保持
  
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
  
  // 入力履歴をlocalStorageに保存
  useEffect(() => {
    localStorage.setItem('mira-input-history', JSON.stringify(inputHistory));
  }, [inputHistory]);
  
  const handleSend = () => {
    const trimmed = message.trim();
    if (trimmed && !isLoading && !disabled) {
      // 履歴に追加（重複は除く）
      setInputHistory((prev) => {
        const filtered = prev.filter((h) => h !== trimmed);
        return [trimmed, ...filtered].slice(0, MAX_HISTORY);
      });
      
      onSend(trimmed, selectedMode);
      setMessage('');
      setHistoryIndex(-1);
      setTempMessage('');
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
      return;
    }
    // Enter のみは送信（Shift + Enter は改行）
    if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.metaKey) {
      e.preventDefault();
      handleSend();
      return;
    }
    // @ でモードメニューを開く
    if (e.key === '@' && message === '') {
      e.preventDefault();
      setShowModeMenu(true);
      return;
    }
    // Escape でメニューを閉じる
    if (e.key === 'Escape' && showModeMenu) {
      setShowModeMenu(false);
      return;
    }
    
    // 上下キーで入力履歴をナビゲート
    if (e.key === 'ArrowUp' && inputHistory.length > 0) {
      e.preventDefault();
      if (historyIndex === -1) {
        // 最初の履歴アクセス時、現在の入力を保存
        setTempMessage(message);
      }
      const newIndex = Math.min(historyIndex + 1, inputHistory.length - 1);
      setHistoryIndex(newIndex);
      setMessage(inputHistory[newIndex]);
      return;
    }
    
    if (e.key === 'ArrowDown' && historyIndex >= 0) {
      e.preventDefault();
      const newIndex = historyIndex - 1;
      setHistoryIndex(newIndex);
      if (newIndex < 0) {
        // 履歴を抜けたら保存していた入力を復元
        setMessage(tempMessage);
      } else {
        setMessage(inputHistory[newIndex]);
      }
      return;
    }
  };
  
  // モードボタンクリック時のトグル動作
  const handleModeButtonClick = () => {
    setShowModeMenu((prev) => !prev);
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
            onClick={handleModeButtonClick}
            className={cn(
              "flex items-center gap-1 px-2 py-1.5 text-xs rounded-md border",
              "hover:bg-muted transition-colors shrink-0",
              currentModeConfig.color
            )}
            title="モードを変更 (@)"
          >
            <CurrentIcon className="w-3.5 h-3.5" />
            <span>{currentModeConfig.shortLabel}</span>
            <ChevronDown className={cn("w-3 h-3 opacity-50 transition-transform", showModeMenu && "rotate-180")} />
          </button>
          
          {/* テキストエリア */}
          <textarea
            ref={textareaRef}
            value={message}
            onChange={(e) => {
              setMessage(e.target.value);
              // 手動入力時は履歴インデックスをリセット
              if (historyIndex >= 0) {
                setHistoryIndex(-1);
                setTempMessage('');
              }
            }}
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
          Enter で送信 • Shift+Enter で改行 • @ でモード選択 • ↑↓ で履歴
        </p>
      </div>
    </div>
  );
}
