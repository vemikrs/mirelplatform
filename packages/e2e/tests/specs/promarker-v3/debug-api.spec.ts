import { test, expect } from '@playwright/test';

/**
 * Debug test to check API calls
 */
test.describe('ProMarker v3 API Debug', () => {
  
  test('should show all network requests', async ({ page }) => {
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
    await expect(page.locator('h1').first()).toContainText('ProMarker');
  });
});
