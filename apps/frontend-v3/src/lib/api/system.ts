import { apiClient as client } from './client';

export interface SystemHealth {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
  components?: Record<string, {
    status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
    details?: any;
  }>;
}

export interface MetricResponse {
  name: string;
  measurements: {
    statistic: string;
    value: number;
  }[];
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
