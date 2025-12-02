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
  // readonly otpLoginLink: Locator; // Removed
  readonly passwordResetLink: Locator;
  readonly signupLink: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    super(page);
    
    // Locatorの初期化
    this.titleText = page.locator('h1');
    // Use ID selectors as they are more reliable
    this.usernameOrEmailInput = page.locator('#usernameOrEmail');
    this.passwordInput = page.locator('#password');
    this.loginButton = page.locator('button[type="submit"]').filter({ hasText: 'ログイン' });
    this.githubLoginButton = page.locator('button').filter({ hasText: 'GitHubでログイン' });
    
    // OTP elements
    this.otpEmailInput = page.locator('#email');
    this.otpSubmitButton = page.locator('button[type="submit"]').filter({ hasText: '認証コードを送信' });
    this.passwordToggle = page.locator('#password-login-toggle');

    this.passwordResetLink = page.locator('a[href="/password-reset"]');
    this.signupLink = page.locator('a[href="/signup"]');
    this.errorMessage = page.locator('.bg-red-50');
  }

  readonly otpEmailInput: Locator;
  readonly otpSubmitButton: Locator;
  readonly passwordToggle: Locator;

  /**
   * ログインページに遷移
   */
  async goto() {
    await this.page.goto('/login', { waitUntil: 'networkidle' });
  }

  /**
   * ログインを実行
   * @param usernameOrEmail - ユーザー名またはメールアドレス
   * @param password - パスワード
   */
  async login(usernameOrEmail: string, password: string) {
    console.log('[LoginPage] Starting login flow...');
    
    // Wait for the page content to load
    await this.page.waitForLoadState('domcontentloaded');
    
    // Check if form is already visible
    if (!await this.usernameOrEmailInput.isVisible()) {
      console.log('[LoginPage] Password form not visible. Checking for toggle button...');
      // Try to find the toggle button to show it
      if (await this.passwordToggle.isVisible()) {
        console.log('[LoginPage] Toggle button found. Clicking...');
        await this.passwordToggle.click();
      } else {
        console.log('[LoginPage] Toggle button NOT found.');
        // Log all buttons to debug
        const buttons = await this.page.locator('button').allInnerTexts();
        console.log('[LoginPage] Available buttons:', buttons);
        
        // Dump HTML
        const content = await this.page.content();
        console.log('[LoginPage] Page Content (truncated):', content.substring(0, 2000));
        console.log('[LoginPage] Page Content (buttons area):', content.match(/<button.*?>.*?<\/button>/g));
      }
    } else {
      console.log('[LoginPage] Password form is already visible.');
    }

    // Wait for form to appear
    console.log('[LoginPage] Waiting for username input...');
    await this.usernameOrEmailInput.waitFor({ state: 'visible', timeout: 10000 });
    
    await this.usernameOrEmailInput.fill(usernameOrEmail);
    await this.passwordInput.fill(password);
    
    // Wait for login request to complete
    console.log('[LoginPage] Submitting login form...');
    const loginResponsePromise = this.page.waitForResponse(resp => resp.url().includes('/auth/login') && resp.status() === 200);
    await this.loginButton.click();
    await loginResponsePromise;
    console.log('[LoginPage] Login successful.');
  }

  /**
   * GitHubログインボタンをクリック
   */
  async clickGithubLogin() {
    await this.githubLoginButton.click();
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
