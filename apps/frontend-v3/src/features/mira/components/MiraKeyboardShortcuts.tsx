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

// ショートカットをカテゴリ別に整理
const SHORTCUT_CATEGORIES = [
  {
    category: '入力',
    shortcuts: [
      { keys: ['Enter'], description: 'メッセージを送信' },
      { keys: ['Shift', 'Enter'], description: '改行を挿入' },
      { keys: ['@'], description: 'モードを選択' },
      { keys: ['↑', '↓'], description: '入力履歴をナビゲート' },
    ],
  },
  {
    category: 'ナビゲーション',
    shortcuts: [
      { keys: ['N'], description: '入力欄にフォーカス' },
      { keys: ['J'], description: '次のメッセージへ' },
      { keys: ['K'], description: '前のメッセージへ' },
      { keys: ['G', 'G'], description: '最初のメッセージへ' },
      { keys: ['G', 'E'], description: '最新のメッセージへ' },
    ],
  },
  {
    category: '会話',
    shortcuts: [
      { keys: ['⌘/Ctrl', 'H'], description: '会話履歴を開く' },
      { keys: ['⌘/Ctrl', 'N'], description: '新しい会話' },
      { keys: ['C'], description: 'メッセージをコピー' },
    ],
  },
  {
    category: 'その他',
    shortcuts: [
      { keys: ['Esc'], description: 'メニュー/ダイアログを閉じる' },
      { keys: ['?'], description: 'ショートカット一覧を表示' },
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
        className="bg-popover border rounded-lg shadow-xl max-w-sm w-full mx-4"
        onClick={(e) => e.stopPropagation()}
      >
        {/* ヘッダー */}
        <div className="flex items-center justify-between p-4 border-b">
          <div className="flex items-center gap-2">
            <Keyboard className="w-5 h-5 text-primary" />
            <h2 className="font-semibold">キーボードショートカット</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded hover:bg-muted transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
        
        {/* ショートカット一覧 - カテゴリ別 */}
        <div className="p-4 space-y-4 max-h-[60vh] overflow-y-auto">
          {SHORTCUT_CATEGORIES.map(({ category, shortcuts }) => (
            <div key={category}>
              <h3 className="text-xs font-semibold text-primary uppercase tracking-wider mb-2">
                {category}
              </h3>
              <div className="space-y-2">
                {shortcuts.map(({ keys, description }) => (
                  <div 
                    key={description}
                    className="flex items-center justify-between"
                  >
                    <span className="text-sm text-muted-foreground">{description}</span>
                    <div className="flex items-center gap-1">
                      {keys.map((key, i) => (
                        <span key={i} className="flex items-center gap-1">
                          <kbd
                            className={cn(
                              "px-2 py-1 text-xs font-mono",
                              "bg-muted border rounded shadow-sm"
                            )}
                          >
                            {key}
                          </kbd>
                          {i < keys.length - 1 && (
                            <span className="text-muted-foreground text-xs">+</span>
                          )}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
        
        {/* フッター */}
        <div className="px-4 py-3 border-t bg-muted/30 text-center">
          <p className="text-xs text-muted-foreground">
            <kbd className="px-1.5 py-0.5 text-xs bg-muted border rounded">?</kbd>
            {' '}を押すとこの画面を表示
          </p>
        </div>
      </div>
    </div>
  );
}
