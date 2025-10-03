import { test, expect } from '@playwright/test';
import { ProMarkerPage } from '../pages/promarker.page';
import { VIEWPORT_CONFIGS } from '../fixtures/test-data';

test.describe('ProMarker Visual Regression Tests', () => {
  let proMarkerPage: ProMarkerPage;

  test.beforeEach(async ({ page }) => {
    proMarkerPage = new ProMarkerPage(page);
    await proMarkerPage.navigate();
    await proMarkerPage.verifyPageLoaded();
  });

  test('should match initial page layout', async ({ page }) => {
    // Wait for page to stabilize
    await page.waitForTimeout(1000);
    
    // Take full page screenshot
    await expect(page).toHaveScreenshot('promarker-initial-layout.png', {
      fullPage: true,
      clip: { x: 0, y: 0, width: 1280, height: 720 }
    });
  });

  test('should match page layout on different viewports', async ({ page }) => {
    // Desktop
    await page.setViewportSize(VIEWPORT_CONFIGS.desktop);
    await page.waitForTimeout(500);
    await expect(page).toHaveScreenshot('promarker-desktop.png', {
      fullPage: true
    });

    // Tablet
    await page.setViewportSize(VIEWPORT_CONFIGS.tablet);
    await page.waitForTimeout(500);
    await expect(page).toHaveScreenshot('promarker-tablet.png', {
      fullPage: true
    });

    // Mobile
    await page.setViewportSize(VIEWPORT_CONFIGS.mobile);
    await page.waitForTimeout(500);
    await expect(page).toHaveScreenshot('promarker-mobile.png', {
      fullPage: true
    });
  });

  test('should match button states', async ({ page }) => {
    // Initial button states
    await expect(page.locator('.rightitems')).toHaveScreenshot('buttons-initial-state.png');
    
    // Generate button area
    await expect(page.locator('text=Generate').locator('..')).toHaveScreenshot('generate-button-area.png');
  });

  test('should match modal dialog appearance', async ({ page }) => {
    // Open JSON format modal
    await proMarkerPage.clickJsonFormat();
    
    // Wait for modal animation to complete
    await page.waitForTimeout(500);
    
    // Screenshot the modal
    await expect(page.locator('.modal')).toHaveScreenshot('json-modal.png');
    
    // Close modal for cleanup
    await proMarkerPage.closeModal();
  });

  test('should match form elements appearance', async ({ page }) => {
    // Form container
    await expect(page.locator('form')).toHaveScreenshot('form-container.png');
    
    // Individual form elements if they exist
    const selects = await page.locator('select').count();
    if (selects > 0) {
      await expect(page.locator('select').first()).toHaveScreenshot('form-select.png');
    }
    
    const inputs = await page.locator('input[type="text"]').count();
    if (inputs > 0) {
      await expect(page.locator('input[type="text"]').first()).toHaveScreenshot('form-input.png');
    }
  });

  test('should match header and title area', async ({ page }) => {
    // Page title area
    await expect(page.locator('.container_title')).toHaveScreenshot('page-title.png');
    
    // Action buttons area
    await expect(page.locator('.rightitems')).toHaveScreenshot('action-buttons.png');
  });

  test('should match loading states', async ({ page }) => {
    // Trigger reload to potentially see loading state
    const reloadPromise = proMarkerPage.clickReloadStencilMaster();
    
    // Try to capture any loading indicators quickly
    await page.waitForTimeout(100);
    
    // Take screenshot of any loading states
    const loadingElements = await page.locator('.loading, .spinner, .b-spinner').count();
    if (loadingElements > 0) {
      await expect(page.locator('.loading, .spinner, .b-spinner').first()).toHaveScreenshot('loading-indicator.png');
    }
    
    // Wait for loading to complete
    await reloadPromise;
  });

  test('should match error state appearance', async ({ page }) => {
    // This test will capture the appearance if any errors occur
    // For now, just ensure no errors are visible initially
    
    const errorElements = await page.locator('.alert-danger, .error, .text-danger').count();
    if (errorElements > 0) {
      await expect(page.locator('.alert-danger, .error, .text-danger').first()).toHaveScreenshot('error-state.png');
    }
    
    // Take screenshot of normal state for comparison
    await expect(page.locator('.container')).toHaveScreenshot('normal-state.png');
  });

  test('should match dark mode appearance', async ({ page }) => {
    // Test dark mode if supported
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.waitForTimeout(500);
    
    await expect(page).toHaveScreenshot('promarker-dark-mode.png', {
      fullPage: true
    });
    
    // Reset to light mode
    await page.emulateMedia({ colorScheme: 'light' });
  });

  test('should match high contrast mode', async ({ page }) => {
    // Simulate high contrast preferences
    await page.addStyleTag({
      content: `
        * {
          filter: contrast(200%) !important;
        }
      `
    });
    
    await page.waitForTimeout(500);
    
    await expect(page).toHaveScreenshot('promarker-high-contrast.png', {
      fullPage: true,
      threshold: 0.3 // Higher threshold for high contrast mode
    });
  });

  test('should match focused element states', async ({ page }) => {
    // Focus on different interactive elements and capture their appearance
    
    // Focus on first button
    const firstButton = page.locator('button').first();
    await firstButton.focus();
    await expect(firstButton).toHaveScreenshot('button-focused.png');
    
    // Focus on generate button if visible
    const generateBtn = page.locator('text=Generate');
    if (await generateBtn.isVisible()) {
      await generateBtn.focus();
      await expect(generateBtn).toHaveScreenshot('generate-button-focused.png');
    }
    
    // Focus on form elements if available
    const inputs = await page.locator('input, select').count();
    if (inputs > 0) {
      const firstInput = page.locator('input, select').first();
      await firstInput.focus();
      await expect(firstInput).toHaveScreenshot('form-element-focused.png');
    }
  });

  test('should match hover states', async ({ page }) => {
    // Test hover states on interactive elements
    
    // Hover over buttons
    const buttons = await page.locator('button').all();
    for (let i = 0; i < Math.min(buttons.length, 3); i++) {
      await buttons[i].hover();
      await expect(buttons[i]).toHaveScreenshot(`button-${i}-hovered.png`);
    }
    
    // Hover over generate button
    const generateBtn = page.locator('text=Generate');
    if (await generateBtn.isVisible()) {
      await generateBtn.hover();
      await expect(generateBtn).toHaveScreenshot('generate-button-hovered.png');
    }
  });

  test.afterEach(async ({ page }) => {
    // Reset any style changes
    await page.emulateMedia({ colorScheme: null });
  });
});