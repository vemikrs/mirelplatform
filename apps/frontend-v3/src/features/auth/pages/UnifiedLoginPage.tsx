/**
 * 統合ログインページ
 * OTP認証を優先表示し、GitHub OAuth2とパスワード認証を統合
 */

import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/lib/hooks/useTheme';
import { useRequestOtp } from '@/lib/hooks/useOtp';
import { Button, Card, Input, Toaster, useToast } from '@mirel/ui';

export function UnifiedLoginPage() {
  // テーマを初期化
  useTheme();

  // OTPログイン用の状態
  const [email, setEmail] = useState('');
  const [otpError, setOtpError] = useState<string | null>(null);

  // パスワードログイン用の状態
  const [showPasswordLogin, setShowPasswordLogin] = useState(false);
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);

  // Store & Hooks
  const setOtpState = useAuthStore((state) => state.setOtpState);
  const login = useAuthStore((state) => state.login);
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { toast } = useToast();
  const toastShownRef = React.useRef(false);

  // OTP送信処理
  const { mutate: requestOtp, isPending: isOtpPending } = useRequestOtp({
    onSuccess: (data) => {
      setOtpState(email, 'LOGIN', data.requestId, data.expirationMinutes, data.resendCooldownSeconds);
      navigate('/auth/otp-verify');
    },
    onError: (errors) => {
      setOtpError(errors[0] || '認証コードの送信に失敗しました');
    },
  });

  // GitHub OAuth未設定の判定（環境変数で制御する想定）
  const isGitHubOAuthEnabled = true; // TODO: 実際の環境変数から取得

  // returnUrlパラメータがある場合のメッセージ表示
  useEffect(() => {
    const returnUrl = searchParams.get('returnUrl');
    
    if (returnUrl && !toastShownRef.current) {
      toast({
        variant: "destructive",
        title: "認証が必要です",
        description: "このページにアクセスするにはログインが必要です。",
      });
      toastShownRef.current = true;
    }
  }, [searchParams, toast]);

  // OTPフォーム送信
  const handleOtpSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setOtpError(null);

    // バリデーション
    if (!email || !email.includes('@')) {
      setOtpError('有効なメールアドレスを入力してください');
      return;
    }

    requestOtp({ email, purpose: 'LOGIN' });
  };

  // パスワードログイン送信
  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError('');
    setPasswordLoading(true);

    try {
      await login({ usernameOrEmail, password });
      
      // returnUrlパラメータがあればそこへ、なければホームへ
      const returnUrl = searchParams.get('returnUrl');
      const from = returnUrl || location.state?.from?.pathname || '/home';
      navigate(from, { replace: true });
    } catch {
      setPasswordError('ログインに失敗しました。ユーザー名/メールアドレスとパスワードを確認してください。');
    } finally {
      setPasswordLoading(false);
    }
  };

  // GitHub OAuthリダイレクト
  const handleGitHubLogin = () => {
    window.location.href = 'http://localhost:3000/mipla2/oauth2/authorization/github';
  };

  return (
    <>
      <Toaster />
      <div className="min-h-screen flex items-center justify-center bg-background px-4">
        <Card className="w-full max-w-md p-8">
          <div className="mb-8 text-center">
            <h1 className="text-3xl font-bold mb-2 text-foreground">mirelplatform</h1>
            <p className="text-muted-foreground">SaaS Platform</p>
          </div>

          {/* OTPログインフォーム（メインUI） */}
          <form onSubmit={handleOtpSubmit} className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium mb-2 text-foreground">
                メールアドレスでログイン
              </label>
              <Input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                disabled={isOtpPending}
                required
                className="w-full"
              />
            </div>

            {otpError && (
              <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm">
                {otpError}
              </div>
            )}

            <Button
              type="submit"
              disabled={isOtpPending}
              className="w-full"
            >
              {isOtpPending ? '送信中...' : '認証コードを送信'}
            </Button>
          </form>

          {/* セパレータ */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center" aria-hidden="true">
              <div className="w-full border-t border-border"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="bg-background px-3 text-muted-foreground">または</span>
            </div>
          </div>

          {/* GitHub OAuthボタン */}
          {isGitHubOAuthEnabled && (
            <Button
              type="button"
              variant="outline"
              className="w-full flex items-center justify-center gap-2 mb-4"
              onClick={handleGitHubLogin}
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
              </svg>
              GitHubでログイン
            </Button>
          )}

          {/* パスワードログイン展開式 */}
          <div className="mt-4">
            <button
              id="password-login-toggle"
              type="button"
              onClick={() => setShowPasswordLogin(!showPasswordLogin)}
              className="w-full text-sm text-muted-foreground hover:text-primary hover:underline"
            >
              {showPasswordLogin ? '▼ パスワードログインを非表示' : '▶ パスワードでログイン'}
            </button>

            {showPasswordLogin && (
              <form onSubmit={handlePasswordSubmit} className="mt-4 space-y-4 pt-4 border-t border-border">
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

                {passwordError && (
                  <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm">
                    {passwordError}
                  </div>
                )}

                <Button
                  type="submit"
                  disabled={passwordLoading}
                  variant="secondary"
                  className="w-full"
                >
                  {passwordLoading ? 'ログイン中...' : 'ログイン'}
                </Button>

                <div className="text-center text-sm">
                  <a href="/password-reset" className="text-muted-foreground hover:text-primary hover:underline">
                    パスワードをお忘れですか？
                  </a>
                </div>
              </form>
            )}
          </div>

          {/* フッターリンク */}
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
