# ProMarker

[![Build Status](https://dev.azure.com/vemicho/mir/_apis/build/status/vemic.promarker?branchName=master)](https://dev.azure.com/vemicho/mir/_build/latest?definitionId=2&branchName=azure-pipelines)

開発プロジェクトのテンプレートや機能スケルトンの自動生成アプリケーションです。
定型的なディレクトリ構成やソースコード、初期データを独自技術で自動生成します。
プロジェクトフォルダや機能スケルトンを簡単に自動生成できるため、開発の高速化や品質の平準化に貢献します。
テンプレートには [FreeMarker](https://freemarker.apache.org/) を使用しており、豊富なドキュメントを活用できます。

**ProMarker**は、独自のmirelplatformフレームワーク上で構築された包括的なコード生成・管理プラットフォームです。SpringBootをベースとした拡張性に優れたアーキテクチャにより、柔軟で強力な開発支援機能を提供します。

## 🚀 主な機能

### テンプレート管理・コード生成
- **動的テンプレート処理**: 高度なテンプレートエンジンによるカスタマイズ可能なコード生成
- **関数リゾルバー**: 拡張可能な関数解決システムによる柔軟なテンプレート処理
- **マスターテンプレート機能**: 標準化されたコードベースの効率的な管理

### ファイル管理システム
- **ファイルアップロード/ダウンロード**: 多様なファイル形式への対応
- **バッチ処理**: 複数ファイルの一括処理とZIP圧縮による効率的な配布
- **セキュアな一時ファイル管理**: 安全な作業領域での処理実行

### Web自動化機能
- **Selenide統合**: 高信頼性のWebブラウザ自動化（**現在開発中のため動作しません**）
- **API駆動アーキテクチャ**: RESTfulなインターフェースによる外部システム連携
- **プラグインシステム**: 機能拡張可能なモジュラー設計

### 認証・セキュリティ
- **JWT認証**: 安全なトークンベース認証システム
- **OAuth2対応**: 外部認証プロバイダーとの連携
- **セッション管理**: 高度なセッション制御とセキュリティ機能

## 🔒 資産保護

**重要**: ProMarkerで使用されるソースコードテンプレートやコード雛形は、**プライベート資産**として保護されています。これらの知的財産は個別のプライベートリポジトリ（またはディレクトリ）で管理し、外部への送信を行わないことで適切に保護されています。

## 🏗️ アーキテクチャ

ProMarkerは**mirelplatform**という独自のフレームワーク上で動作します：

- **SpringBoot 3.3基盤**: 最新のSpringBootテクノロジーによる安定性と拡張性
- **Java 21対応**: 最新のJava機能を活用したパフォーマンス最適化
- **モジュラー設計**: 機能別の明確な分離による保守性の向上
- **プラグインアーキテクチャ**: 新機能の柔軟な追加と拡張

### 技術スタック
- Spring Boot 3.3.0 (Web, Security, JPA, Batch)
- Java 21 with Microsoft JVM
- H2/MySQL Database
- Apache POI (Office文書処理)
- Freemarker Template Engine
- Selenide (Web自動化)
- JWT & OAuth2 (認証)

## 🚀 セットアップ

### 前提条件
- Java 21 (Microsoft JVM推奨)
- Node.js 16+ (Frontend用)
- Gradle 8.4+
- MySQL (本番環境)

### DevContainer/Codespaces 環境構築

1. **リポジトリのクローン**:
   ```bash
   git clone https://github.com/vemic/mirelplatform.git
   cd mirelplatform
   ```

2. **サービス一括起動**:
   ```bash
   # Backend (Spring Boot) + Frontend (Nuxt.js) を同時起動
   ./start-services.sh
   ```

3. **アクセスURL**:
   - Frontend: http://localhost:8080/mirel/
   - Backend API: http://localhost:3000

### 管理コマンド

```bash
# サービス起動
./start-services.sh

# サービス停止  
./stop-services.sh

# リアルタイムログ監視
./watch-logs.sh              # 両方のログを監視
./watch-logs.sh backend      # Backendのみ
./watch-logs.sh frontend     # Frontendのみ

# サービス状況確認
./startup-monitor.sh
```

### 手動起動（個別）

**Backend のみ**:
```bash
SPRING_PROFILES_ACTIVE=dev SERVER_PORT=3000 ./gradlew :backend:bootRun
```

**Frontend のみ**:
```bash
cd frontend
HOST=0.0.0.0 PORT=8080 npm run dev
```

### 設定ファイル
主要な設定は `src/main/resources/config/application.yml` で管理されています。

## 📚 API仕様

### 主要エンドポイント
- `/apps/mste/api/{function}` - マスターテンプレート機能
- `/apps/arr/api/{function}` - Web自動化機能  
- `/commons/upload` - ファイルアップロード
- `/commons/download` - ファイルダウンロード
- `/commons/dlsite/{path}` - 直接ダウンロード

## 🤝 開発・拡張

mirelplatformの拡張性により、以下のカスタマイズが可能です：

- **カスタムAPI**: 新しい機能モジュールの追加
- **テンプレート拡張**: 独自のテンプレート関数の実装
- **認証連携**: 外部認証システムとの統合
- **データベース拡張**: 追加のエンティティとリポジトリ

## 🧪 E2E テスト

### 概要
ProMarker アプリケーションのE2Eテスト基盤として Playwright を採用しています。

### 特徴
- **ブラウザサポート**: Chromium, Firefox, WebKit
- **ロケール**: 日本語 (ja-JP) / タイムゾーン: Asia/Tokyo
- **アクセシビリティ**: AXE による自動検査
- **ビジュアル回帰**: スクリーンショット比較
- **POM**: Page Object Model パターン採用

### セットアップ

```bash
# E2E テスト依存関係のインストール
npm install

# Playwright ブラウザのインストール
npx playwright install --with-deps

# テスト実行
npm run test:e2e

# ヘッドモードでテスト実行 (ブラウザ表示)
npm run test:e2e:headed

# UIモードでテスト実行 (対話的)
npm run test:e2e:ui

# デバッグモード
npm run test:e2e:debug
```

### 便利スクリプト

```bash
# E2E テスト専用スクリプト
./scripts/e2e/run-tests.sh                    # 基本実行
./scripts/e2e/run-tests.sh -b firefox -h      # Firefox ヘッドモード
./scripts/e2e/run-tests.sh -u                 # UIモード
./scripts/e2e/run-tests.sh -s                 # スナップショット更新
./scripts/e2e/run-tests.sh -r                 # サービス起動済み
```

### テスト対象
- **基本機能**: ページロード、UI要素、ナビゲーション
- **ワークフロー**: ステンシル選択→パラメータ入力→生成→ダウンロード
- **バリデーション**: 入力検証、エラーハンドリング
- **アクセシビリティ**: WCAG 2.1 AA 準拠検査
- **レスポンシブ**: デスクトップ・タブレット・モバイル対応

### CI/CD
GitHub Actions で自動実行されます：
- **Setup Validation**: E2E テストセットアップの検証 ✅
- **E2E Tests**: Chromiumブラウザでの機能テスト ✅
- **Accessibility Audit**: アクセシビリティ検査 ✅

完全なE2Eテスト実行がCI環境で有効化されています。
ローカル環境でも全機能が利用可能です。

詳細は `docs/E2E_TESTING.md` を参照してください。

## 📄 ライセンス

Copyright(c) 2015-2025 vemi/mirelplatform. All rights reserved.

---

**ProMarker** - 定型ソースコード生成プラットフォーム powered by mirelplatform
