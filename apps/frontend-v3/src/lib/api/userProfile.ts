/**
 * User Profile API
 */

import { apiClient } from './client';

export interface UserProfile {
  userId: string;
  username: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
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
 * Get current user profile
 */
export async function getUserProfile(): Promise<UserProfile> {
  const response = await apiClient.get<UserProfile>('/users/me');
  return response.data;
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
  return response.data;
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
  return response.data;
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
  return response.data;
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
export async function getUserTenants(): Promise<TenantInfo[]> {
  const response = await apiClient.get<TenantInfo[]>('/users/me/tenants');
  return response.data || [];
}

/**
 * Get user licenses
 */
export async function getUserLicenses(): Promise<LicenseInfo[]> {
  const response = await apiClient.get<LicenseInfo[]>('/users/me/licenses');
  return response.data || [];
}
