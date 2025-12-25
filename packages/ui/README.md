# @mireljs/ui-core

mirel Design System - shadcn/ui + Radix UI ベースのReact UIコンポーネントライブラリ

[![npm version](https://badge.fury.io/js/%40mireljs%2Fui-core.svg)](https://www.npmjs.com/package/@mireljs/ui-core)
[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## インストール

```bash
npm install @mireljs/ui-core
# or
pnpm add @mireljs/ui-core
```

### Peer Dependencies

```bash
npm install react react-dom
```

## 使用方法

```tsx
import { Button, Card, Dialog } from "@mireljs/ui-core";

function App() {
  return (
    <Card>
      <Button variant="default">Click me</Button>
    </Card>
  );
}
```

## コンポーネント一覧

### レイアウト

- `Card` - カードコンテナ
- `ScrollArea` - スクロールエリア
- `Separator` - 区切り線

### フォーム

- `Button` - ボタン
- `Input` - テキスト入力
- `Textarea` - テキストエリア
- `Select` - セレクトボックス
- `Switch` - トグルスイッチ
- `Slider` - スライダー
- `Label` - ラベル
- `Form` - フォームラッパー
- `Combobox` - コンボボックス

### フィードバック

- `Alert` - アラート
- `Badge` - バッジ
- `Toast` / `Toaster` - トースト通知
- `Spinner` - ローディングスピナー
- `Skeleton` - スケルトンローダー

### オーバーレイ

- `Dialog` - モーダルダイアログ
- `Sheet` - スライドパネル
- `Popover` - ポップオーバー
- `Tooltip` - ツールチップ
- `DropdownMenu` - ドロップダウンメニュー

### ナビゲーション

- `Tabs` - タブ
- `Accordion` - アコーディオン

### データ表示

- `Table` - テーブル
- `DataTable` - データテーブル（TanStack Table対応）
- `Avatar` - アバター

### その他

- `ThemeProvider` - テーマプロバイダー
- `StepIndicator` - ステップインジケーター
- `SectionHeading` - セクション見出し

## テーマ

```tsx
import { ThemeProvider } from "@mireljs/ui-core";

function App() {
  return <ThemeProvider defaultTheme="dark">{/* your app */}</ThemeProvider>;
}
```

## ライセンス

MIT
