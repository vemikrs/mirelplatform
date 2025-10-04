import { Page, Locator, expect } from '@playwright/test';
import { TestHelpers } from '../utils/test-helpers';
import { AccessibilityUtils } from '../utils/accessibility';

/**
 * Base page class for Page Object Model
 * Contains common functionality shared across all pages
 */
export abstract class BasePage {
  protected page: Page;
  
  constructor(page: Page) {
    this.page = page;
  }
  
  /**
   * Navigate to the page
   * @param url - URL to navigate to
   */
  async navigateTo(url: string) {
    await this.page.goto(url);
    await TestHelpers.waitForPageLoad(this.page);
  }
  
  /**
   * Get page title
   */
  async getTitle(): Promise<string> {
    return await this.page.title();
  }
  
  /**
   * Get current URL
   */
  async getCurrentUrl(): Promise<string> {
    return this.page.url();
  }
  
  /**
   * Wait for element to be visible
   * @param selector - Element selector
   * @param timeout - Timeout in milliseconds
   */
  async waitForVisible(selector: string, timeout = 5000) {
    await TestHelpers.waitForElement(this.page, selector, timeout);
  }
  
  /**
   * Click element safely
   * @param selector - Element selector
   */
  async clickElement(selector: string) {
    await TestHelpers.safeClick(this.page, selector);
  }
  
  /**
   * Fill input safely
   * @param selector - Element selector
   * @param value - Value to fill
   */
  async fillInput(selector: string, value: string) {
    await TestHelpers.safeFill(this.page, selector, value);
  }
  
  /**
   * Get element text
   * @param selector - Element selector
   */
  async getElementText(selector: string): Promise<string> {
    await this.waitForVisible(selector);
    return await this.page.locator(selector).textContent() || '';
  }
  
  /**
   * Check if element is visible
   * @param selector - Element selector
   */
  async isElementVisible(selector: string): Promise<boolean> {
    return TestHelpers.elementExists(this.page, selector);
  }
  
  /**
   * Take screenshot
   * @param name - Screenshot name
   */
  async takeScreenshot(name: string) {
    await TestHelpers.takeTimestampedScreenshot(this.page, name);
  }
  
  /**
   * Run accessibility scan on the page
   */
  async runAccessibilityScan() {
    await AccessibilityUtils.runProMarkerAccessibilityScan(this.page);
  }
  
  /**
   * Wait for loading to complete
   */
  async waitForLoadingComplete() {
    // Wait for any loading indicators to disappear
    const loadingSelectors = [
      '.loading',
      '.spinner',
      '[data-test-id="loading"]',
      '.b-spinner'
    ];
    
    for (const selector of loadingSelectors) {
      if (await TestHelpers.elementExists(this.page, selector)) {
        await this.page.waitForSelector(selector, { state: 'hidden', timeout: 10000 });
      }
    }
  }
  
  /**
   * Wait for API call to complete
   * @param urlPattern - URL pattern to match
   */
  async waitForApiCall(urlPattern: string | RegExp) {
    return TestHelpers.waitForApiResponse(this.page, urlPattern);
  }
  
  /**
   * Scroll to element
   * @param selector - Element selector
   */
  async scrollToElement(selector: string) {
    await TestHelpers.scrollIntoView(this.page, selector);
  }
  
  /**
   * Select option from dropdown
   * @param selector - Select element selector
   * @param value - Option value to select
   */
  async selectOption(selector: string, value: string) {
    await this.waitForVisible(selector);
    await this.page.locator(selector).selectOption(value);
  }
  
  /**
   * Check checkbox
   * @param selector - Checkbox selector
   */
  async checkCheckbox(selector: string) {
    await this.waitForVisible(selector);
    await this.page.locator(selector).check();
  }
  
  /**
   * Uncheck checkbox
   * @param selector - Checkbox selector
   */
  async uncheckCheckbox(selector: string) {
    await this.waitForVisible(selector);
    await this.page.locator(selector).uncheck();
  }
  
  /**
   * Wait for element to contain text
   * @param selector - Element selector
   * @param text - Text to wait for
   */
  async waitForText(selector: string, text: string) {
    await TestHelpers.waitForText(this.page, selector, text);
  }
}