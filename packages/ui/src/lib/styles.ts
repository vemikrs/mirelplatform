import { CSSProperties } from 'react';

/**
 * オーバーレイコンポーネント（Dialog, Popover, DropdownMenu等）の
 * 背景色と文字色をCSS変数から強制的に適用するためのスタイルを生成します。
 * 
 * Tailwindのクラス適用がうまくいかない場合や、透過を防ぐために使用します。
 * Javaでいう基底クラスの共通プロパティ定義のような役割を果たします。
 * 
 * @param type 'dialog' (モーダル系) または 'popover' (ポップアップ系)
 * @param style 追加または上書きするスタイルオブジェクト
 */
export function getOverlayStyle(
  type: 'dialog' | 'popover',
  style?: CSSProperties
): CSSProperties {
  const baseStyle: CSSProperties = {
    backgroundColor: type === 'dialog' ? 'hsl(var(--background))' : 'hsl(var(--popover))',
    color: type === 'dialog' ? 'hsl(var(--foreground))' : 'hsl(var(--popover-foreground))',
  };

  return {
    ...baseStyle,
    ...style,
  };
}
