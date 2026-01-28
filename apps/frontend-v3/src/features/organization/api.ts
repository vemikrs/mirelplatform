import { apiClient } from '@/lib/api/client';
import type { Organization, UserOrganization, CompanySettings, OrganizationSettings } from './types';

// ============================================================
// Organization API (Admin)
// ============================================================

export async function getOrganizations(): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>('/api/admin/organizations');
  return response.data;
}

export async function getOrganizationTree(tenantId: string): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>('/api/admin/organizations/tree', {
    params: { tenantId }
  });
  return response.data;
}

export async function createOrganization(data: Partial<Organization>): Promise<Organization> {
  const response = await apiClient.post<Organization>('/api/admin/organizations', data);
  return response.data;
}

export async function getOrganizationAncestors(id: string): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>(`/api/admin/organizations/${id}/ancestors`);
  return response.data;
}

export async function getOrganizationDescendants(id: string): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>(`/api/admin/organizations/${id}/descendants`);
  return response.data;
}

export async function getOrganizationMembers(
  id: string,
  includeSubOrgs: boolean = false
): Promise<UserOrganization[]> {
  const response = await apiClient.get<UserOrganization[]>(
    `/api/admin/organizations/${id}/members`,
    { params: { includeSubOrgs } }
  );
  return response.data;
}

// ============================================================
// Organization Settings API
// ============================================================

export async function getCompanySettings(organizationId: string): Promise<CompanySettings[]> {
  const response = await apiClient.get<CompanySettings[]>(
    `/api/admin/organizations/${organizationId}/settings/company`
  );
  return response.data;
}

export async function createCompanySettings(
  organizationId: string,
  data: Partial<CompanySettings>
): Promise<CompanySettings> {
  const response = await apiClient.post<CompanySettings>(
    `/api/admin/organizations/${organizationId}/settings/company`,
    data
  );
  return response.data;
}

export async function getOrganizationSettings(organizationId: string): Promise<OrganizationSettings[]> {
  const response = await apiClient.get<OrganizationSettings[]>(
    `/api/admin/organizations/${organizationId}/settings`
  );
  return response.data;
}

export async function updateOrganizationSettings(
  organizationId: string,
  settingsId: string,
  data: Partial<OrganizationSettings>
): Promise<OrganizationSettings> {
  const response = await apiClient.put<OrganizationSettings>(
    `/api/admin/organizations/${organizationId}/settings/${settingsId}`,
    data
  );
  return response.data;
}

// ============================================================
// User Organization API
// ============================================================

export async function getUserOrganizations(userId: string): Promise<UserOrganization[]> {
  const response = await apiClient.get<UserOrganization[]>(`/api/users/${userId}/organizations`);
  return response.data;
}

export async function assignUserOrganization(
  userId: string,
  data: Partial<UserOrganization>
): Promise<UserOrganization> {
  const response = await apiClient.post<UserOrganization>(`/api/users/${userId}/organizations`, data);
  return response.data;
}
