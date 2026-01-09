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
  withCredentials: true, // Enable Cookie-based authentication
});

/**
 * Handle logout: clear auth state and redirect to login
 */
async function handleLogout() {
  if (window.location.pathname.startsWith('/login') || window.location.pathname.startsWith('/auth/')) {
    return; // Already on login page, don't redirect
  }
  
  const { useAuthStore } = await import('@/stores/authStore');
  const { clearAuth } = useAuthStore.getState();
  clearAuth();
  
  const currentPath = window.location.pathname + window.location.search;
  console.log('[Auth] Logging out, redirecting to login', { returnUrl: currentPath });
  window.location.href = `/login?returnUrl=${encodeURIComponent(currentPath)}`;
}

/**
 * Request interceptor
 * - Logs all requests in development mode
 * - Adds Authorization header with JWT token if available
 */
apiClient.interceptors.request.use(
  async (config) => {
    // Dynamically import authStore to get JWT token
    const { useAuthStore } = await import('@/stores/authStore');
    const tokens = useAuthStore.getState().tokens;
    
    if (tokens?.accessToken) {
      config.headers.Authorization = `Bearer ${tokens.accessToken}`;
    }
    
    if (import.meta.env.DEV) {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, {
        data: config.data,
        withCredentials: config.withCredentials,
        hasAuthHeader: !!config.headers.Authorization,
        cookies: document.cookie || '(no cookies visible - may be HttpOnly)',
      });
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
 * - Handles 401 Unauthorized globally by clearing auth and redirecting to login
 */
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    if (import.meta.env.DEV) {
      console.log(`[API Response] ${response.config.url}`, {
        status: response.status,
        data: response.data,
        setCookieHeader: response.headers['set-cookie'] || '(none)',
      });
    }
    
    // Check for API-level errors
    if (response.data.errors && response.data.errors.length > 0) {
      console.warn('[API Errors]', response.data.errors);
    }
    
    return response;
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    // Handle 401 Unauthorized with auto-refresh
    if (error.response?.status === 401) {
      // Check if redirect should be skipped (e.g. for authLoader requests)
      if (error.config && error.config.headers && error.config.headers['X-Mirel-Skip-Auth-Redirect']) {
        console.log('[401 Unauthorized] Skipping global redirect due to header');
        return Promise.reject(error);
      }

      // Skip auto-refresh if already retried
      if (error.config && (error.config as any)._retry) {
        console.log('[401 Unauthorized] Already retried, clearing auth');
        await handleLogout();
        return Promise.reject(error);
      }

      // Try to refresh token
      console.log('[401 Unauthorized] Attempting token refresh...');
      try {
        const { useAuthStore } = await import('@/stores/authStore');
        const { refreshAccessToken } = useAuthStore.getState();
        
        const refreshed = await refreshAccessToken();
        
        if (refreshed && error.config) {
          // Mark as retried to prevent infinite loop
          (error.config as any)._retry = true;
          
          // Update Authorization header with new token
          const tokens = useAuthStore.getState().tokens;
          if (tokens?.accessToken) {
            error.config.headers.Authorization = `Bearer ${tokens.accessToken}`;
          }
          
          console.log('[401 Unauthorized] Token refreshed, retrying request');
          return apiClient(error.config);
        }
      } catch (refreshError) {
        console.error('[401 Unauthorized] Refresh failed:', refreshError);
      }

      // Refresh failed or not available, logout
      await handleLogout();
      return Promise.reject(error);
    }

    
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
export function createApiRequest<T>(model: T) {
  return { model };
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
