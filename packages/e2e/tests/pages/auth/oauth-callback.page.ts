import { Page, Locator } from '@playwright/test';
import { BasePage } from '../base.page';

/**
 * OAuth2コールバックページ
 * URL: /auth/oauth2/success
 */
export class OAuthCallbackPage extends BasePage {
  // セレクタ
  readonly loadingIndicator: Locator;
  readonly loadingText: Locator;

  constructor(page: Page) {
    super(page);
    
    // Locatorの初期化
    this.loadingIndicator = page.locator('.animate-spin');
    this.loadingText = page.locator('text=ログイン処理中...');
  }

  /**
   * OAuth2コールバックページに遷移（通常は自動リダイレクト）
   * @param token - JWTトークン
   */
  async gotoWithToken(token: string) {
    await this.navigateTo(`/auth/oauth2/success?token=${token}`);
  }

  /**
   * エラー付きでコールバックページに遷移
   * @param error - エラーコード
   */
  async gotoWithError(error: string) {
    await this.navigateTo(`/auth/oauth2/success?error=${error}`);
  }

  /**
   * ローディング表示が見えるか確認
   */
  async isLoadingVisible(): Promise<boolean> {
    return await this.loadingIndicator.isVisible();
  }

  /**
   * ダッシュボードへのリダイレクトを待機
   */
  async waitForDashboardRedirect() {
    await this.page.waitForURL('/', { timeout: 10000 });
  }

  /**
   * ログインページへのリダイレクトを待機
   */
  async waitForLoginRedirect() {
    await this.page.waitForURL(/\/login/, { timeout: 10000 });
  }
}
