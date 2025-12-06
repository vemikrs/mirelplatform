/**
 * OTP Authentication Hooks
 * TanStack Query を使用したOTP操作のカスタムフック
 */

import { useMutation } from '@tanstack/react-query';
import { requestOtp, verifyOtp, resendOtp } from '@/lib/api/otp';
import type {
  OtpRequestPayload,
} from '@/lib/api/otp.types';
import { getApiErrors } from '@/lib/api/client';

/**
 * OTPリクエストフック
 * 
 * @example
 * const { mutate: sendOtp, isPending } = useRequestOtp({
 *   onSuccess: (data) => console.log(data.data?.requestId),
 *   onError: (error) => console.error(error)
 * });
 * sendOtp({ email: 'user@example.com', purpose: 'LOGIN' });
 */
export function useRequestOtp(options?: {
  onSuccess?: (data: { requestId: string; expirationMinutes: number; resendCooldownSeconds?: number }) => void;
  onError?: (errors: string[]) => void;
}) {
  return useMutation({
    mutationFn: requestOtp,
    onSuccess: (response) => {
      if (response.data) {
        options?.onSuccess?.({
          requestId: response.data.requestId,
          expirationMinutes: response.data.expirationMinutes,
          resendCooldownSeconds: response.data.resendCooldownSeconds,
        });
      } else if (response.errors.length > 0) {
        options?.onError?.(response.errors);
      }
    },
    onError: (error) => {
      const errors = getApiErrors(error);
      options?.onError?.(errors);
    },
  });
}

/**
 * OTP検証フック
 * 
 * @example
 * const { mutate: verify, isPending } = useVerifyOtp({
 *   onSuccess: () => navigate('/dashboard'),
 *   onError: (errors) => toast.error(errors[0])
 * });
 * verify({ email: 'user@example.com', otpCode: '123456', purpose: 'LOGIN' });
 */
export function useVerifyOtp(options?: {
  onSuccess?: (data: any) => void;
  onError?: (errors: string[]) => void;
}) {
  return useMutation({
    mutationFn: verifyOtp,
    onSuccess: (response) => {
      // response is ApiResponse<boolean | AuthenticationResponse>
      // response.data is boolean | AuthenticationResponse
      
      if (response.data === true) {
        // Boolean success (e.g. password reset)
        options?.onSuccess?.(true);
      } else if (typeof response.data === 'object' && response.data !== null && 'tokens' in response.data) {
        // AuthenticationResponse success (e.g. login)
        options?.onSuccess?.(response.data);
      } else if (response.errors.length > 0) {
        options?.onError?.(response.errors);
      } else {
        // Fallback for unexpected success structure
        // If response.data is false, it means verification failed
        if (response.data === false) {
             options?.onError?.(['認証コードが正しくありません']);
        } else {
             // Should not happen if backend returns 200 OK with valid data
             console.warn('Unexpected OTP verification response:', response);
             options?.onError?.(['予期しないレスポンス形式です']);
        }
      }
    },
    onError: (error) => {
      const errors = getApiErrors(error);
      options?.onError?.(errors);
    },
  });
}

/**
 * OTP再送信フック
 * 
 * @example
 * const { mutate: resend, isPending } = useResendOtp({
 *   onSuccess: () => toast.success('認証コードを再送信しました'),
 *   onError: (errors) => toast.error(errors[0])
 * });
 * resend({ email: 'user@example.com', purpose: 'LOGIN' });
 */
export function useResendOtp(options?: {
  onSuccess?: (data: { expirationMinutes: number; resendCooldownSeconds?: number }) => void;
  onError?: (errors: string[]) => void;
}) {
  return useMutation({
    mutationFn: resendOtp,
    onSuccess: (response) => {
      if (response.data) {
        options?.onSuccess?.({
          expirationMinutes: response.data.expirationMinutes,
          resendCooldownSeconds: response.data.resendCooldownSeconds,
        });
      } else if (response.errors.length > 0) {
        options?.onError?.(response.errors);
      }
    },
    onError: (error) => {
      const errors = getApiErrors(error);
      options?.onError?.(errors);
    },
  });
}

/**
 * 複合OTPフック（リクエスト + 検証）
 * ログイン・パスワードリセット・メールアドレス検証の完全フロー用
 * 
 * @example
 * const otp = useOtpFlow('LOGIN');
 * 
 * // Step 1: OTPリクエスト
 * otp.request('user@example.com');
 * 
 * // Step 2: OTP検証
 * otp.verify('123456');
 */
export function useOtpFlow(
  purpose: OtpRequestPayload['purpose'],
  options?: {
    onRequestSuccess?: (expirationMinutes: number) => void;
    onVerifySuccess?: () => void;
    onError?: (errors: string[]) => void;
  }
) {
  const requestMutation = useRequestOtp({
    onSuccess: (data) => options?.onRequestSuccess?.(data.expirationMinutes),
    onError: options?.onError,
  });

  const verifyMutation = useVerifyOtp({
    onSuccess: options?.onVerifySuccess,
    onError: options?.onError,
  });

  const resendMutation = useResendOtp({
    onSuccess: (data) => options?.onRequestSuccess?.(data.expirationMinutes),
    onError: options?.onError,
  });

  const request = (email: string) => {
    requestMutation.mutate({ email, purpose });
  };

  const verify = (email: string, otpCode: string) => {
    verifyMutation.mutate({ email, otpCode, purpose });
  };

  const resend = (email: string) => {
    resendMutation.mutate({ email, purpose });
  };

  return {
    request,
    verify,
    resend,
    isRequesting: requestMutation.isPending,
    isVerifying: verifyMutation.isPending,
    isResending: resendMutation.isPending,
    requestData: requestMutation.data,
    verifyData: verifyMutation.data,
  };
}
