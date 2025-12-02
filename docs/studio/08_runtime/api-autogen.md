# mirel Studio Runtime API 自動生成仕様

> モデル定義からの REST API 自動生成

---

## 1. 概要

mirel Studio Runtime は Release に含まれるモデルをもとに **REST API を自動生成** し、UI と Flow が利用する共通 backend を形成する。

---

## 2. API の原則

| 原則 | 説明 |
|------|------|
| モデルを源泉 | 自動生成 |
| 手書きコード | 存在しない |
| Release バージョン固定 | API スキーマが固定 |
| Entity ごと | CRUD が生成される |

---

## 3. 生成される API 一覧

### 標準 API

| メソッド | パス | 説明 |
|---------|------|------|
| POST | /mapi/studio/{entity}/list | 一覧取得 |
| POST | /mapi/studio/{entity}/get | 単体取得 |
| POST | /mapi/studio/{entity}/create | 新規作成 |
| POST | /mapi/studio/{entity}/update | 更新 |
| POST | /mapi/studio/{entity}/delete | 削除 |

### 将来対応予定

| メソッド | パス | 説明 |
|---------|------|------|
| GET | /mapi/studio/{entity}/search | 検索 |
| POST | /mapi/studio/{entity}/bulk-update | 一括更新 |

---

## 4. API スキーマ（例）

### 4.1 list

**Request:**

```json
{
  "entity": "customer",
  "conditions": [
    { "field": "customerId", "operator": "like", "value": "A%" }
  ],
  "sort": [{ "field": "updateDate", "order": "desc" }],
  "page": { "size": 20, "number": 1 }
}
```

**Response:**

```json
{
  "records": [ ... ],
  "page": { "size": 20, "number": 1, "total": 300 }
}
```

### 4.2 get

**Request:**

```json
{
  "transactionId": "TX12345"
}
```

**Response:**

```json
{
  "transactionId": "TX12345",
  "customerId": "A001",
  "customerName": "山田太郎",
  "address": { ... }
}
```

### 4.3 create / update

**Request:**

```json
{
  "customerId": "A001",
  "customerName": "山田太郎",
  "address": { ... }
}
```

バリデーションは Modeler 定義を参照して実行。

---

## 5. バリデーション仕様

| 項目 | 説明 |
|------|------|
| 必須 | required フィールドのチェック |
| 型チェック | データ型の整合性 |
| キー重複 | 一意性の確認 |
| 関係整合性 | 複合モデルの整合 |
| フローエラー | フロー内で検出された事前エラー |

---

## 6. 生成アルゴリズム

```
① Release から model.entities を読み込む
② 各 entity について CRUD API テンプレートを生成
③ フィールド定義から Schema を生成
④ 関連モデルを再帰的に展開
⑤ ValidationRule を合成
⑥ REST Router を構築
```

---

## 7. Runtime が保証すること

- UI との整合性
- Flow の前提条件保証
- モデル変更による破壊を防止
- Release ごとの安定性

---

## 関連ドキュメント

- [API スキーマ生成](./api-schema.md)
- [キャッシュ戦略](./cache-strategy.md)
- [Modeler データモデル](../03_modeler/data-model.md)

---

*Powered by Copilot 🤖*
