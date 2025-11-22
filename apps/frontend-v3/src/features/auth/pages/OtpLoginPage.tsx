/**
 * OTPログインページ
 * パスワードレス認証のメールアドレス入力画面
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { useRequestOtp } from '@/lib/hooks/useOtp';
import { useAuthStore } from '@/stores/authStore';

export function OtpLoginPage() {
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
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full space-y-8">
        {/* ヘッダー */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">ログイン</h1>
          <p className="mt-2 text-sm text-gray-600">
            メールアドレスを入力してください。認証コードを送信します。
          </p>
        </div>

        {/* フォーム */}
        <form onSubmit={handleSubmit} className="mt-8 space-y-6">
          <div className="rounded-md shadow-sm space-y-4">
            {/* メールアドレス */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                メールアドレス
              </label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-primary focus:border-primary focus:z-10 sm:text-sm"
                placeholder="your@email.com"
                disabled={isPending}
              />
            </div>
          </div>

          {/* エラーメッセージ */}
          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          {/* 送信ボタン */}
          <div>
            <Button
              type="submit"
              className="w-full"
              disabled={isPending}
            >
              {isPending ? '送信中...' : '認証コードを送信'}
            </Button>
          </div>

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
        <div className="text-center">
          <p className="text-sm text-gray-600">
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
      </div>
    </div>
  );
}
