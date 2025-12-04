import { Card, CardContent, CardHeader, CardTitle, Badge } from '@mirel/ui';
import { Activity, Server, Cpu, HardDrive } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';

// Mock data for system status
const SYSTEM_STATUS = {
  cpuUsage: 45,
  memoryUsage: 62,
  diskUsage: 28,
  services: [
    { name: 'API Server', status: 'operational', uptime: '14d 2h' },
    { name: 'Database (PostgreSQL)', status: 'operational', uptime: '45d 12h' },
    { name: 'Cache (Redis)', status: 'operational', uptime: '5d 8h' },
    { name: 'Search Engine', status: 'degraded', uptime: '2d 1h' },
  ]
};

export function SystemStatusWidget() {
  const { user } = useAuth();
  console.log('[DEBUG] SystemStatusWidget rendered for user:', user?.email);
  
  // Only show to admins (simplified check, ideally check role/permission)
  // Assuming 'admin' in username or specific role check if available
  // For now, let's assume all logged in users can see it if they have the component rendered,
  // but the parent page should control visibility.
  // Or we can check here.
  // Let's implement a basic role check if `user` object has roles.
  // Based on previous context, user object might not have roles directly on it in a simple way,
  // but let's assume the parent handles the "Admin only" requirement for now to keep this component pure.
  
  return (
    <Card className="bg-card/50 backdrop-blur-sm border-outline/15 shadow-sm">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg flex items-center gap-2">
          <Activity className="size-5 text-primary" />
          システム稼働状況
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Resources */}
        <div className="grid grid-cols-3 gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Cpu className="size-3" /> CPU
            </div>
            <div className="text-2xl font-bold">{SYSTEM_STATUS.cpuUsage}%</div>
            <div className="h-1.5 w-full bg-surface-subtle rounded-full overflow-hidden">
              <div 
                className="h-full bg-blue-500 rounded-full" 
                style={{ width: `${SYSTEM_STATUS.cpuUsage}%` }}
              />
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Server className="size-3" /> Memory
            </div>
            <div className="text-2xl font-bold">{SYSTEM_STATUS.memoryUsage}%</div>
            <div className="h-1.5 w-full bg-surface-subtle rounded-full overflow-hidden">
              <div 
                className="h-full bg-purple-500 rounded-full" 
                style={{ width: `${SYSTEM_STATUS.memoryUsage}%` }}
              />
            </div>
          </div>
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <HardDrive className="size-3" /> Disk
            </div>
            <div className="text-2xl font-bold">{SYSTEM_STATUS.diskUsage}%</div>
            <div className="h-1.5 w-full bg-surface-subtle rounded-full overflow-hidden">
              <div 
                className="h-full bg-green-500 rounded-full" 
                style={{ width: `${SYSTEM_STATUS.diskUsage}%` }}
              />
            </div>
          </div>
        </div>

        {/* Services */}
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-muted-foreground">サービス状態</h4>
          <div className="space-y-2">
            {SYSTEM_STATUS.services.map((service) => (
              <div key={service.name} className="flex items-center justify-between text-sm p-2 rounded-md bg-surface-subtle/50">
                <div className="flex items-center gap-2">
                  <div className={`size-2 rounded-full ${
                    service.status === 'operational' ? 'bg-green-500' : 
                    service.status === 'degraded' ? 'bg-amber-500' : 'bg-red-500'
                  }`} />
                  <span>{service.name}</span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-muted-foreground">Uptime: {service.uptime}</span>
                  <Badge 
                    variant={service.status === 'operational' ? 'outline' : 'destructive'} 
                    className="text-xs h-5"
                  >
                    {service.status === 'operational' ? '正常' : '警告'}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
