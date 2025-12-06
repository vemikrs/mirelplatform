/**
 * Mira コンテキストパネルコンポーネント
 * 
 * 会話のメタデータ表示、クイックアクション、キーボードショートカット情報を提供
 */
import { useState } from 'react';
import { cn, Button, ScrollArea, Badge } from '@mirel/ui';
import { 
  HelpCircle, 
  AlertTriangle, 
  Paintbrush2, 
  Workflow,
  Clock,
  MessageCircle,
  Calendar,
  Settings,
  Keyboard,
  ChevronDown,
  ChevronRight,
  Tag,
} from 'lucide-react';
import type { MiraConversation } from '@/stores/miraStore';
import type { MiraMode } from '@/lib/api/mira';

interface MiraContextPanelProps {
  conversation: MiraConversation | null;
  currentMode: MiraMode;
  onModeChange: (mode: MiraMode) => void;
  onUpdateContext?: (context: string) => void;
  className?: string;
}

export function MiraContextPanel({
  conversation,
  currentMode,
  onModeChange,
  onUpdateContext,
  className,
}: MiraContextPanelProps) {
  return (
    <div className={cn("w-72 border-l flex flex-col bg-surface-subtle/30", className)}>
      <ScrollArea className="flex-1">
        <div className="p-4 space-y-6">
          {/* 現在のコンテキスト */}
          <ContextSection 
            conversation={conversation} 
            currentMode={currentMode}
            onUpdateContext={onUpdateContext}
          />
          
          {/* クイックアクション */}
          <QuickActionsSection 
            currentMode={currentMode} 
            onModeChange={onModeChange} 
          />
          
          {/* 会話情報 */}
          <ConversationInfoSection conversation={conversation} />
          
          {/* キーボードショートカット */}
          <KeyboardShortcutsSection />
        </div>
      </ScrollArea>
    </div>
  );
}

/** コンテキストセクション */
interface ContextSectionProps {
  conversation: MiraConversation | null;
  currentMode: MiraMode;
  onUpdateContext?: (context: string) => void;
}

