import { apiClient } from '@/lib/api/client';
import type { Organization, OrganizationUnit, UserOrganization } from './types';

// ============================================================
// Organization API (Admin)
// ============================================================

export async function getOrganizations(): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>('/admin/organizations');
  return response.data;
}

export async function createOrganization(data: Partial<Organization>): Promise<Organization> {
  const response = await apiClient.post<Organization>('/admin/organizations', data);
  return response.data;
}

// ============================================================
// Organization Unit API (Admin)
// ============================================================

export async function getOrganizationTree(organizationId: string): Promise<OrganizationUnit[]> {
  const response = await apiClient.get<OrganizationUnit[]>(`/admin/organizations/${organizationId}/tree`);
  return response.data;
}

export async function createOrganizationUnit(
  organizationId: string, 
  data: Partial<OrganizationUnit>
): Promise<OrganizationUnit> {
  const response = await apiClient.post<OrganizationUnit>(`/admin/organizations/${organizationId}/units`, data);
  return response.data;
}

export async function getOrganizationUnitAncestors(
  organizationId: string,
  unitId: string
): Promise<OrganizationUnit[]> {
  const response = await apiClient.get<OrganizationUnit[]>(
    `/admin/organizations/${organizationId}/units/${unitId}/ancestors`
  );
  return response.data;
}

export async function getOrganizationUnitMembers(
  organizationId: string,
  unitId: string,
  includeSubUnits: boolean = false
): Promise<UserOrganization[]> {
  const response = await apiClient.get<UserOrganization[]>(
    `/admin/organizations/${organizationId}/units/${unitId}/members`,
    { params: { includeSubUnits } }
  );
  return response.data;
}

// ============================================================
// User Organization API
// ============================================================

export async function getUserOrganizations(userId: string): Promise<UserOrganization[]> {
  const response = await apiClient.get<UserOrganization[]>(`/users/${userId}/organizations`);
  return response.data;
}

export async function assignUserOrganization(
  userId: string,
  data: Partial<UserOrganization>
): Promise<UserOrganization> {
  const response = await apiClient.post<UserOrganization>(`/users/${userId}/organizations`, data);
  return response.data;
}
