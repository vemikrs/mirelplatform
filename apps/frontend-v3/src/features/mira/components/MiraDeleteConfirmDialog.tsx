/**
 * Mira 削除確認ダイアログ
 * 
 * 会話やメッセージの削除時に確認を求めるダイアログ
 */
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Button,
} from '@mirel/ui';
import { AlertTriangle } from 'lucide-react';

interface MiraDeleteConfirmDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title?: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDestructive?: boolean;
}

export function MiraDeleteConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title = '本当に削除しますか?',
  description = 'この操作は取り消せません。',
  confirmLabel = '削除',
  cancelLabel = 'キャンセル',
  isDestructive = true,
}: MiraDeleteConfirmDialogProps) {
  const handleConfirm = () => {
    onConfirm();
    onClose();
  };
  
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <div className="flex items-center gap-3">
            {isDestructive && (
              <div className="w-10 h-10 rounded-full bg-destructive/10 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-5 h-5 text-destructive" />
              </div>
            )}
            <DialogTitle>{title}</DialogTitle>
          </div>
          <DialogDescription className={isDestructive ? 'pl-13' : ''}>
            {description}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {cancelLabel}
          </Button>
          <Button
            variant={isDestructive ? 'destructive' : 'default'}
            onClick={handleConfirm}
          >
            {confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
