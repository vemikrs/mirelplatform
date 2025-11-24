import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import type { ReactNode } from 'react';
import { useMemo } from 'react';

interface ProtectedRouteProps {
  children: ReactNode;
  redirectTo?: string;
  requiredRole?: string;
}

/**
 * JWT Payload structure
 * Based on backend JwtService.java
 */
interface JwtPayload {
  iss: string;       // Issuer ("self")
  iat: number;       // Issued at (Unix timestamp)
  exp: number;       // Expiration (Unix timestamp)
  sub: string;       // Subject (username)
  roles: string[];   // User roles
}

/**
 * Decode JWT token payload
 * @param token JWT token string
 * @returns Decoded payload or null if invalid
 */
function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    // JWT is "header.payload.signature"
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    
    // Base64URL decode (add padding if needed)
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Failed to decode JWT:', error);
    return null;
  }
}

/**
 * 認証必須ルートコンポーネント
 * 未認証の場合はログイン画面にリダイレクト
 * トークン期限切れもチェック
 */
export function ProtectedRoute({ children, redirectTo = '/login' }: ProtectedRouteProps) {
  const { isAuthenticated, tokens } = useAuth();
  const location = useLocation();

  // トークン期限切れチェック(JWTデコード)
  const isTokenValid = useMemo(() => {
    if (!tokens?.accessToken) return false;
    
    const payload = decodeJwtPayload(tokens.accessToken);
    if (!payload) return false;
    
    // exp は秒単位、Date.now() はミリ秒単位
    // 5秒のバッファを持たせてクロックスキュー対策
    const now = Date.now();
    const expiresAt = payload.exp * 1000;
    const buffer = 5000; // 5 seconds
    
    return expiresAt > (now + buffer);
  }, [tokens]);

  if (!isAuthenticated || !isTokenValid) {
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
