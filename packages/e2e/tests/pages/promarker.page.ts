import { Page, Locator } from '@playwright/test';
import { BasePage } from './base.page';

/**
 * Page Object Model for ProMarker (MSTE) page
 * Handles all interactions with the ProMarker interface at /mirel/mste
 */
export class ProMarkerPage extends BasePage {
  // Page URL
  readonly url = '/mirel/mste';
  
  // Main UI elements
  readonly pageTitle = 'ProMarker 払出画面';
  
  // Selectors
  private readonly selectors = {
    // Main container and title
    container: '.space-y-6', // Updated for React structure
    pageTitle: '.container_title', // Use unique class to avoid multiple matches
    
    // Action buttons
    clearStencilBtn: '[data-testid="clear-stencil-btn"]',
    clearAllBtn: '[data-testid="clear-all-btn"]',
    // Broadened to cover migration variants and accessible text
    jsonEditorBtn: [
      '[data-testid="json-edit-btn"]',
      '[data-testid="json-editor-btn"]',
      '[data-testid="json-format-btn"]',
      'button:has-text("JSON")',
      'button:has-text("JSON編集")'
    ].join(', '),
    reloadStencilBtn: '[data-testid="reload-stencil-btn"]',
    
    // Form elements - React uses divs with data-testid instead of form element
    form: '.border.rounded-lg', // Main content container in React
    categorySelect: '[data-testid="category-select"]',
    stencilSelect: '[data-testid="stencil-select"]',
    serialSelect: '[data-testid="serial-select"]',
    generateBtn: '[data-testid="generate-btn"]',
    
    // Parameter inputs (dynamic based on stencil)
    parameterContainer: '.fm_notes',
    parameterInput: (id: string) => `[data-testid="param-${id}"]`,
    fileUploadBtn: '[data-testid="file-upload-btn"]',
    
    // Modals and dialogs - support Radix UI variants
    modal: '[role="dialog"], [role="alertdialog"]',
    modalDialog: '[role="dialog"], [role="alertdialog"]',
    
    // Loading and status indicators
    loadingSpinner: '[data-testid="loading-indicator"]',
    
    // Error and success messages
    alertContainer: '.alert',
    errorMessage: '.alert-danger',
    successMessage: '.alert-success',
    
    // File upload/download
    downloadLink: '[data-testid="download-link"]',
  };
  
  constructor(page: Page) {
    super(page);
  }
  
  /**
   * Navigate to ProMarker page
   */
  async navigate() {
    await this.navigateTo(this.url);
    await this.waitForPageLoad();
  }
  
  /**
   * Wait for page to load completely
   */
  async waitForPageLoad() {
    await this.waitForVisible(this.selectors.pageTitle);
    await this.waitForLoadingComplete();
  }
  
  /**
   * Verify page is loaded correctly
   */
  async verifyPageLoaded() {
    await this.waitForText(this.selectors.pageTitle, this.pageTitle);
    await this.waitForVisible(this.selectors.form);
  }
  
  /**
   * Click the clear stencil definitions button
   */
  async clickClearStencil() {
    await this.clickElement(this.selectors.clearStencilBtn);
    await this.waitForLoadingComplete();
  }
  
  /**
   * Click the clear all button
   */
  async clickClearAll() {
    await this.clickElement(this.selectors.clearAllBtn);
  }
  
  /**
   * Click the JSON editor button (opens modal)
   */
  async clickJsonEditor() {
    const btn = this.page.locator(this.selectors.jsonEditorBtn).first();
    // Wait for the button to be visible and interactable
    await btn.waitFor({ state: 'visible', timeout: 10000 });
    await btn.scrollIntoViewIfNeeded();
    // Trial click helps detect interception without committing the action
    try {
      await btn.click({ trial: true });
    } catch (_) {
      // ignore and proceed with a forced click
    }
    await btn.click({ force: true });
    // Radix UI dialogs may animate/portal in; wait longer and for both dialog/alertdialog
    await this.page.waitForSelector(this.selectors.modal, { state: 'visible', timeout: 10000 });
  }
  
