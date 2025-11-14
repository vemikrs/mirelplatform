import { test, expect } from '@playwright/test';

/**
 * Debug test to check API calls
 */
test.describe('ProMarker v3 API Debug', () => {
  let backendAvailable = false;
  
  test.beforeAll(async ({ request }) => {
    try {
      console.log('[debug-api] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok();
      console.log(`[debug-api] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[debug-api] Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test('should show all network requests', async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');
    // Monitor all requests
    const requests: string[] = [];
    page.on('request', request => {
      const url = request.url();
      if (url.includes('mapi') || url.includes('suggest') || url.includes('api')) {
        requests.push(`${request.method()} ${url}`);
        console.log(`[REQUEST] ${request.method()} ${url}`);
      }
    });

    page.on('response', async response => {
      const url = response.url();
      if (url.includes('mapi') || url.includes('suggest') || url.includes('api')) {
        const status = response.status();
        console.log(`[RESPONSE] ${status} ${url}`);
        try {
          const body = await response.text();
          if (body.length < 500) {
            console.log(`[BODY] ${body}`);
          } else {
            console.log(`[BODY] ${body.substring(0, 200)}... (${body.length} bytes)`);
          }
        } catch (e) {
          console.log(`[BODY] Error reading body: ${e}`);
        }
      }
    });

    // Navigate to ProMarker page
    console.log('[TEST] Navigating to ProMarker page...');
    await page.goto('http://localhost:5173/promarker');
    
    // Wait a bit for async calls
    await page.waitForTimeout(5000);
    
    console.log(`[TEST] Total API requests captured: ${requests.length}`);
    console.log(`[TEST] Requests:`, requests);
    
    // Check if page loaded
    await expect(page.getByRole('heading', { name: /ProMarker ワークスペース/ })).toBeVisible({ timeout: 10000 });
  });
});
