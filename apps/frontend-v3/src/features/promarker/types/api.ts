/**
 * ProMarker API Request/Response Types
 * Types for API communication with backend ProMarker service
 * 
 * Based on existing Vue.js implementation:
 * - frontend/pages/mste/index.vue
 * - docs/api-reference.md
 */

/**
 * Value-Text Item for dropdown options
 */
export interface ValueTextItem {
  value: string;
  text: string;
}

/**
 * Value-Text Items collection
 * Contains dropdown options and currently selected value
 */
export interface ValueTextItems {
  items: ValueTextItem[];
  selected: string;
}

/**
 * Stencil Configuration metadata
 */
export interface StencilConfig {
  id: string;
  name: string;
  categoryId: string;
  categoryName: string;
  serial: string;
  lastUpdate: string;
  lastUpdateUser: string;
  description: string | null;
}

/**
 * Data Element (Parameter definition)
 * Describes a single input parameter for the stencil
 */
export interface DataElement {
  id: string;
  name: string;
  valueType: 'text' | 'file';
  value: string;
  placeholder: string;
  note: string;
  nodeType: 'ELEMENT';
}

/**
 * Parameter Tree Structure
 * Contains all parameters for the selected stencil
 */
export interface ParameterTree {
  childs: DataElement[];
  nodeType: 'ROOT';
}

// ============================================
// Suggest API Types
// ============================================

/**
 * Suggest API Request Parameters
 * Used to fetch stencil information and dropdown options
 * 
 * Note: 'stencilCategoy' is a typo in the existing backend API
 * but we must match it exactly for compatibility
 */
export interface SuggestRequest {
  stencilCategoy: string;          // Typo: should be 'Category', but matches backend
  stencilCanonicalName: string;
  serialNo: string;
  selectFirstIfWildcard?: boolean; // * の自動選択を backend に委譲
  // 動的パラメータ（実際は値に number や boolean が来る可能性もあるので unknown）
  [key: string]: string | number | boolean | undefined;           // Allow additional dynamic parameters
}

/**
 * Suggest API Response Model
 * Contains stencil configuration, parameters, and dropdown data
 * 
 * Response structure: ApiResponse<ModelWrapper<SuggestResult>>
 * Access pattern: response.data.data.model
 */
export interface SuggestResult {
  stencil: {
    config: StencilConfig;
  };
  params: ParameterTree;
  fltStrStencilCategory: ValueTextItems;
  fltStrStencilCd: ValueTextItems;
  fltStrSerialNo: ValueTextItems;
}

// ============================================
// Generate API Types
// ============================================

/**
 * Generate API Request Parameters
 * Same as SuggestRequest - all dynamic parameters handled by index signature
 */
export type GenerateRequest = SuggestRequest;

/**
 * Generate API Response Model
 * Contains generated file information
 * 
 * Response structure: ApiResponse<GenerateResult>
 * Access pattern: response.data.data
 * 
 * files format: [{ fileId: fileName }, ...]
 * Example: [{ "abc123-def456": "hello-world-250913A.zip" }]
 */
export interface GenerateResult {
  files: Array<Record<string, string>>;
}

/**
 * Parsed file entry from generate response
 */
export interface GeneratedFile {
  fileId: string;
  fileName: string;
}

/**
 * Helper function to parse files array
 * Converts [{ fileId: fileName }] format to structured objects
 */
export function parseGeneratedFiles(result: GenerateResult): GeneratedFile[] {
  return result.files.map(fileObj => {
    const entry = Object.entries(fileObj)[0];
    if (!entry) {
      throw new Error('Invalid file object format');
    }
    const [fileId, fileName] = entry;
    return { fileId, fileName };
  });
}

// ============================================
// Reload Stencil Master API Types
// ============================================

/**
 * Reload Stencil Master API Request
 * Empty content body as per API specification
 */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type
export interface ReloadStencilMasterRequest {
  // Empty interface - API expects { content: {} }
}

/**
 * Reload Stencil Master API Response
 * Returns null data with success message
 */
export type ReloadStencilMasterResult = null;

// ============================================
// File Upload API Types
// ============================================

/**
 * File Upload Response
 */
export interface FileUploadResult {
  fileId: string;
  name: string;
}

// ============================================
// Default Values and Constants
// ============================================

/**
 * Default values for suggest request (wildcard selection)
 */
export const DEFAULT_SUGGEST_REQUEST: SuggestRequest = {
  stencilCategoy: '*',
  stencilCanonicalName: '*',
  serialNo: '*',
};

/**
 * API endpoint paths (relative to /mapi base)
 */
export const API_ENDPOINTS = {
  suggest: '/apps/mste/api/suggest',
  generate: '/apps/mste/api/generate',
  reloadStencilMaster: '/apps/mste/api/reloadStencilMaster',
  upload: '/commons/upload',
  download: '/commons/download',
  dlsite: (fileId: string) => `/commons/dlsite/${fileId}`,
} as const;
