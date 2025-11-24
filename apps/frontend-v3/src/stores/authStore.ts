import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { OtpPurpose } from '@/lib/api/otp.types';
import { authApi, type LoginRequest, type SignupRequest, type TokenDto, type TenantContextDto } from '@/lib/api/auth';
import { getUserProfile, getUserTenants, getUserLicenses, type UserProfile, type TenantInfo, type LicenseInfo } from '@/lib/api/userProfile';
import { setTokenProvider } from '@/lib/api/client';

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
  user: UserProfile | null;
  currentTenant: TenantContextDto | null;
  tokens: TokenDto | null;
  tenants: TenantInfo[];
  licenses: LicenseInfo[];
  isAuthenticated: boolean;
  
  // OTP認証状態
  otpState: OtpState | null;

  // Actions
  login: (data: LoginRequest) => Promise<void>;
  signup: (data: SignupRequest) => Promise<void>;
  logout: () => Promise<void>;
  switchTenant: (tenantId: string) => Promise<void>;
  fetchProfile: () => Promise<void>;
  
  setAuth: (user: UserProfile, tenant: TenantContextDto | null, tokens: TokenDto) => void;
  clearAuth: () => void;
  updateUser: (updatedUser: Partial<UserProfile>) => void;
  
  // OTP Actions
  setOtpState: (email: string, purpose: OtpPurpose, requestId: string, expirationMinutes: number) => void;
  clearOtpState: () => void;
  isOtpExpired: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      currentTenant: null,
      tokens: null,
      tenants: [],
      licenses: [],
      isAuthenticated: false,
      otpState: null,

      login: async (request: LoginRequest) => {
        const response = await authApi.login(request);
        set({
          user: response.user,
          currentTenant: response.currentTenant,
          tokens: response.tokens,
          isAuthenticated: true,
        });
        // Login successful, fetch full profile data
        await get().fetchProfile();
      },

      signup: async (request: SignupRequest) => {
        const response = await authApi.register(request);
        set({
          user: response.user,
          currentTenant: response.currentTenant,
          tokens: response.tokens,
          isAuthenticated: true,
        });
        await get().fetchProfile();
      },

      logout: async () => {
        const { tokens } = get();
        await authApi.logout(tokens?.refreshToken);
        set({ 
          user: null, 
          currentTenant: null, 
          tokens: null, 
          tenants: [],
          licenses: [],
          isAuthenticated: false 
        });
      },

      switchTenant: async (tenantId: string) => {
        const response = await authApi.switchTenant(tenantId);
        set({
          user: response.user,
          currentTenant: response.currentTenant,
          tokens: response.tokens,
        });
        // Refresh licenses as they might be tenant-specific
        const licenses = await getUserLicenses();
        set({ licenses });
      },

      fetchProfile: async () => {
        try {
          const [user, tenants, licenses] = await Promise.all([
            getUserProfile(),
            getUserTenants(),
            getUserLicenses()
          ]);
          set({ user, tenants, licenses });
        } catch (error) {
          console.error('Failed to fetch profile data', error);
          // If profile fetch fails (e.g. 401), we might want to logout or handle it
        }
      },

      setAuth: (user, tenant, tokens) => {
        set({ user, currentTenant: tenant, tokens, isAuthenticated: true });
      },

      clearAuth: () => {
        set({ 
          user: null, 
          currentTenant: null, 
          tokens: null, 
          tenants: [],
          licenses: [],
          isAuthenticated: false 
        });
      },

      updateUser: (updatedUser: Partial<UserProfile>) => {
        const { user } = get();
        if (!user) return;
        set({ user: { ...user, ...updatedUser } });
      },
      
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
        otpState: state.otpState,
        // Don't persist tenants/licenses if they are dynamic, or do if we want offline support
        // For now, let's not persist them to ensure freshness on reload (fetchProfile will run)
      }),
    }
  )
);

// Initialize API client token provider
setTokenProvider(() => useAuthStore.getState().tokens?.accessToken);

