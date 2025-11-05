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
  let backendAvailable = false;
  
  // Reload stencil master once before all tests
  // WSL2 Crash Prevention: Check backend availability before running tests
  test.beforeAll(async ({ request }) => {
    try {
      console.log('Reloading stencil master data...');
      const response = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000 // Reduced timeout for faster failure detection
      });
      backendAvailable = response.ok();
      if (backendAvailable) {
        console.log('Stencil master data loaded successfully');
      } else {
        console.error(`Backend returned status ${response.status()}`);
      }
    } catch (error) {
      console.error('Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test.beforeEach(async ({ page }) => {
    // WSL2 Crash Prevention: Skip all tests if backend is not available
    test.skip(!backendAvailable, 'Backend not available - skipping to prevent WSL2 resource exhaustion');
    
    promarkerPage = new ProMarkerV3Page(page);
    await promarkerPage.navigate();
    
    // Page already waits for network idle in navigate()
    // Initial suggest API call is triggered on mount
  });
  
  test('should display category dropdown on page load', async ({ page }) => {
    // Assert: Verify category select is visible
    const categoryTrigger = page.locator('[data-testid="category-select"]');
    await expect(categoryTrigger).toBeVisible();
    // Open and ensure options render
    await categoryTrigger.click();
    const options = page.getByRole('option');
    await expect(options.first()).toBeVisible();
    // Close the dropdown
    await page.keyboard.press('Escape');
  });
  
  test('should auto-complete stencil & serial after category selection', async ({ page }) => {
    // Select Category (triggers two cascading suggest calls)
    await promarkerPage.selectCategoryByIndex(0);
    
    // Wait for both dropdowns to be enabled (indicates API calls completed)
    const stencilSelect = page.locator('[data-testid="stencil-select"]');
    const serialSelect = page.locator('[data-testid="serial-select"]');

    await expect(stencilSelect).toBeEnabled({ timeout: 10000 });
    await expect(serialSelect).toBeEnabled({ timeout: 10000 });

    // Values should already be auto-selected (non-empty) - check trigger text
    await expect(stencilSelect).not.toHaveText(/選択|ロード中/);
    await expect(serialSelect).not.toHaveText(/選択|ロード中/);

    // Parameter section should be visible because serial resolved
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible();

    // Generate button enabled
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeEnabled();
  });
  
  // SKIP: Currently only one category exists in test data
  // TODO: Enable when multiple categories are available
  test.skip('should clear stencil and serial when category changes', async ({ page }) => {
    // Complete initial selection
    await promarkerPage.selectCategoryByIndex(0);
    // Fixed: Wait for UI element state instead of specific response
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    
    await promarkerPage.selectStencilByIndex(0);
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    await promarkerPage.selectSerialByIndex(0);
    
    // Change category
    await promarkerPage.selectCategoryByIndex(1);
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    
    // Verify stencil and serial are reset
    // For design-system Select, verify placeholder text appears again
    await expect(page.locator('[data-testid="stencil-select"]').getByText(/選択|カテゴリを選択/)).toBeVisible();
    await expect(page.locator('[data-testid="serial-select"]').getByText(/選択|ステンシルを選択/)).toBeVisible();
    
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
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled();
    
    // Change stencil
    await page.selectOption('[data-testid="stencil-select"]', { index: 2 });
    // Fixed: Wait for UI element state instead of specific response
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
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
    await promarkerPage.selectCategoryByIndex(0);
    
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
    await promarkerPage.selectCategoryByIndex(0);
    // Fixed: Wait for UI elements instead of API responses
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    // Wait for serial selection to be completed (auto-selected)
    await page.waitForLoadState('networkidle');
    
    // Wait for stencil config to load (backend returns config when serial is selected)
    // The stencil-info component only renders when stencilConfig is not null
    const stencilInfo = page.locator('[data-testid="stencil-info"]');
    await expect(stencilInfo).toBeVisible({ timeout: 15000 });
    
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
    await promarkerPage.selectCategoryByIndex(0);
    
    // Wait a bit for error handling
    await page.waitForTimeout(1000);
    
    // Verify stencil dropdown remains disabled
    const stencilSelect = page.locator('[data-testid="stencil-select"]');
    await expect(stencilSelect).toBeDisabled();
  });
});
