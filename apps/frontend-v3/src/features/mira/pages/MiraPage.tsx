/**
 * Mira AI Assistant - 専用ページ
 * 
 * チャット履歴の一覧と汎用AIチャット機能を提供する専用画面。
 * 3カラムレイアウト:
 * - 左: 会話履歴リスト
 * - 中央: チャットエリア
 * - 右: コンテキストパネル（メタデータ・アクション）
 */
import { useCallback, useEffect, useRef } from 'react';
import { Button, ScrollArea } from '@mirel/ui';
import { 
  Bot, 
  Sparkles,
  Trash2,
  ChevronRight,
} from 'lucide-react';
import { useMira } from '@/hooks/useMira';
import { useMiraStore } from '@/stores/miraStore';
import type { MiraMode } from '@/lib/api/mira';
import { MiraChatMessage } from '../components/MiraChatMessage';
import { MiraChatInput } from '../components/MiraChatInput';
import { MiraConversationList } from '../components/MiraConversationList';
import { MiraContextPanel } from '../components/MiraContextPanel';

export function MiraPage() {
  const {
    sendMessage,
    isLoading,
    error,
    clearError,
    messages,
    activeConversation,
    conversations,
    clearConversation,
    newConversation,
  } = useMira();
  
  const setActiveConversation = useMiraStore((state) => state.setActiveConversation);
  const deleteConversation = useMiraStore((state) => state.deleteConversation);
  
  // 現在のモードはアクティブな会話から取得
  const currentMode = activeConversation?.mode ?? 'GENERAL_CHAT';
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // メッセージ送信ハンドラ
  const handleSend = useCallback((message: string, mode?: MiraMode) => {
    sendMessage(message, { mode });
  }, [sendMessage]);
  
  // モード変更ハンドラ：現在の会話のモードを変更するか、新しい会話を開始
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const handleModeChange = useCallback((_mode: MiraMode) => {
    // 既存の会話がない場合、新しい会話を開始
    if (!activeConversation) {
      newConversation();
    }
    // TODO: ストアにsetMode追加後、モード切替を実装
  }, [activeConversation, newConversation]);
  
  // 会話選択ハンドラ
  const handleSelectConversation = useCallback((conversationId: string) => {
    setActiveConversation(conversationId);
  }, [setActiveConversation]);
  
  // 会話削除ハンドラ
  const handleDeleteConversation = useCallback((conversationId: string) => {
    deleteConversation(conversationId);
  }, [deleteConversation]);
  
  // 会話の要約を取得
  const getConversationSummary = useCallback(() => {
    if (!activeConversation) return 'Mira AI Assistant';
    const firstUserMessage = activeConversation.messages.find((m) => m.role === 'user');
    if (firstUserMessage) {
      return firstUserMessage.content.length > 50
        ? firstUserMessage.content.substring(0, 50) + '...'
        : firstUserMessage.content;
    }
    return '新しい会話';
  }, [activeConversation]);
  
  // モード表示用のバッジラベル
  const getModeLabel = (mode?: string) => {
    const labels: Record<string, string> = {
      GENERAL_CHAT: 'General',
      CONTEXT_HELP: 'Help',
      ERROR_ANALYZE: 'Error',
      STUDIO_AGENT: 'Studio',
      WORKFLOW_AGENT: 'Workflow',
    };
    return mode ? labels[mode] || mode : null;
  };
  
  return (
    <div className="h-[calc(100vh-6rem)] flex">
      {/* 左サイドバー: 会話履歴 */}
      <MiraConversationList
        conversations={conversations}
        activeConversationId={activeConversation?.id ?? null}
        onSelect={handleSelectConversation}
        onDelete={handleDeleteConversation}
        onNewConversation={newConversation}
      />
      
      {/* メインエリア: チャット */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* チャットヘッダー */}
        <div className="flex items-center justify-between p-4 border-b bg-surface">
          <div className="flex items-center gap-3">
            <Sparkles className="w-5 h-5 text-primary" />
            <div>
              <h2 className="font-semibold">
                {getConversationSummary()}
              </h2>
              {activeConversation?.mode && (
                <span className="text-xs text-muted-foreground">
                  モード: {getModeLabel(activeConversation.mode)}
                </span>
              )}
            </div>
          </div>
          {messages.length > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => clearConversation()}
              className="gap-1"
            >
              <Trash2 className="w-4 h-4" />
              クリア
            </Button>
          )}
        </div>
        
        {/* エラー表示 */}
        {error && (
          <div className="p-3 bg-destructive/10 text-destructive text-sm flex items-center justify-between">
            <span>{error}</span>
            <Button variant="ghost" size="sm" onClick={clearError}>
              閉じる
            </Button>
          </div>
        )}
        
        {/* メッセージエリア */}
        <ScrollArea className="flex-1">
          <div className="p-4 max-w-4xl mx-auto">
            {messages.length === 0 ? (
              <MiraEmptyState onSendMessage={handleSend} />
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
            showShortcuts={true}
          />
        </div>
      </div>
      
      {/* 右サイドパネル: コンテキスト・メタデータ */}
      <MiraContextPanel
        conversation={activeConversation ?? null}
        currentMode={currentMode}
        onModeChange={handleModeChange}
      />
    </div>
  );
}

/** 空状態表示コンポーネント */
interface MiraEmptyStateProps {
  onSendMessage: (message: string) => void;
}

function MiraEmptyState({ onSendMessage }: MiraEmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center h-96 text-muted-foreground">
      <Bot className="w-16 h-16 mb-6 opacity-50" />
      <h3 className="text-lg font-medium mb-2">
        Mira AI Assistant へようこそ
      </h3>
      <p className="text-center max-w-md mb-6">
        何でも質問してください。プログラミング、設計、ドキュメント作成など、
        様々なタスクをサポートします。
      </p>
      <div className="grid grid-cols-2 gap-3 max-w-lg">
        <SuggestionCard
          icon={<ChevronRight className="w-4 h-4" />}
          title="コードの説明"
          description="コードの動作を解説"
          onClick={() => onSendMessage('このコードについて説明してください')}
        />
        <SuggestionCard
          icon={<ChevronRight className="w-4 h-4" />}
          title="設計のアドバイス"
          description="アーキテクチャの相談"
          onClick={() => onSendMessage('システム設計についてアドバイスをください')}
        />
        <SuggestionCard
          icon={<ChevronRight className="w-4 h-4" />}
          title="ドキュメント作成"
          description="README や仕様書の作成"
          onClick={() => onSendMessage('ドキュメントの作成を手伝ってください')}
        />
        <SuggestionCard
          icon={<ChevronRight className="w-4 h-4" />}
          title="バグの解析"
          description="エラーの原因を特定"
          onClick={() => onSendMessage('このエラーの原因を教えてください')}
        />
      </div>
    </div>
  );
}

/** サジェストカード */
interface SuggestionCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  onClick: () => void;
}

function SuggestionCard({ icon, title, description, onClick }: SuggestionCardProps) {
  return (
    <button
      onClick={onClick}
      className="flex items-start gap-3 p-3 rounded-lg border bg-surface hover:bg-surface-raised transition-colors text-left"
    >
      <div className="mt-0.5 text-primary">{icon}</div>
      <div>
        <p className="font-medium text-sm">{title}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
    </button>
  );
}
