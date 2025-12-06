/**
 * Mira Fullscreen View Component
 * 
 * 全画面表示のチャットUI（2カラムレイアウト）
 * ui-design.md §3.3 準拠
 */
import { useEffect, useRef } from 'react';
import { cn, Button, ScrollArea } from '@mirel/ui';
import { X, MessageSquarePlus, Trash2, Bot, Minimize2, HelpCircle, AlertCircle, Wrench, GitBranch } from 'lucide-react';
import { useMira, useMiraPanel } from '@/hooks/useMira';
import { useMiraStore } from '@/stores/miraStore';
import { MiraChatMessage } from './MiraChatMessage';
import { MiraChatInput } from './MiraChatInput';
import type { MiraMode } from '@/lib/api/mira';

/** ショートカットボタンの定義 */
const MODE_SHORTCUTS = [
  { id: 'help', label: 'Help', icon: HelpCircle, mode: 'CONTEXT_HELP' as const },
  { id: 'error', label: 'Error', icon: AlertCircle, mode: 'ERROR_ANALYZE' as const },
  { id: 'studio', label: 'Studio', icon: Wrench, mode: 'STUDIO_AGENT' as const },
  { id: 'workflow', label: 'Workflow', icon: GitBranch, mode: 'WORKFLOW_AGENT' as const },
] as const;

interface MiraFullscreenViewProps {
  className?: string;
  onMinimize?: () => void;
}

