
import { useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { useTheme } from '@/lib/hooks/useTheme';

/**
 * ログアウトページ
 * マウント時に明示的にログアウト処理を実行する
 */
export function LogoutPage() {
  useTheme();
  const logout = useAuthStore((state) => state.logout);

  useEffect(() => {
    // コンポーネントマウント時にログアウトを実行
    // 戻り先URL（returnUrl）は保持しない（完全なログアウト）
    logout();
  }, [logout]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="text-center space-y-4">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto"></div>
        <p className="text-muted-foreground">ログアウト中...</p>
      </div>
    </div>
  );
}
