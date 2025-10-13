import { useEffect } from 'react';
import { Toaster } from '@mirel/ui';
import { useSuggest } from '../hooks/useSuggest';

/**
 * ProMarker main page component
 * Provides UI for stencil selection, parameter input, and code generation
 * 
 * Phase 1 - Step 4: TanStack Query Hooks integration
 */
export function ProMarkerPage() {
  const suggestMutation = useSuggest();

  useEffect(() => {
    // Set page title for E2E test verification
    document.title = 'ProMarker - 払出画面';

    // Fetch initial stencil data on mount
    // This will populate categories dropdown
    suggestMutation.mutate({
      stencilCategoy: '*',
      stencilCanonicalName: '*',
      serialNo: '*'
    });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="space-y-2">
        <h1 className="text-4xl font-bold text-foreground">
          ProMarker 払出画面
        </h1>
        <p className="text-muted-foreground">
          ステンシルテンプレートからコードを自動生成
        </p>
      </div>

      {/* Loading Indicator */}
      {suggestMutation.isPending && (
        <div className="border rounded-lg p-6 text-center">
          <p className="text-muted-foreground">読み込み中...</p>
        </div>
      )}

      {/* Stencil Data Display (Temporary - for Step 4 verification) */}
      {suggestMutation.data?.data?.model && (
        <div className="border rounded-lg p-6 space-y-4">
          <h2 className="text-xl font-semibold">ステンシル情報 (取得成功)</h2>
          <div className="text-sm space-y-2">
            <p>
              <strong>カテゴリ数:</strong>{' '}
              {suggestMutation.data.data.model.fltStrStencilCategory?.items?.length || 0}
            </p>
            <p>
              <strong>選択中のカテゴリ:</strong>{' '}
              {suggestMutation.data.data.model.fltStrStencilCategory?.selected || 'なし'}
            </p>
          </div>
        </div>
      )}

      {/* Placeholder for Stencil Selection (Step 5) */}
      <div className="border rounded-lg p-6 space-y-4">
        <h2 className="text-xl font-semibold">ステンシル選択</h2>
        <p className="text-sm text-muted-foreground">
          Coming soon in Step 5: 3段階選択UI (Category → Stencil → Serial)
        </p>
      </div>

      {/* Placeholder for Parameter Input (Step 5) */}
      <div className="border rounded-lg p-6 space-y-4">
        <h2 className="text-xl font-semibold">パラメータ入力</h2>
        <p className="text-sm text-muted-foreground">
          Coming soon in Step 5: 動的パラメータフィールド生成
        </p>
      </div>

      {/* Placeholder for Action Buttons (Step 5) */}
      <div className="border rounded-lg p-6 space-y-4">
        <h2 className="text-xl font-semibold">操作</h2>
        <p className="text-sm text-muted-foreground">
          Coming soon in Step 5: Generate, Clear All, JSON Editor 等のボタン
        </p>
      </div>

      {/* Development Status */}
      <div className="bg-muted/50 rounded-lg p-6 space-y-2">
        <h3 className="font-semibold">開発ステータス</h3>
        <ul className="text-sm text-muted-foreground space-y-1">
          <li>✅ Step 0: E2E基盤セットアップ</li>
          <li>✅ Step 1: ルーティング設定</li>
          <li>✅ Step 2: API Client設定</li>
          <li>✅ Step 3: API型定義</li>
          <li>✅ Step 4: TanStack Query Hooks (初回API呼び出し実装)</li>
          <li>⏳ Step 5: UI実装</li>
        </ul>
      </div>

      <Toaster />
    </div>
  );
}
