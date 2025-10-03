# CodeQL: java/path-injection 対応レポート

作成日: 2025-10-03
作成者: ProMarker Team
対象ブランチ: feature/fix-24-openapi-integration

## 概要
GitHub Code Scanning (CodeQL) で検出された java/path-injection 警告について、実際の脆弱性リスクを精査し、修正内容を反映しました。本レポートでは、影響箇所、原因、対策、残課題を一覧化します。

## 影響範囲と実際の脆弱性

以下の観点でリスク評価を実施しました。
- 外部入力（HTTPパラメータ、リクエストボディ、ヘッダなど）がファイルパスに流入しているか
- ベースディレクトリ外への逸脱（パストラバーサル .. や 絶対パス化）の可能性
- ZIP エントリ名等でのパス解釈による意図しない書き込み

### 1) StorageUtil の不安全な解決
- 対象ファイル: `backend/src/main/java/jp/vemi/framework/util/StorageUtil.java`
- 問題点:
  - `parseToCanonicalPath`, `getFile`, `getResource` が `Paths.get(base).resolve(input)` を無検証で使用
  - `../` 等によりベースディレクトリ外へ逸脱可能
- 修正内容:
  - 安全な解決ヘルパ `resolveWithinBase(String)` を新設
  - 先頭スラッシュ除去、バックスラッシュ→スラッシュ正規化、`normalize` 実施
  - `startsWith(base)` 検査でベース外逸脱時に `IllegalArgumentException`
  - 上記ヘルパを `parseToCanonicalPath`, `getFile`, `getResource` で利用
- 期待効果: ベースディレクトリ外へのファイルアクセスを恒常的に遮断

### 2) Selenade: testId からのファイル探索
- 対象ファイル: `backend/src/main/java/jp/vemi/mirel/apps/selenade/domain/service/RunTestServiceImp.java`
- 経路: `testId` (外部入力) → `StorageUtil.getFile("apps/apprunner/defs/" + testId)` → `FileUtil.getFiles`
- 問題点:
  - `testId` のバリデーション無しのため `..` などのパストラバーサルが可能
- 修正内容:
  - 正規表現でホワイトリスト検証を追加: `[A-Za-z0-9_-]+` のみ許可
  - 異常値はエラーレスポンス
- 期待効果: 任意パス走査の封じ込め

### 3) TemplateEngineProcessor: ステンシル指定の検証不足
- 対象ファイル: `backend/src/main/java/jp/vemi/ste/domain/engine/TemplateEngineProcessor.java`
- 経路: `context.getStencilCanonicalName()` → ファイルシステム/クラスパス検索
- 問題点:
  - 先頭 `/` チェックはあるが、`..` や `\` の混入に対する検証なし
- 修正内容:
  - `getStencilStorageDir()` で追加検証: `..` と `\` を禁止、違反時は `IllegalArgumentException`
- 期待効果: ステンシルパスのトラバーサル阻止

### 4) DownloadController: ZIP エントリ名
- 対象ファイル: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/DownloadController.java`
- 問題点:
  - `new ZipEntry(new File(parent).getName() + "-" + item.getV2())` で `item.getV2()` がパス風文字列の場合、意図しないディレクトリ構造として解釈されうる
- 修正内容:
  - ZIP エントリ名をサニタイズ
    - `new File(item.getV2()).getName()` でベース名に限定
    - `/` と `\` を `_` に置換
- 期待効果: ZIP 内での想定外のパス構築を回避

## 補足調査（問題なし）
- UploadController → `FileRegisterServiceImpl.register(MultipartFile)` は一時ディレクトリ配下固定 + 付与ファイル名は別途メタデータ保存のみで、パスとして使用しないため悪用困難
- FileDownloadServiceImpl → DB の `FileManagement.filePath` に基づき `Paths.get` で解決。`filePath` はアップロード/生成時にシステム側が `StorageUtil.getBaseDir()` 配下に設定するため、直接外部入力で任意パスが書き込まれない前提（今回の `StorageUtil` 防御強化により更に安全性増）
- StructureReader → 受け取りは `GenerateServiceImp` 内で DB 保存の `filePath` 利用のため、上記の制御に従う

## 変更差分一覧
- StorageUtil
  - 追加: `resolveWithinBase(String)`
  - 変更: `parseToCanonicalPath`, `getFile`, `getResource` を安全APIに切替
- RunTestServiceImp
  - 追加: `testId` ホワイトリスト検証（英数/ハイフン/アンダースコア）
- TemplateEngineProcessor
  - 追加: `stencilCanonicalName` に対する `..` と `\\` の禁止
- DownloadController
  - 変更: ZIP エントリ名のサニタイズ

## リスク評価と互換性
- 互換性影響: 不正な `testId` 文字列を使用した呼出しや、`..` を含む不正なステンシル指定はエラーとなりますが、正当な利用には影響なし
- ログ/監視: 例外時の 400/500 応答は既存ハンドリングに準拠（ダウンロードは 200 でエラー配列返却運用）

## 推奨追加対策（任意）
- エラーメッセージにトレーサビリティIDを付与
- `StorageUtil` で OS 非依存のセパレータ置換をユーティリティ化
- ZIP 出力時のサイズ上限/拡張子制限（DoS・ZipSlip 二次対策）

## 検証
- ビルド: PASS（バックエンド）
- Lint/Typecheck: PASS（Javaコンパイルにて検出なし）
- 単体テスト: 影響範囲のユニットは未提供のためスキップ（後続で追加可能）

## 参考（主要コード箇所）
- `StorageUtil.resolveWithinBase` による正規化とスコープチェック
- `RunTestServiceImp` の `testId` 検証
- `TemplateEngineProcessor.getStencilStorageDir` の追加検証
- `DownloadController` の ZIP エントリ名サニタイズ

---
本対応により、CodeQL の java/path-injection 警告の主要起点は解消済みです。追加の指摘が残る場合は当該アラートのトレースから具体ファイル/行を本レポートに追補します。