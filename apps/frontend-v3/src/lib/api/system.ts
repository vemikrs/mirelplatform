import { apiClient as client } from './client';

export interface SystemHealth {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
  components?: Record<string, {
    status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
    details?: Record<string, unknown>;
  }>;
}

export interface MetricResponse {
  name: string;
  measurements: {
    statistic: string;
    value: number;
  }[];
  availableTags?: { tag: string; values: string[] }[];
}

export interface SystemInfo {
  app?: {
    name?: string;
    version?: string;
    description?: string;
  };
  java?: {
    version?: string;
    vendor?: {
      name?: string;
    };
    runtime?: {
      name?: string;
      version?: string;
    };
  };
  os?: {
    name?: string;
    version?: string;
    arch?: string;
  };
}

export interface TenantStats {
  totalTenants: number;
  activeTenants: number;
  totalUsers: number;
  activeUsers: number;
}

export interface ApplicationModule {
  id: string;
  name: string;
  version: string;
  status: 'active' | 'inactive' | 'error';
  description?: string;
  lastUpdated?: string;
}

export interface SystemStatusDetail {
  health: SystemHealth;
  resources: {
    cpuUsage: number;
    memoryUsage: number;
    memoryUsed: number;
    memoryMax: number;
    diskUsage: number;
    diskUsed: number;
    diskTotal: number;
  };
  jvm: {
    uptime: number;
    startTime: Date;
    heapUsed: number;
    heapMax: number;
    nonHeapUsed: number;
    threads: number;
    loadedClasses: number;
  };
  services: {
    name: string;
    status: 'operational' | 'degraded' | 'outage';
    details?: Record<string, unknown>;
  }[];
  tenantStats: TenantStats;
  applicationModules: ApplicationModule[];
  systemInfo: SystemInfo;
}

export async function getSystemHealth(): Promise<SystemHealth> {
  const response = await client.get('/actuator/health');
  return response.data;
}

export async function getMetric(metricName: string): Promise<MetricResponse> {
  const response = await client.get(`/actuator/metrics/${metricName}`);
  return response.data;
}

export async function getSystemStatus() {
  try {
    const [health, cpu, memoryUsed, memoryMax, diskFree, diskTotal] = await Promise.all([
      getSystemHealth(),
      getMetric('system.cpu.usage'),
      getMetric('jvm.memory.used'),
      getMetric('jvm.memory.max'),
      getMetric('disk.free'),
      getMetric('disk.total'),
    ]);

    const cpuUsage = cpu.measurements[0]?.value ? Math.round(cpu.measurements[0].value * 100) : 0;
    
    const memUsedVal = memoryUsed.measurements[0]?.value || 0;
    const memMaxVal = memoryMax.measurements[0]?.value || 1;
    const memoryUsage = Math.round((memUsedVal / memMaxVal) * 100);

    const diskFreeVal = diskFree.measurements[0]?.value || 0;
    const diskTotalVal = diskTotal.measurements[0]?.value || 1;
    const diskUsedVal = diskTotalVal - diskFreeVal;
    const diskUsage = Math.round((diskUsedVal / diskTotalVal) * 100);

    // Map health components to services list
    const services = [];
    if (health.components) {
      if (health.components.db) {
        services.push({
          name: 'Database',
          status: health.components.db.status === 'UP' ? 'operational' : 'outage',
          uptime: '-', // Actuator doesn't provide uptime per component easily without more config
        });
      }
      if (health.components.redis) {
        services.push({
          name: 'Redis',
          status: health.components.redis.status === 'UP' ? 'operational' : 'outage',
          uptime: '-',
        });
      }
      if (health.components.diskSpace) {
        services.push({
          name: 'Disk Space',
          status: health.components.diskSpace.status === 'UP' ? 'operational' : 'outage',
          uptime: '-',
        });
      }
    }
    
    // Fallback if no components found (e.g. if details not exposed or simple health)
    if (services.length === 0) {
       services.push({
          name: 'Backend API',
          status: health.status === 'UP' ? 'operational' : 'outage',
          uptime: '-',
       });
    }

    return {
      cpuUsage,
      memoryUsage,
      diskUsage,
      services,
    };
  } catch (error) {
    console.error('Failed to fetch system status:', error);
    throw error;
  }
}

