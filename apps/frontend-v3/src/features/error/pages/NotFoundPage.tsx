import { useNavigate } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { SearchX } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { useAuth } from '@/hooks/useAuth';

/**
 * 404 Not Found Error Page
 * Displays when route does not exist
 */
export function NotFoundPage() {
  const navigate = useNavigate();
  const headingRef = useRef<HTMLHeadingElement>(null);
  const { isAuthenticated } = useAuth();
  
  // フォーカス管理: ページ読み込み時に見出しにフォーカス
  useEffect(() => {
    headingRef.current?.focus();
  }, []);
  
  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <div className="w-full max-w-md text-center space-y-6" role="main">
        <div className="flex justify-center">
          <SearchX className="size-20 text-muted-foreground" aria-hidden="true" />
        </div>
        
        <div className="space-y-2">
          <h1 
            ref={headingRef}
            tabIndex={-1}
            className="text-7xl font-bold text-muted-foreground outline-none"
          >
            404
          </h1>
          <h2 className="text-2xl font-semibold text-foreground">ページが見つかりません</h2>
        </div>
        
        <p className="text-muted-foreground leading-relaxed">
          お探しのページは存在しないか、移動または削除された可能性があります。
        </p>
        
        <nav className="flex flex-col gap-3 pt-4" aria-label="エラーページナビゲーション">
          <Button 
            className="w-full"
            onClick={() => navigate(isAuthenticated ? '/' : '/login')}
            aria-label="ホームページに戻る"
          >
            {isAuthenticated ? 'ホームに戻る' : 'ログインページへ'}
          </Button>
          <Button 
            variant="outline" 
            className="w-full"
            onClick={() => navigate(-1)}
            aria-label="前のページに戻る"
          >
            前のページに戻る
          </Button>
        </nav>
      </div>
    </div>
  );
}
