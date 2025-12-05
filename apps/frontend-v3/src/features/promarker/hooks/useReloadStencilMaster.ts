import { toast } from 'sonner'
import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { ApiRequest, ApiResponse } from '@/lib/api/types'
import { formatError } from '@/lib/utils/error'
import { toastMessages } from '../constants/toastMessages'

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
      toast.success(toastMessages.reloadSuccess.title, {
        description: toastMessages.reloadSuccess.description,
      })
    },
    onError: (error) => {
      console.error('ReloadStencilMaster API error:', error)
      toast.error(toastMessages.reloadError.title, {
        description: formatError(error),
      })
    },
  })
}
