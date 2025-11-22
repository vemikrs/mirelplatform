import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { OtpPurpose } from '@/lib/api/otp.types';

interface User {
  userId: string;
  username: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  isActive: boolean;
  emailVerified: boolean;
}

interface Tenant {
  tenantId: string;
  tenantName: string;
  displayName: string;
}

interface Tokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/**
 * OTP認証状態
 */
interface OtpState {
  email: string | null;
  purpose: OtpPurpose | null;
  requestId: string | null;
  expiresAt: Date | null;
  expirationMinutes: number | null;
}

interface AuthState {
  user: User | null;
  currentTenant: Tenant | null;
  tokens: Tokens | null;
  isAuthenticated: boolean;
  
  // OTP認証状態
  otpState: OtpState | null;

  // Actions
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  signup: (data: { username: string; email: string; password: string; displayName: string; firstName?: string; lastName?: string }) => Promise<void>;
  logout: () => Promise<void>;
  switchTenant: (tenantId: string) => Promise<void>;
  setAuth: (user: User, tenant: Tenant | null, tokens: Tokens) => void;
  clearAuth: () => void;
  updateUser: (updatedUser: Partial<User>) => void;
  
  // OTP Actions
  setOtpState: (email: string, purpose: OtpPurpose, requestId: string, expirationMinutes: number) => void;
  clearOtpState: () => void;
  isOtpExpired: () => boolean;
}
      otpState: null,

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      currentTenant: null,
      tokens: null,
      isAuthenticated: false,

      login: async (usernameOrEmail: string, password: string) => {
        const response = await fetch('/mapi/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ usernameOrEmail, password }),
        });

        if (!response.ok) {
          throw new Error('Login failed');
        }

        const data = await response.json();
        set({
          user: data.user,
          currentTenant: data.currentTenant,
          tokens: data.tokens,
          isAuthenticated: true,
        });
      },

      signup: async (signupData) => {
        const response = await fetch('/mapi/auth/signup', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(signupData),
        });

        if (!response.ok) {
          throw new Error('Signup failed');
        }

        const data = await response.json();
        set({
          user: data.user,
          currentTenant: data.currentTenant,
          tokens: data.tokens,
          isAuthenticated: true,
        });
      },

      logout: async () => {
        const { tokens } = get();
        if (tokens?.refreshToken) {
          try {
            await fetch('/mapi/auth/logout', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({ refreshToken: tokens.refreshToken }),
            });
          } catch (error) {
            console.error('Logout API call failed:', error);
          }
        }
        set({ user: null, currentTenant: null, tokens: null, isAuthenticated: false });
      },

      switchTenant: async (tenantId: string) => {
        const { tokens } = get();
        if (!tokens?.accessToken) {
          throw new Error('Not authenticated');
        }

        const response = await fetch('/mapi/auth/switch-tenant', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${tokens.accessToken}`,
            'X-Tenant-ID': tenantId,
          },
          body: JSON.stringify({ tenantId }),
        });

        if (!response.ok) {
          throw new Error('Tenant switch failed');
        }

        const data = await response.json();
        set({
          user: data.user,
          currentTenant: data.currentTenant,
        });
      },

      setAuth: (user, tenant, tokens) => {
        set({ user, currentTenant: tenant, tokens, isAuthenticated: true });
      },

      clearAuth: () => {
        set({ user: null, currentTenant: null, tokens: null, isAuthenticated: false });
      },

      updateUser: (updatedUser: Partial<User>) => {
        const { user } = get();
        if (!user) return;
        set({ user: { ...user, ...updatedUser } });
      
      setOtpState: (email: string, purpose: OtpPurpose, requestId: string, expirationMinutes: number) => {
        const expiresAt = new Date();
        expiresAt.setMinutes(expiresAt.getMinutes() + expirationMinutes);
        
        set({
          otpState: {
            email,
            purpose,
            requestId,
            expiresAt,
            expirationMinutes,
          },
        });
      },
      
      clearOtpState: () => {
        set({ otpState: null });
      },
      
      isOtpExpired: () => {
        const { otpState } = get();
        if (!otpState || !otpState.expiresAt) return true;
        return new Date() > new Date(otpState.expiresAt);
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        currentTenant: state.currentTenant,
        tokens: state.tokens,
        isAuthenticated: state.isAuthenticated,
        otpState: state.otpState, // OTP状態も永続化
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
