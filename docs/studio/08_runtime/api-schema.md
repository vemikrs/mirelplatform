# mirel Studio Runtime API スキーマ生成仕様

> モデル情報からの動的 API スキーマ生成

---

## 1. 概要

mirel Studio はモデル情報から Runtime の CRUD API スキーマを動的生成する。

---

## 2. API スキーマの構成

Runtime の API は以下を自動生成：

| エンドポイント | 説明 |
|---------------|------|
| /mapi/studio/{entity}/list | 一覧取得 |
| /mapi/studio/{entity}/load | 単体ロード |
| /mapi/studio/{entity}/save | 保存 |
| /mapi/studio/{entity}/delete | 削除 |

---

## 3. スキーマ生成元

| 項目 | 説明 |
|------|------|
| Modeler の Entity 構造 | 基本構造 |
| フィールドの型・必須・コード体系 | 属性情報 |
| 複合要素（Embedded） | ネスト構造 |
| キー項目（Primary Key） | 一意識別子 |

---

## 4. list 仕様

**Request:**

```json
{
  "filters": { ... },
  "page": 1,
  "size": 20
}
```

**Response:**

```json
{
  "total": 100,
  "records": [...]
}
```

---

## 5. load 仕様

**Request:**

```json
{
  "transactionId": "xxx"
}
```

**Response:** Model に基づく正規化済データ。

---

## 6. save 仕様

| チェック | 説明 |
|---------|------|
| 必須チェック | required フィールド |
| 型チェック | データ型の整合性 |
| Key 重複チェック | 一意性の確認 |
| サブモデル | 再帰保存 |

---

## 7. delete 仕様

- transactionId 指定で削除
- 物理削除（将来論理削除オプション）

---

## 8. API スキーマの出力形式

以下形式で自動生成可能：

| 形式 | 説明 |
|------|------|
| JSON Schema | スキーマ定義 |
| OpenAPI (YAML) | API ドキュメント |
| TypeScript 型定義 | フロントエンド型 |

**例（OpenAPI 抜粋）：**

```yaml
components:
  schemas:
    Customer:
      type: object
      properties:
        customerId: { type: string }
        age: { type: number }
```

---

## 9. 設計意図

- モデル主導（Model-driven Architecture）
- API 保守コストの削減
- データ整合性の確保

---

## 関連ドキュメント

- [API 自動生成仕様](./api-autogen.md)
- [Modeler データモデル](../03_modeler/data-model.md)
- [Modeler API 仕様](../03_modeler/api-spec.md)

---

*Powered by Copilot 🤖*
