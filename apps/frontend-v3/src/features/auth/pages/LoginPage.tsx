import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/lib/hooks/useTheme';
import { Button, Card, Input } from '@mirel/ui';

export function LoginPage() {
  // テーマを初期化（ログイン画面でもテーマが適用されるように）
  useTheme();
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const login = useAuthStore((state) => state.login);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError('ログインに失敗しました。メールアドレスとパスワードを確認してください。');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <Card className="w-full max-w-md p-8">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold mb-2 text-foreground">mirelplatform</h1>
          <p className="text-muted-foreground">SaaS Platform</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="email" className="block text-sm font-medium mb-2 text-foreground">
              メールアドレス
            </label>
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="user@example.com"
              className="w-full"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium mb-2 text-foreground">
              パスワード
            </label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              placeholder="••••••••"
              className="w-full"
            />
          </div>

          {error && (
            <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm">
              {error}
            </div>
          )}

          <Button
            type="submit"
            disabled={loading}
            className="w-full"
          >
            {loading ? 'ログイン中...' : 'ログイン'}
          </Button>

          <div className="mt-4 text-center text-sm">
            <a href="/password-reset" className="text-muted-foreground hover:text-primary hover:underline">
              パスワードをお忘れですか？
            </a>
          </div>
        </form>

        <div className="mt-6 text-center text-sm text-muted-foreground">
          <p>アカウントをお持ちでない方は、</p>
          <p className="mt-1">
            <a href="/signup" className="text-primary hover:underline">
              新規登録
            </a>
          </p>
        </div>
      </Card>
    </div>
  );
}