  /**
   * Click the reload stencil master button
   */
  async clickReloadStencilMaster() {
    // Set up API wait before clicking
    const waitForApi = this.waitForApiCall(/\/apps\/mste\/api\/reloadStencilMaster/);
    await this.clickElement(this.selectors.reloadStencilBtn);
    await waitForApi;
    await this.waitForLoadingComplete();
  }
  
  /**
   * Select a category from the dropdown
   * @param categoryValue - Category value to select
   */
  async selectCategory(categoryValue: string) {
    await this.selectOption(this.selectors.categorySelect, categoryValue);
    await this.waitForApiCall(/\/apps\/mste\/api\/suggest/);
    await this.waitForLoadingComplete();
  }
  
  /**
   * Select a stencil from the dropdown
   * @param stencilValue - Stencil value to select
   */
  async selectStencil(stencilValue: string) {
    await this.selectOption(this.selectors.stencilSelect, stencilValue);
    await this.waitForApiCall(/\/apps\/mste\/api\/suggest/);
    await this.waitForLoadingComplete();
  }
  
  /**
   * Fill a parameter input field
   * @param parameterId - Parameter ID
   * @param value - Value to fill
   */
  async fillParameter(parameterId: string, value: string) {
    const selector = this.selectors.parameterInput(parameterId);
    await this.fillInput(selector, value);
  }
  
  /**
   * Fill multiple parameters
   * @param parameters - Object with parameter ID as key and value as value
   */
  async fillParameters(parameters: Record<string, string>) {
    for (const [id, value] of Object.entries(parameters)) {
      await this.fillParameter(id, value);
    }
  }
  
  /**
   * Click the generate button
   */
  async clickGenerate() {
    await this.clickElement(this.selectors.generateBtn);
    await this.waitForApiCall(/\/apps\/mste\/api\/generate/);
    await this.waitForLoadingComplete();
  }
  
  /**
   * Complete stencil selection workflow
   * @param category - Category to select
   * @param stencil - Stencil to select
   * @param parameters - Parameters to fill
   */
  async completeStencilWorkflow(
    category: string, 
    stencil: string, 
    parameters: Record<string, string> = {}
  ) {
    await this.selectCategory(category);
    await this.selectStencil(stencil);
    
    if (Object.keys(parameters).length > 0) {
      await this.fillParameters(parameters);
    }
    
    await this.clickGenerate();
  }
  
  /**
   * Check if generate button is enabled
   */
  async isGenerateButtonEnabled(): Promise<boolean> {
    const button = this.page.locator(this.selectors.generateBtn);
    return await button.isEnabled();
  }
  
  /**
   * Check if any error message is displayed
   */
  async hasErrorMessage(): Promise<boolean> {
    return await this.isElementVisible(this.selectors.errorMessage);
  }
  
  /**
   * Get error message text
   */
  async getErrorMessage(): Promise<string> {
    if (await this.hasErrorMessage()) {
      return await this.getElementText(this.selectors.errorMessage);
    }
    return '';
  }
  
  /**
   * Check if success message is displayed
   */
  async hasSuccessMessage(): Promise<boolean> {
    return await this.isElementVisible(this.selectors.successMessage);
  }
  
  /**
   * Get success message text
   */
  async getSuccessMessage(): Promise<string> {
    if (await this.hasSuccessMessage()) {
      return await this.getElementText(this.selectors.successMessage);
    }
    return '';
  }
  
  /**
   * Check if download link is available
   */
  async isDownloadAvailable(): Promise<boolean> {
    return await this.isElementVisible(this.selectors.downloadLink);
  }
  
  /**
   * Click download link if available
   */
  async clickDownload() {
    if (await this.isDownloadAvailable()) {
      await this.clickElement(this.selectors.downloadLink);
    }
  }
  
  /**
   * Wait for modal to close
   */
  async waitForModalClose() {
    await this.page.waitForSelector(this.selectors.modal, { state: 'hidden' });
  }
  
  /**
   * Close modal if open
   */
  async closeModal() {
    if (await this.isElementVisible(this.selectors.modal)) {
      await this.page.keyboard.press('Escape');
      await this.waitForModalClose();
    }
  }
  
  /**
   * Take screenshot of the current state
   * @param name - Screenshot name suffix
   */
  async takeProMarkerScreenshot(name: string) {
    await this.takeScreenshot(`promarker-${name}`);
  }
}