/**
 * OTPメールアドレス検証ページ
 * 新規登録後のメールアドレス検証フロー
 */

import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import { Button, Card, Input } from '@mirel/ui';
import { useVerifyOtp, useResendOtp } from '@/lib/hooks/useOtp';
import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/lib/hooks/useTheme';
import { apiClient } from '@/lib/api/client';
import { getUserTenants, getUserLicenses } from '@/lib/api/userProfile';

interface SignupData {
  username: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
}

export function OtpEmailVerificationPage() {
  useTheme();
  const [otpCode, setOtpCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [canResend, setCanResend] = useState(false);
  const [countdown, setCountdown] = useState(60);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const location = useLocation();

  const otpState = useAuthStore((state) => state.otpState);
  const clearOtpState = useAuthStore((state) => state.clearOtpState);
  const isOtpExpired = useAuthStore((state) => state.isOtpExpired);
  const setAuth = useAuthStore((state) => state.setAuth);

  const emailFromUrl = searchParams.get('email');
  const signupData = location.state?.signupData as SignupData | undefined;

  useEffect(() => {
    if (!otpState || otpState.purpose !== 'EMAIL_VERIFICATION') {
      if (!emailFromUrl && !signupData) {
        navigate('/signup');
      }
    } else {
      // OTP状態から再送信カウントダウンを設定
      setCountdown(otpState.resendCooldownSeconds ?? 60);
    }
  }, [otpState, emailFromUrl, signupData, navigate]);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else {
      setCanResend(true);
    }
  }, [countdown]);

  const { mutate: verifyOtp, isPending: isVerifying } = useVerifyOtp({
    onSuccess: async (data) => {
      // OTP検証成功後、サインアップデータがある場合はユーザー作成
      if (signupData) {
        try {
          // ユーザー作成APIを呼び出し
          const response = await apiClient.post<{
            user: any;
            tokens: any;
            currentTenant: any;
          }>('/auth/signup/otp', {
            ...signupData,
            emailVerified: true,
          });

          // ログイン処理
          if (response.data && response.data.user && response.data.tokens) {
            // トークンをストアにセット
            setAuth(response.data.user, response.data.currentTenant, response.data.tokens);

            // tenants と licenses も取得してストアに保存
            const [tenants, licenses] = await Promise.all([
              getUserTenants(),
              getUserLicenses(),
            ]);

            useAuthStore.setState({ tenants, licenses });

            clearOtpState();
            navigate('/home', { replace: true });
          } else {
            throw new Error('Invalid response from signup API');
          }
        } catch (err: any) {
          console.error('Signup after OTP verification failed:', err);
          setError(err.response?.data?.message || 'ユーザー作成に失敗しました');
        }
      } else {
        // メールアドレス検証のみの場合
        clearOtpState();
        alert('メールアドレスを検証しました');
        navigate('/login');
      }
    },
    onError: (errors) => {
      setError(errors[0] || '検証に失敗しました');
      setOtpCode('');
    },
  });

  const { mutate: resendOtp, isPending: isResending } = useResendOtp({
    onSuccess: () => {
      setCanResend(false);
      setCountdown(otpState?.resendCooldownSeconds ?? 60);
      alert('認証コードを再送信しました');
    },
    onError: (errors) => {
      setError(errors[0] || '再送信に失敗しました');
    },
  });

  const handleVerify = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!otpCode || otpCode.length !== 6 || !/^\d{6}$/.test(otpCode)) {
      setError('6桁の数字を入力してください');
      return;
    }

    const email = otpState?.email || emailFromUrl || signupData?.email;
    if (!email) {
      setError('メールアドレスが見つかりません');
      return;
    }

    if (otpState && isOtpExpired()) {
      setError('認証コードの有効期限が切れています。再送信してください。');
      return;
    }

    verifyOtp({
      email,
      otpCode,
      purpose: 'EMAIL_VERIFICATION',
    });
  };

  const handleResend = () => {
    const email = otpState?.email || emailFromUrl || signupData?.email;
    if (!email) return;
    resendOtp({ email, purpose: 'EMAIL_VERIFICATION' });
  };

  const email = otpState?.email || emailFromUrl || signupData?.email;

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md p-8">
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold mb-2 text-foreground">
            {signupData ? 'アカウント作成' : 'メールアドレスを検証'}
          </h1>
          <p className="text-sm text-muted-foreground">
            {email ? (
              <>
                <strong className="text-foreground">{email}</strong> に送信された6桁の認証コードを入力してください。
              </>
            ) : (
              'メールアドレスが見つかりません'
            )}
          </p>
          {otpState?.expirationMinutes && (
            <p className="mt-2 text-xs text-muted-foreground">
              有効期限: {otpState.expirationMinutes}分
            </p>
          )}
        </div>

        <form onSubmit={handleVerify} className="space-y-6">
          <div>
            <label htmlFor="otpCode" className="block text-sm font-medium mb-2 text-foreground">
              認証コード（6桁）
            </label>
            <Input
              id="otpCode"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={6}
              required
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
              className="w-full text-center text-2xl tracking-widest font-mono"
              placeholder="000000"
              autoFocus
              disabled={isVerifying}
            />
          </div>

          {error && (
            <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm">
              {error}
            </div>
          )}

          <Button
            type="submit"
            className="w-full"
            disabled={isVerifying || otpCode.length !== 6}
          >
            {isVerifying ? '検証中...' : (signupData ? 'アカウントを作成' : 'メールアドレスを検証')}
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

          <div className="text-center">
            <button
              type="button"
              onClick={() => navigate('/signup')}
              className="text-sm text-muted-foreground hover:text-primary hover:underline"
            >
              戻る
            </button>
          </div>
        </form>
      </Card>
    </div>
  );
}
