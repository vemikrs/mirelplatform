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
      console.error('OAuth2 authentication failed:', error);
      navigate('/login?error=' + error);
      return;
    }

    if (token) {
      // ユーザー存在確認 (インターセプターを回避するために直接fetch/axiosを使用)
      // 401の場合は未登録とみなしてサインアップ画面へ
      fetch('/mapi/users/me', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })
      .then(async (res) => {
        if (res.ok) {
          const userProfile = await res.json();
          // ユーザーが存在するのでログイン完了
          setAuth(
            userProfile,
            null, // テナント情報は別途取得が必要だが、一旦nullで
            {
              accessToken: token,
              refreshToken: '', // リフレッシュトークンはCookieにあるはずだが、ここでもらえてない場合は空
              expiresIn: 3600,
            }
          );
          navigate('/home');
        } else if (res.status === 401 || res.status === 404) {
          // ユーザー未登録 -> サインアップ画面へ
          // トークンをstateで渡す
          navigate('/signup', { state: { oauth2: true, token } });
        } else {
          // その他のエラー
          console.error('Failed to fetch user profile:', res.status);
          navigate('/login?error=profile_fetch_failed');
        }
      })
      .catch((err) => {
        console.error('Error fetching user profile:', err);
        navigate('/login?error=network_error');
      });
    } else {
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
