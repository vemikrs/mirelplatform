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
 * Provides 3-tier cascading selection:
 * 1. Category (カテゴリ)
 * 2. Stencil (ステンシル)
 * 3. Serial Number (シリアル番号)
 * 
 * Each selection triggers API call to fetch next tier options.
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
    <div className="space-y-4" data-testid="stencil-selector">
      {/* Category Selection */}
      <div className="space-y-2">
        <label htmlFor="category-select" className="block text-sm font-medium">
          カテゴリ <span className="text-destructive">*</span>
        </label>
        <select
          id="category-select"
          data-testid="category-select"
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          value={categories.selected}
          onChange={(e) => onCategoryChange(e.target.value)}
          disabled={disabled || !hasCategories}
        >
          <option value="">選択してください</option>
          {categories.items.map((item) => (
            <option key={item.value} value={item.value}>
              {item.text}
            </option>
          ))}
        </select>
      </div>

      {/* Stencil Selection */}
      <div className="space-y-2">
        <label htmlFor="stencil-select" className="block text-sm font-medium">
          ステンシル <span className="text-destructive">*</span>
        </label>
        <select
          id="stencil-select"
          data-testid="stencil-select"
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          value={stencils.selected}
          onChange={(e) => onStencilChange(e.target.value)}
          disabled={disabled || !hasStencils}
        >
          <option value="">選択してください</option>
          {stencils.items.map((item) => (
            <option key={item.value} value={item.value}>
              {item.text}
            </option>
          ))}
        </select>
        {!hasStencils && categories.selected && (
          <p className="text-xs text-muted-foreground">
            カテゴリを選択後、ステンシルが表示されます
          </p>
        )}
      </div>

      {/* Serial Number Selection */}
      <div className="space-y-2">
        <label htmlFor="serial-select" className="block text-sm font-medium">
          シリアル番号 <span className="text-destructive">*</span>
        </label>
        <select
          id="serial-select"
          data-testid="serial-select"
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
          value={serials.selected}
          onChange={(e) => onSerialChange(e.target.value)}
          disabled={disabled || !hasSerials}
        >
          <option value="">選択してください</option>
          {serials.items.map((item) => (
            <option key={item.value} value={item.value}>
              {item.text}
            </option>
          ))}
        </select>
        {!hasSerials && stencils.selected && (
          <p className="text-xs text-muted-foreground">
            ステンシルを選択後、シリアル番号が表示されます
          </p>
        )}
      </div>
    </div>
  );
}
