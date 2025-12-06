/**
 * Mira AI Assistant - 専用ページ
 * 
 * チャット履歴の一覧と汎用AIチャット機能を提供する専用画面
 */
import { useEffect, useRef, useState } from 'react';
import { cn, Button, ScrollArea, Input } from '@mirel/ui';
import { 
  Bot, 
  MessageSquarePlus, 
  Trash2, 
  Search, 
  Clock,
  MessageCircle,
  ChevronRight,
  Sparkles
} from 'lucide-react';
import { useMira } from '@/hooks/useMira';
import { useMiraStore, type MiraConversation } from '@/stores/miraStore';
import { MiraChatMessage } from '../components/MiraChatMessage';
import { MiraChatInput } from '../components/MiraChatInput';
import type { MiraMode } from '@/lib/api/mira';

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
  
  const [searchQuery, setSearchQuery] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // メッセージ追加時に自動スクロール
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  const handleSend = (message: string, mode?: MiraMode) => {
    sendMessage(message, { mode });
  };
  
  // 会話をフィルタリング
  const filteredConversations = conversations.filter((conv) => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    // メッセージ内容で検索
    return conv.messages.some((msg) => 
      msg.content.toLowerCase().includes(query)
    );
  });
  
  // 会話を日付でソート（新しい順）
  const sortedConversations = [...filteredConversations].sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  );
  
  // 会話の要約を取得
  const getConversationSummary = (conv: MiraConversation) => {
    const firstUserMessage = conv.messages.find((m) => m.role === 'user');
    if (firstUserMessage) {
      return firstUserMessage.content.length > 50
        ? firstUserMessage.content.substring(0, 50) + '...'
        : firstUserMessage.content;
    }
    return '新しい会話';
  };
  
  // 相対時間を取得
  const getRelativeTime = (date: Date | string) => {
    const now = new Date();
    const target = new Date(date);
    const diffMs = now.getTime() - target.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);
    
    if (diffMins < 1) return 'たった今';
    if (diffMins < 60) return `${diffMins}分前`;
    if (diffHours < 24) return `${diffHours}時間前`;
    if (diffDays < 7) return `${diffDays}日前`;
    return target.toLocaleDateString('ja-JP');
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
    <div className="h-[calc(100vh-theme(spacing.24))] flex">
      {/* 左サイドバー: 会話履歴 */}
      <div className="w-80 border-r flex flex-col bg-surface-subtle/30">
        {/* ヘッダー */}
        <div className="p-4 border-b">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Bot className="w-5 h-5 text-primary" />
              <h1 className="font-semibold">Mira</h1>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={newConversation}
              className="gap-1"
            >
              <MessageSquarePlus className="w-4 h-4" />
              新規
            </Button>
          </div>
          
          {/* 検索 */}
          <div className="relative">
            <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              type="search"
              placeholder="会話を検索..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8 h-9"
            />
          </div>
        </div>
        
        {/* 会話リスト */}
        <ScrollArea className="flex-1">
          <div className="p-2 space-y-1">
            {sortedConversations.length === 0 ? (
              <div className="p-4 text-center text-muted-foreground text-sm">
                {searchQuery ? '該当する会話がありません' : '会話履歴がありません'}
              </div>
            ) : (
              sortedConversations.map((conv) => (
                <button
                  key={conv.id}
                  onClick={() => setActiveConversation(conv.id)}
                  className={cn(
                    "w-full text-left p-3 rounded-lg transition-colors group",
                    activeConversation?.id === conv.id
                      ? "bg-primary/10 border border-primary/20"
                      : "hover:bg-surface-raised"
                  )}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <MessageCircle className="w-3.5 h-3.5 text-muted-foreground flex-shrink-0" />
                        <span className="text-sm font-medium truncate">
                          {getConversationSummary(conv)}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <Clock className="w-3 h-3" />
                        <span>{getRelativeTime(conv.updatedAt)}</span>
                        {conv.mode && (
                          <span className="px-1.5 py-0.5 rounded bg-muted text-muted-foreground">
                            {getModeLabel(conv.mode)}
                          </span>
                        )}
                        <span>{conv.messages.length}件</span>
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="w-6 h-6 opacity-0 group-hover:opacity-100 transition-opacity"
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteConversation(conv.id);
                      }}
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </Button>
                  </div>
                </button>
              ))
            )}
          </div>
        </ScrollArea>
      </div>
      
      {/* メインエリア: チャット */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* チャットヘッダー */}
        <div className="flex items-center justify-between p-4 border-b bg-surface">
          <div className="flex items-center gap-3">
            <Sparkles className="w-5 h-5 text-primary" />
            <div>
              <h2 className="font-semibold">
                {activeConversation ? getConversationSummary(activeConversation) : 'Mira AI Assistant'}
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
                    onClick={() => handleSend('このコードについて説明してください')}
                  />
                  <SuggestionCard
                    icon={<ChevronRight className="w-4 h-4" />}
                    title="設計のアドバイス"
                    description="アーキテクチャの相談"
                    onClick={() => handleSend('システム設計についてアドバイスをください')}
                  />
                  <SuggestionCard
                    icon={<ChevronRight className="w-4 h-4" />}
                    title="ドキュメント作成"
                    description="README や仕様書の作成"
                    onClick={() => handleSend('ドキュメントの作成を手伝ってください')}
                  />
                  <SuggestionCard
                    icon={<ChevronRight className="w-4 h-4" />}
                    title="バグの解析"
                    description="エラーの原因を特定"
                    onClick={() => handleSend('このエラーの原因を教えてください')}
                  />
                </div>
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
            showShortcuts={true}
          />
        </div>
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
