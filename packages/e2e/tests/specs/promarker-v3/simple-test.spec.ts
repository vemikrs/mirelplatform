import { test, expect } from '@playwright/test';

/**
 * Simplified Form Validation Test
 * Step 6: Direct test without page object
 */
test.describe('ProMarker v3 Form Validation - Simple', () => {
  let backendAvailable = false;
  
  test.beforeAll(async ({ request }) => {
    try {
      console.log('[simple-test] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok();
      console.log(`[simple-test] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[simple-test] Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test('should load page and show form fields', async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');
    test.setTimeout(30000);
    
    // Navigate to ProMarker page
    await page.goto('http://localhost:5173/promarker');
    
    // Wait for page title
    await expect(page.locator('h1').filter({ hasText: '払出画面' })).toBeVisible({ timeout: 10000 });
    
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
    
    // Check if category dropdown is visible
    const categoryDropdown = page.locator('[data-testid="category-select"]');
    await expect(categoryDropdown).toBeVisible();
    
    console.log('[TEST] Page loaded successfully');
  });
});
