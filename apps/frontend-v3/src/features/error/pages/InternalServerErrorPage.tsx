import { useNavigate, useRouteError } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { AlertTriangle } from 'lucide-react';

/**
 * 500 Internal Server Error Page
 * Displays for unexpected errors and React Router ErrorBoundary
 */
export function InternalServerErrorPage() {
  const navigate = useNavigate();
  const error = useRouteError() as Error | null;
  
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="max-w-md text-center space-y-6 px-4">
        <div className="flex justify-center">
          <AlertTriangle className="size-20 text-destructive" aria-hidden="true" />
        </div>
        
        <div className="space-y-2">
          <h1 className="text-7xl font-bold text-destructive">500</h1>
          <h2 className="text-2xl font-semibold text-foreground">サーバーエラー</h2>
        </div>
        
        <p className="text-muted-foreground leading-relaxed">
          申し訳ございません。予期しないエラーが発生しました。
          しばらく時間をおいて再度お試しください。
        </p>
        
        {import.meta.env.DEV && error && (
          <div className="rounded-lg bg-destructive/10 p-4 text-left">
            <p className="text-xs font-mono text-destructive break-all">
              {error.message}
            </p>
            {error.stack && (
              <pre className="mt-2 text-xs text-muted-foreground overflow-auto max-h-32">
                {error.stack}
              </pre>
            )}
          </div>
        )}
        
        <div className="flex flex-col gap-3 pt-4">
          <Button 
            className="w-full"
            onClick={() => window.location.reload()}
          >
            ページを再読み込み
          </Button>
          <Button 
            variant="outline" 
            className="w-full"
            onClick={() => navigate('/')}
          >
            ホームに戻る
          </Button>
        </div>
      </div>
    </div>
  );
}
