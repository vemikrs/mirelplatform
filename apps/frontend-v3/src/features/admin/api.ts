import { apiClient } from '../../lib/api/client';

// User Management Types
export interface AdminUser {
  userId: string;
  email: string;
  username: string;
  displayName: string;
  firstName: string;
  lastName: string;
  isActive: boolean;
  emailVerified: boolean;
  roles: string; // CSV
  avatarUrl?: string;
  lastLoginAt: string;
  createdAt: string;
  tenants: UserTenantInfo[];
}

export interface UserTenantInfo {
  tenantId: string;
  tenantName: string;
  roleInTenant: string;
  isDefault: boolean;
}

export interface UserListResponse {
  users: AdminUser[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  // 管理者作成ユーザーはセットアップリンク経由でパスワード設定するため不要
  displayName: string;
  firstName?: string;
  lastName?: string;
  roles?: string[]; // Backend expects List<String> which we fixed to join
  isActive?: boolean;
}

export interface UpdateUserRequest {
  displayName?: string;
  firstName?: string;
  lastName?: string;
  roles?: string; // CSV
  isActive?: boolean;
}

// Tenant Management Types
export type TenantPlan = 'FREE' | 'PRO' | 'MAX';
export type TenantStatus = 'ACTIVE' | 'SUSPENDED';

export interface Tenant {
  tenantId: string;
  tenantName: string;
  domain: string;
  plan: TenantPlan;
  status: TenantStatus;
  adminUser: string;
  userCount: number;
  createdAt: string;
}

export interface CreateTenantRequest {
  tenantName: string;
  domain: string;
  plan: TenantPlan;
  adminEmail?: string;
}

export interface UpdateTenantRequest {
  tenantName?: string;
  domain?: string;
  plan?: TenantPlan;
}

// License Management Types
export interface LicenseSummary {
  plan: string;
  status: string;
  expiryDate: string;
  maxUsers: number;
  currentUsers: number;
  maxStorage: number;
  currentStorage: number;
  licenseKey: string;
}

// System Settings Types (Key-Value)
export type SystemSettings = Record<string, string>;


// API Functions

// --- Users ---
export const getUsers = async (params?: { page?: number; size?: number; q?: string; role?: string; active?: boolean }) => {
  return apiClient.get<UserListResponse>('/admin/users', { params });
};

export const getUser = async (userId: string) => {
  return apiClient.get<AdminUser>(`/admin/users/${userId}`);
};

export const createUser = async (data: CreateUserRequest) => {
  return apiClient.post<AdminUser>('/admin/users', data);
};

export const updateUser = async (userId: string, data: UpdateUserRequest) => {
  return apiClient.put<AdminUser>(`/admin/users/${userId}`, data);
};

export const deleteUser = async (userId: string) => {
  return apiClient.delete(`/admin/users/${userId}`);
};

// --- Tenants ---
export const getTenants = async (q?: string) => {
  return apiClient.get<Tenant[]>('/api/admin/tenants', { params: { query: q } }); // Assuming search param is query based on backend service
};

export const createTenant = async (data: CreateTenantRequest) => {
  return apiClient.post('/api/admin/tenants', data);
};

export const updateTenant = async (tenantId: string, data: UpdateTenantRequest) => {
  return apiClient.put(`/api/admin/tenants/${tenantId}`, data);
};

export const updateTenantStatus = async (tenantId: string, status: TenantStatus) => {
  return apiClient.put(`/api/admin/tenants/${tenantId}/status`, { status });
};

// --- License ---
export const getLicenseInfo = async () => {
  return apiClient.get<LicenseSummary>('/api/admin/license');
};

export const updateLicenseKey = async (licenseKey: string) => {
  return apiClient.post('/api/admin/license/key', { licenseKey });
};

// --- System Settings ---
export const getSystemSettings = async () => {
  return apiClient.get<SystemSettings>('/api/admin/system-settings');
};

export const updateSystemSettings = async (settings: SystemSettings) => {
  return apiClient.post('/api/admin/system-settings', settings);
};
