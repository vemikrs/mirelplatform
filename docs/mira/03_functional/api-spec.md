# api-spec.md

## 1. 概要

Mira バックエンドは、フロントエンド（Interaction Layer）からのリクエストを受け取り、  
Orchestration Layer → AI Model Layer → Platform Integration Layer を経由して応答を返す。

本書では、MVP で利用する主要エンドポイントを定義する。

---

## 2. 共通仕様

### 2.1 ベースパス

- `BASE_PATH = /apps/mira/api`

> **注**: mirelplatform の API パス規約（`/apps/{appId}/api/*`）に従う。Vite プロキシ経由では `/mapi/apps/mira/api/*` でアクセス可能。

### 2.2 認証

- mirelplatform 共通の認証方式（例：Bearer Token / セッション Cookie）を利用
- 全てのエンドポイントで認証必須

### 2.3 共通ヘッダ

| ヘッダ名           | 必須 | 説明                              |
|--------------------|------|-----------------------------------|
| Authorization      | Yes  | `Bearer <token>`                  |
| X-Tenant-Id        | Yes  | テナント ID                       |
| X-User-Id          | Yes  | ユーザ ID                         |
| Content-Type       | Yes  | `application/json`                |
| X-Client-Version   | No   | フロントエンドバージョン（任意） |

---

## 3. エンドポイント一覧

1. `POST /chat` — 汎用チャット＋モード別会話処理  
2. `POST /context-snapshot` — 画面コンテキストのスナップショット登録  
3. `POST /error-report` — エラー情報の登録＋解析トリガ  
4. `GET  /conversation/{id}` — 会話履歴の取得（将来拡張）  
5. `POST /mode-switch` — モード変更（任意。MVP では `/chat` 内で処理してもよい）

MVP としては、`/chat` を中核とし、`/context-snapshot` / `/error-report` は  
画面側やバックエンドから呼び出す補助エンドポイントとする。

---

## 4. POST /chat

### 4.1 概要

- チャットメッセージ送信と、Mira からの応答取得を行う。
- `mode` に応じて、Orchestration Layer がプロンプト組み立てを変える。

### 4.2 URL

- `POST /apps/mira/api/chat`

### 4.3 リクエストボディ

```json
{
  "conversationId": "conv-123",      // null or empty の場合、新規セッション扱い
  "mode": "context_help",            // "general_chat" | "context_help" | "error_analyze" | "studio_agent" | "workflow_agent"
  "context": {
    "snapshotId": "ctx-456",         // ContextSnapshot ID（省略可）
    "appId": "studio",
    "screenId": "stencil-editor",
    "systemRole": "ROLE_USER",       // Spring Security ロール
    "appRole": "Builder",            // アプリケーション固有ロール（Studio: SystemAdmin/Builder/Operator/Viewer）
    "payload": {
      "entityName": "Customer"
    }
  },
  "message": {
    "content": "この画面で何ができますか？",
    "contentType": "plain"           // "plain" | "markdown"
  }
}
```

> **ロール**: `systemRole` は Spring Security のロール（`ROLE_ADMIN` / `ROLE_USER`）、`appRole` は Studio 等のアプリケーション固有ロール。詳細は [docs/studio/09_operations/rbac-model.md](../../studio/09_operations/rbac-model.md) を参照。

### 4.4 レスポンスボディ

```json
{
  "conversationId": "conv-123",
  "messageId": "msg-789",
  "mode": "context_help",
  "assistantMessage": {
    "content": "この画面では、エンティティのフィールド定義を編集できます。主な機能は…",
    "contentType": "markdown"
  },
  "metadata": {
    "usedModel": "gpt-5.1",
    "latencyMs": 1234
  }
}
```

### 4.5 エラー

* 400: リクエスト不正（必須項目欠落等）
* 401: 認証エラー
* 403: 利用権限なし（テナント設定で AI 無効 等）
* 500: 内部エラー

---

## 5. POST /context-snapshot

### 5.1 概要

* 現在の画面コンテキストをサーバ側に登録する。
* `snapshotId` は `/chat` から参照される。

### 5.2 URL

* `POST /apps/mira/api/context-snapshot`

### 5.3 リクエストボディ

