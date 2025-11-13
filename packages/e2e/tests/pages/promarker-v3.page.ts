import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';
import { TestHelpers } from '../utils/test-helpers';

/**
 * Page Object Model for ProMarker v3 (React/Vite frontend)
 * Provides methods to interact with ProMarker UI elements
 */
export class ProMarkerV3Page extends BasePage {
  readonly url = '/promarker';
  
  // Selectors for React components (using data-testid attributes)
  private readonly selectors = {
    // Page elements
    pageTitle: 'h1',
    
    // Stencil selection dropdowns
    categorySelect: '[data-testid="category-select"]',
    stencilSelect: '[data-testid="stencil-select"]',
    serialSelect: '[data-testid="serial-select"]',
    
    // Dynamic parameter inputs
    parameterInput: (id: string) => `[data-testid="param-${id}"]`,
    parameterLabel: (id: string) => `[data-testid="label-${id}"]`,
    parameterError: (id: string) => `[data-testid="error-${id}"]`,
    
    // File upload
    fileUploadButton: (id: string) => `[data-testid="file-upload-${id}"]`,
    fileInput: (id: string) => `[data-testid="file-input-${id}"]`,
    
    // Action buttons
    generateBtn: '[data-testid="generate-btn"]',
    clearAllBtn: '[data-testid="clear-all-btn"]',
    reloadStencilBtn: '[data-testid="reload-stencil-btn"]',
    refreshStencilBtn: '[data-testid="refresh-stencil-btn"]',
    jsonEditorBtn: '[data-testid="json-editor-btn"]',
    
    // JSON Editor dialog
    jsonTextarea: '[data-testid="json-textarea"]',
    jsonApplyBtn: '[data-testid="json-apply-btn"]',
    jsonCancelBtn: '[data-testid="json-cancel-btn"]',
    
    // Toast notifications
    toast: '[role="alert"]',
    toastTitle: '[role="alert"] [data-testid="toast-title"]',
    toastMessage: '[role="alert"] [data-testid="toast-message"]',
    
    // Loading indicators
    loadingSpinner: '[data-testid="loading"]',
    buttonLoading: (btnId: string) => `[data-testid="${btnId}"][aria-busy="true"]`,
  };
  
  constructor(page: Page) {
    super(page);
  }
  
  /**
   * Navigate to ProMarker v3 page
   */
  async navigate() {
    await this.navigateTo(this.url);
    await this.page.waitForLoadState('networkidle');
    await expect(this.page).toHaveURL(new RegExp(this.url));
    
    // Wait for ProMarker main UI element to appear (handles Lazy/Suspense rendering)
    await this.waitForVisible('[data-testid="category-select"]', 15000);
    
    // Try to wait for initial suggest API call, but don't fail if it doesn't happen
    try {
      await this.waitForApiCall(/\/mapi\/apps\/mste\/api\/suggest/, 5000);
    } catch (error) {
      // API call might not happen automatically - this is OK
      console.log('[ProMarker] Initial suggest API call not detected - continuing');
    }
    
    // Wait for category select to be enabled (populated with data)
    await expect(this.page.locator('[data-testid="category-select"]')).toBeEnabled({ timeout: 15000 });
  }
  
  // ============================================
  // Stencil Selection Methods
  // ============================================
  
  /**
   * Select category from dropdown
   * @param category - Category value to select
   */
  async selectCategory(category: string) {
    await this.waitForVisible(this.selectors.categorySelect);
    await this.page.locator(this.selectors.categorySelect).click();
    // Wait for options to be visible and click by text content
    await this.page.waitForSelector('[role="option"]', { timeout: 5000 });
    const option = this.page.locator(`[role="option"]:has-text("${category}")`).first();
    await option.click();
    await this.waitForApiCall(/\/mapi\/apps\/mste\/api\/suggest/);
  }
  
  /**
   * Select stencil from dropdown
   * @param stencil - Stencil value to select
   */
  async selectStencil(stencil: string) {
    await this.waitForVisible(this.selectors.stencilSelect);
    await this.page.locator(this.selectors.stencilSelect).click();
    await this.page.waitForSelector('[role="option"]', { timeout: 5000 });
    const option = this.page.locator(`[role="option"]:has-text("${stencil}")`).first();
    await option.click();
    await this.waitForApiCall(/\/mapi\/apps\/mste\/api\/suggest/);
  }
  
