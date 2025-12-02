# mirel Studio データモデル全体像

> Builder と Runtime で異なるデータモデルの統合的な構造

---

## 1. 概要

mirel Studio は **Builder（設計）と Runtime（実行）で異なるデータモデル** を採用する。

- Builder では Draft／Release といった構造化 JSON を保持
- Runtime では環境ごとに最適化された JSONB + メタ情報を保持

ここでは両者を統合したデータモデルの全体像を示す。

---

## 2. 全体構成

```
Workspace
├─ Draft (複数)
├─ Release (複数)
├─ Deployments (Dev / Stg / Prod)
└─ RuntimeData (environment別)
```

---

## 3. Builder モデルの ER 図（概念モデル）

```
Workspace
├─ Draft
│    ├─ Model
│    ├─ Forms
│    ├─ Flows
│    ├─ Codes
│    └─ Metadata
│
└─ Release
     ├─ Model
     ├─ Forms
     ├─ Flows
     ├─ Codes
     ├─ Metadata
     └─ Diff
```

Draft と Release の中身は同形式の JSON。Release のみに diff が付与される。

---

## 4. Runtime モデル（概念）

```
RuntimeEntity
├─ transactionId
├─ entityId
├─ transactionData (JSONB)
├─ updateDate
└─ metadata (変更元の Release, Workspace)
```

---

## 5. Builder JSON の構造（統合形式）

```jsonc
{
  "model": { "entities": [ … ] },
  "forms": { … },
  "flows": { … },
  "codes": { … },
  "metadata": { … },
  "diff": { … }      // Release のみ
}
```

---

## 6. Runtime JSONB の構造例

```jsonc
{
  "transactionId": "TX001",
  "entityId": "customer",
  "transactionData": {
    "customerId": "C001",
    "customerName": "山田太郎",
    "address": {
      "zip": "1000001",
      "pref": "東京都",
      "detail": "千代田1-1"
    },
    "orderHistory": [
      { "orderId": "O1", "amount": 1000 },
      { "orderId": "O2", "amount": 2000 }
    ]
  },
  "updateDate": "2025-11-20T10:20:00Z"
}
```

---

## 7. Builder ↔ Runtime の関係

```
Draft → Release → Deploy → RuntimeData
```

| フェーズ | データ形式 | 説明 |
|---------|-----------|------|
| Draft | JSON | 編集中の構造 |
| Release | JSON + Diff | 確定したバージョン |
| Deploy | - | 環境への適用処理 |
| RuntimeData | JSONB | 実行時データ |

---

## 8. DB テーブル設計（概要）

### Builder 側

| テーブル | 説明 |
|---------|------|
| `stu_workspace` | ワークスペース |
| `stu_draft` | ドラフト |
| `stu_release` | リリース |
| `stu_model_header` | モデルヘッダー |
| `stu_field` | フィールド定義 |
| `stu_code` | コード定義 |

### Runtime 側

| テーブル | 説明 |
|---------|------|
| `stu_runtime_entity` | 実行時エンティティデータ |
| `stu_runtime_metadata` | メタデータ |

---

## 9. この構造の利点

| 項目 | 説明 |
|------|------|
| 責務分離 | Builder と Runtime の責務を分離 |
| 可逆性 | Release は完全に固定されるため再現性あり |
| 安全性 | モデル変更の衝突や破壊を防止 |
| 柔軟性 | JSONB による柔軟なデータ保持 |

---

## 関連ドキュメント

- [アーキテクチャ概要](./overview.md)
- [Builder/Runtime 連携](./builder-runtime.md)
- [メタデータ永続化](../10_cross-cutting/metadata-persistence.md)

---

*Powered by Copilot 🤖*
