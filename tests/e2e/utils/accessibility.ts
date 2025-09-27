import { Page, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

/**
 * Accessibility testing utilities using AXE
 */
export class AccessibilityUtils {
  /**
   * Run accessibility scan on the current page
   * @param page - Playwright page instance
   * @param options - AXE scan options
   */
  static async runAccessibilityScan(
    page: Page, 
    options: {
      exclude?: string[];
      include?: string[];
      tags?: string[];
      disableRules?: string[];
    } = {}
  ) {
    let axeBuilder = new AxeBuilder({ page });
    
    // Configure AXE builder
    if (options.exclude?.length) {
      axeBuilder = axeBuilder.exclude(options.exclude);
    }
    
    if (options.include?.length) {
      axeBuilder = axeBuilder.include(options.include);
    }
    
    if (options.tags?.length) {
      axeBuilder = axeBuilder.withTags(options.tags);
    }
    
    if (options.disableRules?.length) {
      axeBuilder = axeBuilder.disableRules(options.disableRules);
    }
    
    // Run the scan
    const accessibilityScanResults = await axeBuilder.analyze();
    
    // Assert no violations
    expect(accessibilityScanResults.violations).toEqual([]);
    
    return accessibilityScanResults;
  }
  
  /**
   * Run accessibility scan with common exclusions for the ProMarker app
   * @param page - Playwright page instance
   */
  static async runProMarkerAccessibilityScan(page: Page) {
    return this.runAccessibilityScan(page, {
      // Exclude common third-party components that may have violations
      exclude: [
        '[data-test-id="bootstrap-modal"]',
        '.ag-grid-container'
      ],
      // Focus on WCAG 2.1 AA compliance
      tags: ['wcag2a', 'wcag2aa', 'wcag21aa'],
      // Disable rules that are not applicable or problematic
      disableRules: [
        'color-contrast', // May be affected by custom themes
        'landmark-one-main' // Single page application structure
      ]
    });
  }
}