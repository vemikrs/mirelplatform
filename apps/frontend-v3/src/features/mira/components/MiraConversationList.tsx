/**
 * Mira 会話履歴リストコンポーネント
 * 
 * 会話の一覧表示、検索、選択、削除機能を提供
 * スライドインドロワーとして使用される
 */
import { useState } from 'react';
import { cn, Button, ScrollArea, Input, Badge } from '@mirel/ui';
import { 
  Bot, 
  MessageSquarePlus, 
  Trash2, 
  Search, 
  Clock,
  X,
  HelpCircle,
  AlertTriangle,
  Paintbrush2,
  Workflow,
  MessageSquare,
} from 'lucide-react';
import type { MiraConversation } from '@/stores/miraStore';

interface MiraConversationListProps {
  conversations: MiraConversation[];
  activeConversationId: string | null;
  onSelect: (conversationId: string) => void;
  onDelete: (conversationId: string) => void;
  onNewConversation: () => void;
  onClose?: () => void;
}

export function MiraConversationList({
  conversations,
  activeConversationId,
  onSelect,
  onDelete,
  onNewConversation,
  onClose,
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
  
  // 会話を日付でグルーピング
  const groupedConversations = groupConversationsByDate(filteredConversations);
  
  return (
    <div className="w-[380px] h-full border-r flex flex-col bg-surface shadow-lg">
      {/* ヘッダー */}
      <div className="p-4 border-b">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Bot className="w-5 h-5 text-primary" />
            <h1 className="font-semibold">会話履歴</h1>
          </div>
          <div className="flex items-center gap-1">
            <Button
              variant="outline"
              size="sm"
              onClick={onNewConversation}
              className="gap-1"
            >
              <MessageSquarePlus className="w-4 h-4" />
              新規
            </Button>
            {onClose && (
              <Button
                variant="ghost"
                size="icon"
                onClick={onClose}
                className="w-8 h-8"
              >
                <X className="w-4 h-4" />
              </Button>
            )}
          </div>
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
      
      {/* 会話リスト（日付グループ） */}
      <ScrollArea className="flex-1">
        <div className="p-3">
          {Object.keys(groupedConversations).length === 0 ? (
            <div className="p-4 text-center text-muted-foreground text-sm">
              {searchQuery ? '該当する会話がありません' : '会話履歴がありません'}
            </div>
          ) : (
            Object.entries(groupedConversations).map(([dateGroup, convs]) => (
              <div key={dateGroup} className="mb-4">
                <p className="text-xs font-medium text-muted-foreground px-1 py-1 mb-2">
                  {dateGroup}
                </p>
                <div className="space-y-2">
                  {convs.map((conv) => (
                    <ConversationItem
                      key={conv.id}
                      conversation={conv}
                      isActive={activeConversationId === conv.id}
                      onSelect={() => onSelect(conv.id)}
                      onDelete={() => onDelete(conv.id)}
                    />
                  ))}
                </div>
              </div>
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
  const title = getConversationTitle(conversation);
  const preview = getConversationPreview(conversation);
  const relativeTime = getRelativeTime(conversation.updatedAt);
  const modeConfig = getModeConfig(conversation.mode);
  const ModeIcon = modeConfig.icon;
  
  return (
    <button
      onClick={onSelect}
      className={cn(
        "w-full text-left p-3 rounded-lg transition-colors group",
        isActive
          ? "bg-primary/10 border border-primary/20"
          : "hover:bg-surface-raised border border-transparent"
      )}
    >
      {/* 上段: タイトルと削除ボタン */}
      <div className="flex items-start justify-between gap-2 mb-1">
        <h3 className="text-sm font-medium line-clamp-1 flex-1">
          {title}
        </h3>
        <Button
          variant="ghost"
          size="icon"
          className="w-6 h-6 opacity-0 group-hover:opacity-100 transition-opacity shrink-0 -mt-0.5"
          onClick={(e) => {
            e.stopPropagation();
            onDelete();
          }}
        >
          <Trash2 className="w-3.5 h-3.5" />
        </Button>
      </div>
      
      {/* 中段: プレビュー */}
      {preview && (
        <p className="text-xs text-muted-foreground line-clamp-1 mb-2">
          {preview}
        </p>
      )}
      
      {/* 下段: メタ情報（モード、時間、メッセージ数） */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Badge 
          variant="outline" 
          className={cn("gap-1 py-0 h-5", modeConfig.color)}
        >
          <ModeIcon className="w-3 h-3" />
          {modeConfig.label}
        </Badge>
        <span className="flex items-center gap-1">
          <Clock className="w-3 h-3" />
          {relativeTime}
        </span>
        <span>{conversation.messages.length}件</span>
      </div>
    </button>
  );
}

// ユーティリティ関数

/** 会話タイトルを取得（title優先、なければ最初のメッセージ） */
function getConversationTitle(conv: MiraConversation): string {
  if (conv.title) {
    return conv.title;
  }
  // 最初のユーザーメッセージをタイトルとして使用
  const firstUserMessage = conv.messages.find((m) => m.role === 'user');
  if (firstUserMessage) {
    return firstUserMessage.content.length > 50
      ? firstUserMessage.content.substring(0, 50) + '...'
      : firstUserMessage.content;
  }
  return '新しい会話';
}

/** 会話プレビューを取得（最後のメッセージの先頭） */
function getConversationPreview(conv: MiraConversation): string | null {
  if (conv.messages.length <= 1) return null;
  const lastMessage = conv.messages[conv.messages.length - 1];
  if (lastMessage) {
    const prefix = lastMessage.role === 'user' ? 'You: ' : 'Mira: ';
    const content = lastMessage.content.length > 40
      ? lastMessage.content.substring(0, 40) + '...'
      : lastMessage.content;
    return prefix + content;
  }
  return null;
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

/** モード設定を取得 */
function getModeConfig(mode?: string): { label: string; icon: typeof MessageSquare; color: string } {
  type ModeConfig = { label: string; icon: typeof MessageSquare; color: string };
  const defaultConfig: ModeConfig = { label: 'General', icon: MessageSquare, color: '' };
  const configs: Record<string, ModeConfig> = {
    GENERAL_CHAT: defaultConfig,
    CONTEXT_HELP: { label: 'Help', icon: HelpCircle, color: 'text-blue-500 border-blue-500/30' },
    ERROR_ANALYZE: { label: 'Error', icon: AlertTriangle, color: 'text-red-500 border-red-500/30' },
    STUDIO_AGENT: { label: 'Studio', icon: Paintbrush2, color: 'text-purple-500 border-purple-500/30' },
    WORKFLOW_AGENT: { label: 'Workflow', icon: Workflow, color: 'text-green-500 border-green-500/30' },
  };
  if (!mode) return defaultConfig;
  return configs[mode] ?? defaultConfig;
}

// 日付グループ名を取得
function getDateGroupLabel(date: Date): string {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today.getTime() - 86400000);
  const weekAgo = new Date(today.getTime() - 7 * 86400000);
  
  const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  
  if (targetDate.getTime() >= today.getTime()) return '今日';
  if (targetDate.getTime() >= yesterday.getTime()) return '昨日';
  if (targetDate.getTime() >= weekAgo.getTime()) return '今週';
  return 'それ以前';
}

// 会話を日付でグルーピング
function groupConversationsByDate(conversations: MiraConversation[]): Record<string, MiraConversation[]> {
  // まず日付でソート（新しい順）
  const sorted = [...conversations].sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  );
  
  const groups: Record<string, MiraConversation[]> = {};
  const order = ['今日', '昨日', '今週', 'それ以前'];
  
  for (const conv of sorted) {
    const label = getDateGroupLabel(new Date(conv.updatedAt));
    if (!groups[label]) {
      groups[label] = [];
    }
    groups[label].push(conv);
  }
  
  // 順序を保持したオブジェクトを返す
  const orderedGroups: Record<string, MiraConversation[]> = {};
  for (const key of order) {
    if (groups[key]) {
      orderedGroups[key] = groups[key];
    }
  }
  return orderedGroups;
}
