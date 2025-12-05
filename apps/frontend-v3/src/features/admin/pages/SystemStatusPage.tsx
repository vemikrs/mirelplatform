import {
  SectionHeading,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardDescription,
  Badge,
  Button,
  Tabs,
  TabsList,
  TabsTrigger,
  TabsContent,
  Tooltip,
  TooltipTrigger,
  TooltipContent,
} from '@mirel/ui';
import {
  Activity,
  Server,
  Cpu,
  HardDrive,
  RefreshCw,
  Users,
  Building2,
  Package,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  Database,
  Layers,
  Info,
  ArrowLeft,
} from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getSystemStatusDetail } from '@/lib/api/system';
import { Link } from 'react-router-dom';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
}

function formatUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400);
  const hours = Math.floor((seconds % 86400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  
  const parts = [];
  if (days > 0) parts.push(`${days}日`);
  if (hours > 0) parts.push(`${hours}時間`);
  if (minutes > 0) parts.push(`${minutes}分`);
  return parts.join(' ') || '0分';
}

function StatusBadge({ status }: { status: 'operational' | 'degraded' | 'outage' | 'UP' | 'DOWN' | 'UNKNOWN' | 'OUT_OF_SERVICE' | 'active' | 'inactive' | 'error' }) {
  const config = {
    operational: { label: '正常', variant: 'default' as const, className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' },
    UP: { label: '正常', variant: 'default' as const, className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' },
    active: { label: '有効', variant: 'default' as const, className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200' },
    degraded: { label: '低下', variant: 'default' as const, className: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200' },
    OUT_OF_SERVICE: { label: '停止中', variant: 'default' as const, className: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200' },
    UNKNOWN: { label: '不明', variant: 'default' as const, className: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200' },
    inactive: { label: '無効', variant: 'default' as const, className: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200' },
    outage: { label: '障害', variant: 'destructive' as const, className: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200' },
    DOWN: { label: '停止', variant: 'destructive' as const, className: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200' },
    error: { label: 'エラー', variant: 'destructive' as const, className: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200' },
  };
  const c = config[status] || config.UNKNOWN;
  return <Badge className={c.className}>{c.label}</Badge>;
}

function ResourceCard({ 
  icon: Icon, 
  label, 
  value, 
  usage, 
  usedLabel, 
  totalLabel,
  color 
}: { 
  icon: React.ElementType;
  label: string;
  value: string;
  usage: number;
  usedLabel?: string;
  totalLabel?: string;
  color: string;
}) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex items-center gap-4">
          <div className={`p-3 rounded-lg ${color}`}>
            <Icon className="size-6 text-white" />
          </div>
          <div className="flex-1">
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="text-2xl font-bold">{value}</p>
            {usedLabel && totalLabel && (
              <p className="text-xs text-muted-foreground mt-1">
                {usedLabel} / {totalLabel}
              </p>
            )}
          </div>
        </div>
        <div className="mt-4">
          <div className="h-2 w-full bg-surface-subtle rounded-full overflow-hidden">
            <div 
              className={`h-full rounded-full transition-all duration-500 ${
                usage > 90 ? 'bg-red-500' : usage > 70 ? 'bg-amber-500' : 'bg-green-500'
              }`}
              style={{ width: `${Math.min(usage, 100)}%` }}
            />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export function SystemStatusPage() {
  const queryClient = useQueryClient();
  const { data: status, isLoading, error, isFetching, dataUpdatedAt } = useQuery({
    queryKey: ['system-status-detail'],
    queryFn: getSystemStatusDetail,
    refetchInterval: 30000,
    retry: 1,
  });

  const handleRefresh = () => {
    queryClient.invalidateQueries({ queryKey: ['system-status-detail'] });
  };

  const formatLastUpdated = () => {
    if (!dataUpdatedAt) return '';
    const date = new Date(dataUpdatedAt);
    return date.toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24">
        <RefreshCw className="size-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error || !status) {
    return (
      <div className="space-y-6 pb-16">
        <SectionHeading
          eyebrow={
            <span className="inline-flex items-center gap-2">
              <Activity className="size-4" />
              システム管理
            </span>
          }
          title="システムステータス"
        />
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12 text-muted-foreground gap-4">
            <XCircle className="size-12 text-destructive/50" />
            <p>ステータスの取得に失敗しました</p>
            <Button onClick={handleRefresh}>再試行</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-16">
      <SectionHeading
        eyebrow={
          <span className="inline-flex items-center gap-2">
            <Activity className="size-4" />
            システム管理
          </span>
        }
        title="システムステータス"
        description="システム全体の稼働状況、リソース使用量、テナント統計、アプリケーションモジュールの状態を確認できます。"
        actions={
          <div className="flex items-center gap-3">
            <Button variant="outline" asChild>
              <Link to="/home">
                <ArrowLeft className="size-4 mr-2" />
                ポータルに戻る
              </Link>
            </Button>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              {dataUpdatedAt && <span>最終更新: {formatLastUpdated()}</span>}
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={handleRefresh}
                    disabled={isFetching}
                  >
                    <RefreshCw className={`size-4 ${isFetching ? 'animate-spin' : ''}`} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>更新</TooltipContent>
              </Tooltip>
            </div>
          </div>
        }
      />

      {/* Overall Status Banner */}
      <Card className={`border-2 ${
        status.health.status === 'UP' 
          ? 'border-green-500/30 bg-green-50/50 dark:bg-green-950/20' 
          : 'border-red-500/30 bg-red-50/50 dark:bg-red-950/20'
      }`}>
        <CardContent className="py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              {status.health.status === 'UP' ? (
                <CheckCircle2 className="size-8 text-green-500" />
              ) : (
                <AlertTriangle className="size-8 text-red-500" />
              )}
              <div>
                <h3 className="text-lg font-semibold">
                  {status.health.status === 'UP' ? 'すべてのシステムが正常に稼働中' : 'システムに問題が発生しています'}
                </h3>
                <p className="text-sm text-muted-foreground">
                  稼働時間: {formatUptime(status.jvm.uptime)} | 
                  起動日時: {status.jvm.startTime.toLocaleString('ja-JP')}
                </p>
              </div>
            </div>
            <StatusBadge status={status.health.status} />
          </div>
        </CardContent>
      </Card>

      <Tabs defaultValue="overview" className="w-full">
        <TabsList className="mb-6">
          <TabsTrigger value="overview" className="gap-2">
            <Activity className="size-4" />
            概要
          </TabsTrigger>
          <TabsTrigger value="services" className="gap-2">
            <Server className="size-4" />
            サービス
          </TabsTrigger>
          <TabsTrigger value="tenants" className="gap-2">
            <Building2 className="size-4" />
            テナント
          </TabsTrigger>
          <TabsTrigger value="modules" className="gap-2">
            <Package className="size-4" />
            モジュール
          </TabsTrigger>
          <TabsTrigger value="system" className="gap-2">
            <Info className="size-4" />
            システム情報
          </TabsTrigger>
        </TabsList>

        {/* Overview Tab */}
        <TabsContent value="overview" className="space-y-6">
          {/* Resource Usage */}
          <div className="grid gap-4 md:grid-cols-3">
            <ResourceCard
              icon={Cpu}
              label="CPU使用率"
              value={`${status.resources.cpuUsage}%`}
              usage={status.resources.cpuUsage}
              color="bg-blue-500"
            />
            <ResourceCard
              icon={Server}
              label="メモリ使用率"
              value={`${status.resources.memoryUsage}%`}
              usage={status.resources.memoryUsage}
              usedLabel={formatBytes(status.resources.memoryUsed)}
              totalLabel={formatBytes(status.resources.memoryMax)}
              color="bg-purple-500"
            />
            <ResourceCard
              icon={HardDrive}
              label="ディスク使用率"
              value={`${status.resources.diskUsage}%`}
              usage={status.resources.diskUsage}
              usedLabel={formatBytes(status.resources.diskUsed)}
              totalLabel={formatBytes(status.resources.diskTotal)}
              color="bg-green-500"
            />
          </div>

          {/* Quick Stats */}
          <div className="grid gap-4 md:grid-cols-4">
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <Building2 className="size-8 text-primary" />
                  <div>
                    <p className="text-sm text-muted-foreground">テナント数</p>
                    <p className="text-2xl font-bold">{status.tenantStats.activeTenants}</p>
                    <p className="text-xs text-muted-foreground">/ {status.tenantStats.totalTenants} 全体</p>
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <Users className="size-8 text-primary" />
                  <div>
                    <p className="text-sm text-muted-foreground">ユーザー数</p>
                    <p className="text-2xl font-bold">{status.tenantStats.activeUsers}</p>
                    <p className="text-xs text-muted-foreground">/ {status.tenantStats.totalUsers} 全体</p>
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <Layers className="size-8 text-primary" />
                  <div>
                    <p className="text-sm text-muted-foreground">JVMスレッド</p>
                    <p className="text-2xl font-bold">{status.jvm.threads}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-4">
                  <Clock className="size-8 text-primary" />
                  <div>
                    <p className="text-sm text-muted-foreground">稼働時間</p>
                    <p className="text-2xl font-bold">{formatUptime(status.jvm.uptime)}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Services Overview */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Database className="size-5" />
                サービス状態
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                {status.services.map((service) => (
                  <div 
                    key={service.name} 
                    className="flex items-center justify-between p-3 rounded-lg bg-surface-subtle/50"
                  >
                    <div className="flex items-center gap-2">
                      <div className={`size-2 rounded-full ${
                        service.status === 'operational' ? 'bg-green-500' : 
                        service.status === 'degraded' ? 'bg-amber-500' : 'bg-red-500'
                      }`} />
                      <span className="font-medium">{service.name}</span>
                    </div>
                    <StatusBadge status={service.status} />
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Services Tab */}
        <TabsContent value="services" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>サービス詳細</CardTitle>
              <CardDescription>
                各サービスコンポーネントの詳細な状態を確認できます。
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {status.services.map((service) => (
                  <div 
                    key={service.name} 
                    className="p-4 rounded-lg border bg-card"
                  >
                    <div className="flex items-center justify-between mb-3">
                      <div className="flex items-center gap-3">
                        <div className={`p-2 rounded-lg ${
                          service.status === 'operational' ? 'bg-green-100 dark:bg-green-900/30' : 
                          service.status === 'degraded' ? 'bg-amber-100 dark:bg-amber-900/30' : 
                          'bg-red-100 dark:bg-red-900/30'
                        }`}>
                          {service.status === 'operational' ? (
                            <CheckCircle2 className="size-5 text-green-600 dark:text-green-400" />
                          ) : service.status === 'degraded' ? (
                            <AlertTriangle className="size-5 text-amber-600 dark:text-amber-400" />
                          ) : (
                            <XCircle className="size-5 text-red-600 dark:text-red-400" />
                          )}
                        </div>
                        <div>
                          <h4 className="font-semibold">{service.name}</h4>
                        </div>
                      </div>
                      <StatusBadge status={service.status} />
                    </div>
                    {service.details && Object.keys(service.details).length > 0 && (
                      <div className="mt-3 p-3 rounded bg-surface-subtle/50">
                        <p className="text-xs font-medium text-muted-foreground mb-2">詳細情報</p>
                        <pre className="text-xs overflow-auto">
                          {JSON.stringify(service.details, null, 2)}
                        </pre>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Tenants Tab */}
        <TabsContent value="tenants" className="space-y-6">
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Building2 className="size-5" />
                  テナント統計
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between p-3 rounded-lg bg-surface-subtle/50">
                  <span className="text-muted-foreground">総テナント数</span>
                  <span className="text-xl font-bold">{status.tenantStats.totalTenants}</span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg bg-surface-subtle/50">
                  <span className="text-muted-foreground">アクティブテナント</span>
                  <span className="text-xl font-bold text-green-600">{status.tenantStats.activeTenants}</span>
                </div>
                <div className="h-2 w-full bg-surface-subtle rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-green-500 rounded-full"
                    style={{ 
                      width: `${status.tenantStats.totalTenants > 0 
                        ? (status.tenantStats.activeTenants / status.tenantStats.totalTenants) * 100 
                        : 0}%` 
                    }}
                  />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Users className="size-5" />
                  ユーザー統計
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between p-3 rounded-lg bg-surface-subtle/50">
                  <span className="text-muted-foreground">総ユーザー数</span>
                  <span className="text-xl font-bold">{status.tenantStats.totalUsers}</span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg bg-surface-subtle/50">
                  <span className="text-muted-foreground">アクティブユーザー</span>
                  <span className="text-xl font-bold text-green-600">{status.tenantStats.activeUsers}</span>
                </div>
                <div className="h-2 w-full bg-surface-subtle rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-green-500 rounded-full"
                    style={{ 
                      width: `${status.tenantStats.totalUsers > 0 
                        ? (status.tenantStats.activeUsers / status.tenantStats.totalUsers) * 100 
                        : 0}%` 
                    }}
                  />
                </div>
              </CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle>テナント管理</CardTitle>
              <CardDescription>
                テナントの詳細な管理は管理画面から行えます。
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Button asChild>
                <Link to="/admin/platform/tenants">
                  <Building2 className="size-4 mr-2" />
                  テナント管理へ
                </Link>
              </Button>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Modules Tab */}
        <TabsContent value="modules" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Package className="size-5" />
                アプリケーションモジュール
              </CardTitle>
              <CardDescription>
                インストールされているアプリケーションモジュールの一覧と状態を確認できます。
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {status.applicationModules.map((module) => (
                  <div 
                    key={module.id} 
                    className="flex items-center justify-between p-4 rounded-lg border bg-card"
                  >
                    <div className="flex items-center gap-4">
                      <div className={`p-2 rounded-lg ${
                        module.status === 'active' ? 'bg-green-100 dark:bg-green-900/30' : 
                        module.status === 'inactive' ? 'bg-gray-100 dark:bg-gray-800' : 
                        'bg-red-100 dark:bg-red-900/30'
                      }`}>
                        <Package className={`size-5 ${
                          module.status === 'active' ? 'text-green-600 dark:text-green-400' : 
                          module.status === 'inactive' ? 'text-gray-600 dark:text-gray-400' : 
                          'text-red-600 dark:text-red-400'
                        }`} />
                      </div>
                      <div>
                        <h4 className="font-semibold">{module.name}</h4>
                        {module.description && (
                          <p className="text-sm text-muted-foreground">{module.description}</p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-4">
                      <div className="text-right">
                        <p className="text-sm font-medium">v{module.version}</p>
                        {module.lastUpdated && (
                          <p className="text-xs text-muted-foreground">
                            更新: {new Date(module.lastUpdated).toLocaleDateString('ja-JP')}
                          </p>
                        )}
                      </div>
                      <StatusBadge status={module.status} />
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* System Info Tab */}
        <TabsContent value="system" className="space-y-6">
          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>アプリケーション情報</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">名前</span>
                  <span className="font-medium">{status.systemInfo.app?.name || 'mirelplatform'}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">バージョン</span>
                  <span className="font-medium">{status.systemInfo.app?.version || '-'}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">説明</span>
                  <span className="font-medium">{status.systemInfo.app?.description || '-'}</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Java環境</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">バージョン</span>
                  <span className="font-medium">{status.systemInfo.java?.version || '-'}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">ベンダー</span>
                  <span className="font-medium">{status.systemInfo.java?.vendor?.name || '-'}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">ランタイム</span>
                  <span className="font-medium">{status.systemInfo.java?.runtime?.name || '-'}</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>OS情報</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">OS</span>
                  <span className="font-medium">{status.systemInfo.os?.name || '-'}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">バージョン</span>
                  <span className="font-medium">{status.systemInfo.os?.version || '-'}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">アーキテクチャ</span>
                  <span className="font-medium">{status.systemInfo.os?.arch || '-'}</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>JVM統計</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">ヒープ使用量</span>
                  <span className="font-medium">{formatBytes(status.jvm.heapUsed)} / {formatBytes(status.jvm.heapMax)}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">Non-Heap使用量</span>
                  <span className="font-medium">{formatBytes(status.jvm.nonHeapUsed)}</span>
                </div>
                <div className="flex justify-between p-2 rounded bg-surface-subtle/50">
                  <span className="text-muted-foreground">ロード済みクラス</span>
                  <span className="font-medium">{status.jvm.loadedClasses.toLocaleString()}</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}
