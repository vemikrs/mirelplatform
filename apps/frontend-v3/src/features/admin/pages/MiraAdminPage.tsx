import { useState, useEffect } from 'react';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
  Button,
  Input,
  Label,
  Combobox,
  Textarea, // Restored
  Badge,    // Restored
  useToast
} from '@mirel/ui';
import { 
    Loader2, 
    Settings, 
    BarChart3, 
    CloudCog, 
    ShieldAlert, 
    Layers, 
    Save,   // Restored
    Plus,   // Restored 
    Trash2, // Restored
    Plug    // Added
} from 'lucide-react';

import {
  miraAdminApi,
  type MiraContextLayer,
  type TokenUsageSummary,
  type AiConfig,
  type LimitsSettings
} from '@/lib/api/mira-admin';
import {
  getProviders,
  getModels,
  type ProviderInfo,
  type ModelInfo
} from '@/lib/api/mira';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

export const MiraAdminPage = () => {
  return (
    <div className="container mx-auto py-8 space-y-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Mira AI 管理</h1>
        <p className="text-muted-foreground mt-2">
          AIアシスタントの統合設定、制限、および利用状況を管理します。
        </p>
      </div>

      <Tabs defaultValue="settings" className="space-y-4">
        <TabsList>
          <TabsTrigger value="settings" className="flex items-center gap-2"><Settings className="w-4 h-4"/> 設定 (Settings)</TabsTrigger>
          <TabsTrigger value="insights" className="flex items-center gap-2"><BarChart3 className="w-4 h-4"/> インサイト (Insights)</TabsTrigger>
        </TabsList>

        <TabsContent value="settings" className="space-y-4">
          <SettingsTab />
        </TabsContent>

        <TabsContent value="insights" className="space-y-4">
          <InsightsTab />
        </TabsContent>
      </Tabs>
    </div>
  );
};

// ==========================================
// Settings Tab Components
// ==========================================

