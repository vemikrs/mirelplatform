import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/lib/hooks/useTheme';
import { Button, Card, Input, Toaster, useToast } from '@mirel/ui';

export function LoginPage() {
  // テーマを初期化（ログイン画面でもテーマが適用されるように）
  useTheme();
  
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const login = useAuthStore((state) => state.login);
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { toast } = useToast();
  const toastShownRef = React.useRef(false);

  console.log('[DEBUG LoginPage] Render:', {
    returnUrl: searchParams.get('returnUrl'),
    hasReturnUrlParam: !!searchParams.get('returnUrl'),
    search: location.search,
    state: location.state
  });

  useEffect(() => {
    // returnUrlパラメータがある場合のみメッセージを表示
    // （ログアウト操作からの遷移ではメッセージを表示しない）
    const returnUrl = searchParams.get('returnUrl');
    
    console.log('[DEBUG LoginPage] useEffect:', {
      returnUrl,
      toastShown: toastShownRef.current,
      willShowToast: !!(returnUrl && !toastShownRef.current)
    });
    
    if (returnUrl && !toastShownRef.current) {
      console.log('[DEBUG LoginPage] Showing toast message');
      toast({
        variant: "destructive",
        title: "認証が必要です",
        description: "このページにアクセスするにはログインが必要です。",
      });
      toastShownRef.current = true;
    }
  }, [searchParams, toast]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ usernameOrEmail, password });
      
      // returnUrlパラメータがあればそこへ、なければlocation.stateから、それもなければホームへ
      const returnUrl = searchParams.get('returnUrl');
      const from = returnUrl || location.state?.from?.pathname || '/home';
      navigate(from, { replace: true });
    } catch (_error) {
      setError('ログインに失敗しました。ユーザー名/メールアドレスとパスワードを確認してください。');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Toaster />
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Card className="w-full max-w-md p-8">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold mb-2 text-foreground">mirelplatform</h1>
          <p className="text-muted-foreground">SaaS Platform</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="usernameOrEmail" className="block text-sm font-medium mb-2 text-foreground">
              ユーザー名 または メールアドレス
            </label>
            <Input
              id="usernameOrEmail"
              type="text"
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              required
              placeholder="username or user@example.com"
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

          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-muted"></div>
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-background px-2 text-muted-foreground">または</span>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            className="w-full flex items-center justify-center gap-2"
            onClick={() => window.location.href = 'http://localhost:3000/mipla2/oauth2/authorization/github'}
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
            </svg>
            GitHubでログイン
          </Button>

          <div className="mt-4 text-center space-y-2">
            <div className="text-sm">
              <a href="/password-reset" className="text-muted-foreground hover:text-primary hover:underline">
                パスワードをお忘れですか？
              </a>
            </div>
            <div className="text-sm">
              <a href="/auth/otp-login" className="text-primary hover:underline font-medium">
                パスワードレスログイン（OTP）
              </a>
            </div>
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
    </>
  );
}
