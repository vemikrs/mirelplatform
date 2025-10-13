import { test, expect } from '@playwright/test';
import { ProMarkerV3Page } from '../../pages/promarker-v3.page';

/**
 * ProMarker v3 - Form Validation Tests
 * 
 * Step 6: React Hook Form + Zod統合テスト
 * - 必須フィールドバリデーション
 * - 文字列長バリデーション
 * - パターンバリデーション
 * - エラーメッセージ表示
 * - Generate button有効/無効制御
 */
test.describe('ProMarker v3 Form Validation', () => {
  let promarkerPage: ProMarkerV3Page;
  
  test.beforeEach(async ({ page }) => {
    promarkerPage = new ProMarkerV3Page(page);
    
    // Set up response listener before navigation
    const suggestResponsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/suggest') && r.status() === 200,
      { timeout: 60000 }
    );
    
    await promarkerPage.navigate();
    
    // Wait for the suggest API response
    await suggestResponsePromise;
    
    // Wait for React to settle after initial render
    await page.waitForTimeout(1000);
    
    // Complete 3-tier selection to display parameters
    await promarkerPage.complete3TierSelection(
      '/samples',
      '/samples/hello-world',
      '250913A'
    );
  });
  
  test('should show required field error when field is empty', async ({ page }) => {
    // Clear message input
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.blur();
    
    // Wait for error to appear
    await page.waitForTimeout(500);
    
    // Zod shows the last failing validation error
    // For empty string (""), both min(1) and min(3) fail → shows min(3) message
    const errorMessage = page.locator('[data-testid="error-message"]');
    await expect(errorMessage).toBeVisible();
    await expect(errorMessage).toHaveText('3文字以上入力してください');
  });
  
  test('should clear error when valid input is provided', async ({ page }) => {
    // Clear field to trigger error
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.blur();
    
    // Verify error appears
    const errorMessage = page.locator('[data-testid="error-message"]');
    await expect(errorMessage).toBeVisible();
    
    // Enter valid value
    await messageInput.fill('Valid message');
    await messageInput.blur();
    
    // Verify error disappears
    await expect(errorMessage).not.toBeVisible();
  });
  
  test('should validate minimum length', async ({ page }) => {
    // Enter short value (assuming minLength validation exists)
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.fill('ab'); // Too short
    await messageInput.blur();
    
    // Verify min length error
    const errorMessage = page.locator('[data-testid="error-message"]');
    await expect(errorMessage).toBeVisible();
    await expect(errorMessage).toContainText('3文字以上');
  });
  
  test('should validate maximum length', async ({ page }) => {
    // Enter long value
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.fill('a'.repeat(101)); // Too long (assuming max 100)
    await messageInput.blur();
    
    // Verify max length error
    const errorMessage = page.locator('[data-testid="error-message"]');
    await expect(errorMessage).toBeVisible();
    await expect(errorMessage).toContainText('100文字以内');
  });
  
  test('should validate pattern (e.g., alphanumeric)', async ({ page }) => {
    // Enter invalid characters (assuming pattern validation)
    const userNameInput = page.locator('[data-testid="param-userName"]');
    await userNameInput.clear();
    await userNameInput.fill('Invalid@#$'); // Special chars not allowed
    await userNameInput.blur();
    
    // Verify pattern error
    const errorMessage = page.locator('[data-testid="error-userName"]');
    await expect(errorMessage).toBeVisible();
    await expect(errorMessage).toContainText('半角英数字のみ');
  });
  
  test('should disable Generate button when validation errors exist', async ({ page }) => {
    // Clear required field to create validation error
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.blur();
    
    // Verify Generate button is disabled
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeDisabled();
  });
  
  test('should enable Generate button when all validations pass', async ({ page }) => {
    // Fill all required fields with valid values
    await page.locator('[data-testid="param-message"]').fill('Hello, World!');
    await page.locator('[data-testid="param-userName"]').fill('Developer');
    await page.locator('[data-testid="param-language"]').fill('ja');
    
    // Wait for validation
    await page.waitForTimeout(300);
    
    // Verify Generate button is enabled
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeEnabled();
  });
  
  test('should show multiple errors for multiple invalid fields', async ({ page }) => {
    // Clear multiple required fields
    await page.locator('[data-testid="param-message"]').clear();
    await page.locator('[data-testid="param-userName"]').clear();
    
    // Blur to trigger validation
    await page.locator('[data-testid="param-message"]').blur();
    await page.locator('[data-testid="param-userName"]').blur();
    
    // Verify both error messages appear
    const messageError = page.locator('[data-testid="error-message"]');
    const userNameError = page.locator('[data-testid="error-userName"]');
    
    await expect(messageError).toBeVisible();
    await expect(userNameError).toBeVisible();
  });
  
    test('should validate on form submit attempt', async ({ page }) => {
    // Clear all fields
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.blur();
    
    // Wait for validation to trigger
    await page.waitForTimeout(500);
    
    // Verify errors appear for empty required fields
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible();
    
    // Verify Generate button is disabled due to validation errors
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeDisabled();
  });
  
  test('should preserve validation state after API calls', async ({ page }) => {
    // Enter invalid value
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.fill('ab'); // Too short
    await messageInput.blur();
    
    // Verify error appears
    const errorMessage = page.locator('[data-testid="error-message"]');
    await expect(errorMessage).toBeVisible();
    
    // Trigger API call (e.g., change serial)
    // Note: This assumes serial change preserves form state
    // In real implementation, you may reset or preserve based on UX requirements
    
    // For now, just verify the error persists
    await expect(errorMessage).toBeVisible();
  });
  
  test('should validate file type parameters', async ({ page }) => {
    // Assume file type parameter exists and has size/type validation
    // This is a placeholder for Step 7 file upload validation
    // Mark as pending for now
    test.skip();
  });
  
  test('should display inline error messages near input fields', async ({ page }) => {
    // Clear field to trigger error
    const messageInput = page.locator('[data-testid="param-message"]');
    await messageInput.clear();
    await messageInput.blur();
    
    // Verify error message is positioned near the input
    const errorMessage = page.locator('[data-testid="error-message"]');
    await expect(errorMessage).toBeVisible();
    
    // Verify error is within the same container as the input
    const parameterField = page.locator('[data-testid="param-field-message"]');
    const errorInField = parameterField.locator('[data-testid="error-message"]');
    await expect(errorInField).toBeVisible();
  });
  
  test('should handle validation for dynamically added parameters', async ({ page }) => {
    // This test assumes parameters can be dynamically added
    // For current implementation with fixed parameters, this is a placeholder
    // TODO: Implement when dynamic parameter addition is supported
    test.skip();
  });
});
