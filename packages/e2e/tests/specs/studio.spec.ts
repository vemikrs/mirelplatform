import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/auth/login.page';

test.describe('Studio Application', () => {
  test.beforeEach(async ({ page }) => {
    // Mock Modeler API - the frontend uses /api/studio/modeler which is a direct path
    await page.route('**/api/studio/modeler/**', async route => {
      const url = route.request().url();
      if (url.includes('listModels')) {
         await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: { models: [] } }) });
      } else if (url.includes('listModel')) {
         await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ data: { modelers: [] } }) });
      } else if (url.includes('list')) {
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

  test('should display studio home from studio root', async ({ page }) => {
    await page.goto('/apps/studio');
    // Studio root shows home page, not redirect
    await expect(page).toHaveTitle(/Studio - Home/);
  });

  test('should navigate to entity list', async ({ page }) => {
    await page.goto('/apps/studio/modeler/entities');
    await expect(page).toHaveTitle(/Modeler - Entity List/);
    await expect(page.getByText('エンティティ一覧')).toBeVisible(); // Context bar title
    // Wait for loading to complete, then check button
    await expect(page.getByRole('button', { name: '新規作成' })).toBeVisible({ timeout: 10000 });
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
    // For select elements, use getByRole('combobox') and check the selected value
    const modelSelector = page.locator('select');
    await expect(modelSelector).toBeVisible();
    // Check that the select has the default option text
    await expect(modelSelector).toHaveValue('');
  });
});
