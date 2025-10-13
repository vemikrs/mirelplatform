import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { ApiResponse } from '@/lib/api/types'
import { handleApiError } from '@/lib/utils/error'

/**
 * File upload response structure
 */
interface FileUploadResult {
  fileId: string
  name: string
}

/**
 * ファイルアップロードHook
 * 
 * パラメータがfile型の場合に使用。
 * アップロード完了後、返却されたfileIdをパラメータ値として設定する。
 * 
 * @example
 * ```tsx
 * const uploadMutation = useFileUpload()
 * 
 * const handleFileChange = async (file: File) => {
 *   const result = await uploadMutation.mutateAsync(file)
 *   
 *   if (result.data && result.data.length > 0) {
 *     const fileId = result.data[0].fileId
 *     // Set fileId to parameter value
 *     setParameterValue('configFile', fileId)
 *   }
 * }
 * ```
 */
export function useFileUpload() {
  return useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData()
      formData.append('file', file)
      
      const response = await apiClient.post<ApiResponse<FileUploadResult[]>>(
        '/commons/upload',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      )
      
      return response.data
    },
    onSuccess: (data) => {
      // Handle API-level errors
      if (data.errors && data.errors.length > 0) {
        handleApiError(data.errors)
        return
      }
      
      // Success notification is handled by the component
      if (data.data && data.data.length > 0) {
        const uploadedFile = data.data[0]
        if (uploadedFile) {
          console.log(`File uploaded: ${uploadedFile.name} (ID: ${uploadedFile.fileId})`)
        }
      }
    },
    onError: (error) => {
      console.error('File upload error:', error)
      handleApiError(['ファイルのアップロードに失敗しました'])
    },
  })
}
