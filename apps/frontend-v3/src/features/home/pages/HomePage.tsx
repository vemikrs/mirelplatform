import { Button, Toaster, toast } from '@mirel/ui';

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
      description: 'mirelplatform へようこそ!',
      variant: 'default',
    });
  };

  return (
    <div className="space-y-8">
      <div className="text-center space-y-4">
        <h1 className="text-5xl font-bold text-foreground">
          Welcome to mirelplatform
        </h1>
        <p className="text-xl text-muted-foreground max-w-2xl mx-auto">
          エンタープライズ向けの軽量な開発支援プラットフォーム
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

      <Toaster />
    </div>
  );
}
