/**
 * OTPパスワードリセット検証ページ
 * 認証コードを入力して新しいパスワードを設定
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { useVerifyOtp, useResendOtp } from '@/lib/hooks/useOtp';
import { useAuthStore } from '@/stores/authStore';

export function OtpPasswordResetVerifyPage() {
  const [otpCode, setOtpCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [step, setStep] = useState<'verify' | 'password'>('verify');
  const [canResend, setCanResend] = useState(false);
  const [countdown, setCountdown] = useState(60);
  const navigate = useNavigate();

  const otpState = useAuthStore((state) => state.otpState);
  const clearOtpState = useAuthStore((state) => state.clearOtpState);
  const isOtpExpired = useAuthStore((state) => state.isOtpExpired);

  useEffect(() => {
    if (!otpState || otpState.purpose !== 'PASSWORD_RESET') {
      navigate('/auth/password-reset');
    }
  }, [otpState, navigate]);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else {
      setCanResend(true);
    }
  }, [countdown]);

  const { mutate: verifyOtp, isPending: isVerifying } = useVerifyOtp({
    onSuccess: () => {
      setStep('password');
      setError(null);
    },
    onError: (errors) => {
      setError(errors[0] || '認証に失敗しました');
      setOtpCode('');
    },
  });

  const { mutate: resendOtp, isPending: isResending } = useResendOtp({
    onSuccess: () => {
      setCanResend(false);
      setCountdown(60);
      alert('認証コードを再送信しました');
    },
    onError: (errors) => {
      setError(errors[0] || '再送信に失敗しました');
    },
  });

  const handleVerify = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!otpCode || otpCode.length !== 6) {
      setError('6桁の数字を入力してください');
      return;
    }

    if (!otpState?.email) return;

    if (isOtpExpired()) {
      setError('認証コードの有効期限が切れています');
      return;
    }

    verifyOtp({
      email: otpState.email,
      otpCode,
      purpose: 'PASSWORD_RESET',
    });
  };

  const handlePasswordReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (newPassword.length < 8) {
      setError('パスワードは8文字以上である必要があります');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('パスワードが一致しません');
      return;
    }

    // TODO: バックエンドAPIでパスワードリセット実行
    // 暫定: 成功として処理
    clearOtpState();
    alert('パスワードをリセットしました');
    navigate('/login');
  };

  const handleResend = () => {
    if (!otpState?.email) return;
    resendOtp({ email: otpState.email, purpose: 'PASSWORD_RESET' });
  };

  if (!otpState) return null;

  if (step === 'verify') {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="max-w-md w-full space-y-8">
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900">認証コードを入力</h1>
            <p className="mt-2 text-sm text-gray-600">
              <strong>{otpState.email}</strong> に送信された認証コードを入力してください。
            </p>
          </div>

          <form onSubmit={handleVerify} className="space-y-6">
            <div>
              <label htmlFor="otpCode" className="block text-sm font-medium text-gray-700">
                認証コード（6桁）
              </label>
              <input
                id="otpCode"
                type="text"
                inputMode="numeric"
                maxLength={6}
                required
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-center text-2xl tracking-widest font-mono"
                placeholder="000000"
                autoFocus
              />
            </div>

            {error && (
              <div className="rounded-md bg-red-50 border border-red-200 p-4">
                <p className="text-sm text-red-800">{error}</p>
              </div>
            )}

            <div className="space-y-3">
              <Button type="submit" className="w-full" disabled={isVerifying || otpCode.length !== 6}>
                {isVerifying ? '検証中...' : '次へ'}
              </Button>

              <Button
                type="button"
                variant="outline"
                className="w-full"
                onClick={handleResend}
                disabled={!canResend || isResending}
              >
                {isResending ? '送信中...' : canResend ? '再送信' : `再送信まであと ${countdown}秒`}
              </Button>

              <Button
                type="button"
                variant="ghost"
                className="w-full"
                onClick={() => navigate('/auth/password-reset')}
              >
                キャンセル
              </Button>
            </div>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">新しいパスワード</h1>
          <p className="mt-2 text-sm text-gray-600">
            新しいパスワードを設定してください
          </p>
        </div>

        <form onSubmit={handlePasswordReset} className="space-y-6">
          <div>
            <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700">
              新しいパスワード
            </label>
            <input
              id="newPassword"
              type="password"
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder="8文字以上"
            />
          </div>

          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
              パスワード確認
            </label>
            <input
              id="confirmPassword"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md"
              placeholder="もう一度入力"
            />
          </div>

          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <Button type="submit" className="w-full">
            パスワードをリセット
          </Button>
        </form>
      </div>
    </div>
  );
}
