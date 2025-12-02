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
  const setAuth = useAuthStore((state) => state.setAuth);

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
      // TODO: トークンをデコードしてユーザー情報を取得する
      // 現在は簡易的に認証状態のみを設定
      setAuth(
        {
          userId: 'oauth-user',
          username: 'oauth-user',
          email: 'oauth@example.com',
          displayName: 'OAuth User',
          isActive: true,
          emailVerified: true,
        },
        null, // tenant情報なし
        {
          accessToken: token,
          refreshToken: '',
          expiresIn: 3600,
        }
      );
      
      // ダッシュボードに遷移
      navigate('/home');
    } else {
      // トークンがない場合はログインページに戻る
      navigate('/login?error=no_token');
    }
  }, [searchParams, navigate, setAuth]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="text-center">
        <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
        <p className="mt-4 text-muted-foreground">ログイン処理中...</p>
      </div>
    </div>
  );
}
