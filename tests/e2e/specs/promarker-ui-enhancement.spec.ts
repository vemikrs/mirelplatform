import { test, expect } from '@playwright/test';
import { ProMarkerPage } from '../pages/promarker.page';

/**
 * Test for UI enhancements needed for better E2E testing
 * This test identifies elements that need data-test-id attributes
 */
test.describe('ProMarker UI Enhancement Requirements', () => {
  let proMarkerPage: ProMarkerPage;

  test.beforeEach(async ({ page }) => {
    // Skip this test if browsers aren't installed yet
    test.skip(!process.env.PLAYWRIGHT_BROWSERS_INSTALLED, 'Playwright browsers not installed');
    
    proMarkerPage = new ProMarkerPage(page);
    await proMarkerPage.navigate();
    await proMarkerPage.verifyPageLoaded();
  });

  test('should identify elements needing data-test-id attributes', async ({ page }) => {
    // Document elements that need data-test-id for reliable testing
    const elementsToEnhance: Array<{selector: string, suggested: string, description: string}> = [];
    
    // Check for category dropdown
    const categoryDropdowns = await page.locator('select').count();
    if (categoryDropdowns > 0) {
      elementsToEnhance.push({
        selector: 'select:first-of-type',
        suggested: '[data-test-id="category-select"]',
        description: 'Category selection dropdown'
      });
    }
    
    // Check for stencil dropdown (might be second select)
    if (categoryDropdowns > 1) {
      elementsToEnhance.push({
        selector: 'select:nth-of-type(2)',
        suggested: '[data-test-id="stencil-select"]',
        description: 'Stencil selection dropdown'
      });
    }
    
    // Check for generate button
    const generateBtn = await page.locator('text=Generate').count();
    if (generateBtn > 0) {
      elementsToEnhance.push({
        selector: 'text=Generate',
        suggested: '[data-test-id="generate-button"]',
        description: 'Generate button'
      });
    }
    
    // Check for parameter inputs
    const paramInputs = await page.locator('input[id^="eparam-"]').count();
    if (paramInputs > 0) {
      elementsToEnhance.push({
        selector: 'input[id^="eparam-"]',
        suggested: '[data-test-id^="param-input-"]',
        description: 'Parameter input fields'
      });
    }
    
    // Log recommendations
    console.log('UI Enhancement Recommendations:');
    console.log('=====================================');
    elementsToEnhance.forEach((item, index) => {
      console.log(`${index + 1}. ${item.description}`);
      console.log(`   Current: ${item.selector}`);
      console.log(`   Suggested: ${item.suggested}`);
      console.log('');
    });
    
    // Take screenshot for documentation
    await proMarkerPage.takeProMarkerScreenshot('ui-enhancement-analysis');
    
    // This test always passes - it's for documentation
    expect(elementsToEnhance.length).toBeGreaterThanOrEqual(0);
  });

  test('should validate current UI structure', async ({ page }) => {
    test.skip(!process.env.PLAYWRIGHT_BROWSERS_INSTALLED, 'Playwright browsers not installed');
    
    // Document current UI structure for enhancement planning
    const uiStructure = {
      title: await page.locator('.container_title').textContent(),
      buttons: await page.locator('button').count(),
      inputs: await page.locator('input').count(),
      selects: await page.locator('select').count(),
      forms: await page.locator('form').count(),
      modals: await page.locator('.modal').count()
    };
    
    console.log('Current UI Structure:');
    console.log('====================');
    console.log(JSON.stringify(uiStructure, null, 2));
    
    // Get all button texts for reference
    const buttonTexts = await page.locator('button').allTextContents();
    console.log('Button texts:', buttonTexts);
    
    // Get form structure
    const formElements = await page.locator('form *').count();
    console.log('Form elements count:', formElements);
    
    expect(uiStructure.title).toContain('ProMarker');
  });
});