  /**
   * Select serial number from dropdown
   * @param serial - Serial number to select
   */
  async selectSerial(serial: string) {
    await this.waitForVisible(this.selectors.serialSelect);
    const trigger = this.page.locator(this.selectors.serialSelect);
    await expect(trigger).toBeEnabled({ timeout: 10000 });
    await trigger.click();
    
    await this.page.waitForSelector('[role="option"]', { timeout: 5000 });

    // Prefer exact text match if provided
    if (serial && serial.length > 0) {
      const option = this.page.locator(`[role="option"]:has-text("${serial}")`).first();
      const exists = await option.count();
      if (exists > 0) {
        await option.click();
        return;
      }
    }

    // Fallback: pick the first option
    await this.page.locator('[role="option"]').first().click();
  }

  /**
   * Select the nth option from a Radix Select by its trigger test id
   */
  private async selectByIndex(triggerTestId: string, index = 0) {
    // Ensure the trigger is enabled before clicking
    await expect(this.page.locator(triggerTestId)).toBeEnabled({ timeout: 10000 });
    
    // Click to open dropdown
    await this.page.locator(triggerTestId).click();
    
    // Wait for options to appear
    await this.page.waitForSelector('[role="option"]', { timeout: 10000 });
    
    // Get all options and ensure we have enough
    const options = this.page.locator('[role="option"]');
    const count = await options.count();
    
    if (count === 0) {
      throw new Error(`No options found for select ${triggerTestId}`);
    }
    
    // Use first option if index is out of bounds
    const targetIndex = index >= count ? 0 : index;
    
    // Click the target option
    await options.nth(targetIndex).click();
    
    // Selection takes effect immediately with efficient element waiting
  }

  async selectCategoryByIndex(index = 0) {
    await this.selectByIndex(this.selectors.categorySelect, index);
  }

  async selectStencilByIndex(index = 0) {
    await this.selectByIndex(this.selectors.stencilSelect, index);
  }

  async selectSerialByIndex(index = 0) {
    await this.selectByIndex(this.selectors.serialSelect, index);
  }
  
  /**
   * Get available category options
   */
  async getCategoryOptions(): Promise<string[]> {
    // Open dropdown to get options
    await this.page.locator(this.selectors.categorySelect).click();
    await this.page.waitForSelector('[role="option"]', { timeout: 3000 });
    const options = await this.page.locator('[role="option"]').allTextContents();
    // Close dropdown by clicking outside
    await this.page.keyboard.press('Escape');
    return options.filter(opt => opt.trim() !== '');
  }
  
  /**
   * Get available stencil options
   */
  async getStencilOptions(): Promise<string[]> {
    // Open dropdown to get options
    await this.page.locator(this.selectors.stencilSelect).click();
    await this.page.waitForSelector('[role="option"]', { timeout: 3000 });
    const options = await this.page.locator('[role="option"]').allTextContents();
    // Close dropdown by clicking outside
    await this.page.keyboard.press('Escape');
    return options.filter(opt => opt.trim() !== '');
  }
  
  /**
   * Get available serial options
   */
  async getSerialOptions(): Promise<string[]> {
    // Open dropdown to get options
    await this.page.locator(this.selectors.serialSelect).click();
    await this.page.waitForSelector('[role="option"]', { timeout: 3000 });
    const options = await this.page.locator('[role="option"]').allTextContents();
    // Close dropdown by clicking outside
    await this.page.keyboard.press('Escape');
    return options.filter(opt => opt.trim() !== '');
  }

  /**
   * Select category by text content
   */
  async selectCategoryByText(categoryText: string): Promise<void> {
    await this.page.locator(this.selectors.categorySelect).click();
    await this.page.waitForSelector('[role="option"]', { timeout: 5000 });
    
    const option = this.page.locator(`[role="option"]:has-text("${categoryText}")`);
    const count = await option.count();
    
    if (count === 0) {
      // Log available options for debugging
      const availableOptions = await this.page.locator('[role="option"]').allTextContents();
      throw new Error(`Category "${categoryText}" not found. Available: ${availableOptions.join(', ')}`);
    }
    
    await option.first().click();
    await this.page.waitForTimeout(500); // Wait for stencil dropdown to update
  }

