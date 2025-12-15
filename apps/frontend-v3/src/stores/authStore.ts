import { create } from 'zustand';
import type { OtpPurpose } from '@/lib/api/otp.types';
import { authApi, type LoginRequest, type SignupRequest, type TokenDto, type TenantContextDto } from '@/lib/api/auth';
import { getUserProfile, getUserTenants, getUserLicenses, type UserProfile, type TenantInfo, type LicenseInfo } from '@/lib/api/userProfile';

/**
 * OTP認証状態
 */
interface OtpState {
  email: string | null;
  purpose: OtpPurpose | null;
  requestId: string | null;
  expiresAt: Date | null;
  expirationMinutes: number | null;
  resendCooldownSeconds: number | null;
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
  rehydrateFromServerSession: () => Promise<void>;
  
  setAuth: (user: UserProfile, tenant: TenantContextDto | null, tokens: TokenDto) => void;
  clearAuth: () => void;
  updateUser: (updatedUser: Partial<UserProfile>) => void;
  
  // OTP Actions
  setOtpState: (email: string, purpose: OtpPurpose, requestId: string, expirationMinutes: number, resendCooldownSeconds?: number) => void;
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
        // セキュリティのため、ログイン時に機密性の高いローカルストレージをクリア
        // (以前のユーザーのデータが残存するのを防ぐ)
        clearClientStorage();

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
        // セキュリティのため、サインアップ時にもローカルストレージをクリア
        clearClientStorage();

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
        console.log('[DEBUG logout] Step 1: Starting logout...');
        const { tokens } = get();
        
        console.log('[DEBUG logout] Step 2: Calling logout API (awaiting)...');
        // 1. バックエンドへログアウト通知(同期的に待つ)
        try {
          await authApi.logout(tokens?.refreshToken);
        } catch (error) {
          console.warn('Logout API call failed', error);
        }
        
        // 2. authLoaderのキャッシュをクリア (重要: Cookie削除後の再アクセス時に必ずサーバー検証を実行)
        console.log('[DEBUG logout] Step 2.5: Clearing authLoader cache...');
        try {
          const { clearAuthLoaderCache } = await import('@/app/router.config');
          clearAuthLoaderCache();
        } catch (error) {
          console.warn('Failed to clear authLoader cache', error);
        }

        // Miraの会話履歴をクリア（ローカルストレージ）
        console.log('[DEBUG logout] Step 2.6: Clearing Mira store persistence...');
        try {
          // useMiraStoreを動的インポート（循環参照回避のため）
          const { useMiraStore } = await import('@/stores/miraStore');
          useMiraStore.persist.clearStorage();
          
          // メモリストアもリセット（念のため）
          // useMiraStore.setState({ conversations: {}, activeConversationId: null });
        } catch (error) {
          console.warn('Failed to clear Mira store persistence', error);
        }
        
        console.log('[DEBUG logout] Step 3: Redirecting to /login (immediate)...');
        // 3. ログイン画面へリダイレクト(履歴を残さない)
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
        
        // テナント切り替え後は権限（ロール）が変わる可能性があるため、
        // ナビゲーションやアクセス権を確実に再評価するために画面をリロード
        // authLoaderのキャッシュをクリアして、次回読み込み時に最新状態を取得
        try {
          const { clearAuthLoaderCache } = await import('@/app/router.config');
          clearAuthLoaderCache();
        } catch (error) {
          console.warn('Failed to clear authLoader cache', error);
        }
        
        // 現在のページをリロードして権限を再評価
        window.location.reload();
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

      // HttpOnly Cookie ベースのセッションからストアを再構築
      // F5 や新規タブなどでメモリストアが空になった場合に使用
      rehydrateFromServerSession: async () => {
        try {
          // authLoader からの呼び出し時は、グローバルな 401 リダイレクトを抑制する
          // (authLoader 側で適切に returnUrl 付きでリダイレクトするため)
          const config = {
            headers: {
              'X-Mirel-Skip-Auth-Redirect': 'true'
            }
          };

          const [user, tenants, licenses] = await Promise.all([
            getUserProfile(config),
            getUserTenants(config),
            getUserLicenses(config),
          ]);

          set({
            user,
            currentTenant: user.currentTenant ?? null,
            tenants,
            licenses,
            // HttpOnly Cookie 経由で /users/me が成功している時点で認証済み
            isAuthenticated: true,
          });
        } catch (error) {
          console.error('Failed to rehydrate auth store from server session', error);
          // 401 などの場合は呼び出し側 (authLoader) でリダイレクト制御を行う
          throw error;
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
      
      setOtpState: (email: string, purpose: OtpPurpose, requestId: string, expirationMinutes: number, resendCooldownSeconds?: number) => {
        const expiresAt = new Date();
        expiresAt.setMinutes(expiresAt.getMinutes() + expirationMinutes);
        
        set({
          otpState: {
            email,
            purpose,
            requestId,
            expiresAt,
            expirationMinutes,
            resendCooldownSeconds: resendCooldownSeconds ?? null,
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

/**
 * 機密性の高いローカルストレージデータをクリアするヘルパー
 * 
 * ログアウト時だけでなく、ログイン/サインアップ時にも実行することで、
 * 共有端末などでのデータ残留リスク（Session Fixationの一種）を防ぐ。
 * 
 * 注意: MiraStoreは永続化されているが、サーバー同期されるためクリアしても復元可能。
 */
const clearClientStorage = () => {
  try {
    console.log('[Auth] Clearing sensitive client storage...');
    
    // 1. Mira(AIチャット)の永続化ストアをクリア
    // Zustand persist middleware のキー
    localStorage.removeItem('mira-store');
    
    // 2. Mira入力履歴をクリア
    localStorage.removeItem('mira-input-history');
    
    console.log('[Auth] Client storage cleared successfully');
  } catch (error) {
    console.warn('[Auth] Failed to clear client storage', error);
  }
};

