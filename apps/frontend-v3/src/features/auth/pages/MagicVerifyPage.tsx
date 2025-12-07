/**
 * Magic Link Verification Page
 * Handles the magic link verification process
 */

import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { apiClient, createApiRequest } from '@/lib/api/client';
import { useAuthStore } from '@/stores/authStore';
import { Spinner } from '@mirel/ui';

export function MagicVerifyPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');
  const [error, setError] = useState<string | null>(null);
  
  const setAuth = useAuthStore((state) => state.setAuth);
  const setOtpState = useAuthStore((state) => state.setOtpState);

  const { mutate: verifyMagicLink } = useMutation({
    mutationFn: async (token: string) => {
      const response = await apiClient.post('/auth/otp/magic-verify', createApiRequest({ token }));
      return response.data.data;
    },
    onSuccess: (data: any) => {
      // data: { verified: true, purpose: string, email?: string, auth?: AuthResponse }
      
      const { purpose, email, auth } = data;

      if (purpose === 'LOGIN') {
        if (auth) {
          setAuth(auth.user, auth.currentTenant, auth.tokens);
          navigate('/');
        } else {
            // Should not happen for LOGIN based on backend logic
            navigate('/login');
        }
      } else if (purpose === 'EMAIL_VERIFICATION') {
         if (auth) {
             // If auth data is returned (auto-login), use it
             setAuth(auth.user, auth.currentTenant, auth.tokens);
         }
         // Redirect to success page or home
         // Ideally show a success toast or page
         // For now, redirect to home (if logged in) or login
         navigate('/', { replace: true });
         // Alert or toast could be added here
         alert('メールアドレスの検証が完了しました');
      } else if (purpose === 'PASSWORD_RESET') {
        // Update OTP State so the next page knows the context
        // We might not have requestId or expirationMinutes from backend response yet
        // but setOtpState requires them.
        // We can use dummy values for requestId and expiration since we are already verified.
        if (email) {
            setOtpState(
                email, 
                'PASSWORD_RESET', 
                'magic-link-req', // Dummy Request ID
                30, // Dummy Expiration
                0 // No cooldown needed
            );
        }
        
        // Navigate to Password Reset Page with verified state
        navigate('/auth/password-reset-verify', { state: { verified: true } });
      } else {
        // Unknown purpose
        navigate('/login');
      }
    },
    onError: (err: any) => {
      console.error('Magic Link Verification Failed', err);
      setError(err.response?.data?.errors?.[0] || '無効または期限切れのリンクです');
    }
  });

  useEffect(() => {
    if (token) {
      verifyMagicLink(token);
    } else {
      setError('トークンが見つかりません');
    }
  }, [token, verifyMagicLink]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="max-w-md w-full text-center">
            <h1 className="text-xl font-bold text-red-600 mb-4">検証エラー</h1>
            <p className="text-gray-700 mb-6">{error}</p>
            <button 
                onClick={() => navigate('/login')}
                className="text-blue-600 hover:text-blue-800 underline"
            >
                ログイン画面へ戻る
            </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <Spinner size="lg" className="mb-4" />
        <p className="text-gray-600">認証中...</p>
      </div>
    </div>
  );
}
