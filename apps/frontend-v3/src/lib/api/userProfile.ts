/**
 * User Profile API
 */

import { apiClient, getApiErrors } from './client';
import type { ApiResponse } from './types';

export interface UserProfile {
  userId: string;
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
export async function getUserProfile(accessToken: string): Promise<UserProfile> {
  try {
    const response = await apiClient.get<ApiResponse<UserProfile>>('/users/me', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (response.data.errors && response.data.errors.length > 0) {
      throw new Error(response.data.errors.join(', '));
    }

    return response.data.data!;
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Update user profile
 */
export async function updateProfile(
  accessToken: string,
  request: UpdateProfileRequest
): Promise<UserProfile> {
  try {
    const response = await apiClient.put<ApiResponse<UserProfile>>(
      '/users/me',
      request,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );

    if (response.data.errors && response.data.errors.length > 0) {
      throw new Error(response.data.errors.join(', '));
    }

    return response.data.data!;
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Update user password
 */
export async function updatePassword(
  accessToken: string,
  request: UpdatePasswordRequest
): Promise<void> {
  try {
    const response = await apiClient.put<ApiResponse<void>>(
      '/users/me/password',
      request,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );

    if (response.data.errors && response.data.errors.length > 0) {
      throw new Error(response.data.errors.join(', '));
    }
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Get user tenants
 */
export async function getUserTenants(accessToken: string): Promise<TenantInfo[]> {
  try {
    const response = await apiClient.get<ApiResponse<TenantInfo[]>>('/users/me/tenants', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (response.data.errors && response.data.errors.length > 0) {
      throw new Error(response.data.errors.join(', '));
    }

    return response.data.data || [];
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}

/**
 * Get user licenses
 */
export async function getUserLicenses(accessToken: string): Promise<LicenseInfo[]> {
  try {
    const response = await apiClient.get<ApiResponse<LicenseInfo[]>>('/users/me/licenses', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (response.data.errors && response.data.errors.length > 0) {
      throw new Error(response.data.errors.join(', '));
    }

    return response.data.data || [];
  } catch (error) {
    const errors = getApiErrors(error);
    throw new Error(errors.join(', '));
  }
}
