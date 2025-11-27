/**
 * OTP検証ページ
 * 6桁の認証コード入力画面
 */

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Input } from '@mirel/ui';
import { useVerifyOtp, useResendOtp } from '@/lib/hooks/useOtp';
import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/lib/hooks/useTheme';
import { getUserTenants, getUserLicenses } from '@/lib/api/userProfile';

export function OtpVerifyPage() {
  // テーマを初期化
  useTheme();
  const [otpCode, setOtpCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [hasCheckedInitialOtpState, setHasCheckedInitialOtpState] = useState(false);
  const [canResend, setCanResend] = useState(false);
  const [countdown, setCountdown] = useState(60);
  const navigate = useNavigate();
  
  const otpState = useAuthStore((state) => state.otpState);
  const clearOtpState = useAuthStore((state) => state.clearOtpState);
  const isOtpExpired = useAuthStore((state) => state.isOtpExpired);
  const setAuth = useAuthStore((state) => state.setAuth);

  // 初回マウント時にのみ、OTP状態がない場合はログインページへ
  useEffect(() => {
    if (hasCheckedInitialOtpState) return;

    if (!otpState || !otpState.email) {
      navigate('/login');
    }

    setHasCheckedInitialOtpState(true);
  }, [otpState, navigate, hasCheckedInitialOtpState]);

  // 再送信カウントダウン
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
      // data is boolean | AuthenticationResponse (passed from useVerifyOtp onSuccess)
      
      if (typeof data === 'object' && data !== null && 'tokens' in data) {
        // It's AuthenticationResponse (Login success)
        try {
          // OTP検証成功時、tenants と licenses も取得してストアに保存
          // これにより /home への遷移時に authLoader が再度取得する必要がなくなる
          const [tenants, licenses] = await Promise.all([
            getUserTenants(),
            getUserLicenses(),
          ]);

          // @ts-expect-error - data is AuthenticationResponse
          setAuth(data.user, data.currentTenant, data.tokens);
          
          // tenants と licenses を手動で設定
          useAuthStore.setState({ tenants, licenses });
          
          clearOtpState();
          navigate('/home', { replace: true });
        } catch (err) {
          console.error('Failed to fetch tenants/licenses after OTP login', err);
          // エラーでもログインは成功しているので遷移
          // @ts-expect-error - data is AuthenticationResponse
          setAuth(data.user, data.currentTenant, data.tokens);
          clearOtpState();
          navigate('/home', { replace: true });
        }
      } else if (data === true) {
        // It's boolean true (e.g. password reset verified)
        if (otpState?.purpose === 'LOGIN') {
             // Should not happen with new backend, but handle gracefully
             console.warn('Expected AuthResponse for LOGIN purpose but got boolean');
             navigate('/login');
        } else {
             // For other purposes (PASSWORD_RESET), navigate to next step
             // TODO: Implement navigation for password reset
             clearOtpState();
             navigate('/home'); 
        }
      }
    },
    onError: (errors) => {
      setError(errors[0] || '認証に失敗しました');
      setOtpCode('');
    },
  });

  const { mutate: resendOtp, isPending: isResending } = useResendOtp({
    onSuccess: () => {
      setError(null);
      setCanResend(false);
      setCountdown(60);
      alert('認証コードを再送信しました');
    },
    onError: (errors) => {
      setError(errors[0] || '再送信に失敗しました');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // バリデーション
    if (!otpCode || otpCode.length !== 6 || !/^\d{6}$/.test(otpCode)) {
      setError('6桁の数字を入力してください');
      return;
    }

    if (!otpState?.email || !otpState?.purpose) {
      setError('セッションが無効です。最初からやり直してください。');
      return;
    }

    if (isOtpExpired()) {
      setError('認証コードの有効期限が切れています。再送信してください。');
      return;
    }

    verifyOtp({
      email: otpState.email,
      otpCode,
      purpose: otpState.purpose,
    });
  };

  const handleResend = () => {
    if (!otpState?.email || !otpState?.purpose) return;
    resendOtp({ email: otpState.email, purpose: otpState.purpose });
  };

  const handleCancel = () => {
    clearOtpState();
    navigate('/login');
  };

  if (!otpState) {
    return null;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <Card className="w-full max-w-md p-8">
        {/* ヘッダー */}
        <div className="mb-8 text-center">
          <h1 className="text-3xl font-bold mb-2 text-foreground">認証コードを入力</h1>
          <p className="text-sm text-muted-foreground">
            <strong className="text-foreground">{otpState.email}</strong> に送信された6桁の認証コードを入力してください。
          </p>
          {otpState.expirationMinutes && (
            <p className="mt-2 text-xs text-muted-foreground">
              有効期限: {otpState.expirationMinutes}分
            </p>
          )}
        </div>

        {/* フォーム */}
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="otpCode" className="block text-sm font-medium mb-2 text-foreground">
              認証コード（6桁）
            </label>
            <Input
              id="otpCode"
              name="otpCode"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={6}
              required
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
              className="w-full text-center text-2xl tracking-widest font-mono"
              placeholder="000000"
              disabled={isVerifying}
              autoFocus
            />
          </div>

          {/* エラーメッセージ */}
          {error && (
            <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm">
              {error}
            </div>
          )}

          {/* 送信ボタン */}
          <Button
            type="submit"
            className="w-full"
            disabled={isVerifying || otpCode.length !== 6}
          >
            {isVerifying ? '検証中...' : '認証'}
          </Button>

          {/* 再送信ボタン */}
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

          {/* キャンセルボタン */}
          <div className="text-center">
            <button
              type="button"
              onClick={handleCancel}
              className="text-sm text-muted-foreground hover:text-primary hover:underline"
            >
              キャンセル
            </button>
          </div>
        </form>
      </Card>
    </div>
  );
}
