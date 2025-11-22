import { useAuthStore } from '@/stores/authStore';

/**
 * 認証状態管理フック
 */
export function useAuth() {
  const user = useAuthStore((state) => state.user);
  const currentTenant = useAuthStore((state) => state.currentTenant);
  const tokens = useAuthStore((state) => state.tokens);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const login = useAuthStore((state) => state.login);
  const signup = useAuthStore((state) => state.signup);
  const logout = useAuthStore((state) => state.logout);
  const switchTenant = useAuthStore((state) => state.switchTenant);

  return {
    user,
    currentTenant,
    tokens,
    isAuthenticated,
    login,
    signup,
    logout,
    switchTenant,
  };
}

/**
 * ライセンスチェックフック
 */
export function useLicense(applicationId: string, requiredTier?: 'FREE' | 'PRO' | 'MAX') {
  const user = useAuthStore((state) => state.user);
  const currentTenant = useAuthStore((state) => state.currentTenant);
  
  // TODO: Implement license checking logic
  // For now, return placeholder values
  const hasLicense = true;
  const currentTier = 'FREE' as 'FREE' | 'PRO' | 'MAX';
  const isUpgradeRequired = requiredTier && currentTier !== requiredTier;

  return {
    hasLicense,
    currentTier,
    isUpgradeRequired,
  };
}
