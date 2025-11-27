/**
 * OTPログインページ
 * パスワードレス認証のメールアドレス入力画面
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '@/lib/hooks/useTheme';
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Input } from '@mirel/ui';
import { useRequestOtp } from '@/lib/hooks/useOtp';
import { useAuthStore } from '@/stores/authStore';

export function OtpLoginPage() {
  // テーマを初期化
  useTheme();
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const setOtpState = useAuthStore((state) => state.setOtpState);

  const { mutate: requestOtp, isPending } = useRequestOtp({
    onSuccess: (data) => {
      // OTP状態を保存
      setOtpState(email, 'LOGIN', data.requestId, data.expirationMinutes);
      // 検証画面へ遷移
      navigate('/auth/otp-verify');
    },
    onError: (errors) => {
      setError(errors[0] || '認証コードの送信に失敗しました');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // バリデーション
    if (!email || !email.includes('@')) {
      setError('有効なメールアドレスを入力してください');
      return;
    }

    requestOtp({ email, purpose: 'LOGIN' });
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-center">ログイン</CardTitle>
          <CardDescription className="text-center">
            メールアドレスを入力してください。認証コードを送信します。
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <label htmlFor="email" className="block text-sm font-medium">
                メールアドレス
              </label>
              <Input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                disabled={isPending}
              />
            </div>

            {/* エラーメッセージ */}
            {error && (
              <div className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 p-4 rounded">
                {error}
              </div>
            )}

            {/* 送信ボタン */}
            <Button
              type="submit"
              className="w-full"
              disabled={isPending}
            >
              {isPending ? '送信中...' : '認証コードを送信'}
            </Button>

            {/* 既存ログインへのリンク */}
            <div className="text-center text-sm">
              <button
                type="button"
                onClick={() => navigate('/login')}
                className="text-primary hover:underline"
              >
                パスワードでログイン
              </button>
            </div>
          </form>

          {/* 新規登録リンク */}
          <div className="text-center mt-4">
            <p className="text-sm text-muted-foreground">
              アカウントをお持ちでないですか？{' '}
              <button
                type="button"
                onClick={() => navigate('/signup')}
                className="font-medium text-primary hover:underline"
              >
                新規登録
              </button>
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
