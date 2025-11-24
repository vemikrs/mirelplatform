/**
 * OTPメールアドレス検証ページ
 * 新規登録後のメールアドレス検証フロー
 */

import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { useVerifyOtp, useResendOtp } from '@/lib/hooks/useOtp';
import { useAuthStore } from '@/stores/authStore';

export function OtpEmailVerificationPage() {
  const [otpCode, setOtpCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [canResend, setCanResend] = useState(false);
  const [countdown, setCountdown] = useState(60);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const otpState = useAuthStore((state) => state.otpState);
  const clearOtpState = useAuthStore((state) => state.clearOtpState);
  const isOtpExpired = useAuthStore((state) => state.isOtpExpired);
  const updateUser = useAuthStore((state) => state.updateUser);

  const emailFromUrl = searchParams.get('email');

  useEffect(() => {
    if (!otpState || otpState.purpose !== 'EMAIL_VERIFICATION') {
      if (!emailFromUrl) {
        navigate('/signup');
      }
    }
  }, [otpState, emailFromUrl, navigate]);

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
      // メールアドレス検証完了
      updateUser({ emailVerified: true });
      clearOtpState();
      alert('メールアドレスを検証しました');
      navigate('/promarker');
    },
    onError: (errors) => {
      setError(errors[0] || '検証に失敗しました');
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

    const email = otpState?.email || emailFromUrl;
    if (!email) {
      setError('メールアドレスが見つかりません');
      return;
    }

    if (otpState && isOtpExpired()) {
      setError('認証コードの有効期限が切れています');
      return;
    }

    verifyOtp({
      email,
      otpCode,
      purpose: 'EMAIL_VERIFICATION',
    });
  };

  const handleResend = () => {
    const email = otpState?.email || emailFromUrl;
    if (!email) return;
    resendOtp({ email, purpose: 'EMAIL_VERIFICATION' });
  };

  const email = otpState?.email || emailFromUrl;

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">メールアドレスを検証</h1>
          <p className="mt-2 text-sm text-gray-600">
            {email ? (
              <>
                <strong>{email}</strong> に送信された認証コードを入力してください。
              </>
            ) : (
              'メールアドレスが見つかりません'
            )}
          </p>
          {otpState?.expirationMinutes && (
            <p className="mt-1 text-xs text-gray-500">
              有効期限: {otpState.expirationMinutes}分
            </p>
          )}
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
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md text-center text-2xl tracking-widest font-mono focus:outline-none focus:ring-primary focus:border-primary"
              placeholder="000000"
              autoFocus
              disabled={isVerifying}
            />
          </div>

          {error && (
            <div className="rounded-md bg-red-50 border border-red-200 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <div className="space-y-3">
            <Button
              type="submit"
              className="w-full"
              disabled={isVerifying || otpCode.length !== 6}
            >
              {isVerifying ? '検証中...' : 'メールアドレスを検証'}
            </Button>

            <Button
              type="button"
              variant="outline"
              className="w-full"
              onClick={handleResend}
              disabled={!canResend || isResending}
            >
              {isResending
                ? '送信中...'
                : canResend
                ? '認証コードを再送信'
                : `再送信まであと ${countdown}秒`}
            </Button>

            <Button
              type="button"
              variant="ghost"
              className="w-full"
              onClick={() => navigate('/login')}
            >
              後で検証する
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
