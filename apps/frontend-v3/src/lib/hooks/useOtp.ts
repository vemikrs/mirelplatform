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
  onSuccess?: (data: { requestId: string; expirationMinutes: number }) => void;
  onError?: (errors: string[]) => void;
}) {
  return useMutation({
    mutationFn: requestOtp,
    onSuccess: (response) => {
      if (response.data) {
        options?.onSuccess?.({
          requestId: response.data.requestId,
          expirationMinutes: response.data.expirationMinutes,
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
  onSuccess?: () => void;
  onError?: (errors: string[]) => void;
}) {
  return useMutation({
    mutationFn: verifyOtp,
    onSuccess: (response) => {
      if (response.data === true) {
        options?.onSuccess?.();
      } else if (response.errors.length > 0) {
        options?.onError?.(response.errors);
      } else {
        options?.onError?.(['認証コードが正しくありません']);
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
  onSuccess?: (expirationMinutes: number) => void;
  onError?: (errors: string[]) => void;
}) {
  return useMutation({
    mutationFn: resendOtp,
    onSuccess: (response) => {
      if (response.data) {
        options?.onSuccess?.(response.data.expirationMinutes);
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
    onSuccess: options?.onRequestSuccess,
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
