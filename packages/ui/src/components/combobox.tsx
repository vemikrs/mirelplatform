/**
 * Combobox - プルダウン選択 + 自由入力可能なコンポーネント
 * Select と Input を組み合わせたシンプルな実装
 */
import { useState } from 'react'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './Select'
import { Input } from './Input'

export interface ComboboxOption {
  value: string
  label: string
}

interface ComboboxProps {
  value?: string
  onValueChange?: (value: string) => void
  options: ComboboxOption[]
  placeholder?: string
  emptyText?: string
  searchPlaceholder?: string
  className?: string
  disabled?: boolean
  allowCustom?: boolean  // 自由入力を許可
}

export function Combobox({
  value = '',
  onValueChange,
  options,
  placeholder = '選択してください',
  emptyText = '該当なし',
  className = '',
  disabled = false,
  allowCustom = false,
}: ComboboxProps) {
  const [isCustomInput, setIsCustomInput] = useState(false)
  const [customValue, setCustomValue] = useState(value || '')

  // 選択肢に含まれているか確認
  const isInOptions = options.some(opt => opt.value === value)

  // カスタム入力モードの切り替え
  const handleToggleCustom = () => {
    if (!allowCustom) return
    
    if (isCustomInput) {
      // Select モードに戻る
      setIsCustomInput(false)
      if (customValue && onValueChange) {
        onValueChange(customValue)
      }
    } else {
      // Input モードに切り替え
      setIsCustomInput(true)
      setCustomValue(value || '')
    }
  }

  const handleSelectChange = (newValue: string) => {
    if (newValue === '__custom__') {
      setIsCustomInput(true)
      setCustomValue('')
    } else {
      onValueChange?.(newValue)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value
    setCustomValue(newValue)
    onValueChange?.(newValue)
  }

  if (isCustomInput || (allowCustom && !isInOptions && value)) {
    return (
      <div className={`flex gap-2 ${className}`}>
        <Input
          value={customValue}
          onChange={handleInputChange}
          placeholder={placeholder}
          disabled={disabled}
          className="flex-1"
        />
        {allowCustom && (
          <button
            type="button"
            onClick={handleToggleCustom}
            className="px-3 py-2 text-sm border border-gray-300 rounded hover:bg-gray-50"
            disabled={disabled}
            title="プルダウンに戻る"
          >
            ↓
          </button>
        )}
      </div>
    )
  }

  return (
    <div className={className}>
      <Select value={value} onValueChange={handleSelectChange} disabled={disabled}>
        <SelectTrigger className="w-full">
          <SelectValue placeholder={placeholder} />
        </SelectTrigger>
        <SelectContent>
          {options.length === 0 ? (
            <div className="px-2 py-1.5 text-sm text-gray-500">{emptyText}</div>
          ) : (
            <>
              {options.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
              {allowCustom && (
                <>
                  <div className="border-t my-1" />
                  <SelectItem value="__custom__">
                    ✏️ 手動入力...
                  </SelectItem>
                </>
              )}
            </>
          )}
        </SelectContent>
      </Select>
    </div>
  )
}
