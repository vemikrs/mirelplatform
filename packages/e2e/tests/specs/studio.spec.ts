import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/auth/login.page';

test.describe('Studio Application', () => {
  test.beforeEach(async ({ page }) => {
    // Mock Modeler API
    await page.route('**/api/studio/modeler/**', async route => {
      if (route.request().url().includes('listModels')) {
         await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: { models: [] } }) });
      } else if (route.request().url().includes('listModel')) {
         await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: { modelers: [] } }) });
      } else if (route.request().url().includes('list')) {
         await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: { records: [], total: 0 } }) });
      } else {
         await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: {} }) });
      }
    });

    // Perform login using LoginPage object
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin@example.com', 'password123');
    
    // Wait for navigation to home
    await page.waitForURL('**/home');
  });

  test('should redirect to entity list from studio root', async ({ page }) => {
    await page.goto('/apps/studio');
    await page.waitForURL('**/modeler/entities');
    await expect(page).toHaveTitle(/Modeler - Entity List/);
  });

  test('should navigate to entity list', async ({ page }) => {
    await page.goto('/apps/studio/modeler/entities');
    await expect(page).toHaveTitle(/Modeler - Entity List/);
    await expect(page.getByText('エンティティ一覧')).toBeVisible(); // Context bar title
    await expect(page.getByText('新規作成')).toBeVisible(); // Create button
  });

  test('should navigate to form list', async ({ page }) => {
    await page.goto('/apps/studio/forms');
    await expect(page).toHaveTitle(/Studio - Forms/);
    await expect(page.getByText('フォーム一覧')).toBeVisible();
  });

  test('should navigate to data browser', async ({ page }) => {
    await page.goto('/apps/studio/data');
    await expect(page).toHaveTitle(/Studio - Data Browser/);
    await expect(page.getByText('Data Browser')).toBeVisible(); // Context bar title in English
    // Expect model selector default option
    await expect(page.getByText('モデルを選択...')).toBeVisible();
  });
});