  /**
   * Select stencil by text content
   */
  async selectStencilByText(stencilText: string): Promise<void> {
    await this.page.locator(this.selectors.stencilSelect).click();
    await this.page.waitForSelector('[role="option"]', { timeout: 5000 });
    
    const option = this.page.locator(`[role="option"]:has-text("${stencilText}")`);
    const count = await option.count();
    
    if (count === 0) {
      // Log available options for debugging
      const availableOptions = await this.page.locator('[role="option"]').allTextContents();
      throw new Error(`Stencil "${stencilText}" not found. Available: ${availableOptions.join(', ')}`);
    }
    
    await option.first().click();
    // Wait for serial dropdown to be populated
    await this.page.locator('[data-testid="serial-select"]').waitFor({ state: 'visible' });
  }
  
  /**
   * Get selected category value
   */
  async getSelectedCategory(): Promise<string> {
    const text = await this.page.locator(this.selectors.categorySelect).textContent();
    return text?.trim() || '';
  }
  
  /**
   * Get selected stencil value
   */
  async getSelectedStencil(): Promise<string> {
    const text = await this.page.locator(this.selectors.stencilSelect).textContent();
    return text?.trim() || '';
  }
  
  /**
   * Get selected serial value
   */
  async getSelectedSerial(): Promise<string> {
    const text = await this.page.locator(this.selectors.serialSelect).textContent();
    return text?.trim() || '';
  }
  
  // ============================================
  // Parameter Input Methods
  // ============================================
  
  /**
   * Fill parameter input field
   * @param parameterId - Parameter ID
   * @param value - Value to fill
   */
  async fillParameter(parameterId: string, value: string) {
    const selector = this.selectors.parameterInput(parameterId);
    await this.waitForVisible(selector);
    await this.page.locator(selector).fill(value);
  }
  
  /**
   * Get parameter value
   * @param parameterId - Parameter ID
   */
  async getParameterValue(parameterId: string): Promise<string> {
    const selector = this.selectors.parameterInput(parameterId);
    await this.waitForVisible(selector);
    return await this.page.locator(selector).inputValue();
  }
  
  /**
   * Check if parameter field exists
   * @param parameterId - Parameter ID
   */
  async hasParameter(parameterId: string): Promise<boolean> {
    return await this.isElementVisible(this.selectors.parameterInput(parameterId));
  }
  
  /**
   * Get parameter error message
   * @param parameterId - Parameter ID
   */
  async getParameterError(parameterId: string): Promise<string | null> {
    const selector = this.selectors.parameterError(parameterId);
    if (await this.isElementVisible(selector)) {
      return await this.page.locator(selector).textContent();
    }
    return null;
  }
  
  /**
   * Get all visible parameter IDs
   */
  async getVisibleParameters(): Promise<string[]> {
    const inputs = await this.page.locator('[data-testid^="param-"]').all();
    const paramIds: string[] = [];
    
    for (const input of inputs) {
      const testId = await input.getAttribute('data-testid');
      if (testId && testId.startsWith('param-')) {
        paramIds.push(testId.replace('param-', ''));
      }
    }
    
    return paramIds;
  }
  
  // ============================================
  // File Upload Methods
  // ============================================
  
  /**
   * Upload file for parameter
   * @param parameterId - Parameter ID
   * @param filePath - Path to file to upload
   */
  async uploadFile(parameterId: string, filePath: string) {
    const fileInputSelector = this.selectors.fileInput(parameterId);
    await this.page.setInputFiles(fileInputSelector, filePath);
    await this.waitForApiCall(/\/mapi\/commons\/upload/);
  }
  
  /**
   * Click file upload button
   * @param parameterId - Parameter ID
   */
  async clickFileUploadButton(parameterId: string) {
    const buttonSelector = this.selectors.fileUploadButton(parameterId);
    await this.waitForVisible(buttonSelector);
    await this.page.locator(buttonSelector).click();
  }
  
  // ============================================
  // Action Button Methods
  // ============================================
  
  /**
   * Click generate button
   */
  async clickGenerate() {
    await this.waitForVisible(this.selectors.generateBtn);
    await this.page.locator(this.selectors.generateBtn).click();
  }
  
  /**
   * Click clear all button
   */
  async clickClearAll() {
    await this.waitForVisible(this.selectors.clearAllBtn);
    await this.page.locator(this.selectors.clearAllBtn).click();
  }
  
