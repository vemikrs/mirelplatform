import { test, expect } from '@playwright/test'

test.describe('Console Errors Check', () => {
  let backendAvailable = false

  test.beforeAll(async ({ request }) => {
    try {
      console.log('[console-errors] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok();
      console.log(`[console-errors] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[console-errors] Backend not available:', error);
      backendAvailable = false;
    }
  });

  test('should load ProMarker page without console errors', async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');
    const consoleMessages: string[] = []
    const consoleErrors: string[] = []
    
    // Listen to console messages
    page.on('console', (msg) => {
      consoleMessages.push(`${msg.type()}: ${msg.text()}`)
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text())
      }
    })
    
    // Listen to page errors
    page.on('pageerror', (error) => {
      consoleErrors.push(`Page error: ${error.message}`)
    })
    
    // Navigate to ProMarker page
  await page.goto('/promarker', { waitUntil: 'networkidle', timeout: 60000 })
    
    // Wait a bit for any async errors
    await page.waitForTimeout(2000)
    
    // Log all console messages
    console.log('=== Console Messages ===')
    consoleMessages.forEach(msg => console.log(msg))
    
    // Log errors
    if (consoleErrors.length > 0) {
      console.log('\n=== Console Errors ===')
      consoleErrors.forEach(err => console.log(err))
    }
    
    // Check if page loaded
    const title = await page.title()
    console.log(`\nPage title: ${title}`)
    
    // Check if root element exists
    const rootExists = await page.locator('#root').count()
    console.log(`Root element exists: ${rootExists > 0}`)
    
    // Take screenshot
    await page.screenshot({ path: 'test-results/promarker-page.png', fullPage: true })
    
    // Filter out known React warnings that are not critical
    const criticalErrors = consoleErrors.filter(err => 
      !err.includes('`value` prop on `%s` should not be null')
    )
    
    // Fail test if there are critical errors
    if (criticalErrors.length > 0) {
      throw new Error(`Found ${criticalErrors.length} console errors:\n${criticalErrors.join('\n')}`)
    }
  })
})
