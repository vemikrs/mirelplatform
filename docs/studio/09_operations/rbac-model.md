# mirel Studio 権限モデル（RBAC）仕様

> Role-Based Access Control による組織統制

---

## 1. 概要

mirel Studio は組織向けを前提として **Role-Based Access Control（RBAC）** を採用する。

---

## 2. 権限モデルの基本構造

```
User
└── Role
    └── Permission
```

---

## 3. 権限カテゴリ

| カテゴリ | 説明 |
|---------|------|
| Builder 権限 | モデル・フォーム・フロー編集 |
| Runtime 権限 | データ操作・フロー実行 |
| Admin 権限 | ユーザー管理・Deploy |

---

## 4. 権限（Permission）一覧

| 権限 | 説明 |
|------|------|
| MODEL_EDIT | モデル編集 |
| FORM_EDIT | フォーム編集 |
| FLOW_EDIT | フロー編集 |
| CODE_EDIT | コード体系編集 |
| DRAFT_CREATE | Draft 作成 |
| RELEASE_CREATE | Release 作成 |
| DEPLOY | Deploy 実行 |
| DATA_VIEW | データ閲覧 |
| DATA_EDIT | データ編集 |
| LOG_VIEW | ログ閲覧 |
| USER_MANAGE | ユーザー管理 |

---

## 5. 権限ロール（Role）例

### SystemAdmin

```
├─ MODEL_EDIT
├─ FORM_EDIT
├─ FLOW_EDIT
├─ RELEASE_CREATE
├─ DEPLOY
└─ USER_MANAGE
```

### Builder

```
├─ MODEL_EDIT
├─ FORM_EDIT
├─ FLOW_EDIT
└─ DRAFT_CREATE
```

### Operator

```
├─ DATA_VIEW
└─ LOG_VIEW
```

### Viewer

```
└─ DATA_VIEW
```

---

## 6. 環境（Dev/Stg/Prod）との組合せ

| ロール | Dev | Stg | Prod |
|--------|-----|------|-------|
| Builder | ○ | × | × |
| Operator | ○ | ○ | △（一部制限） |
| Admin | ○ | ○ | ○ |

Prod では編集権限は基本不可。

---

## 7. 権限チェックの粒度

| 粒度 | 説明 |
|------|------|
| Workspace 単位 | 現行実装 |
| Entity 単位 | 将来拡張 |
| Operation 単位 | 個別の CRUD |

---

## 8. RBAC の目的

- 組織利用に耐えうる統制
- 本番環境の保護
- 開発担当／運用担当の分離
- 内部統制要件（監査）への適合

---

## 関連ドキュメント

- [Workspace/環境管理](./workspace-env.md)
- [ログモデル](./log-model.md)
- [監査ログ](./audit-log.md)

---

*Powered by Copilot 🤖*
