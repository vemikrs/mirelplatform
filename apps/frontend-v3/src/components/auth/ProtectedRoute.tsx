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
export function ProtectedRoute({ children, redirectTo = '/login' }: ProtectedRouteProps) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={redirectTo} state={{ from: location, message: 'このページにアクセスするにはログインが必要です。' }} replace />;
  }

  // 権限チェック
  // TODO: UserProfileにrolesを追加するか、別の方法で権限チェックを行う
  /*
  if (requiredRole && user?.roles) {
    const hasRole = user.roles.includes(requiredRole);
    if (!hasRole) {
      return <Navigate to="/403" replace />;
    }
  }
  */

  return <>{children}</>;
}
