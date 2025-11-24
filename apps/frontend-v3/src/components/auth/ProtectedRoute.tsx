import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import type { ReactNode } from 'react';

interface ProtectedRouteProps {
  children: ReactNode;
  redirectTo?: string;
  requiredRole?: string;
}

/**
 * 認証必須ルートコンポーネント
 * 未認証の場合はログイン画面にリダイレクト
 */
export function ProtectedRoute({ children, redirectTo = '/login', requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={redirectTo} state={{ from: location, message: 'このページにアクセスするにはログインが必要です。' }} replace />;
  }

  // 権限チェック
  if (requiredRole && user?.roles) {
    const hasRole = user.roles.includes(requiredRole);
    if (!hasRole) {
      return <Navigate to="/403" replace />;
    }
  }

  return <>{children}</>;
}
