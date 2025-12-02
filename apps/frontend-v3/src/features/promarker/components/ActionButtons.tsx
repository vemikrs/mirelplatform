import { Button } from '@mirel/ui';
import { ArrowRight, Eraser, FileJson, Loader2, RefreshCw, Edit3 } from 'lucide-react';

interface ActionButtonsProps {
  onGenerate: () => void | Promise<void>;
  onClearAll: () => void | Promise<void>;
  onClearStencil: () => void | Promise<void>;
  onReloadStencilMaster: () => void | Promise<void>;
  onJsonEdit: () => void | Promise<void>;
  onManageStencils: () => void | Promise<void>;
  generateDisabled?: boolean;
  generateLoading?: boolean;
  reloadLoading?: boolean;
}

/**
 * ActionButtons Component
 *
 * Provides action buttons for ProMarker operations with design-system styling.
 */
export function ActionButtons({
  onGenerate,
  onClearAll,
  onClearStencil,
  onReloadStencilMaster,
  onJsonEdit,
  onManageStencils,
  generateDisabled = false,
  generateLoading = false,
  reloadLoading = false,
}: ActionButtonsProps) {
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <div className="flex flex-wrap items-center gap-3">
        <Button
          data-testid="generate-btn"
          onClick={onGenerate}
          disabled={generateDisabled || generateLoading}
          variant="elevated"
          size="lg"
        >
          {generateLoading ? (
            <>
              <Loader2 className="mr-2 size-4 animate-spin" />
              生成中...
            </>
          ) : (
            <>
              コードを生成
              <ArrowRight className="ml-2 size-4" />
            </>
          )}
        </Button>

        <Button
          data-testid="clear-all-btn"
          variant="outline"
          onClick={onClearAll}
          disabled={generateLoading}
        >
          <Eraser className="mr-2 size-4" />
          入力をクリア
        </Button>

        <Button
          data-testid="clear-stencil-btn"
          variant="subtle"
          onClick={onClearStencil}
          disabled={generateLoading}
        >
          <RefreshCw className="mr-2 size-4" />
          ステンシル再取得
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button
          data-testid="manage-stencils-btn"
          variant="outline"
          onClick={onManageStencils}
          disabled={generateLoading}
        >
          <Edit3 className="mr-2 size-4" />
          ステンシル管理
        </Button>

        <Button
          data-testid="reload-stencil-btn"
          variant="ghost"
          onClick={onReloadStencilMaster}
          disabled={reloadLoading}
        >
          {reloadLoading ? (
            <>
              <Loader2 className="mr-2 size-4 animate-spin" />
              マスタ更新中...
            </>
          ) : (
            <>
              <RefreshCw className="mr-2 size-4" />
              マスタをリロード
            </>
          )}
        </Button>

        <Button
          data-testid="json-edit-btn"
          variant="ghost"
          onClick={onJsonEdit}
          disabled={generateLoading}
        >
          <FileJson className="mr-2 size-4" />
          JSON 編集
        </Button>
      </div>
    </div>
  );
}
