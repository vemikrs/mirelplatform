import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Button,
  toast,
} from '@mirel/ui'
import { parametersToJson, jsonToParameters } from '../utils/parameter'
import type { DataElement } from '../types/api'
import { toastMessages } from '../constants/toastMessages'

interface JsonEditorProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  category: string
  stencil: string
  serial: string
  parameters: DataElement[]
  onApply: (data: {
    stencilCategory: string
    stencilCd: string
    serialNo: string
    dataElements: Array<{id: string; value: string}>
  }) => Promise<void> | void
}

export function JsonEditor({
  open,
  onOpenChange,
  category,
  stencil,
  serial,
  parameters,
  onApply
}: JsonEditorProps) {
  const [jsonText, setJsonText] = useState('')
  const [isValid, setIsValid] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  
  // モーダルオープン時にJSON生成
  useEffect(() => {
    if (open) {
      const json = parametersToJson(category, stencil, serial, parameters)
      setJsonText(json)
      setIsValid(true)
      setErrorMessage('')
    }
  }, [open, category, stencil, serial, parameters])
  
  // JSON適用処理
  const handleApply = async () => {
    const parsed = jsonToParameters(jsonText)

    if (parsed) {
      try {
        await onApply(parsed)
        onOpenChange(false)
        toast({
          ...toastMessages.jsonApplySuccess,
        })
      } catch (error) {
        toast({
          ...toastMessages.jsonApplyError,
          description: error instanceof Error ? error.message : toastMessages.jsonApplyError.description,
        })
      }
    } else {
      setIsValid(false)
      setErrorMessage('JSONフォーマットが不正です。stencilCategory, stencilCd, serialNo, dataElements が必要です。')
      toast({
        ...toastMessages.jsonApplyError,
        description: 'JSONフォーマットが不正です。stencilCategory, stencilCd, serialNo, dataElements が必要です。',
      })
    }
  }
  
  // テキスト変更時のバリデーション
  const handleTextChange = (value: string) => {
    setJsonText(value)
    setIsValid(true)
    setErrorMessage('')
    
    // リアルタイムバリデーション（optional）
    if (value.trim()) {
      try {
        JSON.parse(value)
      } catch {
        // パース失敗は警告のみ（Apply時に詳細エラー）
      }
    }
  }
  
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[80vh]">
        <DialogHeader>
          <DialogTitle>実行条件（JSON形式）</DialogTitle>
        </DialogHeader>
        
        <div className="space-y-2 flex-1 overflow-auto">
          <label className="block text-sm text-muted-foreground">
            JSON形式で実行条件を編集できます。編集後、Applyボタンで反映してください。
          </label>

          <textarea
            value={jsonText}
            onChange={(e) => handleTextChange(e.target.value)}
            className={`h-96 w-full resize-none rounded-xl border bg-surface-subtle px-4 py-3 font-mono text-sm shadow-inner focus:outline-none focus:ring-2 focus:ring-focus-ring
              ${!isValid ? 'border-destructive/60 focus:ring-destructive' : 'border-outline/50'}`}
            placeholder='{"stencilCategory": "/samples", "stencilCd": "/samples/hello-world", ...}'
            data-testid="json-textarea"
            spellCheck={false}
          />

          {!isValid && errorMessage && (
            <p className="text-sm text-destructive" data-testid="json-error-message">
              {errorMessage}
            </p>
          )}
        </div>
        
        <DialogFooter>
          <Button 
            variant="outline" 
            onClick={() => onOpenChange(false)}
            data-testid="json-cancel-btn"
          >
            Cancel
          </Button>
          <Button 
            onClick={handleApply}
            data-testid="json-apply-btn"
          >
            Apply
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
