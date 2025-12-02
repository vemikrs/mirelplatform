# mirel Studio Draft 版管理ポリシー

> Builder 作業の安全性と共同編集を支える版管理

---

## 1. Draft の位置づけ

Draft は Builder が行う変更作業の「作業領域」であり、安全かつ共同編集を成立させるための隔離空間。

| 特徴 | 説明 |
|------|------|
| 作業領域 | Builder 個人・チームが作業する領域 |
| Release 候補 | Deploy 可能な Release の候補 |
| 隔離空間 | 破壊的変更を試せる |

```
Workspace
└── Draft
    ├── Model
    ├── Form
    └── Flow
```

---

## 2. Draft の命名規則

```
{builderId}/{featureName}/{yyyymmdd}-{sequence}
例：sakapi/order-enhancement/20251201-01
```

---

## 3. Draft の保存タイミング

| タイミング | 動作 |
|-----------|------|
| 編集ごと | 自動保存（Auto-save） |
| 手動保存 | スナップショット生成可能 |

```
Draft Snapshots:
v1 → v2 → v3 …
```

---

## 4. 同期ルール

Workspace 内の最新 Release が更新された場合、Draft 開始時に「同期作業」が必要となる。

**例：**

```
Release 1.2.0 → 1.3.0
Draft は 1.2.0 ベースだったため、差分同期が必要
```

※ 差分同期は「マージ機能」を利用。

---

## 5. 破壊的変更の扱い

以下は「破壊的変更」として Draft 内限定で許容：

| 変更 | 説明 |
|------|------|
| フィールド削除 | 既存データへの影響あり |
| 型変更 | API 互換性の崩壊 |
| 複合モデルの再構築 | 関連構造の変更 |
| フローの削除 | 実行中ロジックへの影響 |

Release 化時に警告を表示。

---

## 6. 廃棄・クリーンアップ

| 条件 | 動作 |
|------|------|
| 30 日間更新なし | アーカイブ対象 |
| 90 日間アクセスなし | 自動削除（管理者設定） |

---

## 7. 設計意図

- 複数 Builder の衝突を避ける
- 過去作業の再現性を確保
- Release との整合性を維持

---

## 関連ドキュメント

- [Draft/Release データモデル](./draft-release-model.md)
- [差分マージ機能](./diff-merge.md)
- [差分検出アルゴリズム](./diff-algorithm.md)

---

*Powered by Copilot 🤖*
