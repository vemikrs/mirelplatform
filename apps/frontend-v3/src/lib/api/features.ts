/**
 * Feature Flag API
 */

import { apiClient } from './client';

/**
 * Feature Status
 */
export type FeatureStatus = 'STABLE' | 'BETA' | 'ALPHA' | 'PLANNING' | 'DEPRECATED';

/**
 * License Tier
 */
export type LicenseTier = 'FREE' | 'TRIAL' | 'PRO' | 'MAX';

/**
 * License Resolve Strategy
 */
export type LicenseResolveStrategy = 
  | 'TENANT_PRIORITY' 
  | 'USER_PRIORITY' 
  | 'TENANT_ONLY' 
  | 'USER_ONLY' 
  | 'EITHER';

/**
 * Feature Flag DTO
 */
export interface FeatureFlag {
  id: string;
  featureKey: string;
  featureName: string;
  description?: string;
  applicationId: string;
  status: FeatureStatus;
  inDevelopment: boolean;
  requiredLicenseTier?: LicenseTier;
  enabledByDefault: boolean;
  enabledForUserIds?: string;
  enabledForTenantIds?: string;
  rolloutPercentage: number;
  licenseResolveStrategy?: LicenseResolveStrategy;
  metadata?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Feature Flag List Response
 */
export interface FeatureFlagListResponse {
  features: FeatureFlag[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/**
 * Feature Flag Filter Params
 */
export interface FeatureFlagFilterParams {
  page?: number;
  size?: number;
  applicationId?: string;
  status?: FeatureStatus;
  inDevelopment?: boolean;
  q?: string;
}

/**
 * Create Feature Flag Request
 */
export interface CreateFeatureFlagRequest {
  featureKey: string;
  featureName: string;
  description?: string;
  applicationId: string;
  status?: FeatureStatus;
  inDevelopment?: boolean;
  requiredLicenseTier?: LicenseTier;
  enabledByDefault?: boolean;
  enabledForUserIds?: string;
  enabledForTenantIds?: string;
  rolloutPercentage?: number;
  licenseResolveStrategy?: LicenseResolveStrategy;
  metadata?: string;
}

/**
 * Update Feature Flag Request
 */
export interface UpdateFeatureFlagRequest {
  featureName?: string;
  description?: string;
  applicationId?: string;
  status?: FeatureStatus;
  inDevelopment?: boolean;
  requiredLicenseTier?: LicenseTier;
  enabledByDefault?: boolean;
  enabledForUserIds?: string;
  enabledForTenantIds?: string;
  rolloutPercentage?: number;
  licenseResolveStrategy?: LicenseResolveStrategy;
  metadata?: string;
}

// ============================================================
// Admin API (requires ADMIN role)
// ============================================================

/**
 * Get feature flags list (admin)
 */
export async function getFeatures(
  params: FeatureFlagFilterParams = {}
): Promise<FeatureFlagListResponse> {
  const response = await apiClient.get<FeatureFlagListResponse>('/admin/features', {
    params,
  });
  return response.data;
}

/**
 * Get single feature flag by ID (admin)
 */
export async function getFeature(id: string): Promise<FeatureFlag> {
  const response = await apiClient.get<FeatureFlag>(`/admin/features/${id}`);
  return response.data;
}

/**
 * Check if feature key exists (admin)
 */
export async function checkFeatureKeyExists(
  featureKey: string
): Promise<boolean> {
  const response = await apiClient.get<{ exists: boolean }>(
    '/admin/features/check-key',
    { params: { featureKey } }
  );
  return response.data.exists;
}

/**
 * Create feature flag (admin)
 */
export async function createFeature(
  request: CreateFeatureFlagRequest
): Promise<FeatureFlag> {
  const response = await apiClient.post<FeatureFlag>('/admin/features', request);
  return response.data;
}

/**
 * Update feature flag (admin)
 */
export async function updateFeature(
  id: string,
  request: UpdateFeatureFlagRequest
): Promise<FeatureFlag> {
  const response = await apiClient.put<FeatureFlag>(
    `/admin/features/${id}`,
    request
  );
  return response.data;
}

/**
 * Delete feature flag (admin)
 */
export async function deleteFeature(id: string): Promise<void> {
  await apiClient.delete(`/admin/features/${id}`);
}

// ============================================================
// User API (authenticated users)
// ============================================================

/**
 * Get available features for current user
 */
export async function getAvailableFeatures(): Promise<FeatureFlag[]> {
  const response = await apiClient.get<FeatureFlag[]>('/features/available');
  return response.data || [];
}

/**
 * Get available features for specific application
 */
export async function getAvailableFeaturesForApplication(
  applicationId: string
): Promise<FeatureFlag[]> {
  const response = await apiClient.get<FeatureFlag[]>(
    `/features/available/${applicationId}`
  );
  return response.data || [];
}

/**
 * Get in-development features
 */
export async function getInDevelopmentFeatures(): Promise<FeatureFlag[]> {
  const response = await apiClient.get<FeatureFlag[]>('/features/in-development');
  return response.data || [];
}

// ============================================================
// Helper functions
// ============================================================

/**
 * Get status badge color for feature status
 */
export function getStatusBadgeColor(status: FeatureStatus): string {
  switch (status) {
    case 'STABLE':
      return 'green';
    case 'BETA':
      return 'yellow';
    case 'ALPHA':
      return 'orange';
    case 'PLANNING':
      return 'gray';
    case 'DEPRECATED':
      return 'red';
    default:
      return 'gray';
  }
}

/**
 * Get status label for feature status
 */
export function getStatusLabel(status: FeatureStatus): string {
  switch (status) {
    case 'STABLE':
      return '安定版';
    case 'BETA':
      return 'ベータ版';
    case 'ALPHA':
      return 'アルファ版';
    case 'PLANNING':
      return '計画中';
    case 'DEPRECATED':
      return '非推奨';
    default:
      return status;
  }
}

/**
 * Get license tier label
 */
export function getLicenseTierLabel(tier: LicenseTier | undefined): string {
  if (!tier) return 'なし';
  switch (tier) {
    case 'FREE':
      return 'FREE';
    case 'TRIAL':
      return 'TRIAL';
    case 'PRO':
      return 'PRO';
    case 'MAX':
      return 'MAX';
    default:
      return tier;
  }
}
