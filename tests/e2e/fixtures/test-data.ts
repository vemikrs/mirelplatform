/**
 * Test data and fixtures for ProMarker E2E tests
 */

export interface StencilTestData {
  category: string;
  stencil: string;
  parameters: Record<string, string>;
  description: string;
}

export interface ValidationTestData {
  field: string;
  value: string;
  expectedError: string;
  description: string;
}

/**
 * Sample stencil test data
 * These match actual stencils available in the system
 */
export const STENCIL_TEST_DATA: StencilTestData[] = [
  {
    category: 'samples',
    stencil: 'hello-world',
    parameters: {
      'message': 'Hello, E2E Test!',
      'userName': 'Playwright',
      'language': 'ja'
    },
    description: 'Hello World stencil with real test data'
  },
  {
    category: 'samples',
    stencil: 'hello-world',
    parameters: {
      'message': 'Test Message',
      'userName': 'TestUser',
      'language': 'en'
    },
    description: 'Hello World stencil with English language'
  },
];

/**
 * Validation test cases
 */
export const VALIDATION_TEST_DATA: ValidationTestData[] = [
  {
    field: 'message',
    value: '',
    expectedError: 'メッセージが必要です',
    description: 'Empty message validation'
  },
  {
    field: 'userName',
    value: '',
    expectedError: 'ユーザー名が必要です',
    description: 'Empty username validation'
  },
];

/**
 * UI Element selectors that may need data-test-id attributes
 */
export const SELECTORS_NEEDING_TEST_IDS = [
  {
    current: '[data-testid="category-select"]',
    suggested: '[data-test-id="category-select"]',
    description: 'Category selection dropdown'
  },
  {
    current: '[data-testid="stencil-select"]',
    suggested: '[data-test-id="stencil-select"]',
    description: 'Stencil selection dropdown'
  },
  {
    current: '[data-testid="generate-button"]',
    suggested: '[data-test-id="generate-button"]',
    description: 'Generate button'
  },
  {
    current: '[data-testid="download-link"]',
    suggested: '[data-test-id="download-link"]',
    description: 'Download link'
  }
];

/**
 * API endpoints used in tests
 */
export const API_ENDPOINTS = {
  suggest: '/mapi/apps/mste/api/suggest',
  generate: '/mapi/apps/mste/api/generate',
  upload: '/mapi/commons/upload',
  download: '/mapi/commons/download',
  dlsite: '/mapi/commons/dlsite'
};

/**
 * Test timeouts (in milliseconds)
 */
export const TIMEOUTS = {
  short: 5000,
  medium: 10000,
  long: 30000,
  apiCall: 15000,
  fileOperation: 20000
};

/**
 * Expected page elements for verification
 */
export const PAGE_ELEMENTS = {
  title: 'ProMarker 払出画面',
  buttons: [
    '📄ステンシル定義を再取得',
    '📄全てクリア',
    '📎Json形式',
    '📄ステンシルマスタをリロード',
    'Generate'
  ]
};

/**
 * Browser viewport configurations for responsive testing
 */
export const VIEWPORT_CONFIGS = {
  desktop: { width: 1280, height: 720 },
  tablet: { width: 768, height: 1024 },
  mobile: { width: 375, height: 667 }
};

/**
 * File types for download testing
 */
export const EXPECTED_FILE_TYPES = [
  '.zip',
  '.java',
  '.xml',
  '.properties',
  '.md'
];