import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { apiClient } from '@/lib/api/client'

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
export const useGenerate = () => {
  return useMutation({
    mutationFn: async (params: Record<string, unknown>) => {
      console.log('Generating code with params:', params);
      const response = await apiClient.post('/apps/mste/api/generate', {
        content: params,
      });

      if (response.data.errors?.length > 0) {
        throw new Error(response.data.errors.join(', '));
      }

      return response.data.data;
    },
    onSuccess: (data) => {
      console.log('Generation successful:', data);
      
      toast.success('コードが正常に生成されました！', {
        description: 'ダウンロードを開始しています...',
      });

      // Handle file download
      if (data?.files?.length > 0) {
        // Extract fileId from the response
        const fileEntry = data.files[0];
        const fileId = Object.keys(fileEntry)[0];
        const fileName = fileEntry[fileId as keyof typeof fileEntry];
        
        console.log(`Downloading file: ${fileName} (ID: ${fileId})`);
        
        // Create download link and trigger download
        const downloadUrl = `/commons/dlsite/${fileId}`;
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = fileName;
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        console.log('Download triggered successfully');
      }
      
      // Ensure mutation state is properly reset for next execution
      console.log('Generate mutation completed - ready for next execution');
    },
    onError: (error) => {
      console.error('Generation failed:', error);
      toast.error('コード生成に失敗しました', {
        description: error.message || 'エラーが発生しました',
      });
      
      // Ensure UI state is ready for retry after error
      console.log('Generate mutation failed - ready for retry');
    },
    onSettled: () => {
      // Called after both success and error
      console.log('Generate mutation settled - state should be reset');
    },
  });
};
