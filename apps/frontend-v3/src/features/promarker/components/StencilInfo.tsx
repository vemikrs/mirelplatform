import type { StencilConfig } from '../types/api';

interface StencilInfoProps {
  config: StencilConfig | null;
}

/**
 * StencilInfo Component
 * 
 * Displays selected stencil configuration information
 */
export function StencilInfo({ config }: StencilInfoProps) {
  if (!config) {
    return null;
  }

  return (
    <div 
      className="rounded-lg border border-border bg-card p-4 space-y-3"
      data-testid="stencil-info"
    >
      <h3 className="text-lg font-semibold">ステンシル情報</h3>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
        <div>
          <span className="font-medium text-muted-foreground">カテゴリ:</span>
          <span className="ml-2">{config.categoryName}</span>
        </div>
        
        <div>
          <span className="font-medium text-muted-foreground">名前:</span>
          <span className="ml-2">{config.name}</span>
        </div>
        
        <div>
          <span className="font-medium text-muted-foreground">シリアル:</span>
          <span className="ml-2">{config.serial}</span>
        </div>
        
        <div>
          <span className="font-medium text-muted-foreground">最終更新:</span>
          <span className="ml-2">{config.lastUpdate}</span>
        </div>
      </div>

      {config.description && (
        <div className="pt-2 border-t">
          <p className="text-sm text-muted-foreground whitespace-pre-wrap">
            {config.description}
          </p>
        </div>
      )}
    </div>
  );
}
