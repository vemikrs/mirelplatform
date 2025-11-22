import { Page, Locator } from '@playwright/test';
import { BasePage } from '../base.page';

/**
 * OTPログインページ（メールアドレス入力）
 * URL: /auth/otp-login
 */
export class OtpLoginPage extends BasePage {
  // セレクタ
  readonly emailInput: Locator;
  readonly sendCodeButton: Locator;
  readonly passwordLoginLink: Locator;
  readonly signupLink: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    
    // Locatorの初期化
    this.emailInput = page.locator('#email');
    this.sendCodeButton = page.locator('button[type="submit"]').filter({ hasText: '認証コードを送信' });
    this.passwordLoginLink = page.locator('button').filter({ hasText: 'パスワードでログイン' });
    this.signupLink = page.locator('button').filter({ hasText: '新規登録' });
    this.errorMessage = page.locator('.bg-red-50');
  }

  /**
   * OTPログインページに遷移
   */
  async goto() {
    await this.navigateTo('/auth/otp-login');
  }

  /**
   * メールアドレスを入力してOTPを送信
   * @param email - メールアドレス
   */
  async requestOtp(email: string) {
    await this.emailInput.fill(email);
    await this.sendCodeButton.click();
  }

  /**
   * パスワードログインリンクをクリック
   */
  async clickPasswordLogin() {
    await this.passwordLoginLink.click();
  }

  /**
   * 新規登録リンクをクリック
   */
  async clickSignup() {
    await this.signupLink.click();
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
}
