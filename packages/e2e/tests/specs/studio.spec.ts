import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/auth/login.page';

test.describe('Studio Application', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin@example.com', 'password123');
    await page.waitForLoadState('networkidle');
  });

  test('should display studio home from studio root', async ({ page }) => {
    await page.goto('/apps/studio');
    await expect(page).toHaveTitle(/Studio/, { timeout: 15000 });
    await expect(page.getByRole('heading', { level: 1 })).toContainText(/Studio/i);
  });

  test('should navigate to entity list', async ({ page }) => {
    await page.goto('/apps/studio/modeler/entities');
    await expect(page.getByText('読み込み中...')).toBeHidden(); 

    await expect(page).toHaveTitle(/Modeler/);
    await expect(page.getByText('エンティティ一覧')).toBeVisible(); 
    
    // Robust check for seeded or empty data
    const hasData = await page.getByText('Customer Model').isVisible();
    if (hasData) {
        await expect(page.getByText('Customer Model')).toBeVisible();
    } else {
        await expect(page.getByText(/モデルが定義されていません/)).toBeVisible();
    }
  });

  test('should navigate to form list', async ({ page }) => {
    await page.goto('/apps/studio/forms');
    await expect(page).toHaveTitle(/Studio/);
    await expect(page.getByText('フォーム一覧').first()).toBeVisible();
  });

  test('should navigate to data browser', async ({ page }) => {
    await page.goto('/apps/studio/data');
    await expect(page).toHaveTitle(/Studio/);
    // Verify URL matches
    expect(page.url()).toContain('/apps/studio/data');
  });
});
