import { Button } from '@mirel/ui'
import { Upload } from 'lucide-react'
import { useRef } from 'react'
import { useFileUpload } from '../hooks/useFileUpload'
import { toast } from 'sonner'

interface FileUploadButtonProps {
  parameterId: string
  value: string
  onFileUploaded: (parameterId: string, fileId: string, fileName: string) => void
  disabled?: boolean
}

export function FileUploadButton({
  parameterId,
  value,
  onFileUploaded,
  disabled = false,
}: FileUploadButtonProps) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const uploadMutation = useFileUpload()

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    try {
      const result = await uploadMutation.mutateAsync(file)

      if (result.errors && result.errors.length > 0) {
        result.errors.forEach((error) => {
          toast.error('エラー', {
            description: error,
          })
        })
        return
      }

      if (result.data && result.data.length > 0) {
        const uploadedFile = result.data[0]
        if (uploadedFile) {
          const { fileId, name } = uploadedFile
          onFileUploaded(parameterId, fileId, name)

          toast.success('成功', {
            description: `ファイル "${name}" をアップロードしました`,
          })
        }
      }
    } catch {
      toast.error('エラー', {
        description: 'ファイルアップロードに失敗しました',
      })
    }

    // Reset file input
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleButtonClick = () => {
    fileInputRef.current?.click()
  }

  return (
    <div className="flex items-center gap-2">
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        onChange={handleFileSelect}
        data-testid={`file-input-${parameterId}`}
        disabled={disabled || uploadMutation.isPending}
      />
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={handleButtonClick}
        disabled={disabled || uploadMutation.isPending}
        data-testid={`file-upload-btn-${parameterId}`}
      >
        <Upload className="h-4 w-4" />
        {uploadMutation.isPending ? 'アップロード中...' : 'ファイル選択'}
      </Button>
      {value && (
        <span
          className="text-sm text-muted-foreground"
          data-testid={`file-name-${parameterId}`}
        >
          ファイルID: {value.substring(0, 8)}...
        </span>
      )}
    </div>
  )
}
