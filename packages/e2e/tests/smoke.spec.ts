import { test, expect } from '@playwright/test';

/**
 * Smoke test for ProMarker v3 application
 * Basic verification that the application is accessible and responsive
 */
test.describe('ProMarker Smoke Test', () => {
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
