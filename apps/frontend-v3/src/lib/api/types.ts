/**
 * API Base Types
 * Common types used across all API endpoints
 */

/**
 * Standard API Request wrapper
 * All API requests must wrap their payload in a 'content' field
 */
export interface ApiRequest<T> {
  content: T;
}

/**
 * Standard API Response wrapper
 * All API responses follow this structure
 */
export interface ApiResponse<T> {
  data: T | null;
  messages: string[];
  errors: string[];
}

/**
 * ModelWrapper structure (used by Suggest API only)
 * FIXME: Temporary measure to match existing backend structure
 * 
 * Response structure: ApiResponse<ModelWrapper<SuggestResult>>
 * Access pattern: response.data.data.model
 */
export interface ModelWrapper<T> {
  model: T;
}

/**
 * Type guard to check if response contains errors
 */
export function hasErrors<T>(response: ApiResponse<T>): boolean {
  return response.errors !== null && response.errors.length > 0;
}

/**
 * Type guard to check if response contains messages
 */
export function hasMessages<T>(response: ApiResponse<T>): boolean {
  return response.messages !== null && response.messages.length > 0;
}

/**
 * Type guard to check if response has data
 */
export function hasData<T>(response: ApiResponse<T>): response is ApiResponse<T> & { data: T } {
  return response.data !== null;
}

/**
 * Extract model from ModelWrapper response
 * Used specifically for Suggest API responses
 */
export function extractModel<T>(
  response: ApiResponse<ModelWrapper<T>>
): T | null {
  if (hasData(response) && response.data.model) {
    return response.data.model;
  }
  return null;
}