```json
{
  "appId": "studio",
  "screenId": "stencil-editor",
  "role": "developer",
  "payload": {
    "entityName": "Customer",
    "fieldCount": 12
  }
}
```

### 5.4 レスポンスボディ

```json
{
  "snapshotId": "ctx-456",
  "createdAt": "2025-12-06T03:21:00Z"
}
```

---

## 6. POST /error-report

### 6.1 概要

* バックエンド API などで発生したエラー情報を Mira に通知する。
* 即時に解析する場合は、このエンドポイントから `/chat` 相当の処理まで行うか、
  もしくは `errorId` を返し、フロントエンドから `/chat` を呼ぶ方式のいずれかを選択できる。

ここでは、**エラー登録のみ**を行う簡易版を定義する。

### 6.2 URL

* `POST /apps/mira/api/error-report`

### 6.3 リクエストボディ

```json
{
  "source": "studio-api",
  "code": "VALIDATION_ERROR",
  "message": "必須項目が未入力です",
  "detail": {
    "field": "customerName",
    "reason": "required"
  },
  "context": {
    "appId": "studio",
    "screenId": "stencil-editor"
  }
}
```

### 6.4 レスポンスボディ

```json
{
  "errorId": "err-001",
  "registeredAt": "2025-12-06T03:22:00Z"
}
```

後続の `/chat` 呼び出しで `mode=error_analyze` とし、
`context.payload.errorId = "err-001"` を渡す設計とする。

---

## 7. GET /conversation/{id}（将来拡張）

### 7.1 概要

* 会話履歴を取得する。
* MVP の実装優先度は低いが、全画面ビューで過去会話をロードする際に利用可能。

### 7.2 URL

* `GET /apps/mira/api/conversation/{id}`

### 7.3 レスポンスボディ（例）

```json
{
  "conversationId": "conv-123",
  "tenantId": "tenant-001",
  "userId": "user-001",
  "mode": "general_chat",
  "messages": [
    {
      "id": "msg-1",
      "senderType": "user",
      "content": "この画面の使い方を教えて",
      "createdAt": "2025-12-06T03:00:00Z"
    },
    {
      "id": "msg-2",
      "senderType": "assistant",
      "content": "この画面では…",
      "createdAt": "2025-12-06T03:00:02Z"
    }
  ]
}
```

---

## 8. POST /mode-switch（任意）

### 8.1 概要

* クライアント側から明示的にモード変更を指示したい場合に利用する。
* MVP では `/chat` 内でモード指定・変更を完結させても良い。

### 8.2 URL

* `POST /apps/mira/api/mode-switch`

### 8.3 リクエストボディ

```json
{
  "conversationId": "conv-123",
  "mode": "studio_agent"
}
```

### 8.4 レスポンスボディ

```json
{
  "conversationId": "conv-123",
  "mode": "studio_agent"
}
```

---

## 9. ロギング・監査

### 9.1 記録対象

全ての `/chat`, `/context-snapshot`, `/error-report` リクエスト・レスポンスについて、`MiraAuditLog` に以下を記録する。

| 項目 | 説明 |
|------|------|
| tenantId | テナントID |
| userId | ユーザID |
| conversationId | 会話セッションID |
| messageId | メッセージID（該当する場合） |
| action | `chat` / `context_help` / `error_analyze` / `mode_switch` |
| mode | 実行時のモード |
| appId / screenId | コンテキスト情報 |
| promptLength / responseLength | 文字数 |
| usedModel | 使用モデル名 |
| latencyMs | レイテンシ |
| status | `success` / `error` / `timeout` |

### 9.2 保存ポリシー

content 本文は、テナント設定（`mira.audit.storage-policy`）に応じて以下を選択可能。

| 設定 | 説明 |
|------|------|
| `FULL` | プロンプト・応答本文を暗号化して保存 |
| `SUMMARY` | 要約のみ保存（本文は保存しない） |
| `METADATA_ONLY` | メタデータのみ（デフォルト） |

### 9.3 データ保持期間

- 標準: 90日
- テナント設定で延長可能（最大365日）

> **参照**: [data-model.md](../02_architecture/data-model.md) の「2.6 MiraAuditLog」

