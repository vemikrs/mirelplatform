/**
 * Mira キーボードショートカットオーバーレイ
 * 
 * ?キーで表示されるキーボードショートカット一覧
 */
import { useEffect, useCallback } from 'react';
import { cn } from '@mirel/ui';
import { X, Keyboard } from 'lucide-react';

interface MiraKeyboardShortcutsProps {
  isOpen: boolean;
  onClose: () => void;
}

// ショートカットをカテゴリ別に整理（2カラム用に左右に分割）
const LEFT_CATEGORIES = [
  {
    category: '入力',
    icon: '✏️',
    shortcuts: [
      { keys: ['Enter'], description: '送信' },
      { keys: ['Shift', 'Enter'], description: '改行' },
      { keys: ['@'], description: 'モード選択' },
      { keys: ['↑', '↓'], description: '履歴' },
    ],
  },
  {
    category: 'ナビゲーション',
    icon: '🧭',
    shortcuts: [
      { keys: ['N'], description: '入力欄へ' },
      { keys: ['J'], description: '次へ' },
      { keys: ['K'], description: '前へ' },
      { keys: ['G', 'G'], description: '先頭へ' },
      { keys: ['G', 'E'], description: '末尾へ' },
    ],
  },
];

const RIGHT_CATEGORIES = [
  {
    category: '会話',
    icon: '💬',
    shortcuts: [
      { keys: ['⌘', 'H'], description: '履歴を開く' },
      { keys: ['⌘', 'N'], description: '新規会話' },
      { keys: ['C'], description: 'コピー' },
    ],
  },
  {
    category: 'その他',
    icon: '⚙️',
    shortcuts: [
      { keys: ['Esc'], description: '閉じる' },
      { keys: ['?'], description: 'ヘルプ' },
    ],
  },
];

export function MiraKeyboardShortcuts({ isOpen, onClose }: MiraKeyboardShortcutsProps) {
  // Escapeキーで閉じる
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      onClose();
    }
  }, [onClose]);
  
  useEffect(() => {
    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      return () => document.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, handleKeyDown]);
  
  if (!isOpen) return null;
  
  return (
    <div 
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      onClick={onClose}
    >
      <div 
        className="bg-popover border rounded-xl shadow-2xl max-w-lg w-full mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        {/* ヘッダー */}
        <div className="flex items-center justify-between px-5 py-4 border-b">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center">
              <Keyboard className="w-4 h-4 text-primary" />
            </div>
            <h2 className="font-semibold text-base">キーボードショートカット</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-md hover:bg-muted transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
        
        {/* ショートカット一覧 - 2カラム */}
        <div className="p-5 grid grid-cols-2 gap-6">
          {/* 左カラム */}
          <div className="space-y-5">
            {LEFT_CATEGORIES.map(({ category, icon, shortcuts }) => (
              <ShortcutCategory key={category} category={category} icon={icon} shortcuts={shortcuts} />
            ))}
          </div>
          
          {/* 右カラム */}
          <div className="space-y-5">
            {RIGHT_CATEGORIES.map(({ category, icon, shortcuts }) => (
              <ShortcutCategory key={category} category={category} icon={icon} shortcuts={shortcuts} />
            ))}
          </div>
        </div>
        
        {/* フッター */}
        <div className="px-5 py-3 border-t bg-muted/20 flex items-center justify-center gap-1.5">
          <kbd className="px-1.5 py-0.5 text-xs font-mono bg-muted border rounded">?</kbd>
          <span className="text-xs text-muted-foreground">でこの画面を表示</span>
        </div>
      </div>
    </div>
  );
}

/** カテゴリ表示コンポーネント */
function ShortcutCategory({ 
  category, 
  icon, 
  shortcuts 
}: { 
  category: string; 
  icon: string; 
  shortcuts: { keys: string[]; description: string }[];
}) {
  return (
    <div className="space-y-2">
      <div className="flex items-center gap-1.5 pb-1 border-b border-border/50">
        <span className="text-sm">{icon}</span>
        <h3 className="text-xs font-semibold text-foreground uppercase tracking-wide">
          {category}
        </h3>
      </div>
      <div className="space-y-1.5">
        {shortcuts.map(({ keys, description }) => (
          <div 
            key={description}
            className="flex items-center justify-between gap-2"
          >
            <span className="text-xs text-muted-foreground truncate">{description}</span>
            <div className="flex items-center gap-0.5 shrink-0">
              {keys.map((key, i) => (
                <span key={i} className="flex items-center">
                  <kbd
                    className={cn(
                      "px-1.5 py-0.5 text-[10px] font-mono",
                      "bg-muted/80 border rounded shadow-sm",
                      "min-w-[20px] text-center"
                    )}
                  >
                    {key}
                  </kbd>
                  {i < keys.length - 1 && (
                    <span className="text-muted-foreground text-[10px] mx-0.5">+</span>
                  )}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
