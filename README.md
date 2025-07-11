# ProMarker

[![Build Status](https://dev.azure.com/vemicho/mir/_apis/build/status/vemic.promarker?branchName=master)](https://dev.azure.com/vemicho/mir/_build/latest?definitionId=2&branchName=azure-pipelines)

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
- **Selenide統合**: 高信頼性のWebブラウザ自動化
- **API駆動アーキテクチャ**: RESTfulなインターフェースによる外部システム連携
- **プラグインシステム**: 機能拡張可能なモジュラー設計

### 認証・セキュリティ
- **JWT認証**: 安全なトークンベース認証システム
- **OAuth2対応**: 外部認証プロバイダーとの連携
- **セッション管理**: 高度なセッション制御とセキュリティ機能

## 🔒 資産保護

**重要**: ProMarkerで使用されるソースコードテンプレートやコード雛形は、**プライベート資産**として厳重に管理されています。これらの知的財産は自社または開発者個人の資産であり、適切な保護機能により無断使用や流出を防止しています。

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
- Gradle 8.4+
- MySQL (本番環境)

### 環境構築

1. **リポジトリのクローン**:
   ```bash
   git clone https://github.com/vemic/mirelplatform.git
   cd mirelplatform
   ```

2. **アプリケーションサーバの起動**:
   ```bash
   ./gradlew bootRun
   ```

3. **フロントエンドサービスの起動** ⚠️:
   ```bash
   # 別ターミナルで promarker-ui を起動してください
   # ProMarkerの完全な機能を利用するには、フロントエンドサービス（promarker-ui）を
   # 別途起動する必要があります
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

## 📄 ライセンス

Copyright(c) 2015-2025 vemi/mirelplatform. All rights reserved.

---

**ProMarker** - 次世代コード生成プラットフォーム powered by mirelplatform
