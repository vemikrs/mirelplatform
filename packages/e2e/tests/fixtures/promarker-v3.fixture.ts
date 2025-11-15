/**
 * Test data and fixtures for ProMarker v3 (React/Vite) E2E tests
 */

export interface StencilV3TestData {
  category: string;
  categoryDisplay: string;
  stencil: string;
  stencilDisplay: string;
  serial: string;
  parameters: Record<string, string>;
  description: string;
}

export interface ValidationV3TestData {
  field: string;
  value: string;
  expectedError: string;
  description: string;
}

export interface FileTestData {
  parameterId: string;
  fileName: string;
  fileContent: string;
  description: string;
}

/**
 * Sample stencil test data for frontend-v3
 * These match actual stencils available in the system
 */
export const STENCIL_V3_TEST_DATA: StencilV3TestData[] = [
  {
    category: '/samples',
    categoryDisplay: 'Sample Stencils',
    stencil: '/samples/hello-world',
    stencilDisplay: 'Hello World Generator',
    serial: '250913A',
    parameters: {
      'message': 'Hello, E2E Test!',
      'userName': 'Playwright',
      'language': 'ja'
    },
    description: 'Hello World stencil with Japanese parameters'
  },
  {
    category: '/samples',
    categoryDisplay: 'Sample Stencils',
    stencil: '/samples/hello-world',
    stencilDisplay: 'Hello World Generator',
    serial: '250913A',
    parameters: {
      'message': 'Test Message',
      'userName': 'TestUser',
      'language': 'en'
    },
    description: 'Hello World stencil with English parameters'
  },
];

/**
 * Validation test cases for form fields
 */
export const VALIDATION_V3_TEST_DATA: ValidationV3TestData[] = [
  {
    field: 'message',
    value: '',
    expectedError: 'required',
    description: 'Empty message should show required error'
  },
  {
    field: 'userName',
    value: '',
    expectedError: 'required',
    description: 'Empty username should show required error'
  },
];

/**
 * File upload test data
 */
export const FILE_TEST_DATA: FileTestData[] = [
  {
    parameterId: 'configFile',
    fileName: 'test-file.txt',
    fileContent: 'This is a test file for E2E testing.\nLine 2\nLine 3',
    description: 'Simple text file for upload testing'
  },
  {
    parameterId: 'dataFile',
    fileName: 'test-data.json',
    fileContent: JSON.stringify({ test: 'data', value: 123 }, null, 2),
    description: 'JSON file for upload testing'
  },
];

/**
 * JSON editor test data
 */
export const JSON_EDITOR_TEST_DATA = {
  valid: {
    stencilCategoy: '/samples',
    stencilCanonicalName: '/samples/hello-world',
    serialNo: '250913A',
    message: 'JSON Test',
    userName: 'JSON User',
    language: 'ja'
  },
  invalid: '{ invalid json',
  empty: '{}'
};

/**
 * API response mock data for error testing
 */
export const API_MOCK_RESPONSES = {
  suggest: {
    success: {
      data: {
        model: {
          stencil: {
            config: {
              id: '/samples/hello-world',
              name: 'Hello World Generator',
              categoryId: '/samples',
              categoryName: 'Sample Stencils',
              serial: '250913A',
              lastUpdate: '2025/09/13',
              lastUpdateUser: 'mirelplatform',
              description: 'Test stencil'
            }
          },
          params: {
            childs: [
              {
                id: 'message',
                name: 'Message',
                valueType: 'text',
                value: '',
                placeholder: 'Enter message',
                note: 'Test message',
                nodeType: 'ELEMENT'
              }
            ],
            nodeType: 'ROOT'
          },
          fltStrStencilCategory: {
            items: [
              { value: '/samples', text: 'Sample Stencils' }
            ],
            selected: '/samples'
          },
          fltStrStencilCd: {
            items: [
              { value: '/samples/hello-world', text: 'Hello World Generator' }
            ],
            selected: '/samples/hello-world'
          },
          fltStrSerialNo: {
            items: [
              { value: '250913A', text: '250913A' }
            ],
            selected: '250913A'
          }
        }
      },
      messages: [],
      errors: []
    },
    error: {
      data: null,
      messages: [],
      errors: ['サーバーエラーが発生しました']
    }
  },
  generate: {
    success: {
      data: {
        files: [
          { 'abc123-def456-789': 'hello-world-250913A.zip' }
        ]
      },
      messages: ['コード生成に成功しました'],
      errors: []
    },
    error: {
      data: null,
      messages: [],
      errors: ['コード生成に失敗しました']
    }
  },
  upload: {
    success: {
      data: [
        { fileId: 'test-file-id-123', name: 'test-file.txt' }
      ],
      messages: [],
      errors: []
    },
    error: {
      data: null,
      messages: [],
      errors: ['ファイルアップロードに失敗しました']
    }
  }
};

