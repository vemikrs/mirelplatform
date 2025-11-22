import { Page, Locator } from '@playwright/test';
import { BasePage } from '../base.page';

/**
 * ログインページ（通常のユーザー名/パスワードログイン）
 * URL: /login
 */
export class LoginPage extends BasePage {
  // セレクタ
  readonly titleText: Locator;
  readonly usernameOrEmailInput: Locator;
  readonly passwordInput: Locator;
  readonly loginButton: Locator;
  readonly githubLoginButton: Locator;
  readonly otpLoginLink: Locator;
  readonly passwordResetLink: Locator;
  readonly signupLink: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    
    // Locatorの初期化
    this.titleText = page.locator('h1');
    this.usernameOrEmailInput = page.locator('#usernameOrEmail');
    this.passwordInput = page.locator('#password');
    this.loginButton = page.locator('button[type="submit"]').filter({ hasText: 'ログイン' });
    this.githubLoginButton = page.locator('button').filter({ hasText: 'GitHubでログイン' });
    this.otpLoginLink = page.locator('a[href="/auth/otp-login"]');
    this.passwordResetLink = page.locator('a[href="/password-reset"]');
    this.signupLink = page.locator('a[href="/signup"]');
    this.errorMessage = page.locator('.bg-red-50');
  }

  /**
   * ログインページに遷移
   */
  async goto() {
    await this.navigateTo('/login');
  }

  /**
   * ユーザー名/パスワードでログイン
   * @param usernameOrEmail - ユーザー名またはメールアドレス
   * @param password - パスワード
   */
  async login(usernameOrEmail: string, password: string) {
    await this.usernameOrEmailInput.fill(usernameOrEmail);
    await this.passwordInput.fill(password);
    await this.loginButton.click();
  }

  /**
   * GitHubログインボタンをクリック
   */
  async clickGithubLogin() {
    await this.githubLoginButton.click();
  }

  /**
   * OTPログインリンクをクリック
   */
  async clickOtpLogin() {
    await this.otpLoginLink.click();
  }

  /**
   * パスワードリセットリンクをクリック
   */
  async clickPasswordReset() {
    await this.passwordResetLink.click();
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
