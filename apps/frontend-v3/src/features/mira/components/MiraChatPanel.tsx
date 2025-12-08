/**
 * Mira Chat Panel Component
 * 
 * GitHub Copilot風の右下コンパクトウィンドウUI
 */
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  cn,
  Button,
  ScrollArea,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@mirel/ui';
import { X, MessageSquarePlus, Trash2, Bot, ExternalLink, Minus, Maximize2, Settings as SettingsIcon } from 'lucide-react';
import { useMira, useMiraPanel } from '@/hooks/useMira';
import { useMiraStore } from '@/stores/miraStore';
import { MiraChatMessage } from './MiraChatMessage';
import { MiraChatInput } from './MiraChatInput';
import type { MiraMode, MessageConfig } from '@/lib/api/mira';
import { useAuthStore } from '@/stores/authStore';
import {
  DropdownMenu,
  DropdownMenuContent,

  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuCheckboxItem,
} from '@mirel/ui';

interface MiraChatPanelProps {
  className?: string;
}

export function MiraChatPanel({ className }: MiraChatPanelProps) {
  const navigate = useNavigate();
  const {
    sendMessage,
    isLoading,
    error,
    clearError,
    messages,
    activeConversation,
    clearConversation,
    newConversation,
    editingMessageId,
    editingMessageContent,
    startEditMessage,
    cancelEditMessage,
    resendEditedMessage,
  } = useMira();
  
  const { isOpen, close: closePanel } = useMiraPanel();
  const togglePanel = useMiraStore((state) => state.togglePanel);
  const activeConversationId = useMiraStore((state) => state.activeConversationId);
  const useStream = useMiraStore((state) => state.useStream);
  const setUseStream = useMiraStore((state) => state.setUseStream);

  
  // 最小化状態
  const [isMinimized, setIsMinimized] = useState(false);
  
  // 編集確認ダイアログ
  const [showEditConfirm, setShowEditConfirm] = useState(false);
  const [pendingEditMessageId, setPendingEditMessageId] = useState<string | null>(null);
  const [affectedMessagesCount, setAffectedMessagesCount] = useState(0);
  


  // Settings State (G-5)
  const [selectedProvider, setSelectedProvider] = useState<string>(''); // empty = auto
  const user = useAuthStore((state) => state.user);
  const isAdmin = user?.roles?.includes('ADMIN') || user?.roles?.includes('SYSTEM_ADMIN');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // Ctrl+Shift+M でパネル開閉
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key === 'M') {
        e.preventDefault();
        togglePanel();
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [togglePanel]);
  
  const handleSend = (message: string, mode?: MiraMode, config?: MessageConfig) => {
    if (editingMessageId && activeConversationId) {
      // 編集モードでの再送信
      resendEditedMessage(activeConversationId, editingMessageId);
      // 編集後のメッセージで新規送信
      sendMessage(message, { mode, messageConfig: config, forceProvider: selectedProvider || undefined });
    } else {
      // 通常の送信
      sendMessage(message, { mode, messageConfig: config, forceProvider: selectedProvider || undefined });
    }
    // 送信時に最小化を解除
    if (isMinimized) {
      setIsMinimized(false);
    }
  };
  
  const handleEditMessage = (messageId: string) => {
    if (!activeConversationId) return;
    
    // 編集点以降のメッセージ数をカウント
    const messageIndex = messages.findIndex(m => m.id === messageId);
    if (messageIndex === -1) return;
    
    const affectedCount = messages.length - messageIndex - 1;
    
    if (affectedCount > 0) {
      // 以降にメッセージがある場合は警告ダイアログを表示
      setPendingEditMessageId(messageId);
      setAffectedMessagesCount(affectedCount);
      setShowEditConfirm(true);
    } else {
      // 最新メッセージの場合は直ちに編集モードへ
      startEditMessage(activeConversationId, messageId);
    }
  };
  
  const handleConfirmEdit = () => {
    if (activeConversationId && pendingEditMessageId) {
      startEditMessage(activeConversationId, pendingEditMessageId);
    }
    setShowEditConfirm(false);
    setPendingEditMessageId(null);
  };
  
  const handleCancelConfirmEdit = () => {
    setShowEditConfirm(false);
    setPendingEditMessageId(null);
  };
  
  // 専用画面で開く
  const handleOpenInFullPage = () => {
    closePanel();
    if (activeConversationId) {
      navigate(`/mira?conversation=${activeConversationId}`);
    } else {
      navigate('/mira');
    }
  };
  
  if (!isOpen) {
    return null;
  }
  
  // モード表示用のバッジラベル
  const getModeLabel = (mode?: string) => {
    const labels: Record<string, string> = {
      general_chat: 'General',
      context_help: 'Help',
      error_analyze: 'Error',
      studio_agent: 'Studio',
      workflow_agent: 'Workflow',
      GENERAL_CHAT: 'General',
      CONTEXT_HELP: 'Help',
      ERROR_ANALYZE: 'Error',
      STUDIO_AGENT: 'Studio',
      WORKFLOW_AGENT: 'Workflow',
    };
    return mode ? labels[mode] || mode : null;
  };
  
  return (
    <div
      className={cn(
        // 右下に固定配置（GitHub Copilot風）
        'fixed right-4 bottom-4 z-50',
        // サイズ
        'w-[380px] max-w-[calc(100vw-2rem)]',
        // 最小化時は高さを縮める
        isMinimized ? 'h-auto' : 'h-[500px] max-h-[calc(100vh-6rem)]',
        // スタイル
        'bg-background rounded-xl border shadow-2xl',
        'flex flex-col overflow-hidden',
        // アニメーション
        'animate-in slide-in-from-bottom-4 fade-in duration-200',
        className
      )}
      role="dialog"
      aria-label="Mira AI アシスタント"
    >
      {/* ヘッダー */}
      <div className="flex items-center justify-between px-3 py-2 border-b bg-muted/30">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center">
            <Bot className="w-3.5 h-3.5 text-primary" />
          </div>
          <span className="font-medium text-sm">Mira</span>
          {activeConversation?.mode && activeConversation.mode !== 'GENERAL_CHAT' && (
            <span className="px-1.5 py-0.5 text-[10px] rounded bg-primary/10 text-primary font-medium">
              {getModeLabel(activeConversation.mode)}
            </span>
          )}
        </div>
        <div className="flex items-center gap-0.5">
          {isAdmin && (
             <DropdownMenu>
               <DropdownMenuTrigger asChild>
                 <Button variant="ghost" size="icon" className="h-7 w-7" title="管理者設定">
                   <SettingsIcon className="w-3.5 h-3.5" />
                 </Button>
               </DropdownMenuTrigger>
               <DropdownMenuContent align="end">
                 <DropdownMenuLabel>AI Provider (Debug)</DropdownMenuLabel>
                 <DropdownMenuSeparator />
                 <DropdownMenuRadioGroup value={selectedProvider} onValueChange={setSelectedProvider}>
                   <DropdownMenuRadioItem value="">Auto (Default)</DropdownMenuRadioItem>
                   <DropdownMenuRadioItem value="azure-openai">Azure OpenAI</DropdownMenuRadioItem>
                   <DropdownMenuRadioItem value="openai">OpenAI (Original)</DropdownMenuRadioItem>
                    <DropdownMenuRadioItem value="mock">Mock Provider</DropdownMenuRadioItem>
                   </DropdownMenuRadioGroup>
                   <DropdownMenuSeparator />
                   <DropdownMenuLabel>Streaming</DropdownMenuLabel>
                   <DropdownMenuCheckboxItem 
                      checked={useStream}
                      onCheckedChange={setUseStream}
                   >
                      Enable Streaming
                   </DropdownMenuCheckboxItem>
               </DropdownMenuContent>
             </DropdownMenu>
          )}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={newConversation}
            title="新しい会話"
          >
            <MessageSquarePlus className="w-3.5 h-3.5" />
          </Button>
          {messages.length > 0 && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => clearConversation()}
              title="会話をクリア"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={handleOpenInFullPage}
            title="専用画面で開く"
          >
            <ExternalLink className="w-3.5 h-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => setIsMinimized(!isMinimized)}
            title={isMinimized ? '展開' : '最小化'}
          >
            {isMinimized ? (
              <Maximize2 className="w-3.5 h-3.5" />
            ) : (
              <Minus className="w-3.5 h-3.5" />
            )}
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={closePanel}
            title="閉じる (Ctrl+Shift+M)"
          >
            <X className="w-3.5 h-3.5" />
          </Button>
        </div>
      </div>
      
      {/* 最小化時は入力欄のみ表示 */}
      {isMinimized ? (
        <MiraChatInput
          onSend={handleSend}
          isLoading={isLoading}
          placeholder="質問を入力..."
          compact
        />
      ) : (
        <>
          {/* エラー表示 */}
          {error && (
            <div className="px-3 py-2 bg-destructive/10 text-destructive text-xs flex items-center justify-between">
              <span className="truncate">{error}</span>
              <Button
                variant="ghost"
                size="sm"
                className="h-6 text-xs"
                onClick={clearError}
              >
                閉じる
              </Button>
            </div>
          )}
          
          {/* メッセージエリア */}
          <ScrollArea className="flex-1">
            <div className="p-3">
              {messages.length === 0 ? (
                <MiraCompactEmptyState onSend={handleSend} />
              ) : (
                <>
                  {messages.map((message) => (
                    <MiraChatMessage 
                      key={message.id} 
                      message={message} 
                      compact 
                      onEdit={handleEditMessage}
                    />
                  ))}
                  <div ref={messagesEndRef} />
                </>
              )}
            </div>
          </ScrollArea>
          
          {/* 入力エリア */}
          <MiraChatInput
            onSend={handleSend}
            isLoading={isLoading}
            placeholder="質問を入力... (Enter で送信)"
            compact
            editingMessageId={editingMessageId || undefined}
            editingMessageContent={editingMessageContent || undefined}
            onCancelEdit={cancelEditMessage}
          />
        </>
      )}
      
      {/* 編集確認ダイアログ */}
      <Dialog open={showEditConfirm} onOpenChange={setShowEditConfirm}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>メッセージを編集</DialogTitle>
            <DialogDescription>
              このメッセージを編集すると、以降の会話履歴（{affectedMessagesCount}件）が削除されます。
              <br />
              よろしいですか？
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={handleCancelConfirmEdit}>
              キャンセル
            </Button>
            <Button onClick={handleConfirmEdit}>
              削除して再送信
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

/** コンパクト空状態 */
function MiraCompactEmptyState({ onSend }: { onSend: (message: string) => void }) {
  const quickActions = [
    { label: 'この画面の説明', message: 'この画面の使い方を教えてください' },
    { label: 'エラー解析', message: 'エラーの原因を分析してください' },
    { label: 'コードレビュー', message: 'コードをレビューしてください' },
  ];
  
  return (
    <div className="flex flex-col items-center py-8 text-center">
      <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center mb-3">
        <Bot className="w-5 h-5 text-primary" />
      </div>
      <p className="text-sm text-muted-foreground mb-4">
        何かお手伝いしましょうか？
      </p>
      <div className="flex flex-wrap justify-center gap-2">
        {quickActions.map((action) => (
          <button
            key={action.label}
            onClick={() => onSend(action.message)}
            className="px-2.5 py-1 text-xs rounded-full border hover:bg-muted transition-colors"
          >
            {action.label}
          </button>
        ))}
      </div>
    </div>
  );
}
