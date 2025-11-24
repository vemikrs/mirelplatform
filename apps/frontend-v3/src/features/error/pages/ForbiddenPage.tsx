import { useLocation, useNavigate } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { ShieldX } from 'lucide-react';
import { useAuth } from '@/hooks/useAuth';

/**
 * 403 Forbidden Error Page
 * Displays when user lacks required permissions
 */
export function ForbiddenPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="max-w-md text-center space-y-6 px-4">
        <div className="flex justify-center">
          <ShieldX className="size-20 text-warning" aria-hidden="true" />
        </div>
        
        <div className="space-y-2">
          <h1 className="text-7xl font-bold text-warning">403</h1>
          <h2 className="text-2xl font-semibold text-foreground">アクセス権限がありません</h2>
        </div>
        
        <p className="text-muted-foreground leading-relaxed">
          {location.state?.message || 
            'このページへのアクセス権限がありません。管理者にお問い合わせください。'}
        </p>
        
        {user && (
          <div className="rounded-lg bg-surface-subtle p-4 text-sm text-muted-foreground">
            <p>現在のアカウント: <span className="font-medium text-foreground">{user.displayName}</span></p>
            <p className="text-xs mt-1">異なるアカウントでログインする必要がある場合があります</p>
          </div>
        )}
        
        <div className="flex flex-col gap-3 pt-4">
          <Button 
            variant="outline" 
            className="w-full"
            onClick={() => navigate(-1)}
          >
            前のページに戻る
          </Button>
          <Button 
            variant="ghost" 
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
