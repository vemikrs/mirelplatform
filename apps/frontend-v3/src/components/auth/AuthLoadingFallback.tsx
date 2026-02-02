import { useState, useEffect } from 'react';
import { Spinner } from '@mirel/ui';
import { RefreshCw } from 'lucide-react';

/**
 * 段階的ローディングフォールバックコンポーネント
 * 
 * RouterProviderのfallbackElementとして使用。
 * authLoaderの完了待ち（バックエンドのコールドスリープからの復帰など）の間に
 * ユーザーに状況を伝える3段階のUIを提供する。
 * 
 * - 即座: ローディングスピナー + 「読み込み中...」
 * - 3秒後: 追加メッセージ「サーバーの起動を待っています」
 * - 30秒後: 「再読み込み」ボタン表示
 */
export function AuthLoadingFallback() {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const showSlowMessage = elapsedSeconds >= 3;
  const showReloadButton = elapsedSeconds >= 30;

  const handleReload = () => {
    window.location.reload();
  };

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center bg-background text-foreground">
      {/* ブランドロゴ/アプリ名 */}
      <div className="mb-8">
        <div className="rounded-full bg-primary/10 px-4 py-2 text-sm font-semibold text-primary">
          mirel platform
        </div>
      </div>

      {/* スピナー */}
      <Spinner size="xl" className="text-primary" />

      {/* メインメッセージ */}
      <p className="mt-6 text-lg text-muted-foreground">
        読み込み中...
      </p>

      {/* 3秒後の追加メッセージ */}
      {showSlowMessage && (
        <p className="mt-2 text-sm text-muted-foreground/80 animate-in fade-in duration-500">
          サーバーの起動を待っています。時間がかかる場合があります。
        </p>
      )}

      {/* 30秒後の再読み込みボタン */}
      {showReloadButton && (
        <button
          onClick={handleReload}
          className="mt-6 flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 animate-in fade-in slide-in-from-bottom-2 duration-300"
        >
          <RefreshCw className="h-4 w-4" />
          再読み込み
        </button>
      )}
    </div>
  );
}