export function MiraFullscreenView({ className, onMinimize }: MiraFullscreenViewProps) {
  const {
    sendMessage,
    isLoading,
    error,
    clearError,
    messages,
    activeConversation,
    clearConversation,
    newConversation,
  } = useMira();
  
  const { close: closePanel } = useMiraPanel();
  const togglePanel = useMiraStore((state) => state.togglePanel);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // Escape でフルスクリーン終了
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onMinimize?.();
      }
      if (e.ctrlKey && e.shiftKey && e.key === 'M') {
        e.preventDefault();
        togglePanel();
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onMinimize, togglePanel]);
  
  const handleSend = (message: string, mode?: MiraMode) => {
    sendMessage(message, { mode });
  };
  
  const handleModeShortcut = (mode: MiraMode) => {
    const shortcutMessages: Record<string, string> = {
      CONTEXT_HELP: 'この画面について説明してください',
      ERROR_ANALYZE: '発生したエラーについて説明してください',
      STUDIO_AGENT: 'Studio での設計について相談させてください',
      WORKFLOW_AGENT: 'ワークフローの設計について相談させてください',
    };
    sendMessage(shortcutMessages[mode] || 'サポートをお願いします', { mode });
  };
  
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
        'fixed inset-0 z-50 bg-background',
        'flex flex-col',
        className
      )}
      role="dialog"
      aria-label="Mira AI アシスタント（全画面）"
    >
      {/* ヘッダー */}
      <div className="flex items-center justify-between p-4 border-b bg-surface">
        <div className="flex items-center gap-3">
          <Bot className="w-6 h-6 text-primary" />
          <h1 className="text-xl font-semibold">Mira</h1>
          {activeConversation?.mode && (
            <span className="px-3 py-1 text-sm rounded-full bg-primary/10 text-primary font-medium">
              {getModeLabel(activeConversation.mode)}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={newConversation}
            title="新しい会話"
          >
            <MessageSquarePlus className="w-5 h-5" />
          </Button>
          {messages.length > 0 && (
            <Button
              variant="ghost"
              size="icon"
              onClick={() => clearConversation()}
              title="会話をクリア"
            >
              <Trash2 className="w-5 h-5" />
            </Button>
          )}
          {onMinimize && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onMinimize}
              title="最小化"
            >
              <Minimize2 className="w-5 h-5" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon"
            onClick={closePanel}
            title="閉じる (Ctrl+Shift+M)"
          >
            <X className="w-5 h-5" />
          </Button>
        </div>
      </div>
      
      {/* エラー表示 */}
      {error && (
        <div className="p-4 bg-destructive/10 text-destructive flex items-center justify-between">
          <span>{error}</span>
          <Button
            variant="ghost"
            size="sm"
            onClick={clearError}
          >
            閉じる
          </Button>
        </div>
      )}
      
      {/* メインコンテンツ - 2カラム */}
      <div className="flex-1 flex min-h-0">
        {/* 左カラム: メッセージエリア (60-70%) */}
        <div className="flex-1 flex flex-col min-w-0 border-r">
          <ScrollArea className="flex-1">
            <div className="p-4 max-w-4xl mx-auto">
              {messages.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-64 text-muted-foreground">
                  <Bot className="w-16 h-16 mb-6 opacity-50" />
                  <h2 className="text-lg font-medium mb-2">
                    こんにちは！
                  </h2>
                  <p className="text-center">
                    何かお手伝いできることはありますか？<br />
                    右側のショートカットから質問を始めることもできます
                  </p>
                </div>
              ) : (
                <>
                  {messages.map((message) => (
                    <MiraChatMessage key={message.id} message={message} />
                  ))}
                  <div ref={messagesEndRef} />
                </>
              )}
            </div>
          </ScrollArea>
          
          {/* 入力エリア */}
          <div className="max-w-4xl mx-auto w-full">
            <MiraChatInput
              onSend={handleSend}
              isLoading={isLoading}
              placeholder="メッセージを入力... (Enter で送信)"
              showShortcuts={false}
            />
          </div>
        </div>
        
        {/* 右カラム: コンテキストパネル (30-40%) */}
        <div className="w-80 flex-shrink-0 flex flex-col bg-surface-subtle/30">
          <ScrollArea className="flex-1">
            <div className="p-4 space-y-6">
              {/* 現在のコンテキスト */}
              <section>
                <h3 className="text-sm font-semibold text-muted-foreground mb-3">
                  現在のコンテキスト
                </h3>
                <div className="space-y-2 text-sm">
                  {activeConversation?.context ? (
                    <>
                      {activeConversation.context.appId && (
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">アプリ</span>
                          <span className="font-medium">{activeConversation.context.appId}</span>
                        </div>
                      )}
                      {activeConversation.context.screenId && (
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">画面</span>
                          <span className="font-medium">{activeConversation.context.screenId}</span>
                        </div>
                      )}
                      {activeConversation.context.appRole && (
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">ロール</span>
                          <span className="font-medium">{activeConversation.context.appRole}</span>
                        </div>
                      )}
                    </>
                  ) : (
                    <p className="text-muted-foreground">
                      コンテキスト情報なし
                    </p>
                  )}
                </div>
              </section>
              
              {/* モードショートカット */}
              <section>
                <h3 className="text-sm font-semibold text-muted-foreground mb-3">
                  クイックアクション
                </h3>
                <div className="space-y-2">
                  {MODE_SHORTCUTS.map((shortcut) => (
                    <Button
                      key={shortcut.id}
                      variant="outline"
                      size="sm"
                      onClick={() => handleModeShortcut(shortcut.mode)}
                      disabled={isLoading}
                      className="w-full justify-start"
                    >
                      <shortcut.icon className="w-4 h-4 mr-2" />
                      {shortcut.label}
                    </Button>
                  ))}
                </div>
              </section>
              
              {/* 会話統計 */}
              <section>
                <h3 className="text-sm font-semibold text-muted-foreground mb-3">
                  会話情報
                </h3>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">メッセージ数</span>
                    <span className="font-medium">{messages.length}</span>
                  </div>
                  {activeConversation?.createdAt && (
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">開始時刻</span>
                      <span className="font-medium">
                        {new Date(activeConversation.createdAt).toLocaleTimeString('ja-JP', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                  )}
                </div>
              </section>
              
              {/* キーボードショートカット */}
              <section>
                <h3 className="text-sm font-semibold text-muted-foreground mb-3">
                  キーボードショートカット
                </h3>
                <div className="space-y-1 text-xs text-muted-foreground">
                  <div className="flex justify-between">
                    <span>パネル開閉</span>
                    <kbd className="px-1.5 py-0.5 bg-muted rounded text-foreground">Ctrl+Shift+M</kbd>
                  </div>
                  <div className="flex justify-between">
                    <span>全画面終了</span>
                    <kbd className="px-1.5 py-0.5 bg-muted rounded text-foreground">Escape</kbd>
                  </div>
                  <div className="flex justify-between">
                    <span>送信</span>
                    <kbd className="px-1.5 py-0.5 bg-muted rounded text-foreground">Enter</kbd>
                  </div>
                </div>
              </section>
            </div>
          </ScrollArea>
        </div>
      </div>
    </div>
  );
}
