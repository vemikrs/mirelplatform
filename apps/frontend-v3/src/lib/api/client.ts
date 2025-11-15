import axios, { AxiosError, type AxiosResponse } from 'axios';
import type { ApiResponse } from './types';

/**
 * Axios client configuration for ProMarker API
 * 
 * Base URL: /mapi (proxied to http://localhost:3000 by Vite)
 * Headers: Content-Type: application/json
 * 
 * Error handling:
 * - Network errors: Logged and re-thrown
 * - HTTP errors: Logged with status code
 * - API errors: Passed through in response.errors array
 */
export const apiClient = axios.create({
  baseURL: '/mapi',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 seconds
});

/**
 * Request interceptor
 * - Logs all requests in development mode
 * - Can add authentication tokens here
 */
apiClient.interceptors.request.use(
  (config) => {
    if (import.meta.env.DEV) {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, config.data);
    }
    return config;
  },
  (error) => {
    console.error('[API Request Error]', error);
    return Promise.reject(error);
  }
);

/**
 * Response interceptor
 * - Logs responses in development mode
 * - Handles global error scenarios
 * - Does NOT throw on API-level errors (errors array)
 */
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    if (import.meta.env.DEV) {
      console.log(`[API Response] ${response.config.url}`, response.data);
    }
    
    // Check for API-level errors
    if (response.data.errors && response.data.errors.length > 0) {
      console.warn('[API Errors]', response.data.errors);
    }
    
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    // Network error or HTTP error
    if (error.response) {
      // HTTP error (4xx, 5xx)
      console.error('[API HTTP Error]', {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data,
      });
      
      // If backend returned ApiResponse structure, pass it through
      if (error.response.data && 'errors' in error.response.data) {
        return Promise.reject(error);
      }
    } else if (error.request) {
      // Network error (no response received)
      console.error('[API Network Error]', {
        message: error.message,
        code: error.code,
      });
    } else {
      // Request setup error
      console.error('[API Request Setup Error]', error.message);
    }
    
    return Promise.reject(error);
  }
);

/**
 * Helper function to create API request body
 * Wraps payload in 'content' field as required by backend
 */
export function createApiRequest<T>(content: T) {
  return { content };
}

/**
 * Helper function to handle API response errors
 * Returns errors array if present, otherwise generic error message
 */
export function getApiErrors(error: unknown): string[] {
  if (axios.isAxiosError(error)) {
    const apiResponse = error.response?.data as ApiResponse<unknown> | undefined;
    if (apiResponse?.errors && apiResponse.errors.length > 0) {
      return apiResponse.errors;
    }
    
    // Network or HTTP error
    if (error.response) {
      return [`HTTP Error ${error.response.status}: ${error.response.statusText}`];
    } else if (error.request) {
      return ['ネットワークエラーが発生しました。接続を確認してください。'];
    }
  }
  
  return ['予期しないエラーが発生しました。'];
}
