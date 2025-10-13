/**
 * Error handling utilities for API responses and user notifications
 */

/**
 * Display API errors as toast notifications
 * 
 * @param errors - Array of error messages from API response
 * 
 * @example
 * ```tsx
 * const response = await apiClient.post('/api/endpoint', data)
 * 
 * if (response.data.errors?.length) {
 *   handleApiError(response.data.errors)
 * }
 * ```
 */
export function handleApiError(errors: string[] | undefined): void {
  if (!errors || errors.length === 0) return

  // TODO: Integrate with toast notification library (@mirel/ui or sonner)
  // For now, log to console and use alert
  errors.forEach((error) => {
    console.error('API Error:', error)
    
    // Temporary: Use alert (will be replaced with toast in Step 9)
    if (typeof window !== 'undefined') {
      alert(`エラー: ${error}`)
    }
  })
}

/**
 * Display success messages as toast notifications
 * 
 * @param messages - Array of success messages from API response
 * 
 * @example
 * ```tsx
 * const response = await apiClient.post('/api/endpoint', data)
 * 
 * if (response.data.messages?.length) {
 *   handleSuccess(response.data.messages)
 * }
 * ```
 */
export function handleSuccess(messages: string[] | undefined): void {
  if (!messages || messages.length === 0) return

  // TODO: Integrate with toast notification library
  messages.forEach((message) => {
    console.log('Success:', message)
    
    // Temporary: Use alert (will be replaced with toast in Step 9)
    if (typeof window !== 'undefined') {
      alert(`成功: ${message}`)
    }
  })
}

/**
 * Format error message for display
 * 
 * @param error - Unknown error object from catch block
 * @returns Formatted error message string
 */
export function formatError(error: unknown): string {
  if (error instanceof Error) {
    return error.message
  }
  
  if (typeof error === 'string') {
    return error
  }
  
  return 'An unexpected error occurred'
}
