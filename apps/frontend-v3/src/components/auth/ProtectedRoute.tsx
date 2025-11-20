import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';

interface ProtectedRouteProps {
  redirectTo?: string;
  requiredRole?: string;
}

/**
 * 認証必須ルートコンポーネント
 * 未認証の場合はログイン画面にリダイレクト
 */
export function ProtectedRoute({ redirectTo = '/login', requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to={redirectTo} replace />;
  }

  // 権限チェック
  if (requiredRole && user?.roles) {
    const hasRole = user.roles.includes(requiredRole);
    if (!hasRole) {
      return <Navigate to="/403" replace />;
    }
  }

  return <Outlet />;
}
