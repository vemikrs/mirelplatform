import { useState, useEffect } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter
} from '@mirel/ui'
import { Button } from '@mirel/ui'
import { toast } from 'sonner'
import { parametersToJson, jsonToParameters } from '../utils/parameter'
import type { DataElement } from '../types/api'

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
  }) => void
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
  const handleApply = () => {
    const parsed = jsonToParameters(jsonText)
    
    if (parsed) {
      onApply(parsed)
      onOpenChange(false)
      toast.success('JSONを適用しました')
    } else {
      setIsValid(false)
      setErrorMessage('JSONフォーマットが不正です。stencilCategory, stencilCd, serialNo, dataElements が必要です。')
      toast.error('JSONフォーマットが不正です')
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
          <label className="text-sm text-muted-foreground block">
            JSON形式で実行条件を編集できます。編集後、Applyボタンで反映してください。
          </label>
          
          <textarea
            value={jsonText}
            onChange={(e) => handleTextChange(e.target.value)}
            className={`w-full h-96 p-4 font-mono text-sm border rounded-md resize-none
              ${!isValid ? 'border-red-500 focus:ring-red-500' : 'border-gray-300'}
              focus:outline-none focus:ring-2 focus:ring-offset-2`}
            placeholder='{"stencilCategory": "/samples", "stencilCd": "/samples/hello-world", ...}'
            data-testid="json-textarea"
            spellCheck={false}
          />
          
          {!isValid && errorMessage && (
            <p className="text-sm text-red-500" data-testid="json-error-message">
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
