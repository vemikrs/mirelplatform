/**
 * Mira 会話履歴リストコンポーネント
 * 
 * 会話の一覧表示、検索、選択、削除機能を提供
 * スライドインドロワーとして使用される
 */
import { useState } from 'react';
import { cn, Button, ScrollArea, Input, toast } from '@mirel/ui';
import { 
  Bot, 
  MessageSquarePlus, 
  Trash2, 
  Search, 

  X,
  HelpCircle,
  AlertTriangle,
  Paintbrush2,
  Workflow,
  MessageSquare,
} from 'lucide-react';
import type { MiraConversation } from '@/stores/miraStore';
import { MiraDeleteConfirmDialog } from './MiraDeleteConfirmDialog';

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
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; title: string } | null>(null);
  
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
    <div className="w-64 h-full border-r flex flex-col bg-surface-subtle">
      {/* ヘッダー */}
      <div className="px-4 py-3 border-b">
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
              className="gap-1.5"
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
        <div className="p-2">
          {Object.keys(groupedConversations).length === 0 ? (
            <div className="p-4 text-center text-muted-foreground text-sm">
              {searchQuery ? '該当する会話がありません' : '会話履歴がありません'}
            </div>
          ) : (
            Object.entries(groupedConversations).map(([dateGroup, convs]) => (
              <div key={dateGroup} className="mb-2">
                <p className="text-xs font-medium text-muted-foreground px-2 py-1 mb-1">
                  {dateGroup}
                </p>
                <div className="space-y-0.5">
                  {convs.map((conv) => (
                    <ConversationItem
                      key={conv.id}
                      conversation={conv}
                      isActive={activeConversationId === conv.id}
                      onSelect={() => onSelect(conv.id)}
                      onDelete={() => {
                        const title = getConversationTitle(conv);
                        setDeleteTarget({ id: conv.id, title });
                      }}
                    />
                  ))}
                </div>
              </div>
            ))
          )}
        </div>
      </ScrollArea>      
      {/* 削除確認ダイアログ */}
      <MiraDeleteConfirmDialog
        isOpen={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) {
            const title = deleteTarget.title;
            onDelete(deleteTarget.id);
            setDeleteTarget(null);
            toast({
              title: '成功',
              description: `「${title}」を削除しました`,
            });
          }
        }}
        title="会話を削除しますか?"
        description={deleteTarget ? `「${deleteTarget.title}」を削除します。この操作は取り消せません。` : ''}
        confirmLabel="削除"
      />    </div>
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
  const modeConfig = getModeConfig(conversation.mode);
  const ModeIcon = modeConfig.icon;

  
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelect();
        }
      }}
      className={cn(
        "w-full text-left px-2 py-1.5 rounded-md transition-colors group cursor-pointer",
        isActive
          ? "bg-primary/10 border border-primary/20 text-primary"
          : "hover:bg-muted text-muted-foreground hover:text-foreground border border-transparent"
      )}
    >
      {/* メイン行: モードアイコン、タイトル */}
      <div className="flex items-center gap-2">
        {/* モードアイコン */}
        <ModeIcon className={cn("w-4 h-4 shrink-0", isActive ? "text-primary" : "text-muted-foreground")} />
        
        {/* タイトル */}
        <span className="text-sm font-medium line-clamp-1 flex-1 min-w-0">
          {title}
        </span>
        
        {/* 削除ボタン */}
        <Button
          variant="ghost"
          size="icon"
          className="w-6 h-6 opacity-0 group-hover:opacity-100 transition-opacity shrink-0 ml-auto"
          onClick={(e) => {
            e.stopPropagation();
            onDelete();
          }}
        >
          <Trash2 className="w-3.5 h-3.5" />
        </Button>
      </div>
    </div>
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
