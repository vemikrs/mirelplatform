# mirel Studio 命名規約

> DB テーブル・API エンドポイント・Java パッケージの命名ルール

---

## 1. 概要

mirel Studio では一貫性のある命名規約を採用し、開発者の混乱を防ぎ、保守性を向上させる。

---

## 2. DB テーブル名

### 規約

```
stu_{entity_name}
```

| ルール | 例 |
|--------|-----|
| プレフィックス | `stu_` |
| エンティティ名 | スネークケース |
| 小文字 | 大文字禁止 |

### 例

| テーブル名 | 説明 |
|-----------|------|
| `stu_model_header` | モデルヘッダー |
| `stu_field` | フィールド定義 |
| `stu_code_group` | コードグループ |
| `stu_code` | コード値 |
| `stu_form_layout` | フォームレイアウト |
| `stu_flow` | フロー定義 |
| `stu_draft` | Draft |
| `stu_release` | Release |

---

## 3. Java パッケージ

### 規約

```
jp.vemi.mirel.apps.studio.{module}
```

| ルール | 例 |
|--------|-----|
| ベースパッケージ | `jp.vemi.mirel.apps.studio` |
| モジュール単位 | `.modeler`, `.form`, `.flow`, `.release` |

### 例

| パッケージ | 説明 |
|-----------|------|
| `jp.vemi.mirel.apps.studio.modeler` | Modeler サービス |
| `jp.vemi.mirel.apps.studio.form` | Form Designer サービス |
| `jp.vemi.mirel.apps.studio.flow` | Flow Designer サービス |
| `jp.vemi.mirel.apps.studio.release` | Release Center サービス |

---

## 4. API エンドポイント

### 規約

```
/mapi/studio/{resource}/{action}
```

| ルール | 例 |
|--------|-----|
| ベースパス | `/mapi/studio` |
| リソース | ケバブケース |
| アクション | `list`, `get`, `create`, `update`, `delete` |

### 例

| エンドポイント | 説明 |
|---------------|------|
| `POST /mapi/studio/model/list` | モデル一覧 |
| `POST /mapi/studio/model/get` | モデル取得 |
| `POST /mapi/studio/form/create` | フォーム作成 |
| `POST /mapi/studio/flow/update` | フロー更新 |
| `POST /mapi/studio/release/deploy` | Deploy 実行 |

---

## 5. コンポーネント名（ユーザー向け）

### 規約

```
mirel Studio {ComponentName}
```

| コンポーネント | 表示名 |
|---------------|--------|
| Modeler | mirel Studio Modeler |
| Form Designer | mirel Studio Form Designer |
| Flow Designer | mirel Studio Flow Designer |
| Data Browser | mirel Studio Data Browser |
| Release Center | mirel Studio Release Center |

---

## 6. フロントエンド（TypeScript）

### 規約

| 種類 | ルール | 例 |
|------|--------|-----|
| 型名 | PascalCase | `ModelHeader`, `FieldDefinition` |
| 変数名 | camelCase | `modelHeader`, `fieldList` |
| 定数 | UPPER_SNAKE_CASE | `MAX_DEPTH`, `DEFAULT_PAGE_SIZE` |
| ファイル名 | kebab-case | `model-header.ts`, `field-list.tsx` |

---

## 7. 設計意図

- 開発者の混乱を防止
- 命名の一貫性による保守性向上
- チーム開発での共通言語化

---

## 関連ドキュメント

- [用語集](../00_GLOSSARY.md)
- [プロジェクト構成](../10_cross-cutting/project-structure.md)
- [Modeler データモデル](../03_modeler/data-model.md)

---

*Powered by Copilot 🤖*