const SettingsTab = () => {
    const { toast } = useToast();
    const [activeCategory, setActiveCategory] = useState<'general' | 'parameters' | 'limits' | 'context' | 'integration'>('general');
    const [tenantId, setTenantId] = useState<string>(''); // Empty = System Default
    
    // Config States
    const [aiConfig, setAiConfig] = useState<AiConfig>({});
    const [limitsConfig, setLimitsConfig] = useState<LimitsSettings>({});
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    
    // Model Selection States (Phase 4)
    const [providers, setProviders] = useState<ProviderInfo[]>([]);
    const [models, setModels] = useState<ModelInfo[]>([]);
    const [loadingProviders, setLoadingProviders] = useState(false);
    const [loadingModels, setLoadingModels] = useState(false);

    // Initial Load & Tenant Change
    useEffect(() => {
        loadConfig();
        loadProviders();
    }, [tenantId]);
    
    // Load models when provider changes
    useEffect(() => {
        if (aiConfig.provider) {
            loadModels(aiConfig.provider);
        }
    }, [aiConfig.provider]);

    const loadConfig = async () => {
        setLoading(true);
        try {
            const [ai, lim] = await Promise.all([
                miraAdminApi.getAiConfig(tenantId || undefined),
                miraAdminApi.getLimitsConfig(tenantId || undefined)
            ]);
            setAiConfig(ai);
            setLimitsConfig(lim);
        } catch (e) {
            console.error(e);
            toast({ variant: "destructive", title: "設定の読み込みに失敗しました" });
        } finally {
            setLoading(false);
        }
    };
    
    const loadProviders = async () => {
        setLoadingProviders(true);
        try {
            const data = await getProviders();
            setProviders(data);
        } catch (e) {
            console.error(e);
            toast({ variant: "destructive", title: "プロバイダ一覧の取得に失敗しました" });
        } finally {
            setLoadingProviders(false);
        }
    };
    
    const loadModels = async (provider: string) => {
        setLoadingModels(true);
        try {
            const data = await getModels(provider);
            setModels(data);
        } catch (e) {
            console.error(e);
            toast({ variant: "destructive", title: "モデル一覧の取得に失敗しました" });
        } finally {
            setLoadingModels(false);
        }
    };

    const handleSaveAi = async () => {
        setSaving(true);
        try {
            await miraAdminApi.saveAiConfig(aiConfig, tenantId || undefined);
            toast({ variant: "success", title: "AI設定を保存しました" });
        } catch (e) {
            toast({ variant: "destructive", title: "保存に失敗しました" });
        } finally {
            setSaving(false);
        }
    };

    const handleSaveLimits = async () => {
        setSaving(true);
        try {
            await miraAdminApi.saveLimitsConfig(limitsConfig, tenantId || undefined);
            toast({ variant: "success", title: "制限設定を保存しました" });
        } catch (e) {
            toast({ variant: "destructive", title: "保存に失敗しました" });
        } finally {
            setSaving(false);
        }
    };
    
    // Render Helpers
    const renderSidebar = () => (
        <div className="flex flex-col gap-1">
            <Button variant={activeCategory === 'general' ? 'secondary' : 'ghost'} className="justify-start" onClick={() => setActiveCategory('general')}>
                <CloudCog className="mr-2 h-4 w-4" /> 全般 (General)
            </Button>
            <Button variant={activeCategory === 'parameters' ? 'secondary' : 'ghost'} className="justify-start" onClick={() => setActiveCategory('parameters')}>
                <Settings className="mr-2 h-4 w-4" /> パラメータ (Parameters)
            </Button>
            <Button variant={activeCategory === 'limits' ? 'secondary' : 'ghost'} className="justify-start" onClick={() => setActiveCategory('limits')}>
                <ShieldAlert className="mr-2 h-4 w-4" /> 制限 (Limits)
            </Button>
            <Button variant={activeCategory === 'context' ? 'secondary' : 'ghost'} className="justify-start" onClick={() => setActiveCategory('context')}>
                <Layers className="mr-2 h-4 w-4" /> コンテキスト (Context)
            </Button>
            <Button variant={activeCategory === 'integration' ? 'secondary' : 'ghost'} className="justify-start" onClick={() => setActiveCategory('integration')}>
                <Plug className="mr-2 h-4 w-4" /> 連携 (Integration)
            </Button>
        </div>
    );

    const renderTenantSelector = () => (
        <div className="mb-6 flex items-center gap-4 p-4 border rounded-lg bg-card text-card-foreground shadow-sm">
            <div className="flex-1">
                <Label>設定対象 (テナント)</Label>
                <div className="flex gap-2 mt-1">
                    <Combobox
                        options={[
                            { value: "", label: "システムデフォルト (System Default)" },
                            // Mock tenants or fetch from API
                            { value: "default", label: "Default Tenant" },
                            { value: "enterprise-001", label: "Enterprise 001" },
                             { value: "enterprise-002", label: "Enterprise 002" }
                        ]}
                        value={tenantId}
                        onValueChange={(val) => setTenantId(val)}
                        placeholder="テナントを選択..."
                        className="w-[300px]"
                    />
                    <Button variant="outline" size="icon" onClick={loadConfig}><Loader2 className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /></Button>
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                    {tenantId ? "このテナント固有の設定を編集しています。" : "システム全体のデフォルト設定を編集しています。"}
                </p>
            </div>
        </div>
    );

    return (
        <div className="flex flex-col gap-6">
            {renderTenantSelector()}
            
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <Card className="md:col-span-1 h-fit">
                    <CardHeader>
                        <CardTitle>カテゴリ</CardTitle>
                    </CardHeader>
                    <CardContent className="p-2">
                        {renderSidebar()}
                    </CardContent>
                </Card>

                <Card className="md:col-span-3 min-h-[500px]">
                    <CardHeader>
                        <CardTitle>
                            {activeCategory === 'general' && "全般設定"}
                            {activeCategory === 'parameters' && "AIパラメータ"}
                            {activeCategory === 'limits' && "制限設定"}
                            {activeCategory === 'context' && "コンテキスト管理"}
                            {activeCategory === 'integration' && "外部連携設定"}
                        </CardTitle>
                        <CardDescription>
                            {activeCategory === 'context' 
                                ? "システムまたはテナント固有のプロンプトコンテキストを管理します。" 
                                : "設定変更は即座に反映されますが、一部のキャッシュにより遅延する場合があります。"}
                        </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-6">
                        {loading && activeCategory !== 'context' ? <Loader2 className="animate-spin mx-auto"/> : (
                            <>
                                {activeCategory === 'general' && (
                                    <div className="space-y-4">
                                        <div className="grid gap-2">
                                            <Label>AI Provider</Label>
                                            {loadingProviders ? (
                                                <div className="flex items-center gap-2 p-2">
                                                    <Loader2 className="h-4 w-4 animate-spin" />
                                                    <span className="text-sm text-muted-foreground">読み込み中...</span>
                                                </div>
                                            ) : (
                                                <Combobox 
                                                    options={providers.map(p => ({
                                                        value: p.name,
                                                        label: `${p.displayName}${!p.available ? ' (利用不可)' : ''}`,
                                                        disabled: !p.available
                                                    }))}
                                                    value={aiConfig.provider || ""}
                                                    onValueChange={(v) => setAiConfig({...aiConfig, provider: v, model: ''})}
                                                    placeholder="プロバイダを選択"
                                                />
                                            )}
                                        </div>
                                        <div className="grid gap-2">
                                            <Label>Model Name</Label>
                                            {loadingModels ? (
                                                <div className="flex items-center gap-2 p-2">
                                                    <Loader2 className="h-4 w-4 animate-spin" />
                                                    <span className="text-sm text-muted-foreground">読み込み中...</span>
                                                </div>
                                            ) : (
                                                <Combobox 
                                                    options={models
                                                        .filter(m => m.isActive)
                                                        .map(m => ({
                                                            value: m.modelName,
                                                            label: `${m.displayName}${m.isRecommended ? ' ⭐' : ''}${m.isExperimental ? ' 🧪' : ''}`
                                                        }))}
                                                    value={aiConfig.model || ""}
                                                    onValueChange={(v) => setAiConfig({...aiConfig, model: v})}
                                                    placeholder={aiConfig.provider ? "モデル名を選択または入力" : "先にプロバイダを選択"}
                                                    allowCustom
                                                    disabled={!aiConfig.provider}
                                                />
                                            )}
                                            <p className="text-xs text-muted-foreground">
                                                {aiConfig.provider ? (
                                                    models.length > 0 
                                                        ? `${models.filter(m => m.isActive).length}個の利用可能なモデル` 
                                                        : 'モデル情報を取得できませんでした'
                                                ) : 'プロバイダで利用可能なモデルIDを指定してください。'}
                                            </p>
                                        </div>
                                        <div className="pt-4">
                                            <Button onClick={handleSaveAi} disabled={saving}><Save className="mr-2 h-4 w-4"/> 保存</Button>
                                        </div>
                                    </div>
                                )}

                                {activeCategory === 'parameters' && (
                                    <div className="space-y-6">
                                        <div className="grid gap-2">
                                            <div className="flex items-center justify-between">
                                                <Label>Temperature (創造性)</Label>
                                                <span className="text-sm text-muted-foreground">{aiConfig.temperature}</span>
                                            </div>
                                            <div className="flex items-center gap-4">
                                                <Input 
                                                    type="number" 
                                                    min={0} 
                                                    max={2} 
                                                    step={0.1}
                                                    value={aiConfig.temperature || 0.7}
                                                    onChange={(e) => setAiConfig({...aiConfig, temperature: parseFloat(e.target.value)})}
                                                />
                                            </div>
                                            <p className="text-xs text-muted-foreground">0.0 (確実) ～ 2.0 (創造的)。推奨: 0.7</p>
                                        </div>

                                        <div className="grid gap-2">
                                            <Label>Max Tokens (最大生成長)</Label>
                                            <Input 
                                                type="number" 
                                                value={aiConfig.maxTokens ?? 4096}
                                                onChange={(e) => setAiConfig({...aiConfig, maxTokens: parseInt(e.target.value)})}
                                            />
                                        </div>
                                        <div className="pt-4">
                                            <Button onClick={handleSaveAi} disabled={saving}><Save className="mr-2 h-4 w-4"/> 保存</Button>
                                        </div>
                                    </div>
                                )}

                                {activeCategory === 'limits' && (
                                    <div className="space-y-4">
                                        <div className="grid grid-cols-2 gap-4">
                                            <div className="space-y-2">
                                                <Label>Rate Limit (RPM)</Label>
                                                <Input 
                                                    type="number" 
                                                    value={limitsConfig.rpm ?? 60}
                                                    onChange={(e) => setLimitsConfig({...limitsConfig, rpm: parseInt(e.target.value)})}
                                                />
                                                <p className="text-xs text-muted-foreground">1分あたりのリクエスト数制限</p>
                                            </div>
                                            <div className="space-y-2">
                                                <Label>Rate Limit (RPH)</Label>
                                                <Input 
                                                    type="number" 
                                                    value={limitsConfig.rph ?? 1000}
                                                    onChange={(e) => setLimitsConfig({...limitsConfig, rph: parseInt(e.target.value)})}
                                                />
                                                <p className="text-xs text-muted-foreground">1時間あたりのリクエスト数制限</p>
                                            </div>
                                        </div>

                                        <div className="space-y-2">
                                            <Label>Daily Token Quota</Label>
                                            <Input 
                                                type="number" 
                                                value={limitsConfig.dailyQuota ?? 100000}
                                                onChange={(e) => setLimitsConfig({...limitsConfig, dailyQuota: parseInt(e.target.value)})}
                                            />
                                            <p className="text-xs text-muted-foreground">1日あたりの最大トークン消費量</p>
                                        </div>
                                        <div className="pt-4">
                                            <Button onClick={handleSaveLimits} disabled={saving}><Save className="mr-2 h-4 w-4"/> 保存</Button>
                                        </div>
                                    </div>
                                )}

                                {activeCategory === 'context' && (
                                    <ContextManagementWrapper tenantId={tenantId} />
                                )}

                                {activeCategory === 'integration' && (
                                    <div className="space-y-4">
                                        <div className="grid gap-2">
                                            <Label>Tavily Search API Key</Label>
                                            <Input
                                                type="password"
                                                value={aiConfig.tavilyApiKey || ""}
                                                onChange={(e) => setAiConfig({...aiConfig, tavilyApiKey: e.target.value})}
                                                placeholder="tvly-..."
                                            />
                                            <p className="text-xs text-muted-foreground">
                                                Tavily Search API キーを設定します。ToolCalling機能でリアルタイム検索が可能になります。
                                                {tenantId ? " (このテナント用に上書き)" : " (システムデフォルト)"}
                                            </p>
                                        </div>
                                        <div className="pt-4">
                                            <Button onClick={handleSaveAi} disabled={saving}><Save className="mr-2 h-4 w-4"/> 保存</Button>
                                        </div>
                                    </div>
                                )}
                            </>
                        )}
                    </CardContent>
                </Card>
            </div>
        </div>
    );
};

