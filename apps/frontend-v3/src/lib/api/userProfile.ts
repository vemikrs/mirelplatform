/**
 * User Profile API
 */

import { apiClient, getApiErrors } from './client';

export interface UserProfile {
  userId: string;
  username: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  isActive: boolean;
  emailVerified: boolean;
  lastLoginAt?: string;
  currentTenant?: {
    tenantId: string;
    tenantName: string;
    displayName: string;
  };
}

export interface UpdateProfileRequest {
  displayName?: string;
  firstName?: string;
  lastName?: string;
}

export interface UpdatePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface TenantInfo {
  tenantId: string;
  tenantName: string;
  displayName: string;
  roleInTenant: string;
  isDefault: boolean;
}

export interface LicenseInfo {
  id: string;
  subjectType: 'USER' | 'TENANT';
  subjectId: string;
  applicationId: string;
  tier: 'FREE' | 'TRIAL' | 'BASIC' | 'PROFESSIONAL' | 'ENTERPRISE';
  grantedAt: string;
  expiresAt?: string;
}

/**
 * Get current user profile
 */
export async function getUserProfile(): Promise<UserProfile> {
  try {
    const response = await apiClient.get<UserProfile>('/users/me');

    return response.data;
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Update user profile
 */
export async function updateProfile(
  request: UpdateProfileRequest
): Promise<UserProfile> {
  try {
    const response = await apiClient.put<UserProfile>(
      '/users/me',
      request
    );

    return response.data;
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Update user password
 */
export async function updatePassword(
  request: UpdatePasswordRequest
): Promise<void> {
  try {
    await apiClient.put<void>(
      '/users/me/password',
      request
    );
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Get user tenants
 */
export async function getUserTenants(): Promise<TenantInfo[]> {
  try {
    const response = await apiClient.get<TenantInfo[]>('/users/me/tenants');

    return response.data || [];
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Get user licenses
 */
export async function getUserLicenses(): Promise<LicenseInfo[]> {
  try {
    const response = await apiClient.get<LicenseInfo[]>('/users/me/licenses');

    return response.data || [];
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}
