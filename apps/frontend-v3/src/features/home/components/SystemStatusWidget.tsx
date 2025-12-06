import { Card, CardContent, CardHeader, CardTitle, Badge, Button, Tooltip, TooltipTrigger, TooltipContent } from '@mirel/ui';
import { Activity, Server, Cpu, HardDrive, RefreshCw, AlertCircle, ExternalLink } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getSystemStatus } from '@/lib/api/system';
import { Link } from 'react-router-dom';

export function SystemStatusWidget() {
  const queryClient = useQueryClient();
  const { data: status, isLoading, error, isFetching, dataUpdatedAt } = useQuery({
    queryKey: ['system-status'],
    queryFn: getSystemStatus,
    refetchInterval: 30000, // Refresh every 30 seconds
    retry: 1,
  });

  const handleRefresh = () => {
    queryClient.invalidateQueries({ queryKey: ['system-status'] });
  };

  // 最終更新時刻をフォーマット
  const formatLastUpdated = () => {
    if (!dataUpdatedAt) return '';
    const date = new Date(dataUpdatedAt);
    return date.toLocaleTimeString('ja-JP', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };
  
  if (isLoading) {
    return (
      <Card className="bg-card/50 backdrop-blur-sm border-outline/15 shadow-sm h-full">
        <CardHeader className="pb-3">
          <CardTitle className="text-lg flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Activity className="size-5 text-primary" />
              システム稼働状況
            </div>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex items-center justify-center py-12">
          <RefreshCw className="size-6 animate-spin text-muted-foreground" />
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className="bg-card/50 backdrop-blur-sm border-outline/15 shadow-sm h-full">
        <CardHeader className="pb-3">
          <CardTitle className="text-lg flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Activity className="size-5 text-primary" />
              システム稼働状況
            </div>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-8"
                  onClick={handleRefresh}
                  disabled={isFetching}
                >
                  <RefreshCw className={`size-4 ${isFetching ? 'animate-spin' : ''}`} />
                </Button>
              </TooltipTrigger>
              <TooltipContent>再取得</TooltipContent>
            </Tooltip>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col items-center justify-center py-8 text-muted-foreground gap-2">
          <AlertCircle className="size-8 text-destructive/50" />
          <p className="text-sm">ステータスの取得に失敗しました</p>
        </CardContent>
      </Card>
    );
  }

  if (!status) return null;

  return (
    <Card className="bg-card/50 backdrop-blur-sm border-outline/15 shadow-sm">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Activity className="size-5 text-primary" />
            システム稼働状況
          </div>
          <div className="flex items-center gap-2">
            {dataUpdatedAt && (
              <span className="text-xs text-muted-foreground font-normal">
                {formatLastUpdated()}
              </span>
            )}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="size-8"
                  onClick={handleRefresh}
                  disabled={isFetching}
                >
                  <RefreshCw className={`size-4 ${isFetching ? 'animate-spin' : ''}`} />
                </Button>
              </TooltipTrigger>
              <TooltipContent>更新</TooltipContent>
            </Tooltip>
          </div>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Resources */}
        <div className="grid grid-cols-3 gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Cpu className="size-3" /> CPU
            </div>
            <div className="text-2xl font-bold">{status.cpuUsage}%</div>
            <div className="h-1.5 w-full bg-surface-subtle rounded-full overflow-hidden">
              <div 
                className="h-full bg-blue-500 rounded-full transition-all duration-500" 
                style={{ width: `${status.cpuUsage}%` }}
              />
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Server className="size-3" /> Memory
            </div>
            <div className="text-2xl font-bold">{status.memoryUsage}%</div>
            <div className="h-1.5 w-full bg-surface-subtle rounded-full overflow-hidden">
              <div 
                className="h-full bg-purple-500 rounded-full transition-all duration-500" 
                style={{ width: `${status.memoryUsage}%` }}
              />
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <HardDrive className="size-3" /> Disk
            </div>
            <div className="text-2xl font-bold">{status.diskUsage}%</div>
            <div className="h-1.5 w-full bg-surface-subtle rounded-full overflow-hidden">
              <div 
                className="h-full bg-green-500 rounded-full transition-all duration-500" 
                style={{ width: `${status.diskUsage}%` }}
              />
            </div>
          </div>
        </div>

        {/* Services */}
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-muted-foreground">サービス状態</h4>
          <div className="space-y-2">
            {status.services.map((service) => (
              <div key={service.name} className="flex items-center justify-between text-sm p-2 rounded-md bg-surface-subtle/50">
                <div className="flex items-center gap-2">
                  <div className={`size-2 rounded-full ${
                    service.status === 'operational' ? 'bg-green-500' : 
                    service.status === 'degraded' ? 'bg-amber-500' : 'bg-red-500'
                  }`} />
                  <span>{service.name}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-muted-foreground">{service.uptime !== '-' ? `Uptime: ${service.uptime}` : ''}</span>
                  <Badge 
                    variant={service.status === 'operational' ? 'outline' : 'destructive'} 
                    className="text-xs h-5"
                  >
                    {service.status === 'operational' ? '正常' : '異常'}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* View Details Link */}
        <div className="pt-2 border-t">
          <Button variant="ghost" size="sm" className="w-full" asChild>
            <Link to="/admin/platform/status">
              詳細を表示
              <ExternalLink className="size-3 ml-2" />
            </Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
