# mirel Studio Draft/Release データモデル仕様

> Builder 編集内容の段階的管理

---

## 1. 概要

mirel Studio では Builder で行った編集内容を **Draft → Release → Deploy** の 3 段階で管理する。

Draft と Release は JSON アーティファクトとして保存され、Runtime に適用される。

---

## 2. Draft とは

Draft は「作業中の状態」を示すスナップショット。

| 特徴 | 説明 |
|------|------|
| 即時保存 | Builder での編集作業を即時保存 |
| 差分検出の元データ | Release 作成時の比較対象 |
| 複数保持可能 | 複数個の Draft を保持可能 |
| Deploy 不可 | 中間成果物（Deploy は Release のみ） |

---

## 3. Release とは

Release は「Deploy 可能な成果物」。

| 特徴 | 説明 |
|------|------|
| 差分解析済み | 破壊変更がないか確認済み |
| バージョン付与 | 自動付与（例：1.2.0） |
| 環境適用可能 | Dev/Stg/Prod へ適用可能 |
| 固定化済み | 差分・構造をすべて含んだモデル |

---

## 4. データモデル（共通構造）

```json
{
  "version": "1.2.0",
  "createdAt": "2025-11-20T10:20:00Z",
  "createdBy": "user001",
  "workspaceId": "ws-001",

  "model": { ... },
  "forms": { ... },
  "flows": { ... },
  "codes": { ... },
  "metadata": { ... },

  "diff": { ... }
}
```

---

## 5. Model の JSON 構造

```json
{
  "model": {
    "entities": [
      {
        "id": "customer",
        "name": "顧客",
        "fields": [
          {
            "id": "customerId",
            "type": "string",
            "required": true,
            "key": true
          },
          {
            "id": "address",
            "type": "addressModel",
            "required": false,
            "relations": ["addressModel"]
          }
        ]
      }
    ]
  }
}
```

---

## 6. Forms の JSON 構造

```json
{
  "forms": {
    "customer_detail": {
      "entity": "customer",
      "layout": {
        "type": "grid-12",
        "items": [
          { "field": "customerId", "col": 6 },
          { "field": "customerName", "col": 6 },
          { "field": "address", "col": 12 }
        ]
      }
    }
  }
}
```

---

## 7. Flows の JSON 構造

```json
{
  "flows": {
    "customer_onCreate": {
      "event": "onCreate",
      "entity": "customer",
      "nodes": [
        { "id": "n1", "type": "SetVariable", "params": { "var": "now" } },
        { "id": "n2", "type": "Update", "params": { ... } }
      ],
      "edges": [
        { "from": "n1", "to": "n2" }
      ]
    }
  }
}
```

---

## 8. Release の差分部（diff）

```json
{
  "diff": {
    "model": [
      { "type": "Add", "path": "$.entities[1]", "new": { ... } },
      { "type": "Update", "path": "$.entities[0].fields[2].type", "old": "string", "new": "number" }
    ],
    "forms": [
      { "type": "Remove", "path": "$.forms.customer_detail.layout.items[3]" }
    ],
    "flows": [
      { "type": "Add", "path": "$.flows.customer_onCreate.nodes[3]" }
    ]
  }
}
```

---

## 9. 設計意図

- Runtime は Release のみを参照し、運用中の変動を防止
- Draft は安全に編集可能
- モデル・フォーム・フローの変更が一元管理可能

---

## 関連ドキュメント

- [差分検出アルゴリズム](./diff-algorithm.md)
- [Draft 版管理ポリシー](./draft-versioning.md)
- [差分マージ機能](./diff-merge.md)

---

*Powered by Copilot 🤖*
