import { test, expect } from '@playwright/test';
import { ProMarkerPage } from '../pages/promarker.page';
import { PAGE_ELEMENTS, TIMEOUTS } from '../fixtures/test-data';

// Increase timeout for React SPA rendering
test.setTimeout(30000);

test.describe('ProMarker Basic Functionality', () => {
  let proMarkerPage: ProMarkerPage;

  test.beforeEach(async ({ page }) => {
    proMarkerPage = new ProMarkerPage(page);
    await proMarkerPage.navigate();
  });

  test('should load ProMarker page successfully', async ({ page }) => {
    // Verify page is loaded
    await proMarkerPage.verifyPageLoaded();
    
    // Check page title
    await expect(page).toHaveTitle(/ProMarker/);
    
    // Verify main elements are present
    await expect(page.locator('.container_title')).toContainText(PAGE_ELEMENTS.title);
    
    // Take screenshot for visual verification
    await proMarkerPage.takeProMarkerScreenshot('page-loaded');
    
    // Run accessibility scan
    await proMarkerPage.runAccessibilityScan();
  });

  test('should display all required UI elements', async ({ page }) => {
    await proMarkerPage.verifyPageLoaded();
    
    // Check action buttons by data-testid (more robust than text match with emojis)
    const buttonTestIds = [
      'generate-btn',
      'clear-all-btn',
      'clear-stencil-btn',
      'reload-stencil-btn',
      'json-edit-btn'
    ];
    for (const id of buttonTestIds) {
      await expect(page.locator(`[data-testid="${id}"]`)).toBeVisible();
    }
    
    // Verify main content container is present (React uses div with border.rounded-lg)
    await expect(page.locator('.border.rounded-lg').first()).toBeVisible();
    
    // Take screenshot
    await proMarkerPage.takeProMarkerScreenshot('ui-elements');
  });

  test('should handle clear all functionality', async ({ page }) => {
    await proMarkerPage.verifyPageLoaded();
    
    // Click clear all button
    await proMarkerPage.clickClearAll();
    
    // Verify the action completed without errors
    await expect(proMarkerPage.hasErrorMessage()).resolves.toBe(false);
    
    // Take screenshot
    await proMarkerPage.takeProMarkerScreenshot('after-clear-all');
  });

  test('should open JSON format modal', async ({ page }) => {
    await proMarkerPage.verifyPageLoaded();
    
    // Click JSON editor button
    await proMarkerPage.clickJsonEditor();
    
    // Verify modal opened (Radix UI dialog)
    await expect(page.locator('[role="dialog"]')).toBeVisible();
    
    // Take screenshot
    await proMarkerPage.takeProMarkerScreenshot('json-modal-open');
    
    // Close modal
    await proMarkerPage.closeModal();
    
    // Verify modal closed
    await expect(page.locator('[role="dialog"]')).not.toBeVisible();
  });

  test('should handle reload stencil master', async ({ page }) => {
    await proMarkerPage.verifyPageLoaded();
    
    // Click reload stencil master button
    await proMarkerPage.clickReloadStencilMaster();
    
    // Wait for any loading to complete
    await proMarkerPage.waitForLoadingComplete();
    
    // Verify no errors occurred
    await expect(proMarkerPage.hasErrorMessage()).resolves.toBe(false);
    
    // Take screenshot
    await proMarkerPage.takeProMarkerScreenshot('after-reload-stencil');
  });

  test('should maintain responsive design on different viewports', async ({ page }) => {
    await proMarkerPage.verifyPageLoaded();
    
    // Test desktop viewport (default)
    await proMarkerPage.takeProMarkerScreenshot('responsive-desktop');
    
    // Test tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });
    await page.waitForTimeout(1000); // Allow UI to adjust
    await proMarkerPage.takeProMarkerScreenshot('responsive-tablet');
    
    // Test mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.waitForTimeout(1000); // Allow UI to adjust
    await proMarkerPage.takeProMarkerScreenshot('responsive-mobile');
    
    // Verify essential elements are still visible on mobile
    await expect(page.locator('.container_title')).toBeVisible();
    await expect(page.locator('.border.rounded-lg').first()).toBeVisible();
  });

  test('should handle keyboard navigation', async ({ page }) => {
    await proMarkerPage.verifyPageLoaded();
    
    // Test tab navigation
    await page.keyboard.press('Tab');
    
    // Take screenshot showing focus
    await proMarkerPage.takeProMarkerScreenshot('keyboard-navigation');
    
    // Test escape key (should not cause errors)
    await page.keyboard.press('Escape');
    
    // Verify no errors
    await expect(proMarkerPage.hasErrorMessage()).resolves.toBe(false);
  });

  test.afterEach(async ({ page }, testInfo) => {
    // Take final screenshot if test failed
    if (testInfo.status !== testInfo.expectedStatus) {
      await proMarkerPage.takeProMarkerScreenshot(`failed-${testInfo.title.replace(/\s+/g, '-')}`);
    }
  });
});
