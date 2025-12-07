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
  Badge,
  Input,
  Label,
  Textarea,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  // Alert, AlertDescription, AlertTitle, // Unused for now
  useToast
} from '@mirel/ui';
import { Loader2, Plus, Save, Trash2 } from 'lucide-react';

import { 
  miraAdminApi, 
  type MiraContextLayer, 
  type LimitsConfig, 
  type TokenUsageSummary 
} from '@/lib/api/mira-admin';
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
          AIアシスタントのコンテキスト、制限、統計情報を管理します。
        </p>
      </div>

      <Tabs defaultValue="context" className="space-y-4">
        <TabsList>
          <TabsTrigger value="context">コンテキスト管理</TabsTrigger>
          <TabsTrigger value="stats">利用統計・制限</TabsTrigger>
        </TabsList>

        <TabsContent value="context" className="space-y-4">
          <ContextManagement />
        </TabsContent>

        <TabsContent value="stats" className="space-y-4">
          <StatsAndLimits />
        </TabsContent>
      </Tabs>
    </div>
  );
};

const ContextManagement = () => {
  const { toast } = useToast();
  const [activeTab, setActiveTab] = useState<'system' | 'tenant'>('system');
  const [layers, setLayers] = useState<MiraContextLayer[]>([]);
  const [loading, setLoading] = useState(false);
  const [tenantId, setTenantId] = useState('default'); // Tentative default

  // Create State
  const [creating, setCreating] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [newContext, setNewContext] = useState({ category: '', content: '', priority: 0 });

  const fetchContexts = async () => {
    setLoading(true);
    try {
      if (activeTab === 'system') {
        const data = await miraAdminApi.getSystemContexts();
        setLayers(data);
      } else {
        const data = await miraAdminApi.getTenantContexts(tenantId);
        setLayers(data);
      }
    } catch (error) {
      console.error(error);
      toast({
        title: 'エラー',
        description: 'コンテキストの取得に失敗しました',
        variant: 'destructive',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async () => {
    setCreateLoading(true);
    try {
      const payload: Partial<MiraContextLayer> = {
        category: newContext.category,
        content: newContext.content,
        priority: newContext.priority,
        enabled: true,
        scope: activeTab === 'system' ? 'SYSTEM' : 'TENANT',
        scopeId: activeTab === 'tenant' ? tenantId : undefined,
      };

      if (activeTab === 'system') {
        await miraAdminApi.saveSystemContext(payload);
      } else {
        await miraAdminApi.saveTenantContext(tenantId, payload);
      }

      toast({ title: '成功', description: 'コンテキストを作成しました', variant: 'success' });
      setCreating(false);
      setNewContext({ category: '', content: '', priority: 0 });
      fetchContexts();
    } catch (error) {
      console.error(error);
      toast({ title: 'エラー', description: '作成に失敗しました', variant: 'destructive' });
    } finally {
      setCreateLoading(false);
    }
  };

  useEffect(() => {
    fetchContexts();
  }, [activeTab, tenantId]);

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
      <Card className="md:col-span-1">
        <CardHeader>
          <CardTitle>設定対象</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label>スコープ</Label>
            <Select 
              value={activeTab} 
              onValueChange={(v: 'system' | 'tenant') => setActiveTab(v)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="system">システム全体 (System)</SelectItem>
                <SelectItem value="tenant">テナント (Tenant)</SelectItem>
              </SelectContent>
            </Select>
          </div>
          
          {activeTab === 'tenant' && (
            <div className="space-y-2">
              <Label>テナントID</Label>
              <Input 
                value={tenantId} 
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setTenantId(e.target.value)} 
                placeholder="tenant-id" 
              />
              <Button 
                variant="outline" 
                size="sm" 
                className="w-full"
                onClick={fetchContexts}
              >
                再読み込み
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="md:col-span-3">
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>コンテキストレイヤー一覧</CardTitle>
            <CardDescription>
              {activeTab === 'system' ? '全ユーザーに適用されるベースプロンプト' : `${tenantId} のユーザーに適用されるプロンプト`}
            </CardDescription>
          </div>
          <Button onClick={() => setCreating(true)}>
            <Plus className="mr-2 h-4 w-4" /> 新規追加
          </Button>
        </CardHeader>
        <CardContent>
          {creating && (
            <div className="mb-6 border rounded-lg p-4 bg-muted/30">
              <h3 className="font-semibold mb-4">新規コンテキスト作成</h3>
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>カテゴリ (例: terminology, style)</Label>
                    <Input 
                      value={newContext.category} 
                      onChange={(e) => setNewContext({ ...newContext, category: e.target.value })} 
                      placeholder="terminology"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>優先度 (Priority)</Label>
                    <Input 
                      type="number"
                      value={newContext.priority} 
                      onChange={(e) => setNewContext({ ...newContext, priority: Number(e.target.value) })} 
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label>初期コンテンツ</Label>
                  <Textarea 
                    value={newContext.content} 
                    onChange={(e) => setNewContext({ ...newContext, content: e.target.value })} 
                    className="font-mono min-h-[100px]"
                    placeholder="# Describe context here..."
                  />
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="ghost" onClick={() => setCreating(false)} disabled={createLoading}>キャンセル</Button>
                  <Button onClick={handleCreate} disabled={createLoading || !newContext.category}>
                    {createLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                    作成
                  </Button>
                </div>
              </div>
            </div>
          )}
          {loading ? (
            <div className="flex justify-center p-8">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : layers.length === 0 ? (
            <div className="text-center p-8 text-muted-foreground">
              設定されているコンテキストはありません。
            </div>
          ) : (
            <div className="space-y-4">
              {layers.map((layer) => (
                <ContextLayerEditor key={layer.id} layer={layer} onSave={fetchContexts} onDelete={fetchContexts} />
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

const ContextLayerEditor = ({ layer, onSave, onDelete }: { layer: MiraContextLayer; onSave: () => void; onDelete: () => void }) => {
  const { toast } = useToast();
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(layer.content);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

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
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('本当に削除しますか？')) return;
    setDeleting(true);
    try {
      await miraAdminApi.deleteContext(layer.id);
      toast({ title: '成功', description: '削除しました', variant: 'success' });
      onDelete();
    } catch (error) {
      console.error(error);
      toast({ title: 'エラー', description: '削除に失敗しました', variant: 'destructive' });
      setDeleting(false);
    }
  };

  return (
    <div className="border rounded-lg p-4 space-y-4">
      <div className="flex justify-between items-start">
        <div>
          <h3 className="font-semibold text-lg flex items-center gap-2">
            {layer.category}
            {!layer.enabled && <Badge variant="neutral">無効</Badge>}
          </h3>
          <p className="text-sm text-muted-foreground">Priority: {layer.priority}</p>
        </div>
        <div className="flex gap-2">
          {editing ? (
            <>
              <Button variant="ghost" size="sm" onClick={() => setEditing(false)}>キャンセル</Button>
              <Button size="sm" onClick={handleSave} disabled={saving}>
                {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                <Save className="mr-2 h-4 w-4" /> 保存
              </Button>
            </>
          ) : (
            <Button variant="outline" size="sm" onClick={() => setEditing(true)}>編集</Button>
          )}
          {!editing && (
             <Button variant="ghost" size="sm" onClick={handleDelete} disabled={deleting} className="text-destructive hover:text-destructive">
               {deleting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
               <Trash2 className="h-4 w-4" />
             </Button>
          )}
        </div>
      </div>

      {editing ? (
        <Textarea 
          value={content} 
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value)}
          className="min-h-[200px] font-mono text-sm"
        />
      ) : (
        <div className="bg-muted p-3 rounded-md text-sm whitespace-pre-wrap font-mono max-h-[200px] overflow-y-auto">
          {layer.content}
        </div>
      )}
    </div>
  );
};

const StatsAndLimits = () => {
  const [limits, setLimits] = useState<LimitsConfig | null>(null);
  const [usage, setUsage] = useState<TokenUsageSummary | null>(null);
  const [trend, setTrend] = useState<{ name: string; total: number }[]>([]);
  const [tenantId] = useState('default'); // Default tenant for now

  useEffect(() => {
    const load = async () => {
      try {
        const l = await miraAdminApi.getLimits();
        setLimits(l);
        const u = await miraAdminApi.getTokenUsage(tenantId);
        setUsage(u);

        // Calculate last 7 days range
        const end = new Date();
        const start = new Date();
        start.setDate(end.getDate() - 6);
        const startDateStr = start.toISOString().split('T')[0] || '';
        const endDateStr = end.toISOString().split('T')[0] || '';

        const trendData = await miraAdminApi.getTokenUsageTrend(tenantId, startDateStr, endDateStr);
        
        // Aggregate by date
        const agg: Record<string, number> = {};
        // Initialize last 7 days with 0
        for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
           const dateStr = d.toISOString().split('T')[0];
           if (dateStr) agg[dateStr] = 0;
        }

        trendData.forEach(item => {
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

  if (!limits) return <Loader2 className="animate-spin" />;

  return (
    <div className="grid gap-6 md:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle>制限設定 (Read Only)</CardTitle>
          <CardDescription>application.yml で設定された値です</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div>
            <h4 className="font-medium mb-2 flex items-center gap-2">
              Rate Limit
              <Badge variant={limits.rateLimit.enabled ? 'neutral' : 'neutral'}>
                {limits.rateLimit.enabled ? 'Enabled' : 'Disabled'}
              </Badge>
            </h4>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="border p-2 rounded">
                <span className="text-muted-foreground block">RPM (User)</span>
                <span className="font-mono text-lg">{limits.rateLimit.requestsPerMinute}</span>
              </div>
            </div>
          </div>

          <div>
            <h4 className="font-medium mb-2 flex items-center gap-2">
              Token Quota
              <Badge variant={limits.quota.enabled ? 'neutral' : 'neutral'}>
                {limits.quota.enabled ? 'Enabled' : 'Disabled'}
              </Badge>
            </h4>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div className="border p-2 rounded">
                <span className="text-muted-foreground block">Daily Limit (Tenant)</span>
                <span className="font-mono text-lg">{limits.quota.dailyTokenLimit.toLocaleString()}</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>本日の使用量</CardTitle>
            <CardDescription>テナント: {tenantId}</CardDescription>
          </div>
          <Button variant="outline" size="sm" onClick={() => window.open(`/apps/mira/api/admin/conversations/export?tenantId=${tenantId}`, '_blank')}>
            <Save className="mr-2 h-4 w-4" /> CSV Export
          </Button>
        </CardHeader>
        <CardContent>
          {usage ? (
            <div className="space-y-4">
              <div className="flex flex-col items-center justify-center p-6 bg-muted/50 rounded-lg">
                <span className="text-4xl font-bold text-primary">
                  {usage.totalTokens.toLocaleString()}
                </span>
                <span className="text-muted-foreground mt-2">Tokens Used Today</span>
              </div>
              
              {limits.quota.enabled && (
                <div className="space-y-2">
                  <div className="flex justify-between text-sm">
                    <span>Quota Usage</span>
                    <span>{Math.round((usage.totalTokens / limits.quota.dailyTokenLimit) * 100)}%</span>
                  </div>
                  <div className="h-2 bg-secondary rounded-full overflow-hidden">
                    <div 
                      className={`h-full ${usage.totalTokens > limits.quota.dailyTokenLimit ? 'bg-destructive' : 'bg-primary'}`}
                      style={{ width: `${Math.min(100, (usage.totalTokens / limits.quota.dailyTokenLimit) * 100)}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          ) : (
             <Loader2 className="animate-spin" />
          )}
        </CardContent>
      </Card>

      <Card className="md:col-span-2">
        <CardHeader>
          <CardTitle>週間トークン消費トレンド</CardTitle>
          <CardDescription>過去7日間のトークン使用状況</CardDescription>
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
