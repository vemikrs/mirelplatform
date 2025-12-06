import { apiClient as client } from '@/lib/api/client';
import type { AxiosResponse } from 'axios';

export type TenantPlan = 'ENTERPRISE' | 'PROFESSIONAL' | 'STARTER';
export type TenantStatus = 'ACTIVE' | 'SUSPENDED';

export type Tenant = {
  tenantId: string;
  tenantName: string;
  domain: string;
  plan: TenantPlan;
  status: TenantStatus;
  adminUser?: string;
  userCount?: number;
  createdAt: string;
};

export const getTenants = async (): Promise<Tenant[]> => {
  const response: AxiosResponse<Tenant[]> = await client.get('/api/admin/tenants');
  return response.data;
};

export const updateTenantStatus = async (tenantId: string, status: TenantStatus): Promise<void> => {
  await client.put(`/api/admin/tenants/${tenantId}/status`, { status });
};
