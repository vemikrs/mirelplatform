/**
 * OTP Authentication API Types
 */

/**
 * OTP用途
 */
export type OtpPurpose = 'LOGIN' | 'PASSWORD_RESET' | 'EMAIL_VERIFICATION';

/**
 * OTPリクエストペイロード
 */
export interface OtpRequestPayload {
  email: string;
  purpose: OtpPurpose;
}

/**
 * OTP検証ペイロード
 */
export interface OtpVerifyPayload {
  email: string;
  otpCode: string;
  purpose: OtpPurpose;
}

/**
 * OTP再送信ペイロード
 */
export interface OtpResendPayload {
  email: string;
  purpose: OtpPurpose;
}

/**
 * OTPレスポンスデータ
 */
export interface OtpResponseData {
  requestId: string;
  message: string;
  expirationMinutes: number;
  resendCooldownSeconds?: number;
}

/**
 * OTP検証レスポンスデータ
 */
export interface OtpVerifyResponseData {
  verified: boolean;
  message?: string;
}
