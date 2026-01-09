/**
 * Bootstrapページ
 * 初回起動時のシステム管理者作成
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Input } from '@mirel/ui';
import { apiClient } from '@/lib/api/client';
import { useTheme } from '@/lib/hooks/useTheme';

interface BootstrapStatusResponse {
  available: boolean;
  message: string;
}

interface CreateInitialAdminRequest {
  token: string;
  username: string;
  email: string;
  password: string;
  displayName?: string;
}

interface AdminUserDto {
  userId: string;
  username: string;
  email: string;
  displayName?: string;
}

/**
 * APIエラーレスポンスからエラーメッセージを抽出する共通関数
 */
function extractErrorMessage(err: any, defaultMessage: string): string {
  if (err.response?.data) {
    if (typeof err.response.data === 'string') {
      return err.response.data;
    }
    if (err.response.data.message) {
      return err.response.data.message;
    }
    if (err.response.data.errors && Array.isArray(err.response.data.errors)) {
      return err.response.data.errors[0] || defaultMessage;
    }
  }
  return defaultMessage;
}

export function BootstrapPage() {
  useTheme();
  const navigate = useNavigate();

  const [isLoading, setIsLoading] = useState(true);
  const [isAvailable, setIsAvailable] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string>('');
  
  const [token, setToken] = useState('');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isComplete, setIsComplete] = useState(false);

  // Bootstrapステータス確認
  useEffect(() => {
    const checkStatus = async () => {
      try {
        const response = await apiClient.get<BootstrapStatusResponse>('/api/bootstrap/status');
        setIsAvailable(response.data.available);
        setStatusMessage(response.data.message);
      } catch (err: any) {
        console.error('Bootstrap status check failed:', err);
        setStatusMessage('ステータス確認に失敗しました');
      } finally {
        setIsLoading(false);
      }
    };

    checkStatus();
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // バリデーション
    if (!token || token.length < 10) {
      setError('有効なトークンを入力してください');
      return;
    }

    if (!username || username.length < 3) {
      setError('ユーザー名は3文字以上で入力してください');
      return;
    }

    if (!email || !email.includes('@')) {
      setError('有効なメールアドレスを入力してください');
      return;
    }

    if (!password || password.length < 8) {
      setError('パスワードは8文字以上で入力してください');
      return;
    }

    if (password !== confirmPassword) {
      setError('パスワードが一致しません');
      return;
    }

    setIsSubmitting(true);

    try {
      const request: CreateInitialAdminRequest = {
        token,
        username,
        email,
        password,
        displayName: displayName || undefined,
      };

      await apiClient.post<AdminUserDto>('/api/bootstrap/admin', request);

      setIsComplete(true);
    } catch (err: any) {
      console.error('Bootstrap failed:', err);
      setError(extractErrorMessage(err, '初期管理者の作成に失敗しました'));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md p-8 text-center">
          <p className="text-muted-foreground">ステータスを確認中...</p>
        </Card>
      </div>
    );
  }

  if (!isAvailable) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md p-8">
          <div className="text-center mb-6">
            <h1 className="text-2xl font-bold text-foreground mb-2">セットアップ完了済み</h1>
            <p className="text-muted-foreground">{statusMessage}</p>
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

  if (isComplete) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md p-8">
          <div className="text-center mb-6">
            <div className="text-green-500 text-5xl mb-4">✓</div>
            <h1 className="text-2xl font-bold text-foreground mb-2">セットアップ完了</h1>
            <p className="text-muted-foreground">
              初期管理者アカウントが作成されました。
              <br />
              設定したパスワードでログインしてください。
            </p>
          </div>
          <Button
            onClick={() => navigate('/login')}
            className="w-full"
          >
            ログインページへ
          </Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4 py-8">
      <Card className="w-full max-w-md p-8">
        {/* ヘッダー */}
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold mb-2 text-foreground">
            初期セットアップ
          </h1>
          <p className="text-sm text-muted-foreground">
            MirelPlatformの初期管理者アカウントを作成します
          </p>
        </div>

        {/* 注意事項 */}
        <div className="mb-6 p-4 bg-amber-50 dark:bg-amber-950 border border-amber-200 dark:border-amber-800 rounded-md">
          <p className="text-sm text-amber-800 dark:text-amber-200">
            <strong>トークンの取得方法:</strong>
            <br />
            サーバーのストレージディレクトリ内にある
            <code className="text-xs bg-amber-100 dark:bg-amber-900 px-1 rounded">
              bootstrap/setup-token.txt
            </code>
            からトークンを取得してください。
          </p>
        </div>

        {/* フォーム */}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label htmlFor="token" className="block text-sm font-medium mb-2 text-foreground">
              セットアップトークン <span className="text-red-500">*</span>
            </label>
            <Input
              id="token"
              type="text"
              required
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="トークンをペースト"
              disabled={isSubmitting}
              autoComplete="off"
            />
          </div>

          <hr className="border-border" />

          <div>
            <label htmlFor="username" className="block text-sm font-medium mb-2 text-foreground">
              ユーザー名 <span className="text-red-500">*</span>
            </label>
            <Input
              id="username"
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="admin"
              disabled={isSubmitting}
              autoComplete="username"
            />
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium mb-2 text-foreground">
              メールアドレス <span className="text-red-500">*</span>
            </label>
            <Input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@example.com"
              disabled={isSubmitting}
              autoComplete="email"
            />
          </div>

          <div>
            <label htmlFor="displayName" className="block text-sm font-medium mb-2 text-foreground">
              表示名
            </label>
            <Input
              id="displayName"
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="システム管理者"
              disabled={isSubmitting}
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium mb-2 text-foreground">
              パスワード <span className="text-red-500">*</span>
            </label>
            <Input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="8文字以上"
              disabled={isSubmitting}
              autoComplete="new-password"
            />
          </div>

          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium mb-2 text-foreground">
              パスワード（確認） <span className="text-red-500">*</span>
            </label>
            <Input
              id="confirmPassword"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="パスワードを再入力"
              disabled={isSubmitting}
              autoComplete="new-password"
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
            disabled={isSubmitting || !token || !username || !email || !password || !confirmPassword}
          >
            {isSubmitting ? 'セットアップ中...' : '初期管理者を作成'}
          </Button>
        </form>
      </Card>
    </div>
  );
}
