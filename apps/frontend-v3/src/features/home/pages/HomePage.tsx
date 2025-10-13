import { Button } from '@mirel/ui';
import { Toaster, toast } from '@mirel/ui';

/**
 * Home page component
 * Landing page with navigation to ProMarker
 */
export function HomePage() {
  const handleNavigateToProMarker = () => {
    window.location.href = '/promarker';
  };

  const handleShowToast = () => {
    toast({
      title: 'ようこそ',
      description: 'ProMarker Platform へようこそ!',
      variant: 'default',
    });
  };

  return (
    <div className="space-y-8">
      <div className="text-center space-y-4">
        <h1 className="text-5xl font-bold text-foreground">
          Welcome to ProMarker Platform
        </h1>
        <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
          コードテンプレートから自動的にコードを生成する強力なプラットフォーム
        </p>
      </div>

      <div className="flex justify-center gap-4">
        <Button size="lg" onClick={handleNavigateToProMarker}>
          ProMarker を開く
        </Button>
        <Button variant="outline" size="lg" onClick={handleShowToast}>
          トースト表示
        </Button>
      </div>

      <div className="bg-muted/50 rounded-lg p-8 space-y-4">
        <h2 className="text-2xl font-semibold">Phase 1: ProMarker Core Feature Migration</h2>
        <div className="grid gap-4 md:grid-cols-2">
          <div className="space-y-2">
            <h3 className="font-medium">実装済み機能</h3>
            <ul className="text-sm text-muted-foreground space-y-1">
              <li>✅ React Router v7 設定</li>
              <li>✅ レイアウトシステム</li>
              <li>✅ ホームページ</li>
              <li>✅ ProMarkerページ (基本構造)</li>
            </ul>
          </div>
          <div className="space-y-2">
            <h3 className="font-medium">次のステップ</h3>
            <ul className="text-sm text-muted-foreground space-y-1">
              <li>⏳ API Client設定</li>
              <li>⏳ TanStack Query Hooks</li>
              <li>⏳ ステンシル選択UI</li>
              <li>⏳ パラメータ入力フォーム</li>
            </ul>
          </div>
        </div>
      </div>

      <Toaster />
    </div>
  );
}
