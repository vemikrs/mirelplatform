/**
 * Mira FAB (Floating Action Button)
 * 
 * フッターの上に表示するMira起動ボタン
 */
import { cn, Button } from '@mirel/ui';
import { Bot, Sparkles } from 'lucide-react';
import { useMiraPanel } from '@/hooks/useMira';

interface MiraFabProps {
  className?: string;
}

export function MiraFab({ className }: MiraFabProps) {
  const { isOpen, toggle } = useMiraPanel();
  
  if (isOpen) {
    return null;
  }
  
  return (
    <Button
      onClick={toggle}
      className={cn(
        'fixed bottom-16 right-6 z-40',
        'h-12 px-4 rounded-full shadow-lg',
        'bg-gradient-to-r from-primary to-primary/80',
        'hover:from-primary/90 hover:to-primary/70',
        'text-primary-foreground font-medium',
        'transition-all duration-200 hover:scale-105 hover:shadow-xl',
        'flex items-center gap-2',
        className
      )}
      title="Mira AI アシスタント (Ctrl+Shift+M)"
    >
      <div className="relative">
        <Bot className="w-5 h-5" />
        <Sparkles className="w-3 h-3 absolute -top-1 -right-1 text-yellow-300" />
      </div>
      <span className="hidden sm:inline">Mira</span>
    </Button>
  );
}
