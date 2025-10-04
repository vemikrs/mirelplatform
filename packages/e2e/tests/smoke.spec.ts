import { test, expect } from '@playwright/test';

/**
 * Smoke test for ProMarker application
 * Basic verification that the application is accessible and responsive
 */
test.describe('ProMarker Smoke Test', () => {
  test('should load ProMarker page and perform basic assertion', async ({ page }) => {
    // Navigate to ProMarker MSTE page
    await page.goto('/mirel/mste');
    
    // Wait for page to be ready
    await page.waitForLoadState('networkidle');
    
    // Verify page title contains expected text
    await expect(page).toHaveTitle(/ProMarker|mirel/i);
    
    // Verify main container is visible
    const mainContainer = page.locator('.container, [role="main"], main').first();
    await expect(mainContainer).toBeVisible({ timeout: 10000 });
    
    // Take a screenshot for visual verification
    await page.screenshot({ path: 'test-results/smoke-test.png', fullPage: true });
    
    console.log('✅ Smoke test passed: ProMarker page loaded successfully');
  });
});
