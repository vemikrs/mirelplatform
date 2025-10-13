import { test, expect } from '@playwright/test';
import { ProMarkerV3Page } from '../../pages/promarker-v3.page';

/**
 * ProMarker v3 Routing E2E Tests
 * Tests React Router v7 configuration and navigation
 * 
 * TDD Phase: RED - Tests fail initially (route not implemented)
 */
test.describe('ProMarker v3 Routing', () => {
  let promarkerPage: ProMarkerV3Page;
  
  test.beforeEach(async ({ page }) => {
    promarkerPage = new ProMarkerV3Page(page);
  });
  
  test('should navigate to ProMarker page', async ({ page }) => {
    // Navigate to ProMarker page
    await promarkerPage.navigate();
    
    // Verify URL
    await expect(page).toHaveURL(/\/promarker/);
    
    // Verify page is loaded (check for specific ProMarker heading)
    await expect(page.getByRole('heading', { name: /ProMarker 払出画面/ })).toBeVisible();
  });
  
  test('should have correct page title', async ({ page }) => {
    await promarkerPage.navigate();
    
    // Verify browser title contains "ProMarker"
    await expect(page).toHaveTitle(/ProMarker/);
  });
  
  test('should render ProMarker page content', async ({ page }) => {
    await promarkerPage.navigate();
    
    // Verify main heading
    const heading = page.getByRole('heading', { name: /ProMarker 払出画面/ });
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
    
    // Verify no console errors
    expect(consoleErrors).toHaveLength(0);
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
    await expect(page.getByRole('heading', { name: /ProMarker 払出画面/ })).toBeVisible();
  });
  
  test('should handle page refresh', async ({ page }) => {
    await promarkerPage.navigate();
    
    // Verify page loaded
    await expect(page.getByRole('heading', { name: /ProMarker 払出画面/ })).toBeVisible();
    
    // Reload page
    await page.reload();
    await page.waitForLoadState('networkidle');
    
    // Verify page still loaded
    await expect(page).toHaveURL(/\/promarker/);
    await expect(page.getByRole('heading', { name: /ProMarker 払出画面/ })).toBeVisible();
  });
});
