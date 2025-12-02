# mirel Studio Flow Designer パレット構成と拡張ポイント

> 標準パレットとプラグイン拡張機構

---

## 1. 概要

mirel Studio Flow Designer のノード一覧（パレット）は、モデル中心の業務フロー構築に必要な機能のみ厳選されている。

同時に、拡張可能なプラグイン構造も備える。

---

## 2. パレット構成（標準）

```
[イベント]
├─ onCreate
├─ onUpdate
├─ onDelete
├─ onLoad
└─ cron

[データ操作]
├─ Create
├─ Update
├─ Delete
└─ Get

[条件]
└─ IF

[変数操作]
├─ Set
├─ Merge
└─ Extract

[外部連携]
└─ REST Call

[通知]
└─ Email

[システム]
├─ End
└─ ErrorHandler
```

---

## 3. ノードの UI 表示仕様

| 項目 | 仕様 |
|------|------|
| 表示形式 | アイコン＋ノード名 |
| 色分け | カテゴリー単位 |
| サイズ | 固定幅（可読性優先） |
| 選択時 | 右ペインが開く |
| エラーチェック | リアルタイム実行 |

---

## 4. 拡張ポイント（プラグイン構造）

将来、外部開発者・内部チームがノードを追加できるよう、以下の拡張性を提供する。

### 4.1 ノード定義の JSON

```json
{
  "id": "WebhookCall",
  "category": "external",
  "params": [
    { "name": "url", "type": "string" },
    { "name": "headers", "type": "object" },
    { "name": "method", "type": "string", "enum": ["GET", "POST"] }
  ]
}
```

### 4.2 追加手順

```
① ノード定義(JSON)を plugins/flow-nodes 配下へ追加
② Studio 起動時に自動ロード
③ UI にパレットとして追加
④ Runtime では拡張ノード用ハンドラをディスパッチ
```

---

## 5. ノード互換性保証

- 追加ノードは既存モデルと衝突しない範囲で作成
- 型安全性は Studio 側で検証
- Runtime では `execute()` 関数を介して統一的に扱う

---

## 6. Flow Designer が目指す方向性

- ノードの追加より、**組み合わせで業務を表現できる最小構成**
- 過剰なノードは排除して思考負荷を軽減
- ただし外部連携等の拡張には柔軟に対応

---

## 関連ドキュメント

- [ノード体系仕様](./node-spec.md)
- [実行モデル](./execution-model.md)
- [並列実行（将来拡張）](./parallel-execution.md)

---

*Powered by Copilot 🤖*
