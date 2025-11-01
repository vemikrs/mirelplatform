interface ActionButtonsProps {
  onGenerate: () => void;
  onClearAll: () => void;
  onClearStencil: () => void;
  onReloadStencilMaster: () => void;
  onJsonEdit: () => void;
  generateDisabled?: boolean;
  generateLoading?: boolean;
  reloadLoading?: boolean;
}

/**
 * ActionButtons Component
 * 
 * Provides action buttons for ProMarker operations:
 * - Generate: コード生成
 * - Clear All: 全クリア
 * - Reload Stencil Master: ステンシルマスタ再読み込み
 * - JSON Editor: JSON形式編集 (Step 8で実装)
 */
export function ActionButtons({
  onGenerate,
  onClearAll,
  onClearStencil,
  onReloadStencilMaster,
  onJsonEdit,
  generateDisabled = false,
  generateLoading = false,
  reloadLoading = false,
}: ActionButtonsProps) {
  return (
    <div className="space-y-4">
      {/* Primary Actions */}
      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          data-testid="generate-btn"
          onClick={onGenerate}
          disabled={generateDisabled || generateLoading}
          className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-2.5 text-sm font-medium text-primary-foreground ring-offset-background transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          {generateLoading ? (
            <>
              <svg
                className="mr-2 h-4 w-4 animate-spin"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              生成中...
            </>
          ) : (
            <>🚀 Generate</>
          )}
        </button>

        <button
          type="button"
          data-testid="clear-all-btn"
          onClick={onClearAll}
          disabled={generateLoading}
          className="inline-flex items-center justify-center rounded-md border border-input bg-background px-4 py-2.5 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          🔄 全てクリア
        </button>

        <button
          type="button"
          data-testid="clear-stencil-btn"
          onClick={onClearStencil}
          disabled={generateLoading}
          className="inline-flex items-center justify-center rounded-md border border-input bg-background px-4 py-2.5 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          🔄 ステンシル定義を再取得
        </button>
      </div>

      {/* Secondary Actions */}
      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          data-testid="reload-stencil-btn"
          onClick={onReloadStencilMaster}
          disabled={reloadLoading}
          className="inline-flex items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          {reloadLoading ? (
            <>
              <svg
                className="mr-2 h-4 w-4 animate-spin"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              再読み込み中...
            </>
          ) : (
            <>♻️ Reload Stencil Master</>
          )}
        </button>

        <button
          type="button"
          data-testid="json-edit-btn"
          onClick={onJsonEdit}
          disabled={generateLoading}
          className="inline-flex items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50"
        >
          📝 Json形式
        </button>
      </div>
    </div>
  );
}
