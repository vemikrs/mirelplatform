
import { test, expect } from '@playwright/test';
import { ProMarkerV3Page } from '../../pages/promarker-v3.page';
import { LoginPage } from '../../pages/auth/login.page';

/**
 * Simplified Form Validation Test
 * Step 6: Direct test without page object
 */
test.describe.skip('ProMarker v3 Form Validation - Simple', () => {
  let backendAvailable = false;
  
  test.beforeAll(async ({ request }) => {
    try {
      console.log('[simple-test] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok() || resp.status() === 401;
      console.log(`[simple-test] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[simple-test] Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test('should load page and show form fields', async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');
    test.setTimeout(30000);
    
    // Increase timeout for login
    test.setTimeout(120000);
    test.slow();

    // Perform login
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', 'password123');

    const promarkerPage = new ProMarkerV3Page(page);
    await promarkerPage.navigate();
    
    // Wait for page heading (SectionHeading renders heading role)
    await expect(page.getByRole('heading', { name: /ProMarker/ })).toBeVisible({ timeout: 10000 });
    
    // Wait for API call or timeout
    try {
      await page.waitForResponse(
        r => r.url().includes('/mapi/apps/mste/api/suggest') && r.status() === 200,
        { timeout: 10000 }
      );
      console.log('[TEST] Suggest API called successfully');
    } catch (e) {
      console.log('[TEST] Suggest API not called within timeout, continuing...');
    }
    
    // Wait a bit
    await page.waitForTimeout(2000);
    
    // Check if category dropdown trigger is visible (design-system Select)
    const categoryTrigger = page.locator('[data-testid="category-select"]');
    await expect(categoryTrigger).toBeVisible();
    
    console.log('[TEST] Page loaded successfully');
  });
});
