import { apiClient } from './client';

export interface MiraContextLayer {
  id: string;
  scope: 'SYSTEM' | 'TENANT' | 'ORGANIZATION' | 'USER';
  scopeId?: string;
  category: string;
  content: string;
  priority: number;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
  version?: number;
}

export interface LimitsConfig {
  rateLimit: {
    enabled: boolean;
    requestsPerMinute: number;
    burstCapacity: number;
  };
  quota: {
    enabled: boolean;
    dailyTokenLimit: number;
    alertThresholdPercent: number;
  };
}

export interface TokenUsageSummary {
  tenantId: string;
  date: string;
  totalTokens: number;
}

export interface MiraTokenUsage {
  id: string;
  tenantId: string;
  userId: string;
  usageDate: string;
  inputTokens: number;
  outputTokens: number;
  requestCount: number;
}

export const miraAdminApi = {
  // System Context Management
  getSystemContexts: async (): Promise<MiraContextLayer[]> => {
    const response = await apiClient.get<MiraContextLayer[]>('/apps/mira/api/admin/context/system');
    return response.data;
  },

  saveSystemContext: async (layer: Partial<MiraContextLayer>): Promise<MiraContextLayer> => {
    const response = await apiClient.post<MiraContextLayer>('/apps/mira/api/admin/context/system', layer);
    return response.data;
  },

  // Tenant Context Management
  getTenantContexts: async (tenantId: string): Promise<MiraContextLayer[]> => {
    const response = await apiClient.get<MiraContextLayer[]>(`/apps/mira/api/admin/context/tenant/${tenantId}`);
    return response.data;
  },

  saveTenantContext: async (tenantId: string, layer: Partial<MiraContextLayer>): Promise<MiraContextLayer> => {
    const response = await apiClient.post<MiraContextLayer>(`/apps/mira/api/admin/context/tenant/${tenantId}`, layer);
    return response.data;
  },

  deleteContext: async (contextId: string): Promise<void> => {
    await apiClient.delete(`/apps/mira/api/admin/context/${contextId}`);
  },

  // Limits & Usage
  getLimits: async (): Promise<LimitsConfig> => {
    const response = await apiClient.get<LimitsConfig>('/apps/mira/api/admin/limits');
    return response.data;
  },

  getTokenUsage: async (tenantId: string, date?: string): Promise<TokenUsageSummary> => {
    const params = new URLSearchParams({ tenantId });
    if (date) params.append('date', date);
    
    const response = await apiClient.get<TokenUsageSummary>(`/apps/mira/api/admin/token-usage/summary?${params.toString()}`);
    return response.data;
  },

  getTokenUsageTrend: async (tenantId: string, startDate: string, endDate: string): Promise<MiraTokenUsage[]> => {
    const params = new URLSearchParams({ tenantId, startDate, endDate });
    const response = await apiClient.get<MiraTokenUsage[]>(`/apps/mira/api/admin/token-usage/trend?${params.toString()}`);
    return response.data;
  },

  // Dynamic Settings
  getAiConfig: async (tenantId?: string): Promise<AiConfig> => {
    const params = new URLSearchParams();
    if (tenantId) params.append('tenantId', tenantId);
    const response = await apiClient.get<AiConfig>(`/apps/mira/api/admin/config/ai?${params.toString()}`);
    return response.data;
  },

  saveAiConfig: async (config: AiConfig, tenantId?: string): Promise<void> => {
    const params = new URLSearchParams();
    if (tenantId) params.append('tenantId', tenantId);
    await apiClient.post(`/apps/mira/api/admin/config/ai?${params.toString()}`, config);
  },

  getLimitsConfig: async (tenantId?: string): Promise<LimitsSettings> => {
    const params = new URLSearchParams();
    if (tenantId) params.append('tenantId', tenantId);
    const response = await apiClient.get<LimitsSettings>(`/apps/mira/api/admin/config/limits?${params.toString()}`);
    return response.data;
  },

  saveLimitsConfig: async (config: LimitsSettings, tenantId?: string): Promise<void> => {
    const params = new URLSearchParams();
    if (tenantId) params.append('tenantId', tenantId);
    await apiClient.post(`/apps/mira/api/admin/config/limits?${params.toString()}`, config);
  }
};

export interface AiConfig {
  provider?: string;
  model?: string;
  temperature?: number;
  maxTokens?: number;
}

export interface LimitsSettings {
  rpm?: number;
  rph?: number;
  dailyQuota?: number;
}
