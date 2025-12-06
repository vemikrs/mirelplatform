/**
 * Mira 会話履歴リストコンポーネント
 * 
 * 会話の一覧表示、検索、選択、削除機能を提供
 */
import { useState } from 'react';
import { cn, Button, ScrollArea, Input } from '@mirel/ui';
import { 
  Bot, 
  MessageSquarePlus, 
  Trash2, 
  Search, 
  Clock,
  MessageCircle
} from 'lucide-react';
import type { MiraConversation } from '@/stores/miraStore';

interface MiraConversationListProps {
  conversations: MiraConversation[];
  activeConversationId: string | null;
  onSelect: (conversationId: string) => void;
  onDelete: (conversationId: string) => void;
  onNewConversation: () => void;
}

export function MiraConversationList({
  conversations,
  activeConversationId,
  onSelect,
  onDelete,
  onNewConversation,
}: MiraConversationListProps) {
  const [searchQuery, setSearchQuery] = useState('');
  
  // 会話をフィルタリング
  const filteredConversations = conversations.filter((conv) => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return conv.messages.some((msg) => 
      msg.content.toLowerCase().includes(query)
    );
  });
  
  // 会話を日付でソート（新しい順）
  const sortedConversations = [...filteredConversations].sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  );
  
  return (
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
            onClick={onNewConversation}
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
              <ConversationItem
                key={conv.id}
                conversation={conv}
                isActive={activeConversationId === conv.id}
                onSelect={() => onSelect(conv.id)}
                onDelete={() => onDelete(conv.id)}
              />
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}

/** 会話アイテム */
interface ConversationItemProps {
  conversation: MiraConversation;
  isActive: boolean;
  onSelect: () => void;
  onDelete: () => void;
}

function ConversationItem({ conversation, isActive, onSelect, onDelete }: ConversationItemProps) {
  const summary = getConversationSummary(conversation);
  const relativeTime = getRelativeTime(conversation.updatedAt);
  const modeLabel = getModeLabel(conversation.mode);
  
  return (
    <button
      onClick={onSelect}
      className={cn(
        "w-full text-left p-3 rounded-lg transition-colors group",
        isActive
          ? "bg-primary/10 border border-primary/20"
          : "hover:bg-surface-raised"
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-1">
            <MessageCircle className="w-3.5 h-3.5 text-muted-foreground shrink-0" />
            <span className="text-sm font-medium truncate">
              {summary}
            </span>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Clock className="w-3 h-3" />
            <span>{relativeTime}</span>
            {modeLabel && (
              <span className="px-1.5 py-0.5 rounded bg-muted text-muted-foreground">
                {modeLabel}
              </span>
            )}
            <span>{conversation.messages.length}件</span>
          </div>
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="w-6 h-6 opacity-0 group-hover:opacity-100 transition-opacity"
          onClick={(e) => {
            e.stopPropagation();
            onDelete();
          }}
        >
          <Trash2 className="w-3.5 h-3.5" />
        </Button>
      </div>
    </button>
  );
}

// ユーティリティ関数
function getConversationSummary(conv: MiraConversation): string {
  const firstUserMessage = conv.messages.find((m) => m.role === 'user');
  if (firstUserMessage) {
    return firstUserMessage.content.length > 50
      ? firstUserMessage.content.substring(0, 50) + '...'
      : firstUserMessage.content;
  }
  return '新しい会話';
}

function getRelativeTime(date: Date | string): string {
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
}

function getModeLabel(mode?: string): string | null {
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
}
