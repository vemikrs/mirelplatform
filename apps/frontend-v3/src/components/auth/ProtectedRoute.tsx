import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';

interface ProtectedRouteProps {
  children: ReactNode;
  redirectTo?: string;
  requiredRole?: string;
}

/**
 * 認証必須ルートコンポーネント
 * HttpOnly Cookie + authLoader 前提のシンプルなガード。
 *
 * - 実際の認証判定はルートの loader(authLoader) が /users/me の結果で行う
 * - ここでは、loader を素通りしてきたコンテンツをそのまま描画する
 * - 追加でクライアント側だけのロールチェックなどが必要になった場合に拡張する
 */
export function ProtectedRoute({ children, redirectTo = '/login' }: ProtectedRouteProps) {
  const location = useLocation();

  // ここまで到達している時点で、authLoader が成功している想定。
  // もし loader を経由していないパスで誤って使われた場合のみ、安全側にログインへ飛ばす。
  if (!location.key) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
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
