/**
 * Mira ユーザーコンテキスト編集コンポーネント
 * 
 * ユーザー固有のAIコンテキスト設定を管理するダイアログ。
 * 以下のカテゴリをサポート:
 * - terminology: 専門用語・略語の定義
 * - style: 回答スタイルの設定
 * - workflow: ワークフローパターン
 */
import { useState, useCallback, useEffect } from 'react';
import {
  cn,
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  Textarea,
  Label,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Badge,
} from '@mirel/ui';
import { Settings2, Save, RotateCcw, Loader2, BookOpen, Palette, GitBranch, Plug } from 'lucide-react';

/** コンテキストカテゴリ */
type ContextCategory = 'terminology' | 'style' | 'workflow' | 'integration';

/** ユーザーコンテキスト */
interface UserContext {
  terminology: string;
  style: string;
  workflow: string;
  tavilyApiKey: string;
}

/** デフォルトのコンテキスト */
const DEFAULT_CONTEXT: UserContext = {
  terminology: '',
  style: '',
  workflow: '',
  tavilyApiKey: '',
};

/** カテゴリ設定 */
const CATEGORY_CONFIG: Record<ContextCategory, { 
  label: string; 
  icon: typeof BookOpen; 
  description: string;
  placeholder: string;
}> = {
  terminology: {
    label: '専門用語',
    icon: BookOpen,
    description: 'プロジェクト固有の用語や略語を定義します。AIがこれらを理解して回答します。',
    placeholder: `例:
- FE: フロントエンド (React, TypeScript)
- BE: バックエンド (Spring Boot, Java)
- DB: データベース (PostgreSQL)
- CI: 継続的インテグレーション
- mira: AIアシスタント機能の名称`,
  },
  style: {
    label: '回答スタイル',
    icon: Palette,
    description: 'AIの回答スタイルをカスタマイズします。',
    placeholder: `例:
- 回答は日本語で、簡潔に
- コード例を含める場合はTypeScriptを優先
- 専門用語は初出時に簡単な説明を付ける
- 箇条書きを活用して読みやすく`,
  },
  workflow: {
    label: 'ワークフロー',
    icon: GitBranch,
    description: 'よく使うワークフローやパターンを登録します。',
    placeholder: `例:
- コードレビュー時は1.セキュリティ 2.パフォーマンス 3.可読性の順でチェック
- 新機能追加時はまずテストを書く(TDD)
- エラー発生時は1.ログ確認 2.再現手順特定 3.原因調査`,
  },
  integration: {
    label: '連携設定',
    icon: Plug,
    description: '外部サービスとの連携に必要なAPIキーなどを設定します。',
    placeholder: 'Tavily API Key (tvly-...)',
  },
};

interface MiraUserContextEditorProps {
  className?: string;
  /** トリガーボタンを表示するか（falseの場合は外部からDialogを制御） */
  showTrigger?: boolean;
  /** ダイアログの開閉状態（外部制御用） */
  open?: boolean;
  /** ダイアログの開閉コールバック */
  onOpenChange?: (open: boolean) => void;
}