function ContextSection({ conversation, currentMode }: ContextSectionProps) {
  const contextDescription = getContextDescription(currentMode);
  const context = conversation?.context;
  const hasCustomContext = Boolean(context?.appId || context?.screenId || context?.systemRole);
  
  return (
    <section>
      <div className="flex items-center justify-between mb-2">
        <h3 className="text-sm font-medium flex items-center gap-2">
          <Settings className="w-4 h-4" />
          現在のコンテキスト
        </h3>
      </div>
      
      <div className="p-3 rounded-lg bg-muted/50 border border-muted">
        <p className="text-sm text-muted-foreground mb-2">
          {contextDescription}
        </p>
        {hasCustomContext && (
          <div className="mt-2 pt-2 border-t border-muted space-y-1">
            <p className="text-xs text-muted-foreground mb-1">カスタムコンテキスト:</p>
            {context?.appId && (
              <p className="text-xs"><span className="text-muted-foreground">App:</span> {context.appId}</p>
            )}
            {context?.screenId && (
              <p className="text-xs"><span className="text-muted-foreground">Screen:</span> {context.screenId}</p>
            )}
            {context?.systemRole && (
              <p className="text-xs"><span className="text-muted-foreground">Role:</span> {context.systemRole}</p>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

/** クイックアクションセクション */
interface QuickActionsSectionProps {
  currentMode: MiraMode;
  onModeChange: (mode: MiraMode) => void;
}

function QuickActionsSection({ currentMode, onModeChange }: QuickActionsSectionProps) {
  const actions: { mode: MiraMode; label: string; icon: React.ComponentType<{ className?: string }>; color: string }[] = [
    { mode: 'CONTEXT_HELP', label: 'Help', icon: HelpCircle, color: 'text-blue-500' },
    { mode: 'ERROR_ANALYZE', label: 'Error', icon: AlertTriangle, color: 'text-red-500' },
    { mode: 'STUDIO_AGENT', label: 'Studio', icon: Paintbrush2, color: 'text-purple-500' },
    { mode: 'WORKFLOW_AGENT', label: 'Workflow', icon: Workflow, color: 'text-green-500' },
  ];
  
  return (
    <section>
      <h3 className="text-sm font-medium flex items-center gap-2 mb-2">
        <Tag className="w-4 h-4" />
        クイックアクション
      </h3>
      <div className="grid grid-cols-2 gap-2">
        {actions.map(({ mode, label, icon: Icon, color }) => (
          <Button
            key={mode}
            variant={currentMode === mode ? 'default' : 'outline'}
            size="sm"
            className={cn(
              "justify-start gap-2",
              currentMode !== mode && color
            )}
            onClick={() => onModeChange(mode)}
          >
            <Icon className="w-4 h-4" />
            {label}
          </Button>
        ))}
      </div>
    </section>
  );
}

/** 会話情報セクション */
interface ConversationInfoSectionProps {
  conversation: MiraConversation | null;
}

function ConversationInfoSection({ conversation }: ConversationInfoSectionProps) {
  if (!conversation) return null;
  
  const messageCount = conversation.messages.length;
  const userMessageCount = conversation.messages.filter((m) => m.role === 'user').length;
  const assistantMessageCount = conversation.messages.filter((m) => m.role === 'assistant').length;
  const startTime = new Date(conversation.createdAt).toLocaleString('ja-JP');
  const lastActivity = new Date(conversation.updatedAt).toLocaleString('ja-JP');
  
  return (
    <section>
      <h3 className="text-sm font-medium flex items-center gap-2 mb-2">
        <MessageCircle className="w-4 h-4" />
        会話情報
      </h3>
      <div className="space-y-2 text-sm">
        <div className="flex items-center justify-between py-1">
          <span className="text-muted-foreground">メッセージ数</span>
          <div className="flex items-center gap-2">
            <Badge variant="neutral" className="text-xs">
              計 {messageCount}
            </Badge>
            <Badge variant="outline" className="text-xs">
              You: {userMessageCount}
            </Badge>
            <Badge variant="outline" className="text-xs">
              AI: {assistantMessageCount}
            </Badge>
          </div>
        </div>
        <div className="flex items-center justify-between py-1">
          <span className="text-muted-foreground flex items-center gap-1">
            <Calendar className="w-3.5 h-3.5" />
            開始時刻
          </span>
          <span className="text-xs">{startTime}</span>
        </div>
        <div className="flex items-center justify-between py-1">
          <span className="text-muted-foreground flex items-center gap-1">
            <Clock className="w-3.5 h-3.5" />
            最終更新
          </span>
          <span className="text-xs">{lastActivity}</span>
        </div>
      </div>
    </section>
  );
}

/** キーボードショートカットセクション */
function KeyboardShortcutsSection() {
  const [isExpanded, setIsExpanded] = useState(false);
  
  const shortcuts = [
    { keys: ['⌘', 'Enter'], description: '送信' },
    { keys: ['⌘', 'K'], description: 'Mira を開く' },
    { keys: ['Esc'], description: '閉じる' },
    { keys: ['⌘', 'N'], description: '新しい会話' },
  ];
  
  return (
    <section>
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center justify-between text-sm font-medium mb-2 hover:text-primary transition-colors"
      >
        <span className="flex items-center gap-2">
          <Keyboard className="w-4 h-4" />
          キーボードショートカット
        </span>
        {isExpanded ? (
          <ChevronDown className="w-4 h-4" />
        ) : (
          <ChevronRight className="w-4 h-4" />
        )}
      </button>
      
      {isExpanded && (
        <div className="space-y-2 text-sm">
          {shortcuts.map(({ keys, description }) => (
            <div 
              key={description}
              className="flex items-center justify-between py-1"
            >
              <span className="text-muted-foreground">{description}</span>
              <div className="flex items-center gap-1">
                {keys.map((key, i) => (
                  <kbd
                    key={i}
                    className="px-1.5 py-0.5 text-xs bg-muted border rounded"
                  >
                    {key}
                  </kbd>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

// ユーティリティ関数
function getContextDescription(mode: MiraMode): string {
  const descriptions: Record<MiraMode, string> = {
    GENERAL_CHAT: '一般的な質問や会話ができます',
    CONTEXT_HELP: 'mirelplatform の機能について質問できます',
    ERROR_ANALYZE: 'エラーの分析と解決策を提案します',
    STUDIO_AGENT: 'Studio のモデル設計を支援します',
    WORKFLOW_AGENT: 'ワークフロー設計を支援します',
  };
  return descriptions[mode] ?? descriptions.GENERAL_CHAT;
}
