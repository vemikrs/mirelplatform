import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/auth/login.page';

test.describe('Studio Seeded Data', () => {
    test.beforeEach(async ({ page }) => {
        const loginPage = new LoginPage(page);
        await loginPage.goto();
        await loginPage.login('admin@example.com', 'password123');
        await page.waitForURL('**/home');
        // Switch to enterprise-001 tenant to ensure data visibility
        await page.evaluate(async () => {
             const res = await fetch('/mapi/auth/switch-tenant', {
                 method: 'POST',
                 headers: { 'Content-Type': 'application/json' },
                 body: JSON.stringify({ tenantId: 'enterprise-001' })
             });
             if (!res.ok) console.error('Failed to switch tenant', res.status);
             else console.log('Switched to enterprise-001');
        });
    });

    test('should load customer model and records', async ({ page }) => {
        // Go to Data Browser
        await page.goto('/apps/studio/data');
        
        // Wait for models to load
        await expect(page.getByText('読み込み中...')).toBeHidden();

        const select = page.locator('select');
        await expect(select).toBeVisible();
        
        // Wait for options to be populated (more than just the placeholder)
        await expect(select.locator('option')).not.toHaveCount(1, { timeout: 10000 });
        
        // Debug: Log options
        const options = await select.locator('option').allTextContents();
        console.log('Available models:', options);

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
