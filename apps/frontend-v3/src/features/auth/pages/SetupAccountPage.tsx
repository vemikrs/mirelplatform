/**
 * アカウントセットアップページ
 * 管理者により作成されたユーザーが初回パスワードを設定
 */

import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, Card, Input } from '@mirel/ui';
import { apiClient } from '@/lib/api/client';
import { createApiRequest } from '@/lib/api/utils';
import { useTheme } from '@/lib/hooks/useTheme';
import { useAuthStore } from '@/stores/authStore';
import { getUserTenants, getUserLicenses } from '@/lib/api/userProfile';

interface VerifySetupTokenResponse {
  email: string;
  username: string;
}

interface SetupAccountRequest {
  token: string;
  newPassword: string;
}

export function SetupAccountPage() {
  useTheme();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [userInfo, setUserInfo] = useState<VerifySetupTokenResponse | null>(null);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isVerifying, setIsVerifying] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const setAuth = useAuthStore((state) => state.setAuth);

  // トークン検証
  useEffect(() => {
    if (!token) {
      setError('セットアップリンクが無効です');
      setIsVerifying(false);
      return;
    }

    const verifyToken = async () => {
      try {
        const response = await apiClient.get<VerifySetupTokenResponse>(
          `/auth/verify-setup-token?token=${encodeURIComponent(token)}`
        );
        setUserInfo(response.data);
        setError(null);
      } catch (err: any) {
        console.error('Token verification failed:', err);
        const errorMessage = 
          err.response?.data?.message || 
          'セットアップリンクが無効または期限切れです';
        setError(errorMessage);
      } finally {
        setIsVerifying(false);
      }
    };

    verifyToken();
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // バリデーション
    if (!password || password.length < 8) {
      setError('パスワードは8文字以上で入力してください');
      return;
    }

    if (password !== confirmPassword) {
      setError('パスワードが一致しません');
      return;
    }

    if (!token) {
      setError('セットアップトークンが見つかりません');
      return;
    }

    setIsSubmitting(true);

    try {
      // パスワード設定
      await apiClient.post<string>(
        '/auth/setup-account',
        createApiRequest<SetupAccountRequest>({
          token,
          newPassword: password,
        })
      );

      // パスワード設定成功後、自動ログイン
      const loginResponse = await apiClient.post(
        '/auth/login',
        createApiRequest({
          username: userInfo?.email || '',
          password,
        })
      );

      if (loginResponse.data?.model) {
        const authData = loginResponse.data.model;
        
        // トークンをストアにセット
        setAuth(authData.user, authData.currentTenant, authData.tokens);
        
        // tenants と licenses を取得
        const [tenants, licenses] = await Promise.all([
          getUserTenants(),
          getUserLicenses(),
        ]);
        
        useAuthStore.setState({ tenants, licenses });
        
        // ホームへ遷移
        navigate('/home', { replace: true });
      }
    } catch (err: any) {
      console.error('Setup account failed:', err);
      const errorMessage = 
        err.response?.data?.message || 
        'アカウントセットアップに失敗しました';
      setError(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isVerifying) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md p-8 text-center">
          <p className="text-muted-foreground">セットアップリンクを検証中...</p>
        </Card>
      </div>
    );
  }

  if (error && !userInfo) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md p-8">
          <div className="text-center mb-6">
            <h1 className="text-2xl font-bold text-foreground mb-2">エラー</h1>
            <p className="text-red-600 dark:text-red-400">{error}</p>
          </div>
          <Button
            onClick={() => navigate('/login')}
            variant="outline"
            className="w-full"
          >
            ログインページへ
          </Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md p-8">
        {/* ヘッダー */}
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold mb-2 text-foreground">
            アカウントセットアップ
          </h1>
          <p className="text-sm text-muted-foreground">
            パスワードを設定してアカウントのセットアップを完了してください
          </p>
        </div>

        {/* ユーザー情報 */}
        {userInfo && (
          <div className="mb-6 p-4 bg-muted rounded-md">
            <p className="text-sm text-muted-foreground mb-1">ユーザー名</p>
            <p className="font-medium text-foreground">{userInfo.username}</p>
            <p className="text-sm text-muted-foreground mt-3 mb-1">メールアドレス</p>
            <p className="font-medium text-foreground">{userInfo.email}</p>
          </div>
        )}

        {/* フォーム */}
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="password" className="block text-sm font-medium mb-2 text-foreground">
              パスワード
            </label>
            <Input
              id="password"
              name="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="8文字以上"
              disabled={isSubmitting}
              autoFocus
            />
          </div>

          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium mb-2 text-foreground">
              パスワード（確認）
            </label>
            <Input
              id="confirmPassword"
              name="confirmPassword"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="パスワードを再入力"
              disabled={isSubmitting}
            />
          </div>

          {/* エラーメッセージ */}
          {error && (
            <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm">
              {error}
            </div>
          )}

          {/* 送信ボタン */}
          <Button
            type="submit"
            className="w-full"
            disabled={isSubmitting || !password || !confirmPassword}
          >
            {isSubmitting ? 'セットアップ中...' : 'パスワードを設定してログイン'}
          </Button>

          {/* キャンセルボタン */}
          <div className="text-center">
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="text-sm text-muted-foreground hover:text-primary hover:underline"
              disabled={isSubmitting}
            >
              キャンセル
            </button>
          </div>
        </form>
      </Card>
    </div>
  );
}
