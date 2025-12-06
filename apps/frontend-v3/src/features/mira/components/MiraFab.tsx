/**
 * Mira FAB (Floating Action Button)
 * 
 * 右端にくっついて隠れる形式のMira起動トリガー。
 * ホバーまたはクリックでスライドアウトして表示される。
 */
import { useState } from 'react';
import { cn } from '@mirel/ui';
import { Bot, Sparkles, ChevronLeft } from 'lucide-react';
import { useMiraPanel } from '@/hooks/useMira';

interface MiraFabProps {
  className?: string;
}

export function MiraFab({ className }: MiraFabProps) {
  const { isOpen, toggle } = useMiraPanel();
  const [isHovered, setIsHovered] = useState(false);
  
  // パネルが開いているときは非表示
  if (isOpen) {
    return null;
  }
  
  return (
    <button
      onClick={toggle}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      className={cn(
        // 右端に固定、下から80px（フッターの上）
        'fixed right-0 bottom-20 z-40',
        // 基本スタイル
        'flex items-center gap-2',
        'bg-primary text-primary-foreground',
        'rounded-l-lg shadow-lg',
        'transition-all duration-300 ease-out',
        // ホバー時にスライドアウト
        isHovered 
          ? 'translate-x-0 pr-4 pl-3 py-2.5' 
          : 'translate-x-[calc(100%-12px)] pr-3 pl-2 py-2',
        // ホバーエフェクト
        'hover:shadow-xl',
        className
      )}
      title="Mira AI アシスタント (Ctrl+Shift+M)"
      aria-label="Mira AI アシスタントを開く"
    >
      {/* 隠れているときに見えるインジケータ */}
      <ChevronLeft 
        className={cn(
          'w-3 h-3 transition-opacity duration-200',
          isHovered ? 'opacity-0 w-0' : 'opacity-70'
        )} 
      />
      
      {/* アイコン */}
      <div className="relative shrink-0">
        <Bot className="w-5 h-5" />
        <Sparkles 
          className={cn(
            'w-2.5 h-2.5 absolute -top-0.5 -right-0.5 text-yellow-300',
            'transition-transform duration-200',
            isHovered ? 'scale-110' : 'scale-100'
          )} 
        />
      </div>
      
      {/* テキスト（ホバー時のみ表示） */}
      <span 
        className={cn(
          'text-sm font-medium whitespace-nowrap',
          'transition-all duration-200',
          isHovered ? 'opacity-100 max-w-20' : 'opacity-0 max-w-0 overflow-hidden'
        )}
      >
        Mira
      </span>
    </button>
  );
}
