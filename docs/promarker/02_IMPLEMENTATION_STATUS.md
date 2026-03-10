# ProMarker 実装状況

> **最終更新**: 2026-03-10  
> **凡例**: ✅ 完了 / 🔧 一部実装 / ❌ 未実装 / 🔄 再開発中

---

## 目次

1. [実装ステータスサマリ](#1-実装ステータスサマリ)
2. [バックエンド実装状況](#2-バックエンド実装状況)
3. [フロントエンド実装状況](#3-フロントエンド実装状況)
4. [拡張領域（ステンシル管理・作成系）](#4-拡張領域ステンシル管理作成系)
5. [テスト状況](#5-テスト状況)
6. [ファイル一覧](#6-ファイル一覧)
7. [今後の開発ロードマップ](#7-今後の開発ロードマップ)

---

## 1. 実装ステータスサマリ

### 1.1 機能別ステータス

| 機能 | バックエンド | フロントエンド | 総合 |
|------|:----------:|:----------:|:----:|
| **ステンシル候補取得 (Suggest)** | ✅ | 🔄 | ✅ |
| **コード生成 (Generate)** | ✅ | 🔄 | ✅ |
| **ステンシルマスタリロード** | ✅ | 🔄 | ✅ |
| **ステンシル一覧表示** | ✅ | 🔄 | ✅ |
| **ステンシル読込（参照）** | ✅ | 🔄 | ✅ |
| **ステンシル保存（編集）** | ✅ | 🔄 | ✅ |
| **バージョン履歴** | ✅ | 🔄 | ✅ |
| **ステンシルアップロード (ZIP)** | ❌ | ❌ | ❌ |
| **カテゴリ共通設定保存** | ❌ | 🔧 | ❌ |
| **親ステンシルマージ** | 🔧 | — | 🔧 |
| **ステンシル新規作成 (GUI)** | 🔧 | 🔄 | 🔧 |
| **ステンシル削除** | ❌ | 🔧 | ❌ |
| **ステンシルエクスポート** | ❌ | ❌ | ❌ |
| **テンプレートプレビュー** | ❌ | 🔄 | ❌ |
| **バリデーション (YAML→Zod)** | ✅ | 🔄 | ✅ |
| **ファイルアップロード (type=file)** | ✅ | 🔄 | ✅ |
| **Excel構造化データ読込** | ✅ | — | ✅ |

### 1.2 レイヤー別進捗

```
                    バックエンド          フロントエンド
                    ┌────────────┐       ┌────────────┐
コアエンジン (STE)   │ ████████ 95%│       │     N/A     │
                    └────────────┘       └────────────┘
                    ┌────────────┐       ┌────────────┐
メインAPI (払出)     │ ████████100%│       │ ████████ 90%│ ← UI再開発中
                    └────────────┘       └────────────┘
                    ┌────────────┐       ┌────────────┐
エディタAPI          │ ██████░░ 75%│       │ ██████░░ 70%│ ← UI再開発中
                    └────────────┘       └────────────┘
                    ┌────────────┐       ┌────────────┐
管理系API            │ ██░░░░░░ 25%│       │ ██░░░░░░ 20%│ ← 拡張領域
                    └────────────┘       └────────────┘
```

---

## 2. バックエンド実装状況

### 2.1 コントローラ

| クラス | パス | メソッド | 状態 | 備考 |
|--------|------|---------|:----:|------|
| `ApiController` | `/apps/mste/api` | | | |
| | | `POST /suggest` | ✅ | 3段階フィルタ + パラメータ解決 |
| | | `POST /generate` | ✅ | FreeMarkerテンプレート実行→ZIP生成 |
| | | `POST /reloadStencilMaster` | ✅ | YAML→DB同期 |
| | | `POST /uploadStencil` | ❌ | サービス層がTODO |
| `StencilEditorController` | `/apps/mste/editor` | | | |
| | | `GET /list` | ✅ | カテゴリフィルタ付き一覧 |
| | | `GET /{path}/{serial}` | ✅ | ステンシル読込 |
| | | `GET /{path}/versions` | ✅ | バージョン履歴取得 |
| | | `POST /save` | ✅ | 新シリアル生成+保存 |
| | | `POST /common/{categoryId}` | ❌ | TODO |

### 2.2 API / ファサード層

| クラス | インジェクト先 | 状態 |
|--------|-------------|:----:|
| `SuggestApi` | `SuggestServiceImp` | ✅ |
| `GenerateApi` | `GenerateServiceImp` | ✅ |
| `ReloadStencilMasterApi` | `ReloadStencilMasterServiceImp` | ✅ |
| `UploadStencilApi` | `UploadStencilServiceImp` | ❌ TODO |

### 2.3 サービス層

| クラス | メソッド | 状態 | 詳細 |
|--------|---------|:----:|------|
| **SuggestServiceImp** | | | |
| | `invoke()` | ✅ | 3段階フィルタ（カテゴリ→ステンシル→シリアル）|
| | DB検索 (カテゴリ) | ✅ | `findByStencilCd()` with LIKE |
| | DB検索 (ステンシル) | ✅ | `findByStencilCd()` with itemKind='1' |
| | Engine連携 (シリアル) | ✅ | `getSerialNos()` + `getStencilSettings()` |
| | 自動選択 (`selectFirstIfWildcard`) | ✅ | ワイルドカード時に最初の候補を選択 |
| | フォールバック応答 | ✅ | エンジンエラー時にDB情報でmin応答 |
| **GenerateServiceImp** | | | |
| | `invoke()` | ✅ | テンプレート実行→ZIP登録 |
| | パラメータバリデーション | ✅ | 必須チェック等 |
| | file型パラメータ処理 | ✅ | FileManagement + StructureReader |
| | ZIP化 + FileRegister | ✅ | `Pair<fileId, fileName>` 返却 |
| **StencilEditorServiceImp** | | | |
| | `loadStencil()` | ✅ | レイヤード検索(user→standard→samples) |
| | `saveStencil()` | ✅ | 新シリアル生成、userレイヤーに保存 |
| | `getVersionHistory()` | ✅ | ディレクトリ走査 + YAMLメタ情報 |
| | `listStencils()` | ✅ | 全レイヤースキャン + カテゴリフィルタ |
| | パストラバーサル防御 | ✅ | normalize() + startsWith() |
| **ReloadStencilMasterServiceImp** | | | |
| | `invoke()` → `read()` | ✅ | 全レイヤーYAML収集→DB同期 |
| | classpath サンプル展開 | ✅ | ResourcePatternResolver使用 |
| | 重複エラー回避INSERT | ✅ | `saveStencilSafely()` |
| | レガシー互換処理 | ✅ | `readFileManagementLegacy()` |
| **UploadStencilServiceImp** | | | |
| | `invoke()` | ❌ | ビルダー返却のみ (TODO) |

### 2.4 エンジン層 (STE: jp.vemi.ste)

| クラス | 機能 | 状態 | 補足 |
|--------|------|:----:|------|
| **TemplateEngineProcessor** | | | |
| | `create()` | ✅ | SteContext + ResourcePatternResolver からインスタンス生成 |
| | `execute()` | ✅ | テンプレート処理パイプライン全体 |
| | `getStencilSettings()` | ✅ | YAML読込 + 親マージ |
| | `getSerialNos()` | ✅ | レイヤード + classpath シリアル探索 |
| | `createConfiguration()` | ✅ | MultiTemplateLoader (filesystem + classpath + temp) |
| | `getStencilTemplateFiles()` | ✅ | テンプレートファイル一覧取得 |
| | `newTemplateFileSpec3()` | ✅ | テンプレートロード + 変数展開 |
| | `prepareBind()` | ✅ | コンテキスト変数 + DictionaryMetaData バインド |
| | `constructSecurePath()` | ✅ | パストラバーサル防御 |
| | 親ステンシルマージ (`mergeParentStencilSettingsUnified`) | 🔧 | ロジック実装済、実運用の親設定ファイルなし |
| **SteContext** | コンテキスト管理 | ✅ | StringContent (ucc, d2bs) |
| **EngineBinds** | テンプレートバインド | ✅ | HashMap拡張 |
| **DictionaryMetaData** | 文字列変換 | ✅ | toUcc(), toLcc() |
| **StructureReader** | Excel読込 | ✅ | Apache POI, _@generate シート |
| **StencilSettingsYml** | YAML モデル | ✅ | Config, DataElement, DataDomain, CodeInfo, Store |
| **ValidationRule** | バリデーション定義 | ✅ | required, minLength, maxLength, pattern, errorMessage |
| **SteExecutor** | 一括実行 | ✅ | static execute() |

### 2.5 データアクセス層

| クラス | 型 | 状態 |
|--------|-----|:----:|
| `MsteStencil` | @Entity | ✅ |
| `MsteStencilRepository` | JpaRepository | ✅ |

### 2.6 起動・設定

| コンポーネント | 内容 | 状態 |
|-------------|------|:----:|
| `ProMarkerStartupListener` | ApplicationReadyEvent で自動リロード | ✅ |
| `mirel.apps.mste.auto-reload-stencil-on-startup` | 設定プロパティ | ✅ |

---

## 3. フロントエンド実装状況

> **注意**: UI（フロントエンド）は React 19 + Vite 7 で再開発中。以下は再開発版 (`apps/frontend-v3`) の状況。

### 3.1 ルーティング

| パス | コンポーネント | 状態 | 備考 |
|------|-------------|:----:|------|
| `/promarker` | `ProMarkerPage` | 🔄 | 払出画面（メイン機能） |
| `/promarker/stencils` | `StencilListPage` | 🔄 | ステンシル一覧 |
| `/promarker/editor/*` | `StencilEditor` | 🔄 | エディタ |
| `/mirel/mste` | `ProMarkerPage` | ✅ | 後方互換（E2E用） |

### 3.2 払出画面 (ProMarkerPage)

| コンポーネント | ファイル | 状態 | 説明 |
|-------------|---------|:----:|------|
| `ProMarkerPage` | `pages/ProMarkerPage.tsx` | 🔄 | メインページ（435行）|
| `StencilSelector` | `components/StencilSelector.tsx` | 🔄 | 3段階 Select UI |
| `ParameterFields` | `components/ParameterFields.tsx` | 🔄 | 動的パラメータフォーム |
| `ActionButtons` | `components/ActionButtons.tsx` | 🔄 | アクションボタン群 |
| `StencilInfo` | `components/StencilInfo.tsx` | 🔄 | メタ情報表示 |
| `JsonEditor` | `components/JsonEditor.tsx` | 🔄 | JSON直接編集ダイアログ |
| `FileUploadButton` | `components/FileUploadButton.tsx` | 🔄 | ファイルアップロード |
| `ErrorBoundary` | `components/ErrorBoundary.tsx` | 🔄 | エラーバウンダリ |

**フック**:

| フック | ファイル | 状態 | 機能 |
|--------|---------|:----:|------|
| `useSuggest` | `hooks/useSuggest.ts` | 🔄 | Suggest API mutation |
| `useGenerate` | `hooks/useGenerate.ts` | 🔄 | Generate + 自動DL |
| `useParameterForm` | `hooks/useParameterForm.ts` | 🔄 | RHF + Zod 動的バリデーション |
| `useFileUpload` | `hooks/useFileUpload.ts` | 🔄 | ファイルUP（画像圧縮対応）|
| `useReloadStencilMaster` | `hooks/useReloadStencilMaster.ts` | 🔄 | マスタリロード |

**型定義・スキーマ・ユーティリティ**:

| ファイル | 状態 | 内容 |
|---------|:----:|------|
| `types/api.ts` | 🔄 | API契約型（SuggestRequest, GenerateResult等）|
| `types/domain.ts` | 🔄 | ドメイン型（ProMarkerState, StencilSelection等）|
| `schemas/parameter.ts` | 🔄 | Zod動的スキーマ生成 |
| `utils/parameter.ts` | 🔄 | パラメータ操作ユーティリティ |
| `constants/toastMessages.ts` | 🔄 | トースト通知メッセージ定義 |

### 3.3 ステンシル一覧 (StencilListPage)

| コンポーネント | 状態 | 説明 |
|-------------|:----:|------|
| `StencilListPage` | 🔄 | カテゴリフィルタ + ステンシルカード一覧 |

### 3.4 ステンシルエディタ (StencilEditor)

| コンポーネント | ファイル | 状態 | 説明 |
|-------------|---------|:----:|------|
| `StencilEditor` | `components/StencilEditor.tsx` | 🔄 | メインエディタ（F11対応）|
| `YamlEditor` | `components/YamlEditor.tsx` | 🔄 | YAML編集 (CodeMirror) |
| `TemplateEditor` | `components/TemplateEditor.tsx` | 🔄 | FTL編集 (CodeMirror + 構文チェック) |
| `FileExplorer` | `components/FileExplorer.tsx` | 🔄 | ファイルツリー |
| `FileTabs` | `components/FileTabs.tsx` | 🔄 | タブ管理 |
| `HistoryDialog` | `components/HistoryDialog.tsx` | 🔄 | バージョン履歴 |
| `VersionHistory` | `components/VersionHistory.tsx` | 🔄 | バージョン情報表示 |
| `StencilManageDialog` | `components/StencilManageDialog.tsx` | 🔄 | ステンシル管理ダイアログ |
| `DiffViewer` | `components/DiffViewer.tsx` | 🔄 | バージョン差分表示 |
| `ErrorPanel` | `components/ErrorPanel.tsx` | 🔄 | バリデーションエラー |
| `PreviewPanel` | `components/PreviewPanel.tsx` | 🔄 | テンプレートプレビュー |

**エディタ API**:

| 関数 | 状態 | エンドポイント |
|------|:----:|-------------|
| `loadStencil()` | 🔄 | `GET /mapi/apps/mste/editor/{stencilId}/{serial}` |
| `saveStencil()` | 🔄 | `POST /mapi/apps/mste/editor/save` |
| `saveCommonSettings()` | 🔄 | `POST /mapi/apps/mste/editor/common/{categoryId}` |
| `getVersionHistory()` | 🔄 | `GET /mapi/apps/mste/editor/{stencilId}/versions` |
| `listStencils()` | 🔄 | `GET /mapi/apps/mste/editor/list` |

**FreeMarker サポートユーティリティ**:

| ファイル | 状態 | 内容 |
|---------|:----:|------|
| `freemarker-lang.ts` | 🔄 | CodeMirror FreeMarkerシンタックス定義 |
| `freemarker-decorator.ts` | 🔄 | テーマ・デコレータ |
| `freemarker-lang-test.ts` | 🔄 | テスト |
| `ftl-validator.ts` | 🔄 | FTL構文バリデーション |
| `file-classifier.ts` | 🔄 | ファイルタイプ判別 |

### 3.5 状態管理方針

| 範囲 | 方式 | 備考 |
|------|------|------|
| ページローカル状態 | `useState` | 選択状態、パラメータ、ダイアログ開閉 |
| API通信状態 | TanStack Query (`useMutation`) | ローディング、エラー、キャッシュ |
| フォーム状態 | React Hook Form + Zod | `useParameterForm` カスタムフック |
| グローバル状態 | なし (Zustand未使用) | ProMarkerはストア不要と判断 |

---

## 4. 拡張領域（ステンシル管理・作成系）

> この章では、現在バックエンドに実装インフラの一部があるが、フルに機能していない/追加開発が必要な領域を整理する。

### 4.1 ステンシルアップロード (ZIP Import)

| 項目 | 状態 |
|------|------|
| **コントローラエンドポイント** | ✅ `POST /api/uploadStencil` 定義済 |
| **APIファサード** | ✅ `UploadStencilApi` 定義済 |
| **サービスインターフェース** | ✅ `UploadStencilService` 定義済 |
| **サービス実装** | ❌ `UploadStencilServiceImp.invoke()` が空 (TODO) |
| **フロントエンド UI** | ❌ 未実装 |

**必要な実装**:
- ZIP解凍ロジック
- stencil-settings.yml バリデーション
- テンプレートファイルの配置先決定（userレイヤー）
- DB マスタ更新
- 上書き確認フロー

### 4.2 カテゴリ共通設定

| 項目 | 状態 |
|------|------|
| **コントローラエンドポイント** | ✅ `POST /editor/common/{categoryId}` 定義済 |
| **サービス実装** | ❌ TODO |
| **フロントエンド API関数** | 🔧 `saveCommonSettings()` 定義済 |
| **フロントエンド UI** | ❌ 未実装 |

**必要な実装**:
- `*_stencil-settings.yml` の保存ロジック
- カテゴリ単位の設定編集UI
- 既存ステンシルへの反映確認

### 4.3 親ステンシルマージ

| 項目 | 状態 |
|------|------|
| **マージロジック** | ✅ `mergeParentStencilSettingsUnified()` 実装済 |
| **親ファイル探索** | ✅ `findParentStencilSettings()` パターンマッチ実装済 |
| **dataDomain マージ** | ✅ `mergeMapList()` 子優先マージ実装済 |
| **実運用の親設定ファイル** | ❌ 未配置 |
| **テスト** | 🔧 部分的 (マージロジックの単体テストあり) |

**現状**: コードインフラは完備しているが、`*_stencil-settings.yml` ファイルの実運用例がない。カテゴリ共通設定機能と連携して有効化する想定。

### 4.4 ステンシル新規作成 (GUI)

| 項目 | 状態 |
|------|------|
| **バックエンド保存** | ✅ `saveStencil()` は新ステンシルも対応 |
| **フロントエンド** | 🔧 `StencilManageDialog` コンポーネントあり |
| **新規作成フロー** | 🔧 エディタの `mode=create` は定義済だが完全なフローは未完成 |

**必要な実装**:
- 新規作成ウィザード（カテゴリ選択→メタ情報入力→テンプレート追加）
- デフォルトテンプレート生成
- stencil-settings.yml 初期生成

### 4.5 ステンシル削除

| 項目 | 状態 |
|------|------|
| **バックエンドAPI** | ❌ エンドポイント未定義 |
| **サービス実装** | ❌ 未実装 |
| **フロントエンドUI** | 🔧 `StencilManageDialog` に削除アクション想定 |

**必要な実装**:
- DELETE エンドポイント追加
- userレイヤーのファイル削除
- DB マスタからの削除
- 確認ダイアログ

### 4.6 ステンシルエクスポート

| 項目 | 状態 |
|------|------|
| **バックエンドAPI** | ❌ 未実装 |
| **フロントエンドUI** | ❌ 未実装 |

**必要な実装**:
- ステンシルディレクトリのZIP化
- ダウンロードエンドポイント
- Export UI (一覧画面 or エディタから)

### 4.7 テンプレートプレビュー

| 項目 | 状態 |
|------|------|
| **バックエンドAPI** | ❌ プレビュー専用エンドポイントなし |
| **フロントエンドUI** | 🔄 `PreviewPanel` コンポーネントあり |

**必要な実装**:
- プレビュー用APIエンドポイント（生成実行せずテンプレート展開結果を返す）
- サンプルパラメータの自動適用
- リアルタイムプレビュー更新

---

## 5. テスト状況

### 5.1 バックエンドテスト

| テストクラス | 対象 | テストケース数 | 状態 |
|------------|------|:----------:|:----:|
| `SuggestServiceImpTest` | SuggestServiceImp | 3+ | ✅ |
| `TemplateEngineProcessorTest` | TemplateEngineProcessor | 1+ | ✅ |
| `LayeredStencilStorageTest` | レイヤード検索 | 1+ | ✅ |
| `ValidationRuleTest` | ValidationRule | 3+ | ✅ |
| `StencilSettingsValidationTest` | YAML + validation | 2+ | ✅ |

**カバレッジ分析**:

| コンポーネント | テスト有無 | 備考 |
|-------------|:--------:|------|
| SuggestServiceImp | ✅ | 3段階フィルタの各状態テスト |
| GenerateServiceImp | ❌ | テストなし — 統合テスト(E2E)で担保 |
| StencilEditorServiceImp | ❌ | テストなし — 手動検証 |
| ReloadStencilMasterServiceImp | ❌ | テストなし — 起動テストで間接的に検証 |
| TemplateEngineProcessor | ✅ | レイヤード検索テスト |
| ValidationRule | ✅ | DTO テスト |
| StencilSettingsYml | ✅ | YAML パース + 後方互換テスト |

### 5.2 フロントエンドテスト

| 対象 | テスト有無 | 備考 |
|------|:--------:|------|
| ProMarkerPage | ❌ | Vitest テスト未実装 |
| StencilEditor | ❌ | Vitest テスト未実装 |
| useParameterForm | ❌ | フック単体テスト未実装 |
| Zod スキーマ | ❌ | スキーマテスト未実装 |
| freemarker-lang | 🔧 | `freemarker-lang-test.ts` あり |

### 5.3 E2E テスト

| ディレクトリ | 対象 | 状態 |
|------------|------|:----:|
| `packages/e2e/tests/specs/promarker-v3/` | ProMarker フロントエンド | ✅ |

---

## 6. ファイル一覧

### 6.1 バックエンド (Java)

```
backend/src/main/java/jp/vemi/mirel/apps/mste/
├── application/controller/
│   ├── ApiController.java              # メインAPI (suggest, generate, reload, upload)
│   └── StencilEditorController.java    # エディタAPI (list, load, save, versions)
├── application/api/
│   ├── MsteApi.java                    # 基底インターフェース
│   ├── SuggestApi.java                 # サジェストファサード
│   ├── GenerateApi.java                # 生成ファサード
│   ├── UploadStencilApi.java           # アップロードファサード
│   └── ReloadStencilMasterApi.java     # リロードファサード
├── domain/service/
│   ├── SuggestService.java             # IF
│   ├── SuggestServiceImp.java          # 実装 ✅
│   ├── GenerateService.java            # IF
│   ├── GenerateServiceImp.java         # 実装 ✅
│   ├── StencilEditorService.java       # IF
│   ├── StencilEditorServiceImp.java    # 実装 ✅
│   ├── UploadStencilService.java       # IF
│   ├── UploadStencilServiceImp.java    # 実装 ❌ TODO
│   ├── ReloadStencilMasterService.java # IF
│   └── ReloadStencilMasterServiceImp.java # 実装 ✅
├── domain/dto/
│   ├── SuggestParameter.java
│   ├── SuggestResult.java
│   ├── GenerateParameter.java
│   ├── GenerateResult.java
│   ├── UploadStencilParameter.java
│   ├── UploadStencilResult.java
│   ├── ReloadStencilMasterParameter.java
│   ├── ReloadStencilMasterResult.java
│   ├── LoadStencilParameter.java
│   ├── LoadStencilResult.java
│   ├── SaveStencilParameter.java
│   ├── SaveStencilResult.java
│   ├── StencilConfigDto.java
│   ├── StencilVersionDto.java
│   └── StencilFileDto.java
├── domain/dao/
│   ├── MsteStencil.java                # @Entity
│   └── MsteStencilRepository.java      # JPA Repository
└── domain/config/
    └── ProMarkerStartupListener.java   # 起動時リロード

backend/src/main/java/jp/vemi/ste/
├── domain/engine/
│   └── TemplateEngineProcessor.java    # FreeMarkerエンジン
├── domain/context/
│   ├── SteContext.java                 # 抽象コンテキスト
│   └── DefaultSteContext.java          # デフォルト実装
├── domain/dto/
│   ├── EngineBinds.java                # テンプレートバインド
│   ├── DictionaryMetaData.java         # 文字列変換ユーティリティ
│   └── yml/
│       ├── StencilSettingsYml.java     # YAMLモデル
│       └── ValidationRule.java         # バリデーション定義
├── domain/reader/
│   └── StructureReader.java            # Excel読込
└── SteExecutor.java                    # 一括実行
```

### 6.2 フロントエンド (TypeScript/React)

```
apps/frontend-v3/src/features/promarker/
├── pages/
│   └── ProMarkerPage.tsx               # 払出画面メイン
├── hooks/
│   ├── useSuggest.ts                   # Suggest API mutation
│   ├── useGenerate.ts                  # Generate + 自動DL
│   ├── useParameterForm.ts             # RHF + Zod 動的validation
│   ├── useFileUpload.ts                # ファイルUP
│   └── useReloadStencilMaster.ts       # マスタリロード
├── components/
│   ├── StencilSelector.tsx             # 3段階Select
│   ├── ParameterFields.tsx             # 動的フォーム
│   ├── ActionButtons.tsx               # ボタン群
│   ├── StencilInfo.tsx                 # メタ情報表示
│   ├── JsonEditor.tsx                  # JSON編集Dialog
│   ├── FileUploadButton.tsx            # ファイルUPボタン
│   └── ErrorBoundary.tsx               # エラーバウンダリ
├── types/
│   ├── api.ts                          # API契約型
│   └── domain.ts                       # ドメイン型
├── schemas/
│   └── parameter.ts                    # Zodスキーマ生成
├── utils/
│   └── parameter.ts                    # パラメータ操作
└── constants/
    └── toastMessages.ts                # 通知メッセージ

apps/frontend-v3/src/features/stencil-editor/
├── pages/
│   └── StencilListPage.tsx             # ステンシル一覧
├── components/
│   ├── StencilEditor.tsx               # メインエディタ
│   ├── YamlEditor.tsx                  # YAML編集
│   ├── TemplateEditor.tsx              # FTL編集
│   ├── FileExplorer.tsx                # ファイルツリー
│   ├── FileTabs.tsx                    # タブ管理
│   ├── HistoryDialog.tsx               # 履歴ダイアログ
│   ├── VersionHistory.tsx              # バージョン表示
│   ├── StencilManageDialog.tsx         # ステンシル管理
│   ├── DiffViewer.tsx                  # 差分表示
│   ├── ErrorPanel.tsx                  # エラーパネル
│   └── PreviewPanel.tsx                # プレビュー
├── api/
│   └── stencil-editor-api.ts           # APIクライアント
├── types/
│   └── index.ts                        # 型定義
└── utils/
    ├── freemarker-lang.ts              # FreeMarkerシンタックス
    ├── freemarker-decorator.ts         # テーマ
    ├── freemarker-lang-test.ts         # テスト
    ├── ftl-validator.ts                # FTL検証
    └── file-classifier.ts              # ファイル分類
```

### 6.3 リソース (ステンシルサンプル)

```
backend/src/main/resources/promarker/stencil/samples/
├── hello-world/
│   └── 250913A/
│       ├── stencil-settings.yml
│       ├── README.md
│       └── files/
│           └── hello.txt.ftl
└── springboot/
    └── spring-boot-service/
        └── 250101A/
            ├── stencil-settings.yml
            ├── README.md
            └── files/
                ├── _packageGroup.d2bs()_/_serviceId.ucc()_Controller.java.ftl
                ├── _packageGroup.d2bs()_/_serviceId.ucc()_Service.java.ftl
                ├── _packageGroup.d2bs()_/_serviceId.ucc()_ServiceImpl.java.ftl
                ├── _packageGroup.d2bs()_/_serviceId.ucc()_Mapper.java.ftl
                ├── _packageGroup.d2bs()_/dto/_eventId.ucc()_Request.java.ftl
                ├── _packageGroup.d2bs()_/dto/_eventId.ucc()_Response.java.ftl
                ├── mapper/_serviceId.ucc()_Mapper.xml.ftl
                └── application.yml.ftl
```

---

## 7. 今後の開発ロードマップ

### 7.1 短期（次スプリント想定）

| 優先度 | タスク | 種別 |
|:------:|--------|------|
| 🔴 高 | フロントエンド UI 品質向上（再開発の安定化） | フロントエンド |
| 🔴 高 | `stencilCategoy` タイポの計画的修正 | 横断 |
| 🟡 中 | GenerateServiceImp の単体テスト追加 | バックエンド |
| 🟡 中 | StencilEditorServiceImp の単体テスト追加 | バックエンド |

### 7.2 中期（拡張機能）

| 優先度 | タスク | 種別 |
|:------:|--------|------|
| 🔴 高 | UploadStencilServiceImp 実装 | バックエンド |
| 🔴 高 | ステンシル新規作成ウィザード完成 | 横断 |
| 🟡 中 | カテゴリ共通設定 (saveCommonSettings) 実装 | バックエンド |
| 🟡 中 | 親ステンシルマージの運用化 | バックエンド |
| 🟡 中 | テンプレートプレビューAPI | バックエンド |
| 🟢 低 | ステンシルエクスポート機能 | 横断 |

### 7.3 長期（将来構想）

| タスク | 備考 |
|--------|------|
| 多言語テンプレートエンジン対応 | FreeMarker以外（Jinja2, EJS等）のサポート |
| ステンシルマーケットプレイス | ステンシルの共有・配布 |
| AI支援ステンシル生成 | Mira AIとの連携でステンシルの自動生成 |
| `data.data.model` レスポンス構造のリファクタ | ModelWrapper 設計見直し |

---

## 付録: 用語・略語集

| 略語 | 正式名 |
|------|--------|
| MSTE | Mirel Stencil Template Engine |
| STE | Stencil Template Engine |
| FTL | FreeMarker Template Language |
| RHF | React Hook Form |
| DL | Download |
| UP | Upload |