// Wrapper for Context Management to adapt to new layout
const ContextManagementWrapper = ({ tenantId }: { tenantId: string }) => {
     // Reuse existing logic but simplify since tenant selection is global now
     // Actually, the original ContextManagement had tabs for System/Tenant.
     // Here "Settings" tab has a global tenant selector.
     // So if global tenant is selected, we show Tenant Contexts.
     // If global tenant is empty (System Default), we show System Contexts.
     
     const { toast } = useToast();
     const [layers, setLayers] = useState<MiraContextLayer[]>([]);
     const [loading, setLoading] = useState(false);
     const [creating, setCreating] = useState(false);
     const [newContext, setNewContext] = useState({ category: '', content: '', priority: 0 });

     const isSystem = !tenantId;

     const fetchContexts = async () => {
         setLoading(true);
         try {
             if (isSystem) {
                 const data = await miraAdminApi.getSystemContexts();
                 setLayers(data);
             } else {
                 const data = await miraAdminApi.getTenantContexts(tenantId);
                 setLayers(data);
             }
         } catch (e) {
             toast({ variant: "destructive", title: "コンテキスト取得エラー" });
         } finally {
             setLoading(false);
         }
     };

     useEffect(() => {
         fetchContexts();
     }, [tenantId]);

     const handleCreate = async () => {
        try {
            const payload: Partial<MiraContextLayer> = {
                category: newContext.category,
                content: newContext.content,
                priority: newContext.priority,
                enabled: true,
                scope: isSystem ? 'SYSTEM' : 'TENANT',
                scopeId: isSystem ? undefined : tenantId,
            };
            
            if (isSystem) {
                await miraAdminApi.saveSystemContext(payload);
            } else {
               await miraAdminApi.saveTenantContext(tenantId, payload);
            }
            toast({ variant: "success", title: "作成しました" });
            setCreating(false);
            setNewContext({ category: '', content: '', priority: 0 });
            fetchContexts();
        } catch (e) {
            toast({ variant: "destructive", title: "作成失敗" });
        }
     };

     return (
         <div className="space-y-4">
             <div className="flex justify-between items-center">
                 <h3 className="text-lg font-medium">{isSystem ? "システムコンテキスト" : `テナントコンテキスト (${tenantId})`}</h3>
                 <Button onClick={() => setCreating(true)} size="sm"><Plus className="mr-2 h-4 w-4"/> 新規追加</Button>
             </div>

            {creating && (
                <div className="border rounded p-4 bg-muted/20 space-y-4">
                     <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                             <Label>Category</Label>
                             <Combobox 
                                options={[
                                    { value: "role", label: "role" }, 
                                    { value: "style", label: "style" }, 
                                    { value: "terminology", label: "terminology" }
                                ]}
                                value={newContext.category}
                                onValueChange={(v) => setNewContext({...newContext, category: v})}
                                placeholder="カテゴリを選択または入力"
                                allowCustom
                             />
                        </div>
                        <div className="space-y-2">
                            <Label>Priority</Label>
                            <Input type="number" value={newContext.priority} onChange={e => setNewContext({...newContext, priority: Number(e.target.value)})} />
                        </div>
                     </div>
                     <div className="space-y-2">
                         <Label>Content (Prompt)</Label>
                         <Textarea value={newContext.content} onChange={e => setNewContext({...newContext, content: e.target.value})} className="font-mono" />
                     </div>
                     <div className="flex justify-end gap-2">
                         <Button variant="ghost" onClick={() => setCreating(false)}>Cancel</Button>
                         <Button onClick={handleCreate}>Create</Button>
                     </div>
                </div>
            )}

             {loading ? <Loader2 className="animate-spin" /> : (
                 <div className="space-y-4">
                     {layers.map(layer => (
                         <ContextLayerEditor key={layer.id} layer={layer} onSave={fetchContexts} onDelete={fetchContexts} />
                     ))}
                     {layers.length === 0 && <p className="text-muted-foreground p-4 text-center">設定がありません</p>}
                 </div>
             )}
         </div>
     );
};