  /**
   * Click reload stencil master button
   */
  async clickReloadStencilMaster() {
    await this.waitForVisible(this.selectors.reloadStencilBtn);
    await this.page.locator(this.selectors.reloadStencilBtn).click();
    await this.waitForApiCall(/\/mapi\/apps\/mste\/api\/reloadStencilMaster/);
  }
  
  /**
   * Click refresh stencil definition button
   */
  async clickRefreshStencil() {
    await this.waitForVisible(this.selectors.refreshStencilBtn);
    await this.page.locator(this.selectors.refreshStencilBtn).click();
  }
  
  /**
   * Check if generate button is enabled
   */
  async isGenerateButtonEnabled(): Promise<boolean> {
    return await this.page.locator(this.selectors.generateBtn).isEnabled();
  }
  
  // ============================================
  // JSON Editor Methods
  // ============================================
  
  /**
   * Open JSON editor dialog
   */
  async openJsonEditor() {
    await this.waitForVisible(this.selectors.jsonEditorBtn);
    await this.page.locator(this.selectors.jsonEditorBtn).click();
    await this.waitForVisible(this.selectors.jsonTextarea);
  }
  
  /**
   * Get JSON editor content
   */
  async getJsonEditorContent(): Promise<string> {
    await this.waitForVisible(this.selectors.jsonTextarea);
    return await this.page.locator(this.selectors.jsonTextarea).inputValue();
  }
  
  /**
   * Set JSON editor content
   * @param json - JSON string to set
   */
  async setJsonEditorContent(json: string) {
    await this.waitForVisible(this.selectors.jsonTextarea);
    await this.page.locator(this.selectors.jsonTextarea).fill(json);
  }
  
  /**
   * Apply JSON changes
   */
  async applyJsonChanges() {
    await this.waitForVisible(this.selectors.jsonApplyBtn);
    await this.page.locator(this.selectors.jsonApplyBtn).click();
  }
  
  /**
   * Cancel JSON editor
   */
  async cancelJsonEditor() {
    await this.waitForVisible(this.selectors.jsonCancelBtn);
    await this.page.locator(this.selectors.jsonCancelBtn).click();
  }
  
  // ============================================
  // Toast Notification Methods
  // ============================================
  
  /**
   * Wait for toast notification to appear
   */
  async waitForToast(timeout = 5000) {
    await this.page.waitForSelector(this.selectors.toast, { 
      state: 'visible', 
      timeout 
    });
  }
  
  /**
   * Get toast message text
   */
  async getToastMessage(): Promise<string> {
    await this.waitForToast();
    return await this.page.locator(this.selectors.toast).textContent() || '';
  }
  
  /**
   * Check if toast is visible
   */
  async isToastVisible(): Promise<boolean> {
    return await this.isElementVisible(this.selectors.toast);
  }
  
  /**
   * Wait for toast to disappear
   */
  async waitForToastDisappear(timeout = 10000) {
    await this.page.waitForSelector(this.selectors.toast, { 
      state: 'hidden', 
      timeout 
    });
  }
  
  // ============================================
  // Loading State Methods
  // ============================================
  
  /**
   * Wait for loading to complete
   */
  async waitForLoadingComplete() {
    // Wait for any loading spinners to disappear
    const spinner = this.page.locator(this.selectors.loadingSpinner);
    if (await spinner.isVisible().catch(() => false)) {
      await spinner.waitFor({ state: 'hidden', timeout: 10000 });
    }
    
    // Wait for network idle
    await this.page.waitForLoadState('networkidle');
  }
  
  /**
   * Check if button is in loading state
   * @param buttonTestId - Button test ID (e.g., 'generate-btn')
   */
  async isButtonLoading(buttonTestId: string): Promise<boolean> {
    const selector = this.selectors.buttonLoading(buttonTestId);
    return await this.isElementVisible(selector);
  }
  
  // ============================================
  // Complete Workflow Methods
  // ============================================
  
  /**
   * Complete 3-tier selection workflow using index-based selection
   * @param categoryIndex - Category index to select (default: 0)
   * @param stencilIndex - Stencil index to select (default: 0) 
   * @param serialIndex - Serial index to select (default: 0)
   */
  async complete3TierSelectionByIndex(categoryIndex = 0, stencilIndex = 0, serialIndex = 0) {
    await this.selectCategoryByIndex(categoryIndex);
    await this.selectStencilByIndex(stencilIndex);
    await this.selectSerialByIndex(serialIndex);
    await this.waitForLoadingComplete();
  }