export async function getSystemInfo(): Promise<SystemInfo> {
  try {
    const response = await client.get('/actuator/info');
    return response.data || {};
  } catch {
    // actuator/info が公開されていない場合は空オブジェクトを返す
    return {};
  }
}

export async function getTenantStats(): Promise<TenantStats> {
  try {
    const response = await client.get<TenantStats>('/admin/stats/tenants');
    return response.data;
  } catch {
    // API未実装の場合はデフォルト値を返す（将来実装予定）
    return {
      totalTenants: 1,
      activeTenants: 1,
      totalUsers: 1,
      activeUsers: 1,
    };
  }
}

export async function getApplicationModules(): Promise<ApplicationModule[]> {
  try {
    const response = await client.get<ApplicationModule[]>('/admin/modules');
    return response.data;
  } catch {
    // API未実装の場合は既知のモジュールを返す（将来実装予定）
    return [
      { id: 'promarker', name: 'ProMarker', version: '1.0.0', status: 'active', description: 'コード生成ツール' },
      { id: 'studio', name: 'Studio', version: '0.1.0', status: 'active', description: 'ローコード開発環境' },
    ];
  }
}

export async function getSystemStatusDetail(): Promise<SystemStatusDetail> {
  try {
    const [
      health,
      cpu,
      memoryUsed,
      memoryMax,
      diskFree,
      diskTotal,
      heapUsed,
      heapMax,
      nonHeapUsed,
      threads,
      loadedClasses,
      uptime,
      systemInfo,
      tenantStats,
      applicationModules,
    ] = await Promise.all([
      getSystemHealth(),
      getMetric('system.cpu.usage'),
      getMetric('jvm.memory.used'),
      getMetric('jvm.memory.max'),
      getMetric('disk.free'),
      getMetric('disk.total'),
      getMetric('jvm.memory.used').then(m => m.measurements[0]?.value || 0).catch(() => 0),
      getMetric('jvm.memory.max').then(m => m.measurements[0]?.value || 0).catch(() => 0),
      getMetric('jvm.memory.used').then(m => m.measurements[0]?.value || 0).catch(() => 0),
      getMetric('jvm.threads.live').then(m => m.measurements[0]?.value || 0).catch(() => 0),
      getMetric('jvm.classes.loaded').then(m => m.measurements[0]?.value || 0).catch(() => 0),
      getMetric('process.uptime').then(m => m.measurements[0]?.value || 0).catch(() => 0),
      getSystemInfo(),
      getTenantStats(),
      getApplicationModules(),
    ]);

    const cpuUsage = cpu.measurements[0]?.value ? Math.round(cpu.measurements[0].value * 100) : 0;
    const memUsedVal = memoryUsed.measurements[0]?.value || 0;
    const memMaxVal = memoryMax.measurements[0]?.value || 1;
    const memoryUsage = Math.round((memUsedVal / memMaxVal) * 100);
    const diskFreeVal = diskFree.measurements[0]?.value || 0;
    const diskTotalVal = diskTotal.measurements[0]?.value || 1;
    const diskUsedVal = diskTotalVal - diskFreeVal;
    const diskUsage = Math.round((diskUsedVal / diskTotalVal) * 100);

    // Map health components to services
    const services: SystemStatusDetail['services'] = [];
    if (health.components) {
      Object.entries(health.components).forEach(([name, component]) => {
        if (name !== 'ping') {
          services.push({
            name: name.charAt(0).toUpperCase() + name.slice(1).replace(/([A-Z])/g, ' $1'),
            status: component.status === 'UP' ? 'operational' : 'outage',
            details: component.details,
          });
        }
      });
    }
    if (services.length === 0) {
      services.push({
        name: 'Backend API',
        status: health.status === 'UP' ? 'operational' : 'outage',
      });
    }

    const startTime = new Date(Date.now() - uptime * 1000);

    return {
      health,
      resources: {
        cpuUsage,
        memoryUsage,
        memoryUsed: memUsedVal,
        memoryMax: memMaxVal,
        diskUsage,
        diskUsed: diskUsedVal,
        diskTotal: diskTotalVal,
      },
      jvm: {
        uptime,
        startTime,
        heapUsed,
        heapMax,
        nonHeapUsed,
        threads,
        loadedClasses,
      },
      services,
      tenantStats,
      applicationModules,
      systemInfo,
    };
  } catch (error) {
    console.error('Failed to fetch system status detail:', error);
    throw error;
  }
}
