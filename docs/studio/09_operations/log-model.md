# mirel Studio ログモデル仕様

> 操作ログと実行ログの 2 種類の追跡

---

## 1. 概要

mirel Studio では、Builder と Runtime の双方で「何が、いつ、誰によって、どのように」実行されたかを正確に残すため、**2 種類のログ** を保持する。

---

## 2. Log 全体構造

```
OperationLog（Builder）
ExecutionLog（Runtime）
```

---

## 3. OperationLog（Builder 操作ログ）

### 対象

- Modeler 編集
- Form Designer 編集
- Flow Designer 編集
- Draft 作成／削除
- Release 作成／Deploy
- コード体系の変更

### ログ形式（例）

```json
{
  "timestamp": "2025-11-21T10:20:00Z",
  "user": "user001",
  "workspaceId": "ws-001",
  "operation": "update-field",
  "detail": {
    "entity": "customer",
    "field": "customerName",
    "before": "顧客名",
    "after": "氏名"
  }
}
```

### 特徴

- Builder 操作の追跡可能性
- Release 管理の透明性向上

---

## 4. ExecutionLog（Runtime 実行ログ）

### 対象

- Flow の実行
- CRUD API の呼び出し
- バリデーションエラー
- 外部連携の成否

### ログ形式（例）

```json
{
  "timestamp": "2025-11-21T11:00:00Z",
  "flowId": "customer_onCreate",
  "executionId": "exec-12345",
  "event": "onCreate",
  "nodes": [
    { "nodeId": "n1", "result": "success" },
    { "nodeId": "n2", "result": "success" }
  ],
  "status": "completed"
}
```

### 特徴

- 組織向けの監査証跡
- デバッグ時に必要
- 外部呼び出しの失敗も記録

---

## 5. 環境別ログ管理

| 環境 | 操作ログ | 説明 |
|------|---------|------|
| Dev | 詳細 | デバッグ向け |
| Stg | 簡略 | 本番に近い運用 |
| Prod | フル | 書き換え不可 |

---

## 6. ログの利用用途

| 用途 | 説明 |
|------|------|
| 不具合追跡 | Deploy 後の原因追跡 |
| ミス検出 | Builder 作業ミスの検出 |
| 実行可視化 | Flow の実行順序の可視化 |
| 監査対応 | 金融・医療領域等の要件 |

---

## 7. 設計意図

- 安全性向上
- 運用の透明性
- デバッグ容易化
- チーム開発の統制

---

## 関連ドキュメント

- [監査ログ](./audit-log.md)
- [権限モデル（RBAC）](./rbac-model.md)
- [Workspace/環境管理](./workspace-env.md)

---

*Powered by Copilot 🤖*
