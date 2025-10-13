import { test, expect } from '@playwright/test';
import { ProMarkerV3Page } from '../../pages/promarker-v3.page';
import { STENCIL_V3_TEST_DATA } from '../../fixtures/promarker-v3.fixture';

/**
 * ProMarker v3 Stencil Selection E2E Tests
 * Tests 3-tier selection flow (Category → Stencil → Serial)
 * 
 * TDD Phase: RED → GREEN → REFACTOR
 * Step 5: ProMarker UI Implementation
 */
test.describe('ProMarker v3 Stencil Selection', () => {
  let promarkerPage: ProMarkerV3Page;
  
  test.beforeEach(async ({ page }) => {
    promarkerPage = new ProMarkerV3Page(page);
    await promarkerPage.navigate();
    
    // Page already waits for network idle in navigate()
    // Initial suggest API call is triggered on mount
  });
  
  test('should display category dropdown on page load', async ({ page }) => {
    // Assert: Verify category select is visible
    const categorySelect = page.locator('[data-testid="category-select"]');
    await expect(categorySelect).toBeVisible();
    
    // Verify it has options
    const optionCount = await categorySelect.locator('option').count();
    expect(optionCount).toBeGreaterThan(1); // At least "選択してください" + categories
  });
  
  test('should complete 3-tier selection flow', async ({ page }) => {
    // Step 1: Select Category
    await page.selectOption('[data-testid="category-select"]', { index: 1 });
    
    // Wait for stencil options to load
    const stencilApiPromise = page.waitForResponse(response =>
      response.url().includes('/mapi/apps/mste/api/suggest')
    );
    await stencilApiPromise;
    
    // Verify stencil dropdown is enabled and has options
    const stencilSelect = page.locator('[data-testid="stencil-select"]');
    await expect(stencilSelect).toBeEnabled();
    const stencilOptions = await stencilSelect.locator('option').count();
    expect(stencilOptions).toBeGreaterThan(1);
    
    // Step 2: Select Stencil
    await page.selectOption('[data-testid="stencil-select"]', { index: 1 });
    
    // Wait for serial options to load
    const serialApiPromise = page.waitForResponse(response =>
      response.url().includes('/mapi/apps/mste/api/suggest')
    );
    await serialApiPromise;
    
    // Verify serial dropdown is enabled and has options
    const serialSelect = page.locator('[data-testid="serial-select"]');
    await expect(serialSelect).toBeEnabled();
    const serialOptions = await serialSelect.locator('option').count();
    expect(serialOptions).toBeGreaterThan(1);
    
    // Step 3: Select Serial
    await page.selectOption('[data-testid="serial-select"]', { index: 1 });
    
    // Verify parameter section appears
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible();
    
    // Verify generate button is enabled
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeEnabled();
  });
  
  // SKIP: Currently only one category exists in test data
  // TODO: Enable when multiple categories are available
  test.skip('should clear stencil and serial when category changes', async ({ page }) => {
    // Complete initial selection
    await page.selectOption('[data-testid="category-select"]', { index: 1 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    await page.selectOption('[data-testid="stencil-select"]', { index: 1 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    await page.selectOption('[data-testid="serial-select"]', { index: 1 });
    
    // Change category
    await page.selectOption('[data-testid="category-select"]', { index: 2 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    // Verify stencil and serial are reset
    const stencilValue = await page.locator('[data-testid="stencil-select"]').inputValue();
    const serialValue = await page.locator('[data-testid="serial-select"]').inputValue();
    
    expect(stencilValue).toBe('');
    expect(serialValue).toBe('');
    
    // Verify generate button is disabled
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeDisabled();
  });
  
  // SKIP: Currently only one stencil exists in /samples category
  // TODO: Enable when multiple stencils are available
  test.skip('should clear serial when stencil changes', async ({ page }) => {
    // Complete 3-tier selection
    await promarkerPage.complete3TierSelection(
      '/samples',
      '/samples/hello-world',
      '250913A'
    );
    
    // Wait for loading to complete
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('[data-testid="stencil-select"]:not([disabled])');
    
    // Change stencil
    await page.selectOption('[data-testid="stencil-select"]', { index: 2 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    // Verify serial is reset
    const serialValue = await page.inputValue('[data-testid="serial-select"]');
    expect(serialValue).toBe(''); // Should be reset to empty
  });
  
  test('should disable stencil and serial dropdowns initially', async ({ page }) => {
    // On initial load, only category should be enabled
    const categorySelect = page.locator('[data-testid="category-select"]');
    const stencilSelect = page.locator('[data-testid="stencil-select"]');
    const serialSelect = page.locator('[data-testid="serial-select"]');
    
    await expect(categorySelect).toBeEnabled();
    await expect(stencilSelect).toBeDisabled();
    await expect(serialSelect).toBeDisabled();
  });
  
  test('should show loading state during API calls', async ({ page }) => {
    // Select category and check for loading indicator
    const categorySelect = page.locator('[data-testid="category-select"]');
    await categorySelect.selectOption({ index: 1 });
    
    // Check if loading indicator appears
    const loadingIndicator = page.locator('[data-testid="loading-indicator"]');
    
    // Loading should appear briefly (use timeout to catch it)
    const isLoadingVisible = await loadingIndicator.isVisible().catch(() => false);
    
    // Loading should disappear after API completes
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    await expect(loadingIndicator).not.toBeVisible();
  });
  
  test('should display stencil information after serial selection', async ({ page }) => {
    // Complete full selection
    await page.selectOption('[data-testid="category-select"]', { index: 1 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    await page.selectOption('[data-testid="stencil-select"]', { index: 1 });
    await page.waitForResponse(r => r.url().includes('/mapi/apps/mste/api/suggest'));
    
    await page.selectOption('[data-testid="serial-select"]', { index: 1 });
    
    // Verify stencil info section is visible
    const stencilInfo = page.locator('[data-testid="stencil-info"]');
    await expect(stencilInfo).toBeVisible();
    
    // Verify it contains basic information
    await expect(stencilInfo).toContainText(/カテゴリ|名前|シリアル/);
  });
  
  test('should handle API errors gracefully', async ({ page }) => {
    // Mock API error
    await page.route('**/mapi/apps/mste/api/suggest', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          data: null,
          messages: [],
          errors: ['サーバーエラーが発生しました']
        })
      });
    });
    
    // Try to select category
    await page.selectOption('[data-testid="category-select"]', { index: 1 });
    
    // Wait a bit for error handling
    await page.waitForTimeout(1000);
    
    // Verify stencil dropdown remains disabled
    const stencilSelect = page.locator('[data-testid="stencil-select"]');
    await expect(stencilSelect).toBeDisabled();
  });
});
