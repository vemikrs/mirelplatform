import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { ApiRequest, ApiResponse, ModelWrapper } from '@/lib/api/types'
import type { SuggestRequest, SuggestResult } from '../types/api'
import { handleApiError } from '@/lib/utils/error'

/**
 * ステンシル情報取得Hook
 * 
 * カテゴリ/ステンシル/シリアル番号選択時に呼び出され、
 * 次の選択肢とパラメータ情報を取得する
 * 
 * @example
 * ```tsx
 * const suggestMutation = useSuggest()
 * 
 * const handleCategoryChange = async (category: string) => {
 *   const result = await suggestMutation.mutateAsync({
 *     stencilCategoy: category,
 *     stencilCanonicalName: '*',
 *     serialNo: '*'
 *   })
 *   
 *   if (result.data?.model) {
 *     // Update dropdowns with result.data.model.fltStrStencilCd, etc.
 *   }
 * }
 * ```
 */
export function useSuggest() {
  return useMutation({
    mutationFn: async (params: SuggestRequest) => {
      const request: ApiRequest<SuggestRequest> = { content: params }
      
      const response = await apiClient.post<ApiResponse<ModelWrapper<SuggestResult>>>(
        '/apps/mste/api/suggest',
        request
      )
      
      return response.data
    },
    onError: (error) => {
      console.error('Suggest API error:', error)
      handleApiError(['ステンシル情報の取得に失敗しました'])
    },
    onSuccess: (data) => {
      // Handle API-level errors
      if (data.errors && data.errors.length > 0) {
        handleApiError(data.errors)
      }
    },
  })
}
