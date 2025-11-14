import {
  FormField,
  FormHelper,
  FormLabel,
  FormRequiredMark,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@mirel/ui';

export interface ValueTextItem {
  value: string;
  text: string;
}

export interface ValueTextItems {
  items: ValueTextItem[];
  selected: string;
}

interface StencilSelectorProps {
  categories: ValueTextItems;
  stencils: ValueTextItems;
  serials: ValueTextItems;
  onCategoryChange: (value: string) => void;
  onStencilChange: (value: string) => void;
  onSerialChange: (value: string) => void;
  disabled?: boolean;
}

/**
 * StencilSelector Component
 *
 * Provides 3-tier cascading selection with design-system components.
 */
export function StencilSelector({
  categories,
  stencils,
  serials,
  onCategoryChange,
  onStencilChange,
  onSerialChange,
  disabled = false,
}: StencilSelectorProps) {
  const hasCategories = categories.items.length > 0;
  const hasStencils = stencils.items.length > 0 && categories.selected;
  const hasSerials = serials.items.length > 0 && stencils.selected;

  return (
    <div className="grid grid-cols-1 gap-5" data-testid="stencil-selector">
      <FormField>
        <FormLabel requiredMark={<FormRequiredMark />} htmlFor="category-select">
          カテゴリ
        </FormLabel>
        <Select
          value={categories.selected}
          onValueChange={onCategoryChange}
          disabled={disabled || !hasCategories}
        >
          <SelectTrigger id="category-select" data-testid="category-select">
            <SelectValue placeholder={hasCategories ? 'カテゴリを選択' : 'ロード中...'} />
          </SelectTrigger>
          <SelectContent>
            {categories.items.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.text}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <FormHelper>カテゴリを選択すると利用可能なステンシルが表示されます。</FormHelper>
      </FormField>

      <FormField>
        <FormLabel requiredMark={<FormRequiredMark />} htmlFor="stencil-select">
          ステンシル
        </FormLabel>
        <Select
          value={stencils.selected}
          onValueChange={onStencilChange}
          disabled={disabled || !hasStencils}
        >
          <SelectTrigger id="stencil-select" data-testid="stencil-select">
            <SelectValue placeholder={hasStencils ? 'ステンシルを選択' : 'カテゴリを選択してください'} />
          </SelectTrigger>
          <SelectContent>
            {stencils.items.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.text}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {!hasStencils && categories.selected ? (
          <FormHelper>カテゴリ選択後に利用可能なステンシルが展開されます。</FormHelper>
        ) : null}
      </FormField>

      <FormField>
        <FormLabel requiredMark={<FormRequiredMark />} htmlFor="serial-select">
          シリアル番号
        </FormLabel>
        <Select
          value={serials.selected}
          onValueChange={onSerialChange}
          disabled={disabled || !hasSerials}
        >
          <SelectTrigger id="serial-select" data-testid="serial-select">
            <SelectValue placeholder={hasSerials ? 'シリアル番号を選択' : 'ステンシルを選択してください'} />
          </SelectTrigger>
          <SelectContent>
            {serials.items.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.text}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {!hasSerials && stencils.selected ? (
          <FormHelper>ステンシル選択後にシリアル番号候補が表示されます。</FormHelper>
        ) : null}
      </FormField>
    </div>
  );
}
