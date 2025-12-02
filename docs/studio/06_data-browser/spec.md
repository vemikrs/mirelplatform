# mirel Studio Data Browser 仕様

> Runtime データの検索・閲覧・検証ビュー

---

## 1. 概要

mirel Studio Data Browser は、Runtime で保存されたデータを Builder が安全に確認・検証するためのビューである。

---

## 2. 検索仕様（Search）

| 項目 | サポート内容 |
|------|-------------|
| 対象 | 文字列・数値・日付・複合フィールド |
| 条件式 | 完全一致・前方一致・部分一致・範囲検索 |

**例：**

```json
{
  "conditions": [
    { "field": "customerName", "operator": "like", "value": "%田%" },
    { "field": "updateDate", "operator": "between", "value": ["2025-01-01", "2025-01-31"] }
  ]
}
```

---

## 3. フィルタ仕様（Filter）

UI のフィルタバーから以下を指定可能：

| フィルタ種別 | 説明 |
|-------------|------|
| コードグループ選択 | 特定コード値で絞り込み |
| Boolean フィルタ | true/false |
| 日付範囲 | 期間指定 |
| 空欄のみ | null/空文字 |
| 値ありのみ | 非 null |
| 複合モデル | 深い階層にも対応 |

---

## 4. ソート仕様（Sort）

| 項目 | 仕様 |
|------|------|
| 単一キー | 1 項目でソート |
| 複数キー | 優先順位順 |
| 順序 | 昇順・降順 |
| 型対応 | 数値は数値、日付は日付としてソート |

**例：**

```json
{
  "sort": [
    { "field": "customerId", "order": "asc" },
    { "field": "updateDate", "order": "desc" }
  ]
}
```

---

## 5. ページング仕様（Paging）

| 項目 | 仕様 |
|------|------|
| page.number | 1 始まり |
| page.size | デフォルト 20 件 |
| page.total | 総件数 |

---

## 6. 詳細表示との連携

Data Browser でレコードを選択すると、**mirel Studio Form Designer で生成された詳細画面** を自動表示。

| 機能 | 説明 |
|------|------|
| 複合モデル | 折りたたみ可能 |
| Flow 連携 | onLoad が発火して UI を動的調整 |

---

## 7. 設計意図

- Builder が実データを安全に確認できる
- 業務の改善ポイントを把握しやすい
- Form・Flow の動作検証が可能

---

## 関連ドキュメント

- [Form Designer Widget 仕様](../04_form-designer/widget-spec.md)
- [環境管理](../09_operations/workspace-env.md)
- [Runtime API](../08_runtime/api-autogen.md)

---

*Powered by Copilot 🤖*
