/**
 * User Profile API
 */

import { apiClient } from './client';
import type { AxiosRequestConfig } from 'axios';

export interface UserProfile {
  userId: string;
  username: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string; // URL format: /mipla2/api/files/avatars/... (static file proxy)
  bio?: string;
  phoneNumber?: string;
  preferredLanguage?: string;
  timezone?: string;
  isActive: boolean;
  emailVerified: boolean;
  lastLoginAt?: string;
  currentTenant?: {
    tenantId: string;
    tenantName: string;
    displayName: string;
  };
  roles: string[];
  oauth2Provider?: string;
  hasPassword?: boolean;
}

export interface UpdateProfileRequest {
  username?: string;
  displayName?: string;
  firstName?: string;
  lastName?: string;
  bio?: string;
  phoneNumber?: string;
  preferredLanguage?: string;
  timezone?: string;
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
 * Helper to transform avatar URL from backend context to frontend proxy
 */
function transformAvatarUrl(url?: string): string | undefined {
  if (!url) return undefined;
  // Avatar URLs use /mipla2 for static file access (direct passthrough proxy)
  // No transformation needed - backend returns /mipla2/api/files/avatars/...
  // which is proxied directly to backend without rewrite
  return url;
}

/**
 * Get current user profile
 */
export async function getUserProfile(config?: AxiosRequestConfig): Promise<UserProfile> {
  const response = await apiClient.get<UserProfile>('/users/me', config);
  const profile = response.data;
  if (profile.avatarUrl) {
    profile.avatarUrl = transformAvatarUrl(profile.avatarUrl);
  }
  return profile;
}

/**
 * Update user profile
 */
export async function updateProfile(
  request: UpdateProfileRequest
): Promise<UserProfile> {
  const response = await apiClient.put<UserProfile>(
    '/users/me',
    request
  );
  const profile = response.data;
  if (profile.avatarUrl) {
    profile.avatarUrl = transformAvatarUrl(profile.avatarUrl);
  }
  return profile;
}

/**
 * Update user password
 */
export async function updatePassword(
  request: UpdatePasswordRequest
): Promise<void> {
  await apiClient.put<void>(
    '/users/me/password',
    request
  );
}

/**
 * Update user email (requires OTP verification)
 */
export async function updateEmail(email: string): Promise<UserProfile> {
  const response = await apiClient.put<UserProfile>(
    '/users/me/email',
    { email }
  );
  const profile = response.data;
  if (profile.avatarUrl) {
    profile.avatarUrl = transformAvatarUrl(profile.avatarUrl);
  }
  return profile;
}

/**
 * Upload avatar image
 */
export async function uploadAvatar(file: File): Promise<{ avatarUrl: string }> {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await apiClient.post<{ avatarUrl: string }>(
    '/users/me/avatar',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  
  const result = response.data;
  if (result.avatarUrl) {
    result.avatarUrl = transformAvatarUrl(result.avatarUrl) || '';
  }
  return result;
}

/**
 * Delete avatar image
 */
export async function deleteAvatar(): Promise<void> {
  await apiClient.delete('/users/me/avatar');
}

/**
 * Unlink GitHub OAuth2
 */
export async function unlinkGitHub(): Promise<void> {
  await apiClient.delete('/users/me/oauth2/github');
}

/**
 * Enable passwordless login (OTP only)
 */
export async function enablePasswordless(): Promise<void> {
  await apiClient.post('/users/me/passwordless');
}

/**
 * Set password (for passwordless users)
 */
export async function setPassword(newPassword: string): Promise<void> {
  await apiClient.post('/users/me/password/set', { newPassword });
}

/**
 * Get user tenants
 */
export async function getUserTenants(config?: AxiosRequestConfig): Promise<TenantInfo[]> {
  const response = await apiClient.get<TenantInfo[]>('/users/me/tenants', config);
  return response.data || [];
}

/**
 * Get user licenses
 */
export async function getUserLicenses(config?: AxiosRequestConfig): Promise<LicenseInfo[]> {
  const response = await apiClient.get<LicenseInfo[]>('/users/me/licenses', config);
  return response.data || [];
}