export function MiraUserContextEditor({
  className,
  showTrigger = true,
  open: controlledOpen,
  onOpenChange: controlledOnOpenChange,
}: MiraUserContextEditorProps) {
  const [internalOpen, setInternalOpen] = useState(false);
  const [context, setContext] = useState<UserContext>(DEFAULT_CONTEXT);
  const [originalContext, setOriginalContext] = useState<UserContext>(DEFAULT_CONTEXT);
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<ContextCategory>('terminology');
  
  // 外部制御または内部制御
  const isOpen = controlledOpen !== undefined ? controlledOpen : internalOpen;
  const setIsOpen = controlledOnOpenChange ?? setInternalOpen;
  
  // コンテキストを読み込む
  const loadContext = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await fetch('/mapi/apps/mira/api/user-context');
      if (response.ok) {
        const data = await response.json();
        if (data.data) {
          const loaded: UserContext = {
            terminology: data.data.terminology || '',
            style: data.data.style || '',
            workflow: data.data.workflow || '',
            tavilyApiKey: data.data.tavilyApiKey || '',
          };
          setContext(loaded);
          setOriginalContext(loaded);
        }
      }
    } catch (error) {
      console.error('Failed to load user context:', error);
      // エラー時はデフォルト値を使用
    } finally {
      setIsLoading(false);
    }
  }, []);
  
  // ダイアログが開いたときにコンテキストを読み込む
  useEffect(() => {
    if (isOpen) {
      loadContext();
    }
  }, [isOpen, loadContext]);
  
  // 保存処理
  const handleSave = async () => {
    setIsSaving(true);
    try {
      const response = await fetch('/mapi/apps/mira/api/user-context', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(context),
      });
      
      if (response.ok) {
        setOriginalContext(context);
        setIsOpen(false);
      } else {
        console.error('Failed to save context');
      }
    } catch (error) {
      console.error('Failed to save user context:', error);
    } finally {
      setIsSaving(false);
    }
  };
  
  // リセット処理
  const handleReset = () => {
    setContext(originalContext);
  };
  
  // 変更があるか
  const hasChanges = JSON.stringify(context) !== JSON.stringify(originalContext);
  
  // 各カテゴリの文字数
  const getCharCount = (category: ContextCategory) => context[category].length;
  
  const dialogContent = (
    <DialogContent className="sm:max-w-2xl max-h-[80vh] flex flex-col">
      <DialogHeader>
        <DialogTitle className="flex items-center gap-2">
          <Settings2 className="w-5 h-5" />
          ユーザーコンテキスト設定
        </DialogTitle>
        <DialogDescription>
          AIアシスタントがあなたの状況をより良く理解するための情報を設定します。
          これらの設定は全ての会話に適用されます。
        </DialogDescription>
      </DialogHeader>
      
      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as ContextCategory)} className="flex-1 flex flex-col min-h-0">
          <TabsList className="grid grid-cols-3 w-full">
            {(Object.keys(CATEGORY_CONFIG) as ContextCategory[]).map((category) => {
              const config = CATEGORY_CONFIG[category];
              const Icon = config.icon;
              const charCount = getCharCount(category);
              return (
                <TabsTrigger key={category} value={category} className="flex items-center gap-1.5">
                  <Icon className="w-4 h-4" />
                  <span className="hidden sm:inline">{config.label}</span>
                  {charCount > 0 && (
                    <Badge variant="neutral" className="ml-1 text-xs px-1.5 py-0">
                      {charCount}
                    </Badge>
                  )}
                </TabsTrigger>
              );
            })}
          </TabsList>
          
          {(Object.keys(CATEGORY_CONFIG) as ContextCategory[]).map((category) => {
            const config = CATEGORY_CONFIG[category];
            return (
              <TabsContent key={category} value={category} className="flex-1 flex flex-col mt-4">
                <div className="space-y-2 flex-1 flex flex-col">
                  <Label htmlFor={`context-${category}`} className="text-sm text-muted-foreground">
                    {config.description}
                  </Label>
                  <Textarea
                    id={`context-${category}`}
                    value={context[category]}
                    onChange={(e) => setContext({ ...context, [category]: e.target.value })}
                    placeholder={config.placeholder}
                    className={cn(
                        "flex-1 min-h-[200px] font-mono text-sm resize-none",
                        category === 'integration' && "h-[50px] min-h-[50px] flex-none"
                    )}
                  />
                  {category !== 'integration' && (
                    <p className="text-xs text-muted-foreground text-right">
                        {getCharCount(category)} 文字
                    </p>
                  )}
                </div>
              </TabsContent>
            );
          })}
        </Tabs>
      )}
      
      <DialogFooter className="flex items-center justify-between sm:justify-between">
        <Button
          variant="ghost"
          onClick={handleReset}
          disabled={!hasChanges || isSaving}
        >
          <RotateCcw className="w-4 h-4 mr-2" />
          リセット
        </Button>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => setIsOpen(false)}
            disabled={isSaving}
          >
            キャンセル
          </Button>
          <Button
            onClick={handleSave}
            disabled={!hasChanges || isSaving}
          >
            {isSaving ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                保存中...
              </>
            ) : (
              <>
                <Save className="w-4 h-4 mr-2" />
                保存
              </>
            )}
          </Button>
        </div>
      </DialogFooter>
    </DialogContent>
  );
  
  if (!showTrigger) {
    return (
      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        {dialogContent}
      </Dialog>
    );
  }
  
  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon" className={cn(className)} title="ユーザーコンテキスト設定">
          <Settings2 className="w-4 h-4" />
        </Button>
      </DialogTrigger>
      {dialogContent}
    </Dialog>
  );
}
