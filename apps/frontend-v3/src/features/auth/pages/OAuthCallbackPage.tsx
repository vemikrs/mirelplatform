import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

/**
 * OAuth2認証成功後のコールバックページ
 * 
 * バックエンドから /auth/oauth2/success?token={jwt} にリダイレクトされた後、
 * JWTトークンを受け取ってローカルストレージに保存し、ダッシュボードに遷移します。
 */
export function OAuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const setToken = useAuthStore((state) => state.setToken);
  const setAuthenticated = useAuthStore((state) => state.setAuthenticated);

  useEffect(() => {
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    if (error) {
      // エラー発生時はログインページに戻る
      console.error('OAuth2 authentication failed:', error);
      navigate('/login?error=' + error);
      return;
    }

    if (token) {
      // JWTトークンをストアに保存
      setToken(token);
      setAuthenticated(true);
      
      // ダッシュボードに遷移
      navigate('/');
    } else {
      // トークンがない場合はログインページに戻る
      navigate('/login?error=no_token');
    }
  }, [searchParams, navigate, setToken, setAuthenticated]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="text-center">
        <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
        <p className="mt-4 text-muted-foreground">ログイン処理中...</p>
      </div>
    </div>
  );
}
