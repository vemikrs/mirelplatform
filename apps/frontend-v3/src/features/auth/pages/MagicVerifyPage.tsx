/**
 * Magic Link Verification Page
 * Handles the magic link verification process
 */

import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
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

  const { data, isError, error: queryError } = useQuery({
    queryKey: ['magicVerify', token],
    queryFn: async () => {
      const response = await apiClient.post('/auth/otp/magic-verify', createApiRequest({ token }));
      return response.data.data;
    },
    enabled: !!token,
    retry: false,
    staleTime: Infinity,
    gcTime: 1000 * 60 * 60, // Keep in cache for 1 hour
  });

  useEffect(() => {
    if (data) {
      // data: { verified: true, purpose: string, email?: string, auth?: AuthResponse }
      const { purpose, email, auth } = data;

      if (purpose === 'LOGIN') {
        if (auth) {
          setAuth(auth.user, auth.currentTenant, auth.tokens);
          navigate('/');
        } else {
            navigate('/login');
        }
      } else if (purpose === 'EMAIL_VERIFICATION') {
         if (auth) {
             setAuth(auth.user, auth.currentTenant, auth.tokens);
         }
         navigate('/', { replace: true });
         alert('メールアドレスの検証が完了しました');
      } else if (purpose === 'PASSWORD_RESET') {
        if (email) {
            setOtpState(
                email, 
                'PASSWORD_RESET', 
                'magic-link-req',
                30,
                0
            );
        }
        navigate('/auth/password-reset-verify', { state: { verified: true } });
      } else {
        navigate('/login');
      }
    }
  }, [data, navigate, setAuth, setOtpState]);

  useEffect(() => {
    if (isError && queryError) {
      console.error('Magic Link Verification Failed', queryError);
      // @ts-ignore
      setError(queryError.response?.data?.errors?.[0] || '無効または期限切れのリンクです');
    }
  }, [isError, queryError]);

  useEffect(() => {
    if (!token) {
      setError('トークンが見つかりません');
    }
  }, [token]);

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
