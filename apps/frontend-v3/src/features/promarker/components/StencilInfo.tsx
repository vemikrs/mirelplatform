import { Badge, Card, CardContent, CardHeader, CardTitle } from '@mirel/ui';
import { CalendarClock, FolderTree, Hash, UserRound } from 'lucide-react';
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
    <Card data-testid="stencil-info">
      <CardHeader className="space-y-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-xl text-foreground">{config.name}</CardTitle>
          <Badge variant="neutral">{config.categoryName}</Badge>
        </div>
        <p className="text-sm text-muted-foreground">{config.description || 'このステンシルには説明が設定されていません。'}</p>
      </CardHeader>
      <CardContent className="grid grid-cols-1 gap-3 text-sm">
        <div className="flex items-center gap-2">
          <FolderTree className="size-4 text-primary" />
          <span className="text-muted-foreground">カテゴリ ID:</span>
          <span className="font-medium">{config.categoryId}</span>
        </div>
        <div className="flex items-center gap-2">
          <Hash className="size-4 text-primary" />
          <span className="text-muted-foreground">シリアル:</span>
          <span className="font-medium">{config.serial}</span>
        </div>
        <div className="flex items-center gap-2">
          <CalendarClock className="size-4 text-primary" />
          <span className="text-muted-foreground">最終更新:</span>
          <span className="font-medium">{config.lastUpdate}</span>
        </div>
        <div className="flex items-center gap-2">
          <UserRound className="size-4 text-primary" />
          <span className="text-muted-foreground">更新者:</span>
          <span className="font-medium">{config.lastUpdateUser}</span>
        </div>
      </CardContent>
    </Card>
  );
}
