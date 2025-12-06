# Mira Data Model

## 1. 概要

Mira のデータモデルは、以下の観点から設計する。

- 会話セッションとメッセージ履歴の管理
- 画面コンテキスト・ロール情報の付与
- エラー・イベント等の構造化データの連携
- 監査・トレースのための最低限のログ

過度な永続化は避け、**「必要な情報をコンパクトに保持する」**方針とする。

---

## 2. 主要エンティティ

### 2.1 Conversation（会話セッション）

| 項目名           | 型              | 説明                                      |
|------------------|-----------------|-------------------------------------------|
| id               | string          | 会話セッションID                          |
| tenantId         | string          | テナントID                                |
| userId           | string          | ユーザID                                  |
| mode             | string          | モード（general_chat / context_help 等） |
| createdAt        | datetime        | セッション開始日時                        |
| lastActivityAt   | datetime        | 最終アクティビティ日時                    |
| status           | string          | active / closed など                      |

Conversation は、チャットウィンドウ / 全画面ビュー間で共有される。

---

### 2.2 Message（メッセージ）

| 項目名           | 型              | 説明                                    |
|------------------|-----------------|-----------------------------------------|
| id               | string          | メッセージID                            |
| conversationId   | string          | Conversation への外部キー               |
| senderType       | string          | user / assistant / system               |
| content          | text            | メッセージ本文                          |
| contentType      | string          | plain / markdown / structured_json 等   |
| contextSnapshotId| string (nullable)| ContextSnapshot への外部キー           |
| createdAt        | datetime        | 作成日時                                |

---

### 2.3 ContextSnapshot（コンテキストスナップショット）

| 項目名        | 型       | 説明                                         |
|---------------|----------|----------------------------------------------|
| id            | string   | スナップショットID                           |
| tenantId      | string   | テナントID                                   |
| userId        | string   | ユーザID                                     |
| appId         | string   | アプリケーションID（studio / workflow / admin 等） |
| screenId      | string   | 画面ID                                       |
| systemRole    | string   | システムロール（`ROLE_ADMIN` / `ROLE_USER`） |
| appRole       | string   | アプリケーションロール（SystemAdmin / Builder / Operator / Viewer）|
| payload       | json     | 画面固有コンテキスト（選択中のノード等）     |
| createdAt     | datetime | 取得日時                                     |

Message は、当時の画面状態を参照するために ContextSnapshot を紐付ける。

> **ロール構造**: mirelplatform では `systemRole`（Spring Security）と `appRole`（Studio等のアプリ固有権限）の2層でロールを管理する。詳細は [docs/studio/09_operations/rbac-model.md](../../studio/09_operations/rbac-model.md) を参照。

---

### 2.4 ErrorReport（エラーレポート）

バックエンドから渡される構造化エラー情報。

| 項目名        | 型     | 説明                                      |
|---------------|--------|-------------------------------------------|
| id            | string | エラーID（Mira 内での識別子）             |
| source        | string | エラー発生元（api / workflow / studio 等）|
| code          | string | エラーコード                              |
| message       | string | ユーザ向けメッセージ                      |
| detail        | json   | 詳細情報（例：フィールド名、値 等）       |
| occurredAt    | datetime | 発生日時                                |

ErrorReport は Orchestration Layer でプロンプトに統合され、Message と紐付けて保持することもできる。

---

### 2.5 IntentRecord（意図推定ログ）※任意

将来の分析・改善に向け、ユーザ入力と推定意図を記録する。

| 項目名        | 型     | 説明                                      |
|---------------|--------|-------------------------------------------|
| id            | string | レコードID                               |
| conversationId| string | 会話セッションID                           |
| messageId     | string | 対象メッセージID                           |
| intentType    | string | help / error_explain / design_support 等  |
| confidence    | number | 信頼度（0〜1）                             |
| payload       | json   | 補足情報（抽出されたキーワード等）         |

MVP では必須ではないが、プロダクト改善のために導入可能とする。

---

### 2.6 MiraAuditLog（監査ログ）

Mira のすべての AI 操作を監査目的で記録するエンティティ。

| 項目名 | 型 | 説明 |
|--------|------|------|
| id | string | 監査ログID（UUID） |
| tenantId | string | テナントID |
| userId | string | ユーザID |
| conversationId | string | 会話セッションID |
| messageId | string (nullable) | メッセージID（紐付け可能な場合） |
| action | string | アクション種別（`chat`, `context_help`, `error_analyze`, `mode_switch`） |
| mode | string | 実行時のモード |
| appId | string (nullable) | アプリケーションID |
| screenId | string (nullable) | 画面ID |
| promptLength | number | プロンプト文字数 |
| responseLength | number | 応答文字数 |
| promptHash | string (nullable) | プロンプトのハッシュ値（重複検知用） |
| usedModel | string | 使用したモデル名 |
| latencyMs | number | レイテンシ（ミリ秒） |
| status | string | `success` / `error` / `timeout` |
| errorCode | string (nullable) | エラー発生時のコード |
| ipAddress | string (nullable) | クライアントIPアドレス |
| userAgent | string (nullable) | クライアントUserAgent |
| createdAt | datetime | 記録日時 |

#### 保存ポリシー

| 設定 | 説明 |
|------|------|
| `FULL` | プロンプト・応答本文を暗号化して保存 |
| `SUMMARY` | 要約のみ保存（本文は保存しない） |
| `METADATA_ONLY` | メタデータのみ（文字数・ハッシュ等） |

テナントごとに `mira.audit.storage-policy` で設定可能。デフォルトは `METADATA_ONLY`。

#### テーブル定義（参考）

```sql
CREATE TABLE mir_mira_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    message_id VARCHAR(36),
    action VARCHAR(50) NOT NULL,
    mode VARCHAR(50),
    app_id VARCHAR(50),
    screen_id VARCHAR(100),
    prompt_length INT,
    response_length INT,
    prompt_hash VARCHAR(64),
    used_model VARCHAR(100),
    latency_ms INT,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_created (tenant_id, created_at),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_conversation (conversation_id)
);
```

---

## 3. 永続化ポリシー

- Conversation / Message / ContextSnapshot は、テナントごとに論理分離する。
- 保存期間は構成可能とし、短期（例：30日）と長期（90日以上）を選択できるようにする。
- ErrorReport は長期保管の対象とはせず、必要に応じて運用ログに転記する。

---

## 4. プライバシーとマスキング

- API キーやパスワード等の秘匿情報は、バックエンド段階でマスキングする。
- payload 内に機密性の高いデータを含めないように、各アプリケーション側でフィルタリングを行う。
- 学習用途の二次利用は行わず、あくまで運用・監査目的に限定する。

---

## 5. 将来拡張

- Workflow / Studio 用に、`DesignArtifact`（フォーム定義、ノード構造等）のサマリを持つエンティティを追加し、Mira の回答と紐付ける。
- 「Mira が提案した設定」や「提案された Workflow 定義」の草稿を保存するための Draft モデルを検討する。
```
