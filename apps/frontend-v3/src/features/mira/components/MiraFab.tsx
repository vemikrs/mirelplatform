/**
 * Mira FAB (Floating Action Button)
 * 
 * 画面右下に表示するMira起動ボタン
 */
import { cn, Button } from '@mirel/ui';
import { Bot, X } from 'lucide-react';
import { useMiraPanel } from '@/hooks/useMira';

interface MiraFabProps {
  className?: string;
}

export function MiraFab({ className }: MiraFabProps) {
  const { isOpen, toggle } = useMiraPanel();
  
  return (
    <Button
      onClick={toggle}
      size="lg"
      className={cn(
        'fixed bottom-6 right-6 z-40',
        'w-14 h-14 rounded-full shadow-lg',
        'bg-primary hover:bg-primary/90',
        'transition-transform hover:scale-105',
        isOpen && 'hidden',
        className
      )}
      title="Mira AI アシスタント"
    >
      {isOpen ? (
        <X className="w-6 h-6" />
      ) : (
        <Bot className="w-6 h-6" />
      )}
    </Button>
  );
}