  /**
   * Complete 3-tier selection workflow
   * @param category - Category to select
   * @param stencil - Stencil to select
   * @param serial - Serial number to select
   * @deprecated Use complete3TierSelectionByIndex() for Radix Select compatibility
   */
  async complete3TierSelection(category: string, stencil: string, serial: string) {
    // Fallback to index-based selection for Radix compatibility
    await this.complete3TierSelectionByIndex(0, 0, 0);
  }
  
  /**
   * Fill all parameters from object
   * @param parameters - Object with parameter IDs as keys and values
   */
  async fillAllParameters(parameters: Record<string, string>) {
    for (const [paramId, value] of Object.entries(parameters)) {
      if (await this.hasParameter(paramId)) {
        await this.fillParameter(paramId, value);
      }
    }
  }
  
  /**
   * Complete full generation workflow
   * @param category - Category to select
   * @param stencil - Stencil to select  
   * @param serial - Serial number to select
   * @param parameters - Parameters to fill
   */
  async completeGenerationWorkflow(
    category: string, 
    stencil: string, 
    serial: string,
    parameters: Record<string, string>
  ) {
    await this.navigate();
    await this.complete3TierSelection(category, stencil, serial);
    await this.fillAllParameters(parameters);
    await this.clickGenerate();
    await this.waitForApiCall(/\/mapi\/apps\/mste\/api\/generate/);
  }

  /**
   * Setup hello-world stencil for testing - Optimized for parallel execution
   * This ensures we're using the expected stencil with message parameter
   */
  async setupHelloWorldStencil(): Promise<void> {
    await this.navigate();
    
    // Get available categories first to avoid multiple API calls
    const categories = await this.getCategoryOptions();
    const sampleCategory = categories.find(cat => 
      cat.toLowerCase().includes('sample')
    ) || categories.find(cat => cat.includes('サンプル'));
    
    if (!sampleCategory) {
      throw new Error(`Sample category not found. Available: ${categories.join(', ')}`);
    }
    
    // Direct selection without try-catch overhead
    await this.selectCategoryByText(sampleCategory);
    
    // Wait for stencil options to load efficiently
    await expect(this.page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 });
    
    const stencils = await this.getStencilOptions();
    const helloStencil = stencils.find(stencil => 
      stencil.toLowerCase().includes('hello')
    ) || stencils.find(stencil => stencil.includes('Hello World'));
    
    if (!helloStencil) {
      throw new Error(`Hello World stencil not found. Available: ${stencils.join(', ')}`);
    }
    
    await this.selectStencilByText(helloStencil);
    
    // Efficient serial selection
    await expect(this.page.locator('[data-testid="serial-select"]')).toBeEnabled({ timeout: 10000 });
    await this.selectSerialByIndex(0);
    
    // Wait for message parameter with full DOM stability
    const messageInput = this.page.locator('input[name="message"]');
    await expect(messageInput).toBeVisible({ timeout: 15000 });
    await expect(messageInput).toBeEnabled({ timeout: 10000 });
    
    // Ensure input field is fully interactive
    await expect(messageInput).toHaveAttribute('name', 'message');
    
    console.log('[ProMarker] Hello World stencil setup complete with input field ready');
  }
  
  // ============================================
  // Assertion Helpers
  // ============================================
  
  /**
   * Assert page title
   * @param expectedTitle - Expected page title
   */
  async assertPageTitle(expectedTitle: string) {
    await expect(this.page.locator(this.selectors.pageTitle)).toHaveText(expectedTitle);
  }
  
  /**
   * Assert category options count
   * @param expectedCount - Expected number of options
   */
  async assertCategoryOptionsCount(expectedCount: number) {
    const options = await this.getCategoryOptions();
    expect(options.length).toBe(expectedCount);
  }
  
  /**
   * Assert parameter is visible
   * @param parameterId - Parameter ID
   */
  async assertParameterVisible(parameterId: string) {
    await expect(this.page.locator(this.selectors.parameterInput(parameterId))).toBeVisible();
  }
  
  /**
   * Assert generate button is enabled
   */
  async assertGenerateButtonEnabled() {
    await expect(this.page.locator(this.selectors.generateBtn)).toBeEnabled();
  }
  
  /**
   * Assert generate button is disabled
   */
  async assertGenerateButtonDisabled() {
    await expect(this.page.locator(this.selectors.generateBtn)).toBeDisabled();
  }
}
