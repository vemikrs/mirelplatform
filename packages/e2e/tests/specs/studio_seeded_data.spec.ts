import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/auth/login.page';

test.describe.skip('Studio Seeded Data', () => {
    test.beforeEach(async ({ page }) => {
        const loginPage = new LoginPage(page);
        await loginPage.goto();
        await loginPage.login('admin@example.com', 'password123');
        await page.waitForURL('**/home');
    });

    test('should load customer model and records', async ({ page }) => {
        // Go to Data Browser
        await page.goto('/apps/studio/data');
        
        // Wait for models to load
        const select = page.locator('select');
        await expect(select).toBeVisible();
        
        // Select 'customer' model (Label: 顧客情報)
        await select.selectOption({ value: 'customer' });
        
        // Verify records exist in the table
        // We look for text that should appear in the grid
        await expect(page.getByText('cust-001')).toBeVisible();
        await expect(page.getByText('山田 太郎')).toBeVisible();
        await expect(page.getByText('taro@example.com')).toBeVisible();
    });

    test('should load order model and records', async ({ page }) => {
        await page.goto('/apps/studio/data');
        const select = page.locator('select');
        await expect(select).toBeVisible();
        
        await select.selectOption({ value: 'order' });
        
        // Verify order records
        await expect(page.getByText('ord-001')).toBeVisible();
        // Check for related customer ID or amount
        await expect(page.getByText('cust-001')).toBeVisible();
        await expect(page.getByText('153000')).toBeVisible();
    });
});
