import { test, expect } from '@playwright/test'

test.describe('Console Errors Check', () => {
  test('should load ProMarker page without console errors', async ({ page }) => {
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
    
    // Fail test if there are errors
    if (consoleErrors.length > 0) {
      throw new Error(`Found ${consoleErrors.length} console errors:\n${consoleErrors.join('\n')}`)
    }
  })
})
