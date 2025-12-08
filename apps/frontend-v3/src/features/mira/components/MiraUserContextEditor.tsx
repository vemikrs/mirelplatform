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
  DialogTitle,
  DialogTrigger,
  Textarea,
  Label,
  Input,
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
  const [isEditingIntegration, setIsEditingIntegration] = useState(false);
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
      // エラー時はデフォルト値を維持
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
        setIsEditingIntegration(false);
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
    setIsEditingIntegration(false);
  };
  
  // 変更があるか
  const hasChanges = JSON.stringify(context) !== JSON.stringify(originalContext);
  
  // 各カテゴリの文字数
  const getCharCount = (category: ContextCategory) => {
      if (category === 'integration') return context.tavilyApiKey.length;
      return (context[category as keyof UserContext] || '').length;
  };

  const getContextValue = (category: ContextCategory) => {
      if (category === 'integration') return context.tavilyApiKey;
      return context[category as keyof UserContext] || '';
  };

  const handleContextChange = (category: ContextCategory, val: string) => {
      if (category === 'integration') {
          setContext({ ...context, tavilyApiKey: val });
      } else {
          setContext({ ...context, [category]: val });
      }
  };
  
  const dialogContent = (
    <DialogContent className="sm:max-w-5xl max-h-[85vh] h-[650px] flex flex-col p-0 overflow-hidden">
      <div className="p-6 border-b shrink-0 bg-muted/20">
        <DialogTitle className="flex items-center gap-2">
          <Settings2 className="w-5 h-5" />
          ユーザーコンテキスト設定
        </DialogTitle>
        <DialogDescription className="mt-1.5">
          AIアシスタントがあなたの状況をより良く理解するための情報を設定します。
        </DialogDescription>
      </div>
      
      {isLoading ? (
        <div className="flex items-center justify-center flex-1">
          <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <div className="flex flex-1 overflow-hidden">
          {/* Sidebar */}
          <div className="w-64 border-r bg-muted/10 flex flex-col p-2 space-y-1 overflow-y-auto shrink-0">
            {(Object.keys(CATEGORY_CONFIG) as ContextCategory[]).map((category) => {
              const config = CATEGORY_CONFIG[category];
              const Icon = config.icon;
              const isActive = activeTab === category;
              const charCount = getCharCount(category);
              
              return (
                <button
                  key={category}
                  onClick={() => {
                    setActiveTab(category);
                    setIsEditingIntegration(false);
                  }}
                  className={cn(
                    "flex items-center gap-3 px-3 py-2.5 text-sm font-medium rounded-md transition-all w-full text-left",
                    isActive 
                      ? "bg-primary text-primary-foreground shadow-sm" 
                      : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                  )}
                >
                  <Icon className="w-4 h-4 shrink-0" />
                  <span className="flex-1 truncate">{config.label}</span>
                  {charCount > 0 && (
                    <Badge 
                      variant="outline" 
                      className={cn(
                        "ml-auto text-[10px] px-1 h-5 min-w-5 flex items-center justify-center border-none",
                        isActive ? "bg-primary-foreground/20 text-primary-foreground" : "bg-muted text-muted-foreground"
                      )}
                    >
                      {charCount}
                    </Badge>
                  )}
                </button>
              );
            })}
          </div>

          {/* Main Content */}
          <div className="flex-1 flex flex-col overflow-hidden">
            <div className="flex-1 p-6 overflow-y-auto">
              <div className="max-w-2xl mx-auto space-y-6">
                <div>
                  <h3 className="text-lg font-semibold flex items-center gap-2 mb-1">
                    {CATEGORY_CONFIG[activeTab].label}
                  </h3>
                  <p className="text-sm text-muted-foreground leading-relaxed">
                    {CATEGORY_CONFIG[activeTab].description}
                  </p>
                </div>

                {activeTab === 'integration' ? (
                  <div className="space-y-4 p-4 border rounded-lg bg-card">
                    <div className="space-y-4">
                      
                      {/* API Key Section */}
                      <div className="space-y-2">
                        <Label htmlFor="tavily-api-key" className="text-sm font-medium">
                          Tavily API Key
                        </Label>
                        
                        {!isEditingIntegration && context.tavilyApiKey ? (
                          <div className="flex items-center justify-between p-3 bg-muted/30 rounded border border-muted">
                            <div className="flex items-center gap-2 text-sm text-green-600 font-medium">
                              <span className="flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-green-50 border border-green-200 text-xs">
                                <span className="relative flex h-2 w-2">
                                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                                  <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                                </span>
                                設定済み
                              </span>
                              <span className="text-muted-foreground text-xs font-normal ml-2">
                                ************{context.tavilyApiKey.slice(-4)}
                              </span>
                            </div>
                            <Button 
                                variant="outline" 
                                size="sm" 
                                onClick={() => setIsEditingIntegration(true)}
                                className="h-8 text-xs"
                            >
                                変更する
                            </Button>
                          </div>
                        ) : (
                          <div className="space-y-2 animate-in fade-in zoom-in-95 duration-200">
                             <div className="flex gap-2">
                                <Input
                                  id="tavily-api-key"
                                  name="tavily-api-key-no-autofill"
                                  type="password"
                                  value={context.tavilyApiKey}
                                  onChange={(e) => handleContextChange('integration', e.target.value)}
                                  placeholder="tvly-..."
                                  className="font-mono flex-1"
                                  autoComplete="off"
                                  data-lpignore="true"
                                  data-1p-ignore
                                />
                                {isEditingIntegration && (
                                   <Button 
                                      variant="ghost" 
                                      size="sm"
                                      onClick={() => setIsEditingIntegration(false)}
                                      title="キャンセル"
                                   >
                                      キャンセル
                                   </Button>
                                )}
                              </div>
                              <p className="text-xs text-muted-foreground">
                                Tavily Search API キーを設定すると、MiraがリアルタイムなWeb検索を行えるようになります。
                                <br />
                                <a 
                                  href="https://tavily.com/" 
                                  target="_blank" 
                                  rel="noreferrer" 
                                  className="text-primary hover:underline inline-flex items-center gap-1 mt-1"
                                >
                                  公式サイトでキーを取得 <BookOpen className="w-3 h-3" />
                                </a>
                              </p>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-2 h-full flex flex-col">
                    <Textarea
                      id={`context-${activeTab}`}
                      value={getContextValue(activeTab)}
                      onChange={(e) => handleContextChange(activeTab, e.target.value)}
                      placeholder={CATEGORY_CONFIG[activeTab].placeholder}
                      className="flex-1 min-h-[300px] font-mono text-sm leading-relaxed p-4 resize-none focus-visible:ring-1"
                    />
                    <div className="flex justify-end">
                      <span className="text-xs text-muted-foreground">
                        {getCharCount(activeTab)} 文字
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </div>
            
            {/* Footer */}
          </div>
        </div>
      )}
      
      <div className="p-4 border-t bg-muted/20 flex items-center justify-between shrink-0">
        <Button
          variant="ghost"
          onClick={handleReset}
          disabled={!hasChanges || isSaving}
        >
          <RotateCcw className="w-4 h-4 mr-2" />
          変更をリセット
        </Button>
        <div className="flex gap-3">
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
            className="min-w-[100px]"
          >
            {isSaving ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                保存中
              </>
            ) : (
              <>
                <Save className="w-4 h-4 mr-2" />
                設定を保存
              </>
            )}
          </Button>
        </div>
      </div>
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
