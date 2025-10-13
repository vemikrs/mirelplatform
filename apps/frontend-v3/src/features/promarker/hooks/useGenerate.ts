import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { ApiRequest, ApiResponse } from '@/lib/api/types'
import type { GenerateRequest, GenerateResult } from '../types/api'
import { handleApiError, handleSuccess } from '@/lib/utils/error'

/**
 * コード生成Hook
 * 
 * ステンシルとパラメータを元にコードを生成し、
 * 生成されたZIPファイルを自動ダウンロードする
 * 
 * @example
 * ```tsx
 * const generateMutation = useGenerate()
 * 
 * const handleGenerate = async () => {
 *   await generateMutation.mutateAsync({
 *     stencilCategoy: selectedCategory,
 *     stencilCanonicalName: selectedStencil,
 *     serialNo: selectedSerial,
 *     // ... dynamic parameters
 *     packageName: 'com.example',
 *     className: 'Sample'
 *   })
 *   // File download starts automatically on success
 * }
 * ```
 */
export function useGenerate() {
  return useMutation({
    mutationFn: async (params: GenerateRequest) => {
      const request: ApiRequest<GenerateRequest> = { content: params }
      
      const response = await apiClient.post<ApiResponse<GenerateResult>>(
        '/apps/mste/api/generate',
        request
      )
      
      return response.data
    },
    onSuccess: (data) => {
      // Handle API-level errors
      if (data.errors && data.errors.length > 0) {
        handleApiError(data.errors)
        return
      }
      
      // Display success messages
      if (data.messages && data.messages.length > 0) {
        handleSuccess(data.messages)
      }
      
      // Auto-download logic
      if (data.data?.files && data.data.files.length > 0) {
        const fileObj = data.data.files[0]
        
        if (fileObj) {
          const entries = Object.entries(fileObj)
          const entry = entries[0]
          
          if (entry) {
            const fileId = entry[0]
            const fileName = entry[1]
            
            console.log(`Downloading: ${fileName} (ID: ${fileId})`)
            
            // Trigger download via backend endpoint
            if (typeof window !== 'undefined') {
              window.location.href = `/mapi/commons/dlsite/${fileId}`
            }
          }
        }
      }
    },
    onError: (error) => {
      console.error('Generate API error:', error)
      handleApiError(['コード生成に失敗しました'])
    },
  })
}
