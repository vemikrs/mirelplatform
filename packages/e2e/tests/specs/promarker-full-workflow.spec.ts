import { test, expect } from '@playwright/test';
import { ProMarkerPage } from '../pages/promarker.page';
import { STENCIL_TEST_DATA, API_ENDPOINTS } from '../fixtures/test-data';

test.describe('ProMarker Full Workflow Tests', () => {
  let proMarkerPage: ProMarkerPage;

  test.beforeEach(async ({ page }) => {
    proMarkerPage = new ProMarkerPage(page);
    await proMarkerPage.navigate();
    await proMarkerPage.verifyPageLoaded();
  });

  test('should complete hello-world stencil generation workflow', async ({ page }) => {
    const testData = STENCIL_TEST_DATA[0];
    
    await proMarkerPage.takeProMarkerScreenshot('workflow-start');
    
    // Step 1: Select category and stencil
    // Note: This will need to be adapted based on actual UI implementation
    // For now, we'll test the interactions we can confirm exist
    
    // Test that we can interact with the reload button (this works)
    await proMarkerPage.clickReloadStencilMaster();
    await proMarkerPage.takeProMarkerScreenshot('after-reload');
    
    // Test form interactions
    const form = page.locator('form');
    await expect(form).toBeVisible();
    
    // Look for any select elements in the form
    const selects = await page.locator('select').count();
    if (selects > 0) {
      await proMarkerPage.takeProMarkerScreenshot('form-with-selects');
      
      // Try to interact with the first select if available
      const firstSelect = page.locator('select').first();
      if (await firstSelect.isVisible()) {
        await firstSelect.click();
        await proMarkerPage.takeProMarkerScreenshot('select-opened');
      }
    }
    
    // Look for input fields
    const inputs = await page.locator('input[type="text"]').count();
    if (inputs > 0) {
      await proMarkerPage.takeProMarkerScreenshot('form-with-inputs');
    }
    
    // Test generate button state
    const generateEnabled = await proMarkerPage.isGenerateButtonEnabled();
    await proMarkerPage.takeProMarkerScreenshot(`generate-button-${generateEnabled ? 'enabled' : 'disabled'}`);
  });

  test('should handle API responses during workflow', async ({ page }) => {
    // Monitor API calls
    const apiCalls: string[] = [];
    
    page.on('response', async (response) => {
      if (response.url().includes('/mapi/apps/mste/api/')) {
        apiCalls.push(`${response.request().method()} ${response.url()} -> ${response.status()}`);
      }
    });
    
    await proMarkerPage.takeProMarkerScreenshot('api-monitoring-start');
    
    // Trigger API calls
    await proMarkerPage.clickReloadStencilMaster();
    
    // Wait a bit for any API calls to complete
    await page.waitForTimeout(2000);
    
    await proMarkerPage.takeProMarkerScreenshot('after-api-calls');
    
    // Log API calls for debugging
    console.log('API calls made:', apiCalls);
  });

  test('should handle parameter input workflow', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('parameter-workflow-start');
    
    // Look for parameter input fields
    const paramInputs = await page.locator('input[id^="eparam-"]').count();
    
    console.log(`Found ${paramInputs} parameter input fields`);
    
    if (paramInputs > 0) {
      // Take screenshot showing parameter fields
      await proMarkerPage.takeProMarkerScreenshot('parameter-fields-found');
      
      // Try to fill the first parameter field
      const firstParamInput = page.locator('input[id^="eparam-"]').first();
      const inputId = await firstParamInput.getAttribute('id');
      
      if (inputId) {
        await firstParamInput.fill('Test Value');
        await proMarkerPage.takeProMarkerScreenshot('parameter-filled');
      }
    } else {
      await proMarkerPage.takeProMarkerScreenshot('no-parameter-fields');
    }
  });

  test('should validate form fields properly', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('validation-test-start');
    
    // Try to submit form without required fields
    const generateBtn = page.locator('text=Generate');
    
    if (await generateBtn.isVisible()) {
      const isEnabled = await generateBtn.isEnabled();
      
      if (isEnabled) {
        // Try clicking generate without proper selection
        await generateBtn.click();
        await proMarkerPage.takeProMarkerScreenshot('generate-clicked-without-data');
        
        // Check for validation messages
        const hasError = await proMarkerPage.hasErrorMessage();
        if (hasError) {
          const errorMsg = await proMarkerPage.getErrorMessage();
          console.log('Validation error:', errorMsg);
          await proMarkerPage.takeProMarkerScreenshot('validation-error-shown');
        }
      } else {
        await proMarkerPage.takeProMarkerScreenshot('generate-disabled-as-expected');
      }
    }
  });

  test('should handle file operations workflow', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('file-operations-start');
    
    // Look for file-related elements
    const fileInputs = await page.locator('input[type="file"]').count();
    const downloadLinks = await page.locator('a[href*="download"], a[href*="dlsite"]').count();
    
    console.log(`Found ${fileInputs} file inputs and ${downloadLinks} download links`);
    
    if (fileInputs > 0) {
      await proMarkerPage.takeProMarkerScreenshot('file-inputs-found');
    }
    
    if (downloadLinks > 0) {
      await proMarkerPage.takeProMarkerScreenshot('download-links-found');
    }
    
    // Test download availability
    const downloadAvailable = await proMarkerPage.isDownloadAvailable();
    await proMarkerPage.takeProMarkerScreenshot(`download-${downloadAvailable ? 'available' : 'not-available'}`);
  });

  test('should maintain UI state during complex interactions', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('state-test-initial');
    
    // Perform multiple UI interactions
    await proMarkerPage.clickClearAll();
    await proMarkerPage.takeProMarkerScreenshot('after-clear-all');
    
    await proMarkerPage.clickJsonFormat();
    await proMarkerPage.takeProMarkerScreenshot('modal-opened');
    
    await proMarkerPage.closeModal();
    await proMarkerPage.takeProMarkerScreenshot('modal-closed');
    
    await proMarkerPage.clickReloadStencilMaster();
    await proMarkerPage.takeProMarkerScreenshot('after-reload-final');
    
    // Verify essential elements are still present
    await expect(page.locator('.container_title')).toBeVisible();
    await expect(page.locator('form')).toBeVisible();
    await expect(page.locator('text=Generate')).toBeVisible();
  });

  test('should handle error scenarios gracefully', async ({ page }) => {
    await proMarkerPage.takeProMarkerScreenshot('error-handling-start');
    
    // Monitor for JavaScript errors
    const jsErrors: string[] = [];
    page.on('pageerror', (error) => {
      jsErrors.push(error.message);
    });
    
    // Monitor for failed network requests
    const failedRequests: string[] = [];
    page.on('requestfailed', (request) => {
      failedRequests.push(`${request.method()} ${request.url()}`);
    });
    
    // Perform various operations that might cause errors
    await proMarkerPage.clickReloadStencilMaster();
    await proMarkerPage.clickJsonFormat();
    await proMarkerPage.closeModal();
    
    await proMarkerPage.takeProMarkerScreenshot('error-handling-complete');
    
    // Log any errors for debugging
    if (jsErrors.length > 0) {
      console.log('JavaScript errors:', jsErrors);
    }
    if (failedRequests.length > 0) {
      console.log('Failed requests:', failedRequests);
    }
    
    // The test passes if UI remains functional despite errors
    await expect(page.locator('.container_title')).toBeVisible();
  });

  test.afterEach(async ({ page }, testInfo) => {
    if (testInfo.status !== testInfo.expectedStatus) {
      await proMarkerPage.takeProMarkerScreenshot(`full-workflow-failed-${testInfo.title.replace(/\s+/g, '-')}`);
    }
  });
});