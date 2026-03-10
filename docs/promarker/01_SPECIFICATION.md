# ProMarker 仕様書

> **最終更新**: 2026-03-10  
> **バージョン**: 1.0  
> **内部コード名**: MSTE (Mirel Stencil Template Engine)  
> **ステータス**: 本体は稼働中 / UI再開発中 / ステンシル管理系は拡張中

---

## 目次

1. [概要](#1-概要)
2. [用語定義](#2-用語定義)
3. [システムアーキテクチャ](#3-システムアーキテクチャ)
4. [ステンシル定義仕様](#4-ステンシル定義仕様)
5. [テンプレートエンジン (STE)](#5-テンプレートエンジン-ste)
6. [API 仕様](#6-api-仕様)
7. [ステンシルエディタ仕様](#7-ステンシルエディタ仕様)
8. [データベーススキーマ](#8-データベーススキーマ)
9. [セキュリティ仕様](#9-セキュリティ仕様)
10. [フロントエンド仕様](#10-フロントエンド仕様)

---

## 1. 概要

### 1.1 ProMarker とは

ProMarker は、mirelplatform にバンドルされたコード生成ツールである。**ステンシル** と呼ばれるテンプレート定義を基に、FreeMarker テンプレートエンジンを使用してソースコード・設定ファイル等のプロジェクト成果物を一括生成する。

### 1.2 解決する課題

| 課題 | ProMarker による解決 |
|------|-----|
| プロジェクト初期構築の手間 | ステンシル選択 → パラメータ入力 → ZIP一括生成 |
| コーディング規約の逸脱 | テンプレートにパッケージ構造・命名規則を埋め込み |
| 新人メンバーのランプアップ | 選択肢ベースのUI、バリデーション付きパラメータ |
| 成果物の品質ばらつき | 同一ステンシルから生成することで構造を統一 |

### 1.3 主要機能

```
┌────────────────────────────────────────────────────┐
│ 払出機能（Generate）     ← 生成・ダウンロード      │
│ サジェスト機能（Suggest）← ステンシル検索・選択     │
│ ステンシルエディタ        ← 作成・編集・バージョン管理│
│ ステンシルマスタリロード  ← DB同期                  │
│ ステンシルアップロード    ← ZIPインポート（計画中）  │
└────────────────────────────────────────────────────┘
```

---

## 2. 用語定義

| 用語 | 意味 |
|------|------|
| **ステンシル (Stencil)** | テンプレート群 + パラメータ定義 + メタ情報をまとめた「ひな形パッケージ」|
| **シリアル (Serial)** | ステンシルのバージョン識別子。形式: `YYMMDD` + 英大文字（例: `250101A`）|
| **カテゴリ (Category)** | ステンシルの分類。パス形式で階層化可能（例: `/samples/springboot`）|
| **STE** | Stencil Template Engine。FreeMarker ベースの汎用テンプレート処理エンジン |
| **dataElement** | 生成対象の要素ID一覧（ステンシルが扱う変数のリスト）|
| **dataDomain** | 各要素のパラメータ詳細定義（名前・型・デフォルト値・バリデーション・選択肢）|
| **レイヤー (Layer)** | ステンシル格納先の優先階層。`user` → `standard` → `samples` の順に検索 |
| **払出 (Generate)** | ステンシルを基にコードを生成しZIPファイルとしてダウンロードすること |

---

## 3. システムアーキテクチャ

### 3.1 全体構成

```
┌─────────────────────────────────────────────────────────────────┐
│ Browser                                                         │
│  React 19 + Vite (/promarker, /promarker/stencils, /promarker/editor/*)│
└──────────────────────────┬──────────────────────────────────────┘
                           │ /mapi/* (Vite Proxy)
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│ Spring Boot 3.3 (context-path: /mipla2)                         │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Controller Layer                                         │    │
│  │  ApiController        (/apps/mste/api/*)                 │    │
│  │  StencilEditorController (/apps/mste/editor/*)           │    │
│  └──────────────┬──────────────────────────────────────────┘    │
│                 │                                                │
│  ┌──────────────▼──────────────────────────────────────────┐    │
│  │ API Layer (Facade)                                       │    │
│  │  SuggestApi │ GenerateApi │ ReloadStencilMasterApi │ ... │    │
│  └──────────────┬──────────────────────────────────────────┘    │
│                 │                                                │
│  ┌──────────────▼──────────────────────────────────────────┐    │
│  │ Service Layer                                            │    │
│  │  SuggestServiceImp                                       │    │
│  │  GenerateServiceImp                                      │    │
│  │  StencilEditorServiceImp                                 │    │
│  │  ReloadStencilMasterServiceImp                           │    │
│  │  UploadStencilServiceImp (未実装)                         │    │
│  └──────────────┬──────────────────────────────────────────┘    │
│                 │                                                │
│  ┌──────────────▼──────────────────────────────────────────┐    │
│  │ Engine Layer (jp.vemi.ste)                               │    │
│  │  TemplateEngineProcessor (FreeMarker 2.3.29)            │    │
│  │  SteContext / EngineBinds / DictionaryMetaData          │    │
│  │  StructureReader (Apache POI Excel)                     │    │
│  │  StencilSettingsYml (YAML Model)                        │    │
│  └──────────────┬──────────────────────────────────────────┘    │
│                 │                                                │
│  ┌──────────────▼──────────────────────────────────────────┐    │
│  │ Storage / Persistence                                    │    │
│  │  MsteStencilRepository (JPA)                             │    │
│  │  FileManagementService (生成物の一時保存)                  │    │
│  │  StorageService (レイヤードファイルシステム)                │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Stencil Storage (Layered)                                │    │
│  │  [1] user/stencil/     ← ユーザー定義（最優先）          │    │
│  │  [2] standard/stencil/ ← 組織標準                        │    │
│  │  [3] samples/          ← classpath内サンプル（読取専用）  │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 レイヤードストレージ

ステンシルは3つのレイヤーで管理され、上位が優先される:

| 優先度 | レイヤー | パス | 用途 |
|--------|----------|------|------|
| 1 (最高) | **user** | `data/storage/user/stencil/` | テナント・ユーザー独自のステンシル |
| 2 | **standard** | `data/storage/standard/stencil/` | 組織の標準ステンシル |
| 3 (最低) | **samples** | `classpath:/promarker/stencil/samples/` | バンドルサンプル（読取専用） |

同一ステンシルIDが複数レイヤーに存在する場合、上位レイヤーの定義が使われる。

### 3.3 ステンシルディレクトリ構造

```
{layer}/{categoryPath}/{stencilName}/
  └── {serial}/                          # 例: 250101A
      ├── stencil-settings.yml           # ★ ステンシル定義（必須）
      ├── README.md                      # ドキュメント（任意）
      └── files/                         # テンプレートファイル群
          ├── _packageGroup.d2bs()_/     # 変数展開ディレクトリ名
          │   └── _serviceId.ucc()_Controller.java.ftl
          ├── application.yml.ftl
          └── ...
```

---

## 4. ステンシル定義仕様

### 4.1 stencil-settings.yml 構造

```yaml
stencil:
  # ── メタ情報 ──
  config:
    id: "/samples/springboot/spring-boot-service"    # 一意ID（パス形式）
    name: "Spring Boot Service Generator"             # 表示名
    categoryId: "/samples/springboot"                 # カテゴリID
    categoryName: "Spring Boot Samples"               # カテゴリ表示名
    serial: "250101A"                                 # シリアル番号
    lastUpdate: "2025/01/01"                          # 最終更新日
    lastUpdateUser: "mirelplatform"                   # 最終更新者
    description: "Spring Boot + MyBatisのサービスを生成" # 説明

  # ── 生成要素リスト ──
  dataElement:
    - id: "packageGroup"
    - id: "serviceId"
    - id: "serviceName"

  # ── パラメータ詳細定義 ──
  dataDomain:
    - id: "packageGroup"
      name: "パッケージグループ"
      value: "com.example"                # デフォルト値
      type: "text"                        # text | file | select
      placeholder: "例: com.example.app"
      note: "Javaのパッケージ名をドット区切りで"
      validation:
        required: true
        minLength: 3
        maxLength: 100
        pattern: "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$"
        errorMessage: "小文字英数字とドット（.）のみ使用可能です"
      options: []                         # type=select の場合に使用

    - id: "language"
      name: "言語"
      value: "ja"
      type: "select"
      options:
        - value: "ja"
          text: "日本語"
        - value: "en"
          text: "English"

    - id: "structureFile"
      name: "構造定義Excel"
      type: "file"                        # ファイルアップロード型
      note: "_@generate シートを参照"

  # ── コード情報 ──
  codeInfo:
    copyright: "Copyright (c) 2025"
    versionNo: "1.0"
    author: "mirelplatform"
    vendor: "Open Source Community"

  # ── マスタデータ（任意） ──
  store:
    - id: "languageOptions"
      items:
        - value: "java"
          text: "Java"
        - value: "kotlin"
          text: "Kotlin"
```

### 4.2 パラメータ型

| type | 説明 | UI表現 |
|------|------|--------|
| `text` | テキスト入力 | `<Input>` フィールド |
| `file` | ファイルアップロード | `<FileUploadButton>` + StructureReader解析 |
| `select` | 選択肢 | `<Select>` ドロップダウン (options使用) |

### 4.3 バリデーションルール (ValidationRule)

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `required` | Boolean | 必須入力 |
| `minLength` | Integer | 最小文字数 |
| `maxLength` | Integer | 最大文字数 |
| `pattern` | String | 正規表現パターン |
| `errorMessage` | String | エラー時メッセージ |

バリデーションはバックエンド・フロントエンド両方で実施:
- **バックエンド**: `GenerateServiceImp` がパラメータ検証
- **フロントエンド**: `useParameterForm` (React Hook Form + Zod) が動的スキーマ生成

### 4.4 親ステンシル設定（マージ機能）

ステンシル階層で共通のパラメータを親ディレクトリの `*_stencil-settings.yml` に定義でき、子ステンシルにマージされる。

```
/springboot/                          # カテゴリ
  ├── _stencil-settings.yml           # ★ 親設定（dataDomain共通定義）
  └── spring-boot-service/
      └── 250101A/
          └── stencil-settings.yml    # 子設定（親のdataDomainを継承）
```

**マージルール**:
- 子の `dataDomain` が優先
- 親の `dataDomain` のうち、子に同一IDが存在しない要素が追加される
- 同一ID要素は子の値で上書き

> **現状**: マージロジック (`mergeParentStencilSettingsUnified`) は実装済みだが、親設定ファイルの実運用例はまだない。

### 4.5 シリアル番号規則

- **形式**: `YYMMDD` + 英大文字（例: `250101A`, `250101B`）
- **正規表現**: `[0-9]{6}[A-Z]+`
- **同日複数版**: `A` → `B` → `C` と英字部分がインクリメント
- **生成**: `StencilEditorServiceImp.generateNewSerial()` が自動生成

---

## 5. テンプレートエンジン (STE)

### 5.1 概要

STE (Stencil Template Engine) は `jp.vemi.ste` パッケージに実装された汎用テンプレート処理エンジン。FreeMarker 2.3.29 をベースにステンシル固有の機能を追加している。

### 5.2 クラス構成

| クラス | 役割 |
|--------|------|
| `TemplateEngineProcessor` | メインエンジン。設定読込・テンプレート実行・ファイル生成を統括 |
| `SteContext` | コンテキスト（LinkedHashMap拡張）。テンプレート変数を保持 |
| `DefaultSteContext` | SteContext のデフォルト実装 |
| `EngineBinds` | FreeMarker テンプレートバインドマップ |
| `DictionaryMetaData` | 文字列変換ユーティリティ（`toUcc()`, `toLcc()` 等）|
| `StructureReader` | Apache POI による Excel 構造化データ読込 |
| `StencilSettingsYml` | stencil-settings.yml の Java モデル |
| `ValidationRule` | バリデーションルール DTO |
| `SteExecutor` | 一括実行ユーティリティ |

### 5.3 生成パイプライン

```
[1] SteContext 生成
    └─ ステンシル名 + シリアル番号を指定

[2] TemplateEngineProcessor.create(context, resolver)
    └─ レイヤード検索でステンシルを特定
    └─ stencil-settings.yml パース
    └─ 親ステンシル設定マージ

[3] engine.appendContext(userParams)
    └─ ユーザー入力パラメータをコンテキストに追加
    └─ StringContent 変換（型=file の場合は Excel 読込）

[4] engine.execute()
    ├─ FreeMarker Configuration 生成（MultiTemplateLoader）
    │   ├─ FileTemplateLoader（ファイルシステム）
    │   ├─ ClassTemplateLoader（classpath）
    │   └─ FileTemplateLoader（テンポラリ）
    │
    ├─ テンプレートファイル一覧取得
    │   ├─ ファイルシステム探索
    │   └─ classpath リソース探索
    │
    ├─ 各テンプレートに対して:
    │   ├─ [a] ファイル名の変数展開
    │   │    例: _serviceId.ucc()_Controller.java.ftl
    │   │    → UserServiceController.java
    │   │
    │   ├─ [b] FreeMarker テンプレート処理
    │   │    コンテキスト変数 + DictionaryMetaData をバインド
    │   │
    │   └─ [c] 出力ファイル書き込み
    │
    └─ 出力ディレクトリパスを返却

[5] 出力をZIP化 → FileRegisterService で登録 → fileId 返却
```

### 5.4 ファイル名の変数展開

テンプレートファイル名に含まれる変数式がコンテキスト値で置換される:

| 式 | 変換 | 例 (serviceId=userService) |
|----|------|---------------------------|
| `_serviceId_` | そのまま | `userService` |
| `_serviceId.ucc()_` | UpperCamelCase | `UserService` |
| `_serviceId.d2bs()_` | ドット→パスセパレータ | `user/Service` |
| `_packageGroup.d2bs()_` | ドット→パスセパレータ | `com/example/app` |

**SteContext.StringContent** がこれらの変換を提供:
- `ucc()` / `toUpperCamelCase()` — Upper Camel Case
- `d2bs()` — Dot to Backslash (実際はパスセパレータ)

### 5.5 FreeMarker テンプレート内で利用可能な変数

| 変数名 | 型 | 内容 |
|--------|-----|------|
| `{parameterId}` | StringContent | dataDomain の各パラメータ値 |
| `meta` | DictionaryMetaData | `meta.toUcc(str)`, `meta.toLcc(str)` 等 |
| `{storeId}` | List<ValueText> | store 定義のマスタデータ |
| `_@generate` 系 | List<Map> | StructureReader で読み込まれた Excel データ |

### 5.6 Excel 構造化データ (StructureReader)

`type: file` のパラメータがアップロードされた場合、Excel ファイルを Apache POI で解析:

- **対象シート**: `_@generate` で始まるシート名
- **ヘッダ行**: 5行目
- **データ領域**: 6行目以降
- **戻り値**: `Map<String, List<Map<String, Object>>>` (シート名→行リスト)

テンプレート内では `${data_generate[0].columnName}` 等でアクセスできる。

---

## 6. API 仕様

### 6.1 エンドポイント一覧

全エンドポイントのベースパス: `/mipla2/apps/mste`  
フロントエンドからのアクセス: `/mapi/apps/mste` (Vite Proxy 経由)

#### メイン API (`/api`)

| メソッド | パス | 機能 | 実装状態 |
|----------|------|------|---------|
| POST | `/api/suggest` | ステンシル候補取得・パラメータ解決 | ✅ 稼働中 |
| POST | `/api/generate` | コード生成 → ZIP ダウンロード | ✅ 稼働中 |
| POST | `/api/reloadStencilMaster` | YAML→DB 同期 | ✅ 稼働中 |
| POST | `/api/uploadStencil` | ステンシル ZIP アップロード | ❌ 未実装 (TODO) |

#### ステンシルエディタ API (`/editor`)

| メソッド | パス | 機能 | 実装状態 |
|----------|------|------|---------|
| GET | `/editor/list` | ステンシル一覧取得 | ✅ 稼働中 |
| GET | `/editor/{stencilPath}/{serial}` | ステンシル読込 | ✅ 稼働中 |
| GET | `/editor/{stencilPath}/versions` | バージョン履歴取得 | ✅ 稼働中 |
| POST | `/editor/save` | ステンシル保存 | ✅ 稼働中 |
| POST | `/editor/common/{categoryId}` | 共通設定保存 | ❌ 未実装 (TODO) |

#### 共通 API

| メソッド | パス | 機能 |
|----------|------|------|
| POST | `/commons/upload` | ファイルアップロード（multipart） |
| POST | `/commons/download` | ファイルダウンロード (fileIdで指定) |
| GET | `/commons/dlsite/{fileId}` | ダイレクトダウンロードリンク |

### 6.2 Suggest API 詳細

**リクエスト**:
```json
{
  "content": {
    "stencilCategoy": "*",
    "stencilCanonicalName": "*",
    "serialNo": "*",
    "selectFirstIfWildcard": true
  }
}
```

> ⚠️ `stencilCategoy` は既知のタイポ（`stencilCategory` が正）。後方互換のため維持。

**レスポンス**:
```json
{
  "data": {
    "model": {
      "stencil": {
        "config": {
          "id": "/samples/springboot/spring-boot-service",
          "name": "Spring Boot Service Generator",
          "categoryId": "/samples/springboot",
          "categoryName": "Spring Boot Samples",
          "serial": "250101A",
          "lastUpdate": "2025/01/01",
          "lastUpdateUser": "mirelplatform",
          "description": "..."
        }
      },
      "params": {
        "nodeType": "ROOT",
        "childs": [
          {
            "id": "packageGroup",
            "name": "パッケージグループ",
            "valueType": "text",
            "value": "com.example",
            "placeholder": "例: com.example.app",
            "note": "...",
            "nodeType": "ELEMENT",
            "validation": {
              "required": true,
              "minLength": 3,
              "maxLength": 100,
              "pattern": "^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$",
              "errorMessage": "..."
            }
          }
        ]
      },
      "fltStrStencilCategory": {
        "items": [{"value": "/samples", "text": "Samples"}, ...],
        "selected": "/samples"
      },
      "fltStrStencilCd": {
        "items": [{"value": "/samples/springboot/spring-boot-service", "text": "Spring Boot Service Generator"}, ...],
        "selected": "/samples/springboot/spring-boot-service"
      },
      "fltStrSerialNo": {
        "items": [{"value": "250101A", "text": "250101A"}, ...],
        "selected": "250101A"
      }
    }
  }
}
```

**3段階フィルタリング**:

```
[Stage 1] カテゴリ取得
  DB: SELECT * FROM mste_stencil WHERE item_kind='0'
  → fltStrStencilCategory 生成

[Stage 2] ステンシル取得
  DB: SELECT * FROM mste_stencil WHERE stencil_cd LIKE '{category}%' AND item_kind='1'
  → fltStrStencilCd 生成

[Stage 3] シリアル＆パラメータ取得
  TemplateEngineProcessor.getSerialNos()   → fltStrSerialNo
  TemplateEngineProcessor.getStencilSettings() → params + stencil.config
```

`selectFirstIfWildcard: true` の場合、各段階でワイルドカード（`*`）指定時に最初の候補を自動選択する。

### 6.3 Generate API 詳細

**リクエスト**:
```json
{
  "content": {
    "stencilCategoy": "/samples/springboot",
    "stencilCanonicalName": "/samples/springboot/spring-boot-service",
    "serialNo": "250101A",
    "packageGroup": "com.example.myapp",
    "serviceId": "userService",
    "serviceName": "ユーザーサービス",
    "eventId": "get",
    "eventName": "取得"
  }
}
```

**レスポンス**:
```json
{
  "data": {
    "files": [
      { "a1b2c3d4-xxxx-xxxx-xxxx": "spring-boot-service-250101A.zip" }
    ],
    "errors": []
  }
}
```

**処理フロー**:
1. パラメータバリデーション
2. `TemplateEngineProcessor.create()` でエンジン初期化
3. `type: file` パラメータがあれば `StructureReader` で Excel 読込
4. `engine.appendContext()` でユーザー入力値をバインド
5. `engine.execute()` でテンプレート処理 → 出力ディレクトリ生成
6. 出力を ZIP 化 → `FileRegisterService` で登録
7. `Pair<fileId, fileName>` を返却

**後続処理** (フロントエンド):
- `POST /commons/download` で ZIP をダウンロード
- Blob レスポンスをブラウザ経由で自動保存

### 6.4 ReloadStencilMaster API 詳細

**リクエスト**: `{ "content": {} }` (空)

**処理**:
1. 全レイヤーから `stencil-settings.yml` をスキャン
2. classpath サンプルをファイルシステムに展開（初回のみ）
3. 各 YAML をパース → `MsteStencil` エンティティに変換
4. DB に INSERT（重複は無視）
5. カテゴリレコードも自動生成

**起動時自動実行**: `ProMarkerStartupListener` が `ApplicationReadyEvent` で自動リロード。  
設定値: `mirel.apps.mste.auto-reload-stencil-on-startup` (デフォルト: `true`)

### 6.5 Stencil Editor API 詳細

#### GET /editor/list

**リクエスト**: `?categoryId=/samples` (省略可)

**レスポンス**:
```json
{
  "data": {
    "categories": [
      { "id": "/samples", "name": "Samples", "stencilCount": 2 }
    ],
    "stencils": [
      {
        "id": "/samples/hello-world",
        "name": "Hello World Generator",
        "categoryId": "/samples",
        "categoryName": "Samples",
        "latestSerial": "250913A",
        "lastUpdate": "2025/09/13",
        "lastUpdateUser": "mirelplatform",
        "description": "...",
        "versionCount": 1
      }
    ]
  }
}
```

#### GET /editor/{stencilId}/{serial}

**レスポンス**:
```json
{
  "data": {
    "config": { "id": "...", "name": "...", ... },
    "files": [
      {
        "path": "stencil-settings.yml",
        "name": "stencil-settings.yml",
        "content": "stencil:\n  config:\n    ...",
        "type": "stencil-settings",
        "language": "yaml",
        "isEditable": true
      },
      {
        "path": "files/_serviceId.ucc()_Controller.java.ftl",
        "name": "_serviceId.ucc()_Controller.java.ftl",
        "content": "...",
        "type": "template",
        "language": "java",
        "isEditable": true
      }
    ],
    "versions": [
      { "serial": "250101A", "createdAt": "...", "createdBy": "...", "isActive": true }
    ]
  }
}
```

#### POST /editor/save

**リクエスト**:
```json
{
  "content": {
    "stencilId": "/samples/springboot/spring-boot-service",
    "serial": "250101A",
    "config": { ... },
    "files": [
      { "path": "stencil-settings.yml", "content": "..." },
      { "path": "files/template.ftl", "content": "..." }
    ],
    "message": "初版作成"
  }
}
```

**レスポンス**:
```json
{
  "data": {
    "newSerial": "260310A",
    "success": true
  }
}
```

---

## 7. ステンシルエディタ仕様

### 7.1 機能概要

ブラウザ上でステンシルの作成・編集・バージョン管理を行う機能。IDE風のファイルエクスプローラ + コードエディタを提供。

### 7.2 画面構成

```
┌──────────────────────────────────────────────────────────┐
│ [ステンシル一覧] /promarker/stencils                      │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐           │
│  │ カテゴリ    │ │ カテゴリ    │ │ 全て       │  フィルタ   │
│  └────────────┘ └────────────┘ └────────────┘           │
│  ┌──────────────────────────────────────────────┐       │
│  │ ステンシルカード                                │       │
│  │  名前 / カテゴリ / シリアル / 更新日             │       │
│  │  [参照] [編集]                                 │       │
│  └──────────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│ [エディタ] /promarker/editor/{stencilId}/{serial}         │
│  ┌──────────┬──────────────────────────────────┐        │
│  │ファイル   │ [タブ: settings.yml] [tab2] [tab3]│        │
│  │エクスプロ │──────────────────────────────────│        │
│  │ーラ      │                                  │        │
│  │          │  CodeMirror エディタ             │        │
│  │ settings │  (YAML / FreeMarker シンタックス)  │        │
│  │ files/   │                                  │        │
│  │  ├ tmpl1 │                                  │        │
│  │  └ tmpl2 │──────────────────────────────────│        │
│  │          │ [プレビュー] [エラー] [履歴]        │        │
│  └──────────┴──────────────────────────────────┘        │
│  [保存] [バージョン履歴] [F11: フルスクリーン]              │
└──────────────────────────────────────────────────────────┘
```

### 7.3 ファイルタイプ分類

| FileType | 判定条件 | エディタ言語 |
|----------|---------|-------------|
| `stencil-settings` | ファイル名が `stencil-settings.yml` | `yaml` |
| `category-settings` | ファイル名が `*_stencil-settings.yml` | `yaml` |
| `template` | 拡張子 `.ftl` | 元の拡張子から推定 (`.java.ftl`→`java`) |
| `gitkeep` | ファイル名が `.gitkeep` | — (非表示) |
| `other` | 上記以外 | 拡張子から推定 |

### 7.4 FreeMarker シンタックスサポート

フロントエンドに CodeMirror ベースの FreeMarker 言語サポートを実装:
- **freemarker-lang.ts**: シンタックスハイライト定義
- **freemarker-decorator.ts**: テーマ・デコレータ
- **ftl-validator.ts**: FTL 構文バリデーション

### 7.5 バージョン管理

- ステンシルの保存時に新しいシリアル番号が自動生成される
- 既存バージョンは上書きされず、新ディレクトリとして保存
- バージョン履歴はディレクトリ走査 + YAML メタ情報から構築
- 差分表示コンポーネント (`DiffViewer`) でバージョン間比較が可能

---

## 8. データベーススキーマ

### 8.1 mste_stencil テーブル

```sql
CREATE TABLE mste_stencil (
    stencil_cd   VARCHAR(255) PRIMARY KEY,  -- カテゴリID or ステンシルID
    stencil_name VARCHAR(255),              -- 表示名
    item_kind    VARCHAR(1),                -- '0'=カテゴリ, '1'=ステンシル
    sort         INTEGER                    -- ソート順
);
```

**用途**: Suggest API のカテゴリ/ステンシル選択肢をDBクエリで高速取得するためのマスタテーブル。ステンシルの実体（YAML、テンプレート）はファイルシステム/classpath に格納。

**データ例**:
| stencil_cd | stencil_name | item_kind | sort |
|-----------|--------------|-----------|------|
| `/samples` | Samples | 0 | 0 |
| `/samples/springboot` | Spring Boot Samples | 0 | 0 |
| `/samples/hello-world` | Hello World Generator | 1 | 0 |
| `/samples/springboot/spring-boot-service` | Spring Boot Service... | 1 | 0 |

### 8.2 関連テーブル (基盤層)

| テーブル | 関連 | 用途 |
|---------|------|------|
| `file_management` | 生成物ZIP保存 | fileId(UUID), fileName, filePath, expireDate (10年) |

---

## 9. セキュリティ仕様

### 9.1 パストラバーサル防御

| 箇所 | 実装 |
|------|------|
| `TemplateEngineProcessor.constructSecurePath()` | `Path.normalize()` + `startsWith(baseDir)` チェック |
| `StencilEditorServiceImp.loadStencil()` | パス正規化後、ベースディレクトリ内か検証 |
| `StencilEditorServiceImp.saveStencil()` | 保存先パスのトラバーサル検証 |
| `SanitizeUtil.sanitizeCanonicalPath()` | `..` と `\` の除去 |
| `SanitizeUtil.sanitizeIdentifierAllowWildcard()` | シリアル番号の文字種制限 |

### 9.2 入力サニタイズ

- ステンシルID、カテゴリID、シリアル番号は `SanitizeUtil` 経由でサニタイズ
- テンプレート処理中の値は `StringContent` 経由で変換（直接文字列結合なし）
- ファイルアップロードはサイズ制限あり（画像は2MB自動圧縮）

### 9.3 認証・認可

- API アクセスは mirelplatform の認証基盤に統合（JWT / セッション）
- フロントエンドは `authStore` からBearerトークンを付与
- 401レスポンスで自動ログアウト

---

## 10. フロントエンド仕様

### 10.1 ルーティング

| パス | コンポーネント | タイトル |
|------|-------------|---------|
| `/promarker` | `ProMarkerPage` | ProMarker - 払出画面 |
| `/promarker/stencils` | `StencilListPage` | ProMarker - ステンシル一覧 |
| `/promarker/editor/*` | `StencilEditor` | ProMarker - エディタ |
| `/mirel/mste` | `ProMarkerPage` | (後方互換 / E2E用) |

### 10.2 払出画面 (ProMarkerPage)

**3ステップUI**:

```
[Step 1: 選択]  →  [Step 2: 入力]  →  [Step 3: 生成]
  カテゴリ            パラメータ入力       生成ボタン押下
  ステンシル           バリデーション       ZIPダウンロード
  シリアル
```

**コンポーネント構成**:

| コンポーネント | ファイル | 役割 |
|-------------|---------|------|
| `StencilSelector` | StencilSelector.tsx | 3段階 Select UI（カテゴリ→ステンシル→シリアル）|
| `ParameterFields` | ParameterFields.tsx | 動的パラメータフォーム（dataDomain からレンダリング）|
| `ActionButtons` | ActionButtons.tsx | 生成・クリア・リロード等のボタン群 |
| `StencilInfo` | StencilInfo.tsx | ステンシルメタ情報表示（config 由来）|
| `JsonEditor` | JsonEditor.tsx | JSON形式で実行パラメータを直接編集するダイアログ |
| `FileUploadButton` | FileUploadButton.tsx | ファイルアップロード（type=file パラメータ用）|
| `ErrorBoundary` | ErrorBoundary.tsx | React Error Boundary |

**カスタムフック**:

| フック | 用途 |
|--------|------|
| `useSuggest()` | TanStack Query mutation でサジェスト API を呼び出し |
| `useGenerate()` | 生成 API 呼び出し + 自動 ZIP ダウンロード |
| `useParameterForm(parameters)` | React Hook Form + Zod 動的バリデーション |
| `useFileUpload()` | ファイルアップロード（画像自動圧縮対応）|
| `useReloadStencilMaster()` | マスタリロード API |

**状態管理**:
- Zustand ストアは使用せず、ローカル `useState` で管理
- TanStack Query (useMutation) で API 通信状態を管理

### 10.3 バリデーション（フロントエンド）

`useParameterForm` フックが stencil-settings.yml の `validation` 定義から Zod スキーマを動的生成:

```typescript
// validation.required → z.string().min(1)
// validation.minLength → z.string().min(n)
// validation.maxLength → z.string().max(n)
// validation.pattern → z.string().regex(new RegExp(pattern))
// type: file → z.string() (fileId文字列)
```

### 10.4 ステンシル一覧 (StencilListPage)

- カテゴリフィルター付きカード一覧
- `listStencils()` API で取得
- 各カードに「参照」「編集」ボタン → エディタへ遷移

### 10.5 ステンシルエディタ (StencilEditor)

- URL: `/promarker/editor/{stencilId}/{serial}?mode=view|edit`
- CodeMirror ベースのコードエディタ
- ファイルエクスプローラ + タブ管理
- FreeMarker シンタックスハイライト
- バリデーションエラーパネル
- プレビューパネル
- 差分表示 (DiffViewer)
- バージョン履歴ダイアログ
- F11 フルスクリーン対応

### 10.6 共通 UI コンポーネント (@mirel/ui)

ProMarker で使用する `@mirel/ui` コンポーネント:

```
Card, CardHeader, CardTitle, CardContent  — レイアウト
Button (elevated, outline, subtle, ghost) — アクション
Select, SelectContent, SelectItem, SelectTrigger, SelectValue — セレクト
Input — テキスト入力
FormField, FormLabel, FormHelper, FormError, FormRequiredMark — フォーム
Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter — モーダル
Badge — ステータス表示
SectionHeading — ページヘッダ
StepIndicator — ステップ表示
Alert, AlertDescription — エラー表示
toast — 通知トースト
```

### 10.7 トーストメッセージ

| キー | タイトル |
|------|---------|
| `generateSuccess` | 生成が完了しました |
| `generateError` | 生成に失敗しました |
| `clearAll` | 入力をリセットしました |
| `clearStencil` | ステンシル情報を再取得しました |
| `reloadSuccess` | マスタをリロードしました |
| `reloadError` | リロードに失敗しました |
| `jsonApplySuccess` | JSONを適用しました |
| `jsonApplyError` | JSON適用時にエラーが発生しました |
| `fileUploadSuccess` | ファイルをアップロードしました |
| `fileUploadError` | ファイルアップロードに失敗しました |

---

## 付録A: バンドルサンプルステンシル

### A.1 Hello World (`/samples/hello-world/250913A`)

- **パラメータ**: message (text), userName (text, 必須), language (select: ja/en)
- **テンプレート**: 1ファイル
- **用途**: 動作確認・チュートリアル

### A.2 Spring Boot Service (`/samples/springboot/spring-boot-service/250101A`)

- **パラメータ**: packageGroup, applicationId, serviceId, serviceName, eventId, eventName, version, author, vendor (計9個)
- **テンプレート**: 8ファイル (Controller, Service, ServiceImpl, Mapper, RequestDto, ResponseDto, mapper.xml, application.yml)
- **用途**: Spring Boot + MyBatis のサービス雛形

---

## 付録B: 設定プロパティ

| キー | デフォルト | 説明 |
|------|----------|------|
| `mirel.apps.mste.auto-reload-stencil-on-startup` | `true` | 起動時自動リロード |
| `spring.servlet.context-path` | `/mipla2` | APIコンテキストパス（変更禁止）|
| Vite proxy `/mapi` | → `localhost:3000/mipla2` | フロント→API プロキシ |

---

## 付録C: 既知の技術的課題

| 課題 | 影響 | 備考 |
|------|------|------|
| `stencilCategoy` タイポ | API互換性 | フロント・バック両方で維持中。修正時は全面置換が必要 |
| API レスポンスの `data.data.model` 3重ネスト | フロントのデータ取得が冗長 | ModelWrapper の設計見直しが必要 |
| 親ステンシルマージの実運用例なし | 共通パラメータの再利用ができていない | ロジックは実装済み、設定ファイルが未配置 |
