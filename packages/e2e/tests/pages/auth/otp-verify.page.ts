import { Page, Locator } from '@playwright/test';
import { BasePage } from '../base.page';

/**
 * OTP検証ページ（6桁コード入力）
 * URL: /auth/otp-verify
 */
export class OtpVerifyPage extends BasePage {
  // セレクタ
  readonly otpCodeInput: Locator;
  readonly verifyButton: Locator;
  readonly resendButton: Locator;
  readonly cancelButton: Locator;
  readonly emailDisplay: Locator;
  readonly errorMessage: Locator;
  readonly expirationInfo: Locator;

  constructor(page: Page) {
    super(page);
    
    // Locatorの初期化
    this.otpCodeInput = page.locator('#otpCode');
    this.verifyButton = page.locator('button[type="submit"]').filter({ hasText: '認証' });
    this.resendButton = page.locator('button[type="button"]').filter({ hasText: /認証コードを再送信|再送信まであと/ });
    this.cancelButton = page.locator('button[type="button"]').filter({ hasText: 'キャンセル' });
    this.emailDisplay = page.locator('strong');
    this.errorMessage = page.locator('.bg-red-50');
    this.expirationInfo = page.locator('text=/有効期限:/');
  }

  /**
   * OTP検証ページに遷移
   */
  async goto() {
    await this.navigateTo('/auth/otp-verify');
  }

  /**
   * OTPコードを入力して検証
   * @param otpCode - 6桁のOTPコード
   */
  async verifyOtp(otpCode: string) {
    await this.otpCodeInput.fill(otpCode);
    await this.verifyButton.click();
  }

  /**
   * OTPを再送信
   */
  async resendOtp() {
    await this.resendButton.click();
  }

  /**
   * キャンセルボタンをクリック
   */
  async cancel() {
    await this.cancelButton.click();
  }

  /**
   * 表示されているメールアドレスを取得
   */
  async getDisplayedEmail(): Promise<string> {
    return await this.emailDisplay.textContent() || '';
  }

  /**
   * エラーメッセージを取得
   */
  async getErrorMessage(): Promise<string> {
    return await this.errorMessage.textContent() || '';
  }

  /**
   * エラーメッセージが表示されているか確認
   */
  async hasError(): Promise<boolean> {
    return await this.errorMessage.isVisible();
  }

  /**
   * 再送信ボタンが有効か確認
   */
  async isResendButtonEnabled(): Promise<boolean> {
    return await this.resendButton.isEnabled();
  }

  /**
   * 検証ボタンが有効か確認
   */
  async isVerifyButtonEnabled(): Promise<boolean> {
    return await this.verifyButton.isEnabled();
  }
}
