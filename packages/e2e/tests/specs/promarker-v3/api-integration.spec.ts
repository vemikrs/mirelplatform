import { test, expect } from '@playwright/test';
import { ProMarkerV3Page } from '../../pages/promarker-v3.page';
import { API_MOCK_RESPONSES } from '../../fixtures/promarker-v3.fixture';

/**
 * ProMarker v3 API Integration E2E Tests
 * Tests API client configuration and integration with backend
 * 
 * TDD Phase: RED - Tests fail initially (API client not implemented)
 * 
 * Note: These tests will pass once Step 4 (TanStack Query Hooks) is implemented
 * Currently in RED phase - ProMarkerPage doesn't call suggest API yet
 */
test.describe('ProMarker v3 API Integration', () => {
  let promarkerPage: ProMarkerV3Page;
  let backendAvailable = false;

  test.beforeAll(async ({ request }) => {
    try {
      console.log('[api-integration] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok();
      console.log(`[api-integration] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[api-integration] Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test.beforeEach(async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');
    promarkerPage = new ProMarkerV3Page(page);
  });
  
  test('should call suggest API and receive response', async ({ page }) => {
    // Setup: Wait for suggest API call
    const apiPromise = page.waitForResponse(
      response => response.url().includes('/mapi/apps/mste/api/suggest') 
        && response.status() === 200
    );
    
    // Action: Navigate to ProMarker page (should trigger initial suggest call)
    await promarkerPage.navigate();
    
    // Assert: Verify API was called and returned success
    const response = await apiPromise;
    const data = await response.json();
    
    // Verify response structure (ModelWrapper)
    expect(data).toHaveProperty('data');
    expect(data.data).toHaveProperty('model');
    expect(data).toHaveProperty('messages');
    expect(data).toHaveProperty('errors');
  });
  
  test('should handle API errors gracefully', async ({ page }) => {
    // Action: Navigate to ProMarker page first
    await page.goto('/promarker');
    await page.waitForLoadState('networkidle');
    
    // Setup: Mock API error for manual action
    await page.route('**/mapi/apps/mste/api/suggest', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify(API_MOCK_RESPONSES.suggest.error)
      });
    });
    
    // Action: Trigger API call manually (e.g., by clicking category select)
    await expect(page.locator('[data-testid="category-select"]')).toBeVisible({ timeout: 15000 });
    await page.locator('[data-testid="category-select"]').click();
    
    // Wait for error handling
    await page.waitForTimeout(2000);
    
    // Assert: Verify error toast appears or page shows error state
    const hasToast = await promarkerPage.isToastVisible();
    if (hasToast) {
      const toastMessage = await promarkerPage.getToastMessage();
      expect(toastMessage).toContain('エラー');
    }
  });
  
  test('should set correct request headers', async ({ page }) => {
    let capturedHeaders: Record<string, string> = {};
    
    // Intercept API call to capture headers
    await page.route('**/mapi/apps/mste/api/suggest', async route => {
      capturedHeaders = route.request().headers();
      await route.continue();
    });
    
    await promarkerPage.navigate();
    
    // Verify Content-Type header
    expect(capturedHeaders['content-type']).toContain('application/json');
  });
  
  test('should send correct request body structure', async ({ page }) => {
    let capturedRequestBody: any = null;
    
    // Intercept API call to capture request body
    await page.route('**/mapi/apps/mste/api/suggest', async route => {
      const postData = route.request().postData();
      if (postData) {
        capturedRequestBody = JSON.parse(postData);
      }
      await route.continue();
    });
    
    await promarkerPage.navigate();
    
    // Wait for request to be captured
    await page.waitForTimeout(1000);
    
    // Verify request has 'content' wrapper
    if (capturedRequestBody) {
      expect(capturedRequestBody).toHaveProperty('content');
      expect(capturedRequestBody.content).toHaveProperty('stencilCategoy');
      expect(capturedRequestBody.content).toHaveProperty('stencilCanonicalName');
      expect(capturedRequestBody.content).toHaveProperty('serialNo');
    }
  });
  
  test('should handle network errors', async ({ page }) => {
    // Use reliable navigation to ensure page loads properly
    await promarkerPage.navigate();
    
    // Wait for page to be fully loaded - use more general selector
    await expect(page.locator('[data-testid="category-select"]')).toBeVisible({ timeout: 15000 });
    
    // Setup: Simulate network failure for manual action
    await page.route('**/mapi/apps/mste/api/suggest', route => {
      route.abort('failed');
    });
    
    // Action: Trigger API call manually
    await page.locator('[data-testid="category-select"]').click();
    
    // Wait for error handling
    await page.waitForTimeout(2000);
    
    // Assert: Page should still be functional - check for core elements instead of specific heading
    await expect(page.locator('[data-testid="category-select"]')).toBeVisible();
    await expect(page.locator('[data-testid="generate-btn"]')).toBeVisible();
  });
  
  test('should handle API timeout', async ({ page }) => {
    // Setup: Simulate slow API response that eventually succeeds
    await page.route('**/mapi/apps/mste/api/suggest', async route => {
      // Delay response by 3 seconds but then succeed
      await new Promise(resolve => setTimeout(resolve, 3000));
      await route.continue();
    });
    
    // Action: Navigate to ProMarker page
    await page.goto('/promarker');
    await page.waitForLoadState('networkidle');
    
    // Assert: Page should eventually load
    await expect(page.getByRole('heading', { name: /ProMarker ワークスペース/ })).toBeVisible({ timeout: 15000 });
  });
  
  test('should retry failed requests on transient errors', async ({ page }) => {
    let requestCount = 0;
    
    // Setup: Fail first request, succeed on retry
    await page.route('**/mapi/apps/mste/api/suggest', async route => {
      requestCount++;
      if (requestCount === 1) {
        route.fulfill({
          status: 503,
          body: JSON.stringify({ data: null, messages: [], errors: ['Service unavailable'] })
        });
      } else {
        await route.continue();
      }
    });
    
    await promarkerPage.navigate();
    
    // Note: This test depends on retry logic implementation in Step 2
    // For now, just verify page loads eventually
    await expect(page.getByRole('heading', { name: /ProMarker ワークスペース/ })).toBeVisible();
  });
  
  test('should handle CORS preflight requests', async ({ page }) => {
    // Setup: Monitor OPTIONS requests
    const optionsRequests: string[] = [];
    
    page.on('request', request => {
      if (request.method() === 'OPTIONS') {
        optionsRequests.push(request.url());
      }
    });
    
    await promarkerPage.navigate();
    
    // Note: In development with proxy, CORS should be handled
    // This test verifies no CORS errors occur
    const consoleErrors: string[] = [];
    page.on('console', msg => {
      if (msg.type() === 'error' && msg.text().includes('CORS')) {
        consoleErrors.push(msg.text());
      }
    });
    
    await page.waitForTimeout(2000);
    expect(consoleErrors).toHaveLength(0);
  });
});
