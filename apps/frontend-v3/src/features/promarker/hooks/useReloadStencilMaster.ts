import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { apiClient } from '@/lib/api/client'
import type { ApiRequest, ApiResponse } from '@/lib/api/types'
import { handleApiError } from '@/lib/utils/error'

/**
 * ステンシルマスタ再読み込みHook
 * 
 * クラスパスとデータベースからステンシル定義を再読み込みする。
 * 新しいステンシルを追加した場合や、定義を更新した場合に使用。
 * 
 * @example
 * ```tsx
 * const reloadMutation = useReloadStencilMaster()
 * 
 * const handleReload = async () => {
 *   await reloadMutation.mutateAsync()
 *   // Reload complete, re-fetch stencil list
 * }
 * ```
 */
export function useReloadStencilMaster() {
  return useMutation({
    mutationFn: async () => {
      const request: ApiRequest<Record<string, never>> = { content: {} }
      
      const response = await apiClient.post<ApiResponse<null>>(
        '/apps/mste/api/reloadStencilMaster',
        request
      )
      
      return response.data
    },
    onSuccess: () => {
      // Handle success - UI will be updated by calling component
      toast.success('ステンシルマスタを再読み込みしました')
    },
    onError: (error) => {
      console.error('ReloadStencilMaster API error:', error)
      handleApiError(['ステンシルマスタの再読み込みに失敗しました'])
    },
  })
}
