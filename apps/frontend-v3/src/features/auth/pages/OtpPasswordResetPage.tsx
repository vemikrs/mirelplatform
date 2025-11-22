/**
 * OTPパスワードリセット開始ページ
 * メールアドレスを入力してパスワードリセット用OTPを送信
 */

import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { useRequestOtp } from '@/lib/hooks/useOtp';
import { useAuthStore } from '@/stores/authStore';

export function OtpPasswordResetPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const setOtpState = useAuthStore((state) => state.setOtpState);

  const { mutate: requestOtp, isPending } = useRequestOtp({
    onSuccess: (data) => {
      setOtpState(email, 'PASSWORD_RESET', data.requestId, data.expirationMinutes);
      navigate('/auth/password-reset-verify');
    },
    onError: (errors) => {
      setError(errors[0] || 'パスワードリセットコードの送信に失敗しました');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email || !email.includes('@')) {
      setError('有効なメールアドレスを入力してください');
      return;
    }

    requestOtp({ email, purpose: 'PASSWORD_RESET' });
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">パスワードリセット</h1>
          <p className="mt-2 text-sm text-gray-600">
            登録済みのメールアドレスを入力してください。認証コードを送信します。
          </p>
        </div>

        <form onSubmit={handleSubmit} className="mt-8 space-y-6">
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
              className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-primary focus:border-primary sm:text-sm"
              placeholder="your@email.com"
              disabled={isPending}
            />
          </div>

          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <div className="space-y-3">
            <Button type="submit" className="w-full" disabled={isPending}>
              {isPending ? '送信中...' : '認証コードを送信'}
            </Button>

            <Button
              type="button"
              variant="ghost"
              className="w-full"
              onClick={() => navigate('/login')}
            >
              ログインに戻る
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
