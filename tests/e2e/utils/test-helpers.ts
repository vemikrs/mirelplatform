import { Page, expect } from '@playwright/test';

/**
 * Common test utilities and helpers
 */
export class TestHelpers {
  /**
   * Wait for page to be fully loaded
   * @param page - Playwright page instance
   */
  static async waitForPageLoad(page: Page) {
    await page.waitForLoadState('networkidle');
    await page.waitForLoadState('domcontentloaded');
  }
  
  /**
   * Wait for element to be visible and enabled
   * @param page - Playwright page instance
   * @param selector - Element selector
   * @param timeout - Timeout in milliseconds
   */
  static async waitForElement(page: Page, selector: string, timeout = 5000) {
    await page.waitForSelector(selector, { state: 'visible', timeout });
    await expect(page.locator(selector)).toBeEnabled({ timeout });
  }
  
  /**
   * Safely click an element after waiting for it
   * @param page - Playwright page instance
   * @param selector - Element selector
   */
  static async safeClick(page: Page, selector: string) {
    await this.waitForElement(page, selector);
    await page.locator(selector).click();
  }
  
  /**
   * Safely fill input after waiting for it
   * @param page - Playwright page instance
   * @param selector - Element selector
   * @param value - Value to fill
   */
  static async safeFill(page: Page, selector: string, value: string) {
    await this.waitForElement(page, selector);
    await page.locator(selector).fill(value);
  }
  
  /**
   * Take a screenshot with timestamp
   * @param page - Playwright page instance
   * @param name - Screenshot name
   */
  static async takeTimestampedScreenshot(page: Page, name: string) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    await page.screenshot({ 
      path: `test-results/screenshots/${name}-${timestamp}.png`,
      fullPage: true 
    });
  }
  
  /**
   * Wait for API response
   * @param page - Playwright page instance
   * @param urlPattern - URL pattern to match
   * @param timeout - Timeout in milliseconds
   */
  static async waitForApiResponse(page: Page, urlPattern: string | RegExp, timeout = 10000) {
    return page.waitForResponse(urlPattern, { timeout });
  }
  
  /**
   * Check if element exists without throwing
   * @param page - Playwright page instance
   * @param selector - Element selector
   */
  static async elementExists(page: Page, selector: string): Promise<boolean> {
    try {
      await page.waitForSelector(selector, { timeout: 1000 });
      return true;
    } catch {
      return false;
    }
  }
  
  /**
   * Scroll element into view
   * @param page - Playwright page instance
   * @param selector - Element selector
   */
  static async scrollIntoView(page: Page, selector: string) {
    await page.locator(selector).scrollIntoViewIfNeeded();
  }
  
  /**
   * Wait for text to appear in element
   * @param page - Playwright page instance
   * @param selector - Element selector
   * @param text - Text to wait for
   * @param timeout - Timeout in milliseconds
   */
  static async waitForText(page: Page, selector: string, text: string, timeout = 5000) {
    await expect(page.locator(selector)).toContainText(text, { timeout });
  }
}