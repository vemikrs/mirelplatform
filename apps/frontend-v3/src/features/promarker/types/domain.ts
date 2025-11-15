/**
 * ProMarker Domain Model Types
 * Business logic and UI state types
 * 
 * These types represent the application state and are independent of API structure
 */

import type { DataElement, StencilConfig, ValueTextItem } from './api';

/**
 * Form field state for parameter input
 */
export interface ParameterField extends DataElement {
  error?: string;
  touched?: boolean;
}

/**
 * Stencil selection state
 * Represents the current 3-tier selection (Category → Stencil → Serial)
 */
export interface StencilSelection {
  categoryId: string;
  categoryName: string;
  stencilId: string;
  stencilName: string;
  serialNo: string;
}

/**
 * Available options for dropdowns
 */
export interface StencilOptions {
  categories: ValueTextItem[];
  stencils: ValueTextItem[];
  serials: ValueTextItem[];
}

/**
 * Complete stencil information including configuration and parameters
 */
export interface StencilInfo {
  config: StencilConfig;
  parameters: ParameterField[];
}

/**
 * Form values for code generation
 * Contains all parameter values ready to submit
 */
export interface GenerationFormValues {
  stencilCategoy: string;     // Matches API typo
  stencilCanonicalName: string;
  serialNo: string;
  [parameterId: string]: string;  // Dynamic parameters
}

/**
 * Code generation result
 */
export interface GenerationResult {
  fileId: string;
  fileName: string;
  downloadUrl: string;
}

/**
 * Application state for ProMarker page
 */
export interface ProMarkerState {
  // Selection state
  selection: StencilSelection | null;
  options: StencilOptions;
  
  // Stencil information
  stencilInfo: StencilInfo | null;
  
  // Form state
  parameters: Record<string, ParameterField>;
  
  // Loading states
  isLoadingOptions: boolean;
  isLoadingStencil: boolean;
  isGenerating: boolean;
  
  // Error state
  error: string | null;
}

/**
 * Selection change type
 */
export type SelectionLevel = 'category' | 'stencil' | 'serial';

/**
 * Helper function to create empty stencil selection
 */
export function createEmptySelection(): StencilSelection {
  return {
    categoryId: '',
    categoryName: '',
    stencilId: '',
    stencilName: '',
    serialNo: '',
  };
}

/**
 * Helper function to create empty stencil options
 */
export function createEmptyOptions(): StencilOptions {
  return {
    categories: [],
    stencils: [],
    serials: [],
  };
}

/**
 * Helper function to create initial ProMarker state
 */
export function createInitialState(): ProMarkerState {
  return {
    selection: null,
    options: createEmptyOptions(),
    stencilInfo: null,
    parameters: {},
    isLoadingOptions: false,
    isLoadingStencil: false,
    isGenerating: false,
    error: null,
  };
}

/**
 * Helper function to check if selection is complete
 */
export function isSelectionComplete(selection: StencilSelection | null): boolean {
  return Boolean(
    selection &&
    selection.categoryId &&
    selection.stencilId &&
    selection.serialNo
  );
}

/**
 * Helper function to convert parameter fields to form values
 */
export function parametersToFormValues(
  selection: StencilSelection,
  parameters: Record<string, ParameterField>
): GenerationFormValues {
  const formValues: GenerationFormValues = {
    stencilCategoy: selection.categoryId,
    stencilCanonicalName: selection.stencilId,
    serialNo: selection.serialNo,
  };
  
  // Add all parameter values
  Object.entries(parameters).forEach(([id, field]) => {
    formValues[id] = field.value;
  });
  
  return formValues;
}

/**
 * Helper function to validate required parameters
 */
export function validateParameters(
  parameters: Record<string, ParameterField>
): Record<string, string> {
  const errors: Record<string, string> = {};
  
  Object.entries(parameters).forEach(([id, field]) => {
    // Check if required field is empty
    // Note: Backend doesn't currently mark fields as required,
    // but we can add validation based on placeholder or note
    if (!field.value && field.placeholder && field.placeholder.includes('必須')) {
      errors[id] = `${field.name}は必須項目です`;
    }
  });
  
  return errors;
}
