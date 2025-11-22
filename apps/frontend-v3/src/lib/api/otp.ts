/**
 * OTP Authentication API
 * パスワードレス認証・パスワードリセット・メールアドレス検証
 */

import { apiClient } from './client';
import type { ApiResponse } from './types';
import type {
  OtpRequestPayload,
  OtpVerifyPayload,
  OtpResendPayload,
  OtpResponseData,
} from './otp.types';

/**
 * OTPリクエスト
 * 
 * @param payload - メールアドレスと用途
 * @returns OTPレスポンス（リクエストID、有効期限）
 */
export async function requestOtp(
  payload: OtpRequestPayload
): Promise<ApiResponse<OtpResponseData>> {
  const response = await apiClient.post<ApiResponse<OtpResponseData>>(
    '/auth/otp/request',
    { model: payload }
  );
  return response.data;
}

/**
 * OTP検証
 * 
 * @param payload - メールアドレス、OTPコード、用途
 * @returns 検証結果（true/false）
 */
export async function verifyOtp(
  payload: OtpVerifyPayload
): Promise<ApiResponse<boolean>> {
  const response = await apiClient.post<ApiResponse<boolean>>(
    '/auth/otp/verify',
    { model: payload }
  );
  return response.data;
}

/**
 * OTP再送信
 * 
 * @param payload - メールアドレスと用途
 * @returns OTPレスポンス（リクエストID、有効期限）
 */
export async function resendOtp(
  payload: OtpResendPayload
): Promise<ApiResponse<OtpResponseData>> {
  const response = await apiClient.post<ApiResponse<OtpResponseData>>(
    '/auth/otp/resend',
    { model: payload }
  );
  return response.data;
}
