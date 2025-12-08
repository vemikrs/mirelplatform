
import { useEffect } from 'react';
import { useAuthStore } from '@/stores/authStore';
import { useMiraStore } from '@/stores/miraStore';
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
    
    // Miraの会話履歴もクリア（セキュリティとUXのため）
    useMiraStore.persist.clearStorage();
    // メモリ上の状態もリセットするために、初期状態に戻す（必要に応じて）
    useMiraStore.setState({ 
      activeConversationId: null, 
      conversations: {}, 
    }); 
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
