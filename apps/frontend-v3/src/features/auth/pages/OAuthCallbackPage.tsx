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

    console.log(`[OAuthCallbackPage] Mounted. token=${token ? 'YES' : 'NO'}, error=${error}`);

    if (error) {
      console.error('[OAuthCallbackPage] OAuth2 error:', error);
      navigate('/login?error=' + error);
      return;
    }

    if (token) {
      console.log('[OAuthCallbackPage] Fetching user profile...');
      // ユーザー存在確認 (インターセプターを回避するために直接fetch/axiosを使用)
      // 401の場合は未登録とみなしてサインアップ画面へ
      fetch('/mapi/users/me', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      })
      .then(async (res) => {
        console.log(`[OAuthCallbackPage] Response status: ${res.status}`);
        if (res.ok) {
          const userProfile = await res.json();
          console.log('[OAuthCallbackPage] User found, logging in...');
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
          console.log('[OAuthCallbackPage] User not found (401/404), redirecting to signup...');
          // ユーザー未登録 -> サインアップ画面へ
          // トークンをstateで渡す
          navigate('/signup', { state: { oauth2: true, token } });
        } else {
          // その他のエラー
          console.error('[OAuthCallbackPage] Failed to fetch user profile:', res.status);
          navigate('/login?error=profile_fetch_failed');
        }
      })
      .catch((err) => {
        console.error('[OAuthCallbackPage] Error fetching user profile:', err);
        navigate('/login?error=network_error');
      });
    } else {
      console.warn('[OAuthCallbackPage] No token found');
      navigate('/login?error=no_token');
    }
  }, [searchParams, navigate, setAuth]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50" data-testid="oauth-callback-page">
      <div className="text-center">
        <h2 className="text-2xl font-semibold mb-4">Authenticating...</h2>
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
      </div>
    </div>
  );
}
