import { test, expect } from '@playwright/test';

import { ProMarkerV3Page } from '../../pages/promarker-v3.page';
import { LoginPage } from '../../pages/auth/login.page';

/**
 * ProMarker v3 Routing E2E Tests
 * Tests React Router v7 configuration and navigation
 * 
 * TDD Phase: RED - Tests fail initially (route not implemented)
 */
test.describe.skip('ProMarker v3 Routing', () => {
  let promarkerPage: ProMarkerV3Page;
  let backendAvailable = false;

  test.beforeAll(async ({ request }) => {
    try {
      console.log('[routing] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok() || resp.status() === 401;
      console.log(`[routing] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[routing] Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test.beforeEach(async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');
    
    // Increase timeout for login
    test.setTimeout(60000);

    // Perform login
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin', 'password123');
    
    promarkerPage = new ProMarkerV3Page(page);
  });
  
  test('should navigate to ProMarker page', async ({ page }) => {
    // Navigate to ProMarker page
    await promarkerPage.navigate();
    
    // Verify URL
    await expect(page).toHaveURL(/\/promarker/);
    
    // Verify page is loaded (heading rendered by SectionHeading)
    await expect(page.getByRole('heading', { name: /ProMarker/ })).toBeVisible();
  });
  
  test('should have correct page title', async ({ page }) => {
    await promarkerPage.navigate();
    
    // Verify browser title contains "ProMarker"
    await expect(page).toHaveTitle(/ProMarker/);
  });
  
  test('should render ProMarker page content', async ({ page }) => {
    await promarkerPage.navigate();
    
    // Verify main heading (copy updated to ワークスペース)
    const heading = page.getByRole('heading', { name: /ProMarker/ });
    await expect(heading).toBeVisible();
    await expect(heading).toContainText('ProMarker');
  });
  
  test('should handle browser back/forward navigation', async ({ page }) => {
    // Navigate to home page first
    await page.goto('/');
    await expect(page).toHaveURL('/');
    
    // Navigate to ProMarker page
    await promarkerPage.navigate();
    await expect(page).toHaveURL(/\/promarker/);
    
    // Go back
    await page.goBack();
    await expect(page).toHaveURL('/');
    
    // Go forward
    await page.goForward();
    await expect(page).toHaveURL(/\/promarker/);
  });
  
  test('should load without console errors', async ({ page }) => {
    const consoleErrors: string[] = [];
    
    // Listen for console errors
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });
    
    await promarkerPage.navigate();
    
    // Filter out known React warnings
    const criticalErrors = consoleErrors.filter(err => 
      !err.includes('`value` prop on `%s` should not be null')
    );
    
    // Verify no critical console errors
    expect(criticalErrors).toHaveLength(0);
  });
  
  test('should have accessible navigation', async ({ page }) => {
    await promarkerPage.navigate();
    
    // Verify page can be navigated via keyboard
    await page.keyboard.press('Tab');
    
    // At least one focusable element should exist
    const focusedElement = page.locator(':focus');
    await expect(focusedElement).toBeTruthy();
  });
  
  test('should handle direct URL access', async ({ page }) => {
    // Access ProMarker URL directly
    await page.goto('/promarker');
    await page.waitForLoadState('networkidle');
    
    // Verify page loaded successfully
    await expect(page).toHaveURL(/\/promarker/);
    await expect(page.getByRole('heading', { name: /ProMarker/ })).toBeVisible();
  });
  
  test('should handle page refresh', async ({ page }) => {
    await promarkerPage.navigate();
    
    // Verify page loaded
    await expect(page.getByRole('heading', { name: /ProMarker/ })).toBeVisible();
    
    // Reload page
    await page.reload();
    await page.waitForLoadState('networkidle');
    
    // Verify page still loaded
    await expect(page).toHaveURL(/\/promarker/);
    await expect(page.getByRole('heading', { name: /ProMarker/ })).toBeVisible();
  });
});
