import { useAuthStore } from '@/stores/authStore';

/**
 * 認証状態管理フック
 */
export function useAuth() {
  const user = useAuthStore((state) => state.user);
  const currentTenant = useAuthStore((state) => state.currentTenant);
  const tokens = useAuthStore((state) => state.tokens);
  const tenants = useAuthStore((state) => state.tenants);
  const licenses = useAuthStore((state) => state.licenses);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const login = useAuthStore((state) => state.login);
  const signup = useAuthStore((state) => state.signup);
  const logout = useAuthStore((state) => state.logout);
  const switchTenant = useAuthStore((state) => state.switchTenant);
  const updateUser = useAuthStore((state) => state.updateUser);

  return {
    user,
    currentTenant,
    tokens,
    tenants,
    licenses,
    isAuthenticated,
    login,
    signup,
    logout,
    switchTenant,
    updateUser,
  };
}

/**
 * ライセンスチェックフック
 */
export function useLicense(_applicationId: string, requiredTier?: 'FREE' | 'PRO' | 'MAX') {
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
