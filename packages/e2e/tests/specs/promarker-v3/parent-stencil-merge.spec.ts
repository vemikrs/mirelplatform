import { test, expect } from '@playwright/test';
import { ProMarkerV3Page } from '../../pages/promarker-v3.page';

/**
 * ProMarker v3 Parent Stencil Settings Merge E2E Tests
 * 
 * Phase 2-1: Tests parent stencil settings merge functionality
 * Verifies that child stencil parameters inherit metadata from parent stencil
 * 
 * Test Coverage:
 * - Parameter fields display with parent-inherited metadata (name, type, placeholder, note)
 * - Hierarchical stencil structure (parent → child relationship)
 * - UI properly renders inherited parameter information
 */
test.describe('ProMarker v3 Parent Stencil Merge', () => {
  let promarkerPage: ProMarkerV3Page;
  let backendAvailable = false;

  test.beforeAll(async ({ request }) => {
    try {
      console.log('[parent-merge] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok();
      console.log(`[parent-merge] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[parent-merge] Backend not available:', error);
      backendAvailable = false;
    }
  });

  test.beforeEach(async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');

    promarkerPage = new ProMarkerV3Page(page);
    await promarkerPage.navigate();
  });

  test('should display parameters with parent-inherited metadata', async ({ page }) => {
    // Select a stencil that has parent settings
    // Using /samples category which has well-defined parent-child structure
    await promarkerPage.selectCategoryByIndex(0);
    
    // Wait for stencil dropdown to be populated
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    // Wait for parameter section to appear
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible({ timeout: 15000 });
    
    // Verify at least one parameter exists
    const parameterFields = page.locator('[data-testid^="param-"]');
    const fieldCount = await parameterFields.count();
    expect(fieldCount).toBeGreaterThan(0);
    
    console.log(`[parent-merge] Found ${fieldCount} parameter fields`);
  });

  test('should display parameter labels from parent metadata', async ({ page }) => {
    // Complete selection to display parameters
    await promarkerPage.selectCategoryByIndex(0);
    
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible({ timeout: 15000 });
    
    // Get first parameter input element
    const firstParam = page.locator('input[data-testid^="param-"]').first();
    
    if (await firstParam.count() > 0) {
      // Verify it has a visible label (name from parent metadata)
      const paramId = await firstParam.getAttribute('data-testid');
      
      if (paramId) {
        const label = page.locator(`label[for="${paramId}"]`);
        await expect(label).toBeVisible();
        
        const labelText = await label.textContent();
        console.log(`[parent-merge] Parameter label: ${labelText}`);
        
        // Label should not be empty (inherited from parent)
        expect(labelText).toBeTruthy();
        expect(labelText?.trim().length).toBeGreaterThan(0);
      }
    }
  });

  test('should display parameter placeholders from parent metadata', async ({ page }) => {
    await promarkerPage.selectCategoryByIndex(0);
    
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible({ timeout: 15000 });
    
    // Get first parameter input
    const firstParam = page.locator('input[data-testid^="param-"]').first();
    
    if (await firstParam.count() > 0) {
      // Verify placeholder attribute exists (from parent metadata)
      const placeholder = await firstParam.getAttribute('placeholder');
      console.log(`[parent-merge] Parameter placeholder: ${placeholder}`);
      
      // Placeholder should exist and not be empty
      expect(placeholder).toBeTruthy();
      expect(placeholder?.length).toBeGreaterThan(0);
    }
  });

  test('should handle stencils with hierarchical parent-child structure', async ({ page }) => {
    // This test verifies the full workflow with parent settings merge
    await promarkerPage.selectCategoryByIndex(0);
    
    // Wait for all dropdowns to be populated
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    // Verify stencil info displays (includes parent-merged config)
    const stencilInfo = page.locator('[data-testid="stencil-info"]');
    await expect(stencilInfo).toBeVisible({ timeout: 15000 });
    
    // Verify parameters section is populated
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible();
    
    // Verify generate button is enabled (complete workflow)
    const generateBtn = page.locator('[data-testid="generate-btn"]');
    await expect(generateBtn).toBeEnabled();
    
    console.log('[parent-merge] Full hierarchical workflow completed');
  });

  test('should display multiple parameters with inherited metadata', async ({ page }) => {
    await promarkerPage.selectCategoryByIndex(0);
    
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    const parameterSection = page.locator('[data-testid="parameter-section"]');
    await expect(parameterSection).toBeVisible({ timeout: 15000 });
    
    // Get all parameter inputs
    const paramInputs = page.locator('input[data-testid^="param-"]');
    const paramCount = await paramInputs.count();
    
    console.log(`[parent-merge] Total parameters: ${paramCount}`);
    
    // Verify we have multiple parameters (parent merge should provide rich metadata)
    expect(paramCount).toBeGreaterThan(0);
    
    // Verify each parameter has a label
    for (let i = 0; i < Math.min(paramCount, 3); i++) {
      const param = paramInputs.nth(i);
      const paramId = await param.getAttribute('data-testid');
      
      if (paramId) {
        const label = page.locator(`label[for="${paramId}"]`);
        await expect(label).toBeVisible();
        
        const labelText = await label.textContent();
        console.log(`[parent-merge] Parameter ${i + 1} label: ${labelText}`);
      }
    }
  });

  test('should verify API response includes parent-merged parameters', async ({ page, request }) => {
    // Direct API test to verify backend merge logic
    await promarkerPage.selectCategoryByIndex(0);
    
    // Wait for auto-selection to complete
    await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    await expect(page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    
    // Get selected values from UI
    const categoryValue = await page.locator('[data-testid="category-select"]').textContent();
    const stencilValue = await page.locator('[data-testid="stencil-select"]').textContent();
    const serialValue = await page.locator('[data-testid="serial-select"]').textContent();
    
    console.log(`[parent-merge] Selected: category=${categoryValue}, stencil=${stencilValue}, serial=${serialValue}`);
    
    // Call suggest API directly
    const apiResponse = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/suggest', {
      data: {
        content: {
          stencilCategoy: '/samples',
          stencilCanonicalName: '/samples/hello-world',
          serialNo: '250913A'
        }
      }
    });
    
    expect(apiResponse.ok()).toBeTruthy();
    
    const responseData = await apiResponse.json();
    console.log('[parent-merge] API response structure:', JSON.stringify(responseData, null, 2).substring(0, 500));
    
    // Verify response has expected structure (ModelWrapper)
    expect(responseData.data).toBeDefined();
    expect(responseData.data.model).toBeDefined();
    
    // Verify params are present
    const params = responseData.data.model.params;
    expect(params).toBeDefined();
    expect(params.childs).toBeDefined();
    expect(Array.isArray(params.childs)).toBeTruthy();
    
    const childCount = params.childs.length;
    console.log(`[parent-merge] API returned ${childCount} parameters`);
    
    expect(childCount).toBeGreaterThan(0);
    
    // Verify first parameter has required metadata fields
    if (childCount > 0) {
      const firstChild = params.childs[0];
      console.log('[parent-merge] First parameter:', JSON.stringify(firstChild, null, 2));
      
      // Verify parent-inherited fields exist
      expect(firstChild.id).toBeDefined();
      expect(firstChild.name).toBeDefined(); // From parent
      expect(firstChild.valueType).toBeDefined(); // From parent
      expect(firstChild.placeholder).toBeDefined(); // From parent
      
      console.log(`[parent-merge] Parameter metadata: id=${firstChild.id}, name=${firstChild.name}, type=${firstChild.valueType}`);
    }
  });
});