// Copied and adapted ContextLayerEditor
const ContextLayerEditor = ({ layer, onSave, onDelete }: { layer: MiraContextLayer; onSave: () => void; onDelete: () => void }) => {
    const { toast } = useToast();
    const [editing, setEditing] = useState(false);
    const [content, setContent] = useState(layer.content);
    const [saving, setSaving] = useState(false);
  
    const handleSave = async () => {
      setSaving(true);
      try {
        const updateData = { ...layer, content };
        if (layer.scope === 'SYSTEM') {
          await miraAdminApi.saveSystemContext(updateData);
        } else {
          await miraAdminApi.saveTenantContext(layer.scopeId || 'default', updateData);
        }
        toast({ title: '成功', description: '保存しました', variant: 'success' });
        setEditing(false);
        onSave();
      } catch (error) {
        console.error(error);
        toast({ title: 'エラー', description: '保存に失敗しました', variant: 'destructive' });
      } finally {
        setSaving(false);
      }
    };
  
    const handleDelete = async () => {
      if (!confirm('本当に削除しますか？')) return;
      try {
        await miraAdminApi.deleteContext(layer.id);
        toast({ title: '成功', description: '削除しました', variant: 'success' });
        onDelete();
      } catch (error) {
        toast({ title: 'エラー', description: '削除に失敗しました', variant: 'destructive' });
      }
    };
  
    return (
      <div className="border rounded-lg p-4 space-y-2">
        <div className="flex justify-between items-start">
          <div>
            <div className="flex items-center gap-2">
                <Badge variant="outline">{layer.category}</Badge>
                <span className="text-xs text-muted-foreground">Priority: {layer.priority}</span>
            </div>
          </div>
          <div className="flex gap-2">
            {editing ? (
              <>
                <Button variant="ghost" size="sm" onClick={() => setEditing(false)}>キャンセル</Button>
                <Button size="sm" onClick={handleSave} disabled={saving}>
                  <Save className="mr-2 h-4 w-4" /> 保存
                </Button>
              </>
            ) : (
              <Button variant="outline" size="sm" onClick={() => setEditing(true)}>編集</Button>
            )}
            {!editing && (
               <Button variant="ghost" size="sm" onClick={handleDelete} className="text-destructive hover:text-destructive">
                 <Trash2 className="h-4 w-4" />
               </Button>
            )}
          </div>
        </div>
  
        {editing ? (
          <Textarea 
            value={content} 
            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value)}
            className="min-h-[150px] font-mono text-sm"
          />
        ) : (
          <div className="bg-muted/50 p-3 rounded-md text-sm whitespace-pre-wrap font-mono max-h-[150px] overflow-y-auto">
            {layer.content}
          </div>
        )}
      </div>
    );
  };


