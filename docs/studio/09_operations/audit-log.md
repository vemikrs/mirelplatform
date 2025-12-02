# mirel Studio 監査ログ仕様

> 内部統制・コンプライアンス対応のための改ざん不可ログ

---

## 1. 概要

mirel Studio は組織利用を念頭に置き、**操作ログ（Operation Log）** と **監査ログ（Audit Log）** を明確に分離して管理する。

---

## 2. ログの分類

| 種別 | 目的 |
|------|------|
| 操作ログ | Builder 作業の追跡 |
| 監査ログ | コンプライアンス・内部統制 |

---

## 3. 監査ログ（Audit Log）の対象

| 対象 | 説明 |
|------|------|
| Deploy 実行 | 本番適用の記録 |
| 権限変更 | ロール付与・削除 |
| 本番データの閲覧・編集 | 機密データへのアクセス |

---

## 4. 格納内容

```json
{
  "timestamp": "2025-11-21T12:00:00Z",
  "userId": "admin001",
  "action": "DEPLOY",
  "releaseId": "1.3.0",
  "environment": "prod",
  "result": "success"
}
```

---

## 5. 監査ログの特徴

| 特徴 | 説明 |
|------|------|
| 改ざん不可 | Append-only |
| 保存期間 | 1〜3 年（設定） |
| エクスポート | CSV / JSON |
| 監査証跡 | 内部統制基準を満たす |

---

## 6. ログ閲覧 UI

```
Audit Center
├── 操作ログ一覧
├── 監査ログ一覧
└── フィルタ（userId / date / action）
```

---

## 7. 設計意図

- 内部統制基準の遵守
- 事故時の原因追跡
- 組織利用に不可欠な透明性

---

## 関連ドキュメント

- [ログモデル](./log-model.md)
- [権限モデル（RBAC）](./rbac-model.md)
- [Workspace/環境管理](./workspace-env.md)

---

*Powered by Copilot 🤖*
