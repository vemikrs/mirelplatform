import { useState, useCallback } from 'react';
import { Button, Input } from '@mirel/ui';
import { Sparkles, Edit2, Check, X, PanelLeft } from 'lucide-react';
import { useOs } from '@/lib/hooks/useOs';
import { MiraMenu } from './MiraMenu';
import type { MiraConversation } from '@/stores/miraStore';

interface MiraHeaderProps {
  isSidebarOpen: boolean;
  onToggleSidebar: () => void;
  activeConversation: MiraConversation | null;
  getConversationSummary: () => string;
  getModeLabel: (mode?: string) => string | null;
  onNewConversation: () => void;
  onOpenContextEditor: () => void;
  onOpenShortcuts: () => void;
  onExport: () => void;
  onClearConversation: () => void;
  onUpdateTitle: (conversationId: string, title: string) => Promise<void>;
  onRegenerateTitle: () => Promise<void>;
  isExporting: boolean;
  isUpdatingTitle: boolean;
  hasMessages: boolean;
}

export function MiraHeader({
  onToggleSidebar,
  activeConversation,
  getConversationSummary,
  getModeLabel,
  onNewConversation,
  onOpenContextEditor,
  onOpenShortcuts,
  onExport,
  onClearConversation,
  onUpdateTitle,
  onRegenerateTitle,
  isExporting,
  isUpdatingTitle,
  hasMessages,
}: MiraHeaderProps) {
  const { metaKey } = useOs();
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editedTitle, setEditedTitle] = useState('');

  // タイトル編集開始
  const handleStartEditTitle = useCallback(() => {
    if (activeConversation) {
      setEditedTitle(activeConversation.title || getConversationSummary());
      setIsEditingTitle(true);
    }
  }, [activeConversation, getConversationSummary]);

  // タイトル編集保存
  const handleSaveTitle = useCallback(async () => {
    if (activeConversation && editedTitle.trim()) {
      try {
        await onUpdateTitle(activeConversation.id, editedTitle.trim());
        setIsEditingTitle(false);
      } catch (error) {
        console.error('タイトル更新エラー:', error);
      }
    }
  }, [activeConversation, editedTitle, onUpdateTitle]);

  // タイトル編集キャンセル
  const handleCancelEditTitle = useCallback(() => {
    setIsEditingTitle(false);
    setEditedTitle('');
  }, []);

  return (
    <div className="flex items-center justify-between p-3 border-b bg-surface">
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <Button
          variant="ghost"
          size="icon"
          onClick={onToggleSidebar}
          title={`サイドバーを切替 (${metaKey}+Shift+H)`}
        >
          <PanelLeft className="w-5 h-5" />
        </Button>
        <div className="flex items-center gap-2 flex-1 min-w-0">
          <Sparkles className="w-4 h-4 text-primary shrink-0" />
          {isEditingTitle ? (
            <div className="flex items-center gap-2 flex-1 min-w-0">
              <Input
                value={editedTitle}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditedTitle(e.target.value)}
                onKeyDown={(e: React.KeyboardEvent<HTMLInputElement>) => {
                  if (e.key === 'Enter') {
                    handleSaveTitle();
                  } else if (e.key === 'Escape') {
                    handleCancelEditTitle();
                  }
                }}
                className="h-8 text-sm flex-1"
                autoFocus
                disabled={isUpdatingTitle}
              />
              <Button
                variant="ghost"
                size="icon"
                onClick={handleSaveTitle}
                disabled={isUpdatingTitle}
                title="保存 (Enter)"
                className="h-8 w-8"
              >
                <Check className="w-4 h-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                onClick={handleCancelEditTitle}
                disabled={isUpdatingTitle}
                title="キャンセル (Esc)"
                className="h-8 w-8"
              >
                <X className="w-4 h-4" />
              </Button>
            </div>
          ) : (
            <>
              <h2 className="font-medium text-sm truncate">
                {getConversationSummary()}
              </h2>
              {activeConversation && (
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleStartEditTitle}
                  title="タイトルを編集"
                  className="h-6 w-6 shrink-0"
                >
                  <Edit2 className="w-3 h-3" />
                </Button>
              )}
            </>
          )}
          {activeConversation?.mode && activeConversation.mode !== 'GENERAL_CHAT' && (
            <span className="text-xs px-1.5 py-0.5 rounded bg-primary/10 text-primary shrink-0">
              {getModeLabel(activeConversation.mode)}
            </span>
          )}
        </div>
      </div>
      <div>
        <MiraMenu 
          onNewConversation={onNewConversation}
          onOpenContextEditor={onOpenContextEditor}
          onOpenShortcuts={onOpenShortcuts}
          onExport={onExport}
          onClearConversation={onClearConversation}
          onRegenerateTitle={onRegenerateTitle}
          isExporting={isExporting}
          hasMessages={hasMessages}
        />
      </div>
    </div>
  );
}
