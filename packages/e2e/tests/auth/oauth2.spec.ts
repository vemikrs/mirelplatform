import { test, expect } from '@playwright/test';

test.describe('OAuth2 Authentication Flow', () => {
  test.setTimeout(60000); // タイムアウトを60秒に延長

  const MOCK_TOKEN = 'mock-jwt-token';
  const MOCK_USER = {
    userId: 'oauth-user-id',
    username: 'oauth-user',
    email: 'oauth@example.com',
    displayName: 'OAuth User',
    isActive: true,
    emailVerified: true,
  };
  const MOCK_TENANT = {
    tenantId: 'default',
    tenantName: 'Default Workspace',
    displayName: 'Default Workspace',
  };


  test.beforeEach(async ({ page }) => {
    page.on('request', request => console.log('>>', request.method(), request.url()));
    page.on('response', response => console.log('<<', response.status(), response.url()));
    page.on('console', msg => console.log('BROWSER LOG:', msg.text()));

    // デフォルトのモック (200 OK)
    await page.route(/\/mapi\/users\/me$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_USER),
      });
    });

    await page.route(/\/mapi\/users\/me\/tenants$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([MOCK_TENANT]),
      });
    });

    await page.route(/\/mapi\/users\/me\/licenses$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.route(/\/mapi\/api\/v1\/menus\/tree$/, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });
  });

  test('Existing user login flow', async ({ page }) => {
    // 2. OAuth2コールバックページに遷移 (トークン付き)
    await page.goto(`/auth/oauth2/success?token=${MOCK_TOKEN}`);

    // デバッグ
    const url = page.url();
    console.log(`Current URL: ${url}`);
    // const content = await page.content();
    // console.log(`Page Content: ${content.substring(0, 500)}...`);

    // 3. ホーム画面への遷移を確認
    await page.waitForURL(/\/home/, { timeout: 15000 });
    await expect(page).toHaveURL(/\/home/);
  });

  test('New user signup flow', async ({ page }) => {
    // 1. ユーザー存在確認API (/mapi/users/me) が 404 Not Found を返すようにモック (上書き)
    await page.unroute(/\/mapi\/users\/me$/);
    await page.route(/\/mapi\/users\/me$/, async (route) => {
      console.log('Mocking /mapi/users/me -> 404 Not Found');
      await route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'User not found' }),
      });
    });

    // 2. OAuth2コールバックページに遷移 (トークン付き)
    await page.goto(`/auth/oauth2/success?token=${MOCK_TOKEN}`);

    // ページが表示されるのを待つ
    await expect(page.getByTestId('oauth-callback-page')).toBeVisible();

    // /mapi/users/me へのリクエストを待つ
    // const response = await page.waitForResponse(response => response.url().includes('/mapi/users/me'));
    // console.log(`[Test] /mapi/users/me status: ${response.status()}`);

    // 3. サインアップ画面への遷移を確認
    await expect(page).toHaveURL(/\/signup/, { timeout: 15000 });
    
    // 4. "GitHub認証が完了しました" の表示を確認 (部分一致)
    await expect(page.getByText(/GitHub認証が完了しました/)).toBeVisible();

    // メールアドレス入力欄が表示されていないことを確認 (OAuth2モード)
    await expect(page.getByLabel('メールアドレス', { exact: true })).not.toBeVisible();

    // 5. フォーム入力
    await page.getByLabel('ユーザー名').fill('new-oauth-user');
    await page.getByLabel('表示名').fill('New OAuth User');
    await page.getByLabel('名', { exact: true }).fill('Taro');
    await page.getByLabel('姓', { exact: true }).fill('Yamada');

    // 6. OAuth2サインアップAPI (/mapi/auth/signup/oauth2) のモック
    await page.route(/\/mapi\/auth\/signup\/oauth2$/, async (route) => {
      console.log('Mocking /mapi/auth/signup/oauth2 -> 200 OK');
      // リクエスト内容の検証
      const request = route.request();
      const postData = request.postDataJSON();
      console.log('Signup Request Body:', postData);
      expect(postData.username).toBe('new-oauth-user');

      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          user: { ...MOCK_USER, username: 'new-oauth-user' },
          currentTenant: MOCK_TENANT,
          tokens: {
            accessToken: MOCK_TOKEN,
            refreshToken: 'mock-refresh-token',
            expiresIn: 3600,
          },
        }),
      });
    });

    // 7. 登録ボタン押下
    await page.getByRole('button', { name: '登録して開始' }).click();

    // 8. ホーム画面への遷移を確認
    await expect(page).toHaveURL(/\/home/);
  });
});
