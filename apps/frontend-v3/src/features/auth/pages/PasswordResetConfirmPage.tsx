import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useTheme } from '@/lib/hooks/useTheme';
import { Button } from '@mirel/ui';
import { Input } from '@mirel/ui';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@mirel/ui';

export function PasswordResetConfirmPage() {
  // テーマを初期化
  useTheme();
  const [searchParams] = useSearchParams();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [tokenValid, setTokenValid] = useState<boolean | null>(null);
  const navigate = useNavigate();

  const token = searchParams.get('token');

  useEffect(() => {
    // Verify token on mount
    if (!token) {
      setError('無効なリセットリンクです');
      setTokenValid(false);
      return;
    }

    fetch(`/mapi/auth/password-reset/verify?token=${encodeURIComponent(token)}`)
      .then(res => res.json())
      .then(isValid => {
        setTokenValid(isValid);
        if (!isValid) {
          setError('このリセットリンクは無効または期限切れです');
        }
      })
      .catch(() => {
        setTokenValid(false);
        setError('リセットリンクの確認に失敗しました');
      });
  }, [token]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (newPassword !== confirmPassword) {
      setError('パスワードが一致しません');
      return;
    }

    if (newPassword.length < 8) {
      setError('パスワードは8文字以上である必要があります');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const response = await fetch('/mapi/auth/password-reset', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          token,
          newPassword,
        }),
      });

      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'パスワードのリセットに失敗しました');
      }

      setSuccess(true);
      
      // Redirect to login after 3 seconds
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setIsLoading(false);
    }
  };

  if (tokenValid === null) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6">
            <div className="text-center">リセットリンクを確認中...</div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (tokenValid === false) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>無効なリセットリンク</CardTitle>
            <CardDescription>
              このパスワードリセットリンクは無効または期限切れです。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              onClick={() => navigate('/password-reset')}
              className="w-full"
            >
              新しいリセットリンクをリクエスト
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (success) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>パスワードリセット完了</CardTitle>
            <CardDescription>
              パスワードが正常にリセットされました。ログイン画面にリダイレクトしています...
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              onClick={() => navigate('/login')}
              className="w-full"
            >
              ログイン画面へ
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>新しいパスワードを設定</CardTitle>
          <CardDescription>
            新しいパスワードを入力してください。
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="newPassword" className="block text-sm font-medium">新しいパスワード</label>
              <Input
                id="newPassword"
                type="password"
                placeholder="8文字以上"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                disabled={isLoading}
                minLength={8}
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="confirmPassword" className="block text-sm font-medium">パスワード確認</label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="パスワードを再入力"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                disabled={isLoading}
                minLength={8}
              />
            </div>

            {error && (
              <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950 p-3 rounded">
                {error}
              </div>
            )}

            <Button
              type="submit"
              className="w-full"
              disabled={isLoading}
            >
              {isLoading ? 'リセット中...' : 'パスワードをリセット'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