/**
 * UI Element selectors for frontend-v3
 */
export const V3_SELECTORS = {
  // Page elements
  pageTitle: 'h1',
  
  // Stencil selection
  categorySelect: '[data-testid="category-select"]',
  stencilSelect: '[data-testid="stencil-select"]',
  serialSelect: '[data-testid="serial-select"]',
  
  // Parameters
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
  
  // JSON Editor
  jsonTextarea: '[data-testid="json-textarea"]',
  jsonApplyBtn: '[data-testid="json-apply-btn"]',
  jsonCancelBtn: '[data-testid="json-cancel-btn"]',
  
  // Toast
  toast: '[role="alert"]',
  toastTitle: '[data-testid="toast-title"]',
  toastMessage: '[data-testid="toast-message"]',
  
  // Loading
  loadingSpinner: '[data-testid="loading"]',
};

/**
 * API endpoints for frontend-v3
 */
export const V3_API_ENDPOINTS = {
  suggest: '/mapi/apps/mste/api/suggest',
  generate: '/mapi/apps/mste/api/generate',
  reloadStencilMaster: '/mapi/apps/mste/api/reloadStencilMaster',
  upload: '/mapi/commons/upload',
  download: '/mapi/commons/download',
  dlsite: (fileId: string) => `/mapi/commons/dlsite/${fileId}`
};

/**
 * Test timeouts (in milliseconds)
 */
export const V3_TIMEOUTS = {
  short: 5000,
  medium: 10000,
  long: 30000,
  apiCall: 15000,
  fileOperation: 20000,
  download: 30000
};

/**
 * Expected page elements for verification
 */
export const V3_PAGE_ELEMENTS = {
  title: 'ProMarker 払出画面',
  buttons: [
    'Generate',
    'Clear All',
    'Reload Stencil Master',
    'Refresh Stencil Definition',
    'JSON Editor'
  ]
};

/**
 * Browser viewport configurations for responsive testing
 */
export const V3_VIEWPORT_CONFIGS = {
  desktop: { width: 1280, height: 720 },
  tablet: { width: 768, height: 1024 },
  mobile: { width: 375, height: 667 }
};

/**
 * Expected file types for download testing
 */
export const V3_EXPECTED_FILE_TYPES = [
  '.zip',
  '.java',
  '.xml',
  '.properties',
  '.md',
  '.txt'
];

/**
 * Performance thresholds for frontend-v3
 */
export const V3_PERFORMANCE_THRESHOLDS = {
  initialLoad: 3000,      // < 3 seconds
  apiCall: 1000,          // < 1 second
  fileUpload: 5000,       // < 5 seconds
  codeGeneration: 10000,  // < 10 seconds
};

/**
 * Accessibility requirements
 */
export const V3_A11Y_REQUIREMENTS = {
  wcagLevel: 'AA',
  rules: [
    'color-contrast',
    'label',
    'button-name',
    'link-name',
    'image-alt',
    'aria-required-attr',
    'aria-valid-attr-value',
  ]
};
