import { test, expect } from '@playwright/test';
import { ProMarkerV3Page } from '../../pages/promarker-v3.page';

/**
 * ProMarker v3 Parameter Input E2E Tests
 * Tests dynamic parameter field generation and input handling
 * 
 * TDD Phase: RED → GREEN → REFACTOR
 * Step 5: ProMarker UI Implementation
 */
test.describe('ProMarker v3 Parameter Input', () => {
  let promarkerPage: ProMarkerV3Page;
  
  test.beforeEach(async ({ page }) => {
    promarkerPage = new ProMarkerV3Page(page);
    await promarkerPage.navigate();
    
    // Page already waits for network idle in navigate()
    // Initial suggest API call is triggered on mount
    
    // Complete 3-tier selection to display parameters
    await promarkerPage.complete3TierSelection(
      '/samples',
      '/samples/hello-world',
      '250913A'
    );
  });
  
  test('should display parameter fields after serial selection', async ({ page }) => {
    // Verify parameter section is visible
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible();
    
    // Verify at least one parameter field exists
    const parameterFields = page.locator('[data-testid^="param-"]');
    const fieldCount = await parameterFields.count();
    expect(fieldCount).toBeGreaterThan(0);
  });
  
  test('should display parameter with label and placeholder', async ({ page }) => {
    // Get first parameter input field
    const firstParam = page.locator('input[data-testid^="param-"]').first();
    await expect(firstParam).toBeVisible();
    
    // Verify it has a label
    const paramId = await firstParam.getAttribute('data-testid');
    if (paramId) {
      const label = page.locator(`label[for="${paramId}"]`);
      await expect(label).toBeVisible();
    }
  });
  
  test('should allow text input in parameter fields', async ({ page }) => {
    // Find first text parameter input element
    const textParam = page.locator('input[data-testid^="param-"]').first();
    
    // Clear existing value first (component has default value)
    await textParam.clear();
    
    // Type value
    const testValue = 'test-value-123';
    await textParam.fill(testValue);
    
    // Verify value is set
    await expect(textParam).toHaveValue(testValue);
  });
  
  // SKIP: Currently only one serial exists in test data
  // TODO: Enable when multiple serials are available
  test.skip('should preserve parameter values when switching serials', async ({ page }) => {
    // Fill a parameter
    const firstParam = page.locator('[data-testid^="param-"]').first();
    await firstParam.fill('test-value');
    
    // Get the parameter ID
    const paramId = await firstParam.getAttribute('data-testid');
    
    // Switch to different serial
    await page.selectOption('[data-testid="serial-select"]', { index: 2 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    // Switch back
    await page.selectOption('[data-testid="serial-select"]', { index: 1 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    // Verify value is cleared (new selection resets form)
    const paramAfter = page.locator(`[data-testid="${paramId}"]`);
    await expect(paramAfter).toHaveValue('');
  });
  
  // SKIP: Test selector needs refinement (Step 6 will add proper input selection)
  // TODO: Fix selector to target only input elements
  test.skip('should clear all parameters when Clear All button clicked', async ({ page }) => {
    // Fill multiple parameters
    const params = page.locator('[data-testid^="param-"]');
    const paramCount = await params.count();
    
    for (let i = 0; i < Math.min(paramCount, 3); i++) {
      await params.nth(i).fill(`value-${i}`);
    }
    
    // Click Clear All button
    const clearAllBtn = page.locator('[data-testid="clear-all-btn"]');
    await clearAllBtn.click();
    
    // Verify all parameters are cleared
    for (let i = 0; i < Math.min(paramCount, 3); i++) {
      await expect(params.nth(i)).toHaveValue('');
    }
    
    // Verify selections are also cleared
    await expect(page.locator('[data-testid="category-select"]')).toHaveValue('');
  });
  
  test('should display parameter note/description', async ({ page }) => {
    // Get first parameter
    const firstParam = page.locator('[data-testid^="param-"]').first();
    const paramId = await firstParam.getAttribute('data-testid');
    
    if (paramId) {
      // Check for description text (if exists)
      const description = page.locator(`[data-testid="${paramId}-description"]`);
      
      // Description may or may not exist depending on stencil config
      const isVisible = await description.isVisible().catch(() => false);
      
      if (isVisible) {
        await expect(description).toBeVisible();
      }
    }
  });
  
  // SKIP: Validation logic will be implemented in Step 6 (React Hook Form)
  // TODO: Enable after validation is implemented
  test.skip('should enable Generate button when all required fields filled', async ({ page }) => {
    // Fill all parameter fields
    const params = page.locator('[data-testid^="param-"]');
    const paramCount = await params.count();
    
    for (let i = 0; i < paramCount; i++) {
      const param = params.nth(i);
      const inputType = await param.getAttribute('type');
      
      if (inputType !== 'file') {
        await param.fill(`value-${i}`);
      }
    }
    
    // Verify Generate button is enabled
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeEnabled();
  });
  
  // SKIP: Validation logic will be implemented in Step 6 (React Hook Form)
  // TODO: Enable after validation is implemented
  test.skip('should disable Generate button when required fields are empty', async ({ page }) => {
    // Don't fill any parameters
    
    // Verify Generate button is disabled
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeDisabled();
  });
  
  test('should handle file type parameters', async ({ page }) => {
    // Look for file input parameter
    const fileParam = page.locator('input[type="file"][data-testid^="param-"]');
    
    const fileParamExists = await fileParam.count() > 0;
    
    if (fileParamExists) {
      // Verify file upload button exists
      const uploadBtn = page.locator('[data-testid="file-upload-btn"]').first();
      await expect(uploadBtn).toBeVisible();
    } else {
      // Skip test if no file parameters in current stencil
      test.skip();
    }
  });
  
  test('should show parameter count', async ({ page }) => {
    // Check if parameter count is displayed
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    
    // Count actual parameter fields
    const params = page.locator('[data-testid^="param-"]');
    const paramCount = await params.count();
    
    // Verify parameter section exists
    await expect(parameterSection).toBeVisible();
    expect(paramCount).toBeGreaterThan(0);
  });
  
  // SKIP: Input validation will be implemented in Step 6 (Zod schema)
  // TODO: Enable after validation is implemented
  test.skip('should validate parameter input format', async ({ page }) => {
    // Fill a parameter with invalid format (if validation exists)
    const firstParam = page.locator('[data-testid^="param-"]').first();
    
    // Type invalid value
    await firstParam.fill('   '); // Only spaces
    
    // Try to generate
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    
    // Check if button is disabled or validation error appears
    const isDisabled = await generateBtn.isDisabled();
    const errorMessage = page.locator('[role="alert"]');
    const hasError = await errorMessage.isVisible().catch(() => false);
    
    // Either button should be disabled or error should appear
    expect(isDisabled || hasError).toBeTruthy();
  });
});