// ==========================================
// Insights Tab Components
// ==========================================

const InsightsTab = () => {
    const [usage, setUsage] = useState<TokenUsageSummary | null>(null);
    const [trend, setTrend] = useState<{ name: string; total: number }[]>([]);
    const [tenantId, setTenantId] = useState('default');
  
    useEffect(() => {
      const load = async () => {
        try {
          const u = await miraAdminApi.getTokenUsage(tenantId);
          setUsage(u);
  
          // Calculate last 7 days range using LOCAL TIME (JST)
          const formatDate = (d: Date) => {
             const year = d.getFullYear();
             const month = ('0' + (d.getMonth() + 1)).slice(-2);
             const day = ('0' + d.getDate()).slice(-2);
             return `${year}-${month}-${day}`;
          };

          const end = new Date();
          const start = new Date();
          start.setDate(end.getDate() - 6);
          
          const startDateStr = formatDate(start);
          const endDateStr = formatDate(end);
  
          const trendData = await miraAdminApi.getTokenUsageTrend(tenantId, startDateStr, endDateStr);
          
          // Aggregate by date
          const agg: Record<string, number> = {};
          // Initialize last 7 days with 0
          for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
             const dateStr = formatDate(d);
             agg[dateStr] = 0;
          }
  
          trendData.forEach(item => {
             // item.usageDate is YYYY-MM-DD from Server (which matched our JST request ideally)
             if (item.usageDate && agg[item.usageDate] !== undefined) {
               const current = agg[item.usageDate] || 0;
               agg[item.usageDate] = current + ((item.inputTokens || 0) + (item.outputTokens || 0));
             }
          });
  
          const chartData = Object.entries(agg).map(([date, total]) => ({
            name: date.substring(5).replace('-', '/'), // MM/DD
            total,
          }));
          setTrend(chartData);
  
        } catch (e) {
          console.error(e);
        }
      };
      load();
    }, [tenantId]);
  
    return (
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>本日の使用量</CardTitle>
              <CardDescription>テナント: {tenantId}</CardDescription>
            </div>
            <div className="flex gap-2">
                 <Combobox
                    options={[{ value: "default", label: "Default" }, { value: "enterprise-001", label: "Ent-001" }]}
                    value={tenantId}
                    onValueChange={setTenantId}
                    className="w-[150px]"
                 />
                 <Button variant="outline" size="sm" onClick={() => window.open(`/apps/mira/api/admin/conversations/export?tenantId=${tenantId}`, '_blank')}>
                    <Save className="mr-2 h-4 w-4" /> CSV
                 </Button>
            </div>
          </CardHeader>
          <CardContent>
            {usage ? (
              <div className="flex flex-col items-center justify-center p-6 bg-muted/50 rounded-lg">
                <span className="text-4xl font-bold text-primary">
                  {usage.totalTokens.toLocaleString()}
                </span>
                <span className="text-muted-foreground mt-2">Tokens Used Today</span>
              </div>
            ) : (
               <Loader2 className="animate-spin" />
            )}
          </CardContent>
        </Card>
  
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>週間トークン消費トレンド</CardTitle>
            <CardDescription>JST基準での集計</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="h-[300px]">
               <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={trend}
                    margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip formatter={(value: number) => value.toLocaleString()} />
                    <Legend />
                    <Bar dataKey="total" fill="#8884d8" name="Tokens" />
                  </BarChart>
               </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  };
