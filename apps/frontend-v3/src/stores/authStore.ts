import { create } from 'zustand';
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

export const useAuthStore = create<AuthState>()((set, get) => ({
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

      logout: () => {
        console.log('[DEBUG logout] Step 1: Starting logout...');
        const { tokens } = get();
        
        console.log('[DEBUG logout] Step 2: Calling logout API (async)...');
        // 1. バックエンドへログアウト通知(非同期・待たない)
        authApi.logout(tokens?.refreshToken).catch((error) => {
          console.warn('Logout API call failed', error);
        });
        
        console.log('[DEBUG logout] Step 3: Redirecting to /login (immediate)...');
        // 2. ログイン画面へリダイレクト（履歴を残さない）
        // 即座にリダイレクトすることで、React の再レンダリングを防止
        window.location.replace('/login');
        
        // この後の処理は実行されない（ページ遷移するため）
        // state clear は次回ページ読み込み時に自動的にクリアされる（メモリ内のみ）
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
    })
);

// Initialize API client token provider
setTokenProvider(() => useAuthStore.getState().tokens?.accessToken);

