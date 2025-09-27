# ProMarker E2E テスティング ガイド

## 概要

ProMarker アプリケーションの E2E テスト基盤として Playwright を採用しています。このドキュメントでは、テストの実行方法、新しいテストの追加方法、トラブルシューティングについて説明します。

## 🎯 特徴

- **マルチブラウザ対応**: Chromium, Firefox, WebKit
- **日本語環境**: ja-JP ロケール、Asia/Tokyo タイムゾーン
- **アクセシビリティ**: AXE による WCAG 2.1 AA 準拠検査
- **ビジュアル回帰**: スクリーンショット比較による UI 変更検出
- **POM パターン**: Page Object Model による保守性の高いテスト設計
- **CI/CD 統合**: GitHub Actions による自動実行

## 🚀 クイックスタート

### 1. セットアップ検証
```bash
# セットアップが正しく完了しているか検証
npm run test:e2e:validate
```

### 2. Playwright ブラウザのインストール
```bash
# 全ブラウザをインストール
npm run test:install

# Chromium のみインストール（推奨）
npx playwright install chromium --with-deps
```

### 3. サービス起動
```bash
# Backend と Frontend を同時起動
./scripts/start-services.sh
```

### 4. テスト実行
```bash
# 基本実行（ヘッドレスモード）
npm run test:e2e

# ブラウザを表示して実行
npm run test:e2e:headed

# 対話的 UI モードで実行
npm run test:e2e:ui

# デバッグモードで実行
npm run test:e2e:debug
```

## 📁 ディレクトリ構造

```
tests/e2e/
├── fixtures/          # テストデータとフィクスチャ
│   └── test-data.ts   # ステンシルデータ、検証データ
├── pages/             # Page Object Model
│   ├── base.page.ts   # 基底ページクラス
│   └── promarker.page.ts  # ProMarker ページオブジェクト
├── specs/             # テストスペック
│   ├── promarker-basic.spec.ts        # 基本機能テスト
│   ├── promarker-workflow.spec.ts     # ワークフローテスト
│   ├── promarker-full-workflow.spec.ts # 完全ワークフローテスト
│   ├── promarker-accessibility.spec.ts # アクセシビリティテスト
│   └── promarker-visual.spec.ts       # ビジュアル回帰テスト
├── utils/             # ユーティリティ
│   ├── accessibility.ts  # AXE アクセシビリティヘルパー
│   └── test-helpers.ts   # 共通テストヘルパー
├── global-setup.ts    # グローバルセットアップ
├── global-teardown.ts # グローバルクリーンアップ
└── validate-setup.js  # セットアップ検証スクリプト
```

## 🧪 テストカテゴリ

### 基本機能テスト (`promarker-basic.spec.ts`)
- ページロード検証
- UI 要素の表示確認
- 基本的なユーザーインタラクション
- レスポンシブデザイン検証

### ワークフローテスト (`promarker-workflow.spec.ts`, `promarker-full-workflow.spec.ts`)
- ステンシル選択フロー
- パラメータ入力とバリデーション
- コード生成機能
- ファイルダウンロード機能
- エラーハンドリング

### アクセシビリティテスト (`promarker-accessibility.spec.ts`)
- WCAG 2.1 AA 準拠検査
- キーボードナビゲーション
- スクリーンリーダー対応
- カラーコントラスト検証
- フォーカス管理

### ビジュアル回帰テスト (`promarker-visual.spec.ts`)
- UI レイアウト比較
- 複数デバイス対応確認
- ダークモード対応
- ホバー・フォーカス状態

## 🎨 UI エンハンスメント

テストの信頼性を高めるため、以下の `data-test-id` 属性が追加されています：

```html
<!-- カテゴリ選択 -->
<b-form-select data-test-id="category-select">

<!-- ステンシル選択 -->
<b-form-select data-test-id="stencil-select">

<!-- シリアル選択 -->
<b-form-select data-test-id="serial-select">

<!-- 生成ボタン -->
<b-button data-test-id="generate-btn">

<!-- アクションボタン -->
<b-button data-test-id="clear-all-btn">
<b-button data-test-id="clear-stencil-btn">
<b-button data-test-id="json-format-btn">
<b-button data-test-id="reload-stencil-btn">

<!-- パラメータ入力 -->
<b-form-input data-test-id="param-input-{id}">
```

## 🔧 実用的なコマンド

### 開発・デバッグ用
```bash
# コード生成（操作を記録してテストコード生成）
npm run test:e2e:codegen

# 特定のテストファイルのみ実行
npx playwright test promarker-basic

# 特定のブラウザのみでテスト
npx playwright test --project=chromium

# 失敗したテストのみ再実行
npx playwright test --last-failed

# テスト結果レポートを表示
npx playwright show-report
```

### CI/CD・メンテナンス用
```bash
# スナップショットの更新
npm run test:update-snapshots

# テストファイルの一覧表示
npx playwright test --list

# テスト設定の確認
npx playwright test --debug --dry-run
```

### カスタムスクリプト
```bash
# 便利なE2Eテストスクリプト
./scripts/e2e/run-tests.sh                 # 基本実行
./scripts/e2e/run-tests.sh -b firefox -h   # Firefox ヘッドモード
./scripts/e2e/run-tests.sh -u              # UI モード
./scripts/e2e/run-tests.sh -s              # スナップショット更新
./scripts/e2e/run-tests.sh -r              # サービス起動済み
```

## 📊 CI/CD 統合

GitHub Actions ワークフローが以下のジョブを自動実行します：

### E2E Tests
- 全ブラウザ（Chromium, Firefox, WebKit）でのテスト実行
- テスト結果・スクリーンショット・動画の自動保存

### Accessibility Audit
- アクセシビリティ専用テストの実行
- WCAG 準拠レポートの生成

### 実行条件
- `main`, `master`, `develop` ブランチへの push
- これらのブランチへの Pull Request
- 手動実行（workflow_dispatch）

## 🐛 トラブルシューティング

### よくある問題と解決方法

#### 1. ブラウザインストールエラー
```bash
# 解決策:
sudo npx playwright install-deps
npx playwright install --force
```

#### 2. サービス接続エラー
```bash
# 解決策:
./scripts/start-services.sh
curl http://localhost:8080/mirel/  # Frontend 確認
curl http://localhost:3000/actuator/health  # Backend 確認
```

#### 3. スクリーンショット差分エラー
```bash
# 解決策: スナップショット更新
npm run test:update-snapshots
```

## 📈 ベストプラクティス

### 1. テスト設計
- **一つのテストに一つの責任**: 単一機能に焦点を当てる
- **データ独立性**: テスト間でデータを共有しない
- **明確なアサーション**: 何をテストしているか明確にする

### 2. セレクタ戦略
- **data-test-id 優先**: UI 変更に強い
- **論理的な名前**: 機能を表す名前を使用
- **階層的な構造**: 親子関係を活用

## 🔗 関連リンク

- [Playwright 公式ドキュメント](https://playwright.dev/docs/intro)
- [AXE Accessibility Testing](https://github.com/dequelabs/axe-core)
- [Page Object Model Pattern](https://playwright.dev/docs/pom)