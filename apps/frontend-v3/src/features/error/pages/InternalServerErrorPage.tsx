import { useNavigate, useRouteError } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { AlertTriangle } from 'lucide-react';
import { useEffect, useRef } from 'react';

/**
 * 500 Internal Server Error Page
 * Displays for unexpected errors and React Router ErrorBoundary
 */
export function InternalServerErrorPage() {
  const navigate = useNavigate();
  const error = useRouteError() as Error | null;
  const headingRef = useRef<HTMLHeadingElement>(null);
  
  // フォーカス管理: ページ読み込み時に見出しにフォーカス
  useEffect(() => {
    headingRef.current?.focus();
  }, []);
  
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="max-w-md text-center space-y-6 px-4" role="main">
        <div className="flex justify-center">
          <AlertTriangle className="size-20 text-destructive" aria-hidden="true" />
        </div>
        
        <div className="space-y-2">
          <h1 
            ref={headingRef}
            tabIndex={-1}
            className="text-7xl font-bold text-destructive outline-none"
          >
            500
          </h1>
          <h2 className="text-2xl font-semibold text-foreground">サーバーエラー</h2>
        </div>
        
        <p className="text-muted-foreground leading-relaxed">
          申し訳ございません。予期しないエラーが発生しました。
          しばらく時間をおいて再度お試しください。
        </p>
        
        {import.meta.env.DEV && error && (
          <div className="rounded-lg bg-destructive/10 p-4 text-left" role="alert" aria-live="polite">
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
        
        <nav className="flex flex-col gap-3 pt-4" aria-label="エラーページナビゲーション">
          <Button 
            className="w-full"
            onClick={() => window.location.reload()}
            aria-label="ページを再読み込み"
          >
            ページを再読み込み
          </Button>
          <Button 
            variant="outline" 
            className="w-full"
            onClick={() => navigate('/')}
            aria-label="ホームページに戻る"
          >
            ホームに戻る
          </Button>
        </nav>
      </div>
    </div>
  );
}
