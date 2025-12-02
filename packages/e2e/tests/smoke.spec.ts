import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/auth/login.page';

/**
 * Smoke test for ProMarker v3 application
 * Basic verification that the application is accessible and responsive
 */
test.describe('ProMarker Smoke Test', () => {
  test.beforeEach(async ({ page }) => {
    // Listen for console logs
    page.on('console', msg => console.log(`[Browser Console] ${msg.text()}`));

    // Increase timeout for login
    test.setTimeout(60000);

    // Perform login
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', 'password123');
    
    // Debug cookies
    const cookies = await page.context().cookies();
    console.log('[Smoke Test] Cookies after login:', JSON.stringify(cookies, null, 2));
  });
  test('should load ProMarker page and perform basic assertion', async ({ page }) => {
    // Navigate to ProMarker v3 page (React frontend)
    await page.goto('/promarker');
    
    // Wait for page to be ready
    await page.waitForLoadState('networkidle');
    
    // Verify page title contains expected text
    await expect(page).toHaveTitle(/ProMarker/i);
    
    // Verify main heading is visible
    await expect(page.getByRole('heading', { name: /ProMarker ワークスペース/ })).toBeVisible({ timeout: 15000 });
    
    // Take a screenshot for visual verification
    await page.screenshot({ path: 'test-results/smoke-test.png', fullPage: true });
    
    console.log('✅ Smoke test passed: ProMarker page loaded successfully');
  });
});
