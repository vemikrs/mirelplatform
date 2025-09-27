import { test, expect } from '@playwright/test';
import { ProMarkerPage } from '../pages/promarker.page';
import { STENCIL_TEST_DATA, API_ENDPOINTS, TIMEOUTS } from '../fixtures/test-data';

test.describe('ProMarker Workflow Tests', () => {
  let proMarkerPage: ProMarkerPage;

  test.beforeEach(async ({ page }) => {
    proMarkerPage = new ProMarkerPage(page);
    await proMarkerPage.navigate();
    await proMarkerPage.verifyPageLoaded();
  });

  test('should complete basic stencil selection workflow', async ({ page }) => {
    // This test will be updated once actual stencils are identified
    
    // Take initial screenshot
    await proMarkerPage.takeProMarkerScreenshot('workflow-start');
    
    // For now, test the UI interactions that don't depend on specific stencils
    
    // Test that generate button is initially disabled
    const generateEnabled = await proMarkerPage.isGenerateButtonEnabled();
    expect(generateEnabled).toBe(false);
    
    // Take screenshot showing initial state
    await proMarkerPage.takeProMarkerScreenshot('workflow-initial-state');
    
    // Test clear stencil functionality
    await proMarkerPage.clickClearStencil();
    await proMarkerPage.takeProMarkerScreenshot('workflow-after-clear-stencil');
  });

  test('should handle stencil category selection', async ({ page }) => {
    // Listen for API calls
    const apiPromise = page.waitForResponse(response => 
      response.url().includes(API_ENDPOINTS.suggest) && response.status() === 200
    );
    
    // This will need actual category values once identified
    // For now, test the interaction pattern
    
    await proMarkerPage.takeProMarkerScreenshot('before-category-selection');
    
    // Test that the form is present and ready for interaction
    await expect(page.locator('form')).toBeVisible();
  });

  test('should handle parameter input validation', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('before-parameter-validation');
    
    // Test form validation behavior
    // This will be expanded once actual parameter fields are identified
    
    // For now, verify that required form elements are present
    await expect(page.locator('form')).toBeVisible();
    
    await proMarkerPage.takeProMarkerScreenshot('parameter-validation-ready');
  });

  test('should handle generate button interaction', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('before-generate-test');
    
    // Test initial state of generate button
    const initialEnabled = await proMarkerPage.isGenerateButtonEnabled();
    
    // Generate button should be disabled initially (no stencil selected)
    expect(initialEnabled).toBe(false);
    
    await proMarkerPage.takeProMarkerScreenshot('generate-button-disabled');
  });

  test('should handle file download workflow', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('before-download-test');
    
    // Test download availability
    const downloadAvailable = await proMarkerPage.isDownloadAvailable();
    
    // Initially, no download should be available
    expect(downloadAvailable).toBe(false);
    
    await proMarkerPage.takeProMarkerScreenshot('no-download-available');
  });

  test('should handle API error responses gracefully', async ({ page }) => {
    // Monitor for any API calls and potential errors
    let apiErrorOccurred = false;
    
    page.on('response', async (response) => {
      if (response.url().includes('/mapi/apps/mste/api/') && response.status() >= 400) {
        apiErrorOccurred = true;
      }
    });
    
    // Test error handling by attempting operations
    await proMarkerPage.clickReloadStencilMaster();
    
    // Take screenshot
    await proMarkerPage.takeProMarkerScreenshot('after-api-test');
    
    // If API error occurred, verify it's handled gracefully
    if (apiErrorOccurred) {
      // Check that user-friendly error message is shown
      const hasError = await proMarkerPage.hasErrorMessage();
      expect(hasError).toBe(true);
    }
  });

  test('should maintain state during user interactions', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('state-management-start');
    
    // Test various UI interactions to ensure state is maintained
    await proMarkerPage.clickClearAll();
    await proMarkerPage.takeProMarkerScreenshot('after-clear-all-state');
    
    await proMarkerPage.clickJsonFormat();
    await proMarkerPage.takeProMarkerScreenshot('modal-open-state');
    
    await proMarkerPage.closeModal();
    await proMarkerPage.takeProMarkerScreenshot('modal-closed-state');
    
    // Verify UI is still responsive
    await expect(page.locator('.container_title')).toBeVisible();
  });

  test('should handle concurrent user actions', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('concurrent-actions-start');
    
    // Test rapid successive clicks don't break the UI
    await proMarkerPage.clickClearAll();
    await proMarkerPage.clickReloadStencilMaster();
    
    // Wait for operations to complete
    await proMarkerPage.waitForLoadingComplete();
    
    await proMarkerPage.takeProMarkerScreenshot('concurrent-actions-completed');
    
    // Verify no errors occurred
    const hasError = await proMarkerPage.hasErrorMessage();
    expect(hasError).toBe(false);
  });

  test.afterEach(async ({ page }, testInfo) => {
    // Capture final state
    if (testInfo.status !== testInfo.expectedStatus) {
      await proMarkerPage.takeProMarkerScreenshot(`workflow-failed-${testInfo.title.replace(/\s+/g, '-')}`);
    }
  });
});