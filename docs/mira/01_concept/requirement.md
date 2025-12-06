# **Mira（mirel Assist）要件書 v0.1**

## 1. 概要

**Mira（mirel Assist）** は、mirelplatform に統合される AI アシスタントであり、以下の3機能を中核とする。

1. **汎用チャット型生成 AI**
2. **mirelplatform 各アプリケーションのコンテキストヘルプ**
3. **mirel Studio / mirel Workflow の Assistant Agent（操作ガイド・生成補助）**

mirelplatform の UI から統一的に利用でき、管理者・開発者・エンドユーザの運用効率と習熟速度を向上させる。

---

# **2. スコープ（MVP で実装する範囲）**

## 2.1 サービス提供形態

* mirelplatform の共通サービスとして提供する。
* フロントエンド（React）から専用 API 経由で利用。
* **Azure OpenAI API を First Target** として、バックエンドにプロキシレイヤを実装。
* 将来的に OpenAI API（直接）やオンプレモデルへの切り替えも可能な設計とする。

### AI プロバイダ戦略

| プロバイダ | 優先度 | 用途 |
|-----------|--------|------|
| Azure OpenAI | Primary | 本番・開発環境（エンタープライズ SLA 対応） |
| OpenAI API | Secondary | フォールバック・特定モデル検証 |
| Mock Provider | テスト専用 | CI/CD・ユニットテスト |

> **MVP フェーズでは Azure OpenAI のみを実装**し、プロバイダ切り替え機構は抽象化レイヤとして設計する。

---

## 2.2 機能要件（MVP）

### **F-1. 汎用チャット機能**

* 自然言語による対話を提供する。
* 英語・日本語に対応。
* テナント隔離（セッション範囲でのコンテキスト保持）に対応。

### **F-2. mirelplatform 画面コンテキストヘルプ**

UI が提示する次の情報を AI に渡し、文脈適合した回答を行う。

* 現在のアプリケーション（Studio / Workflow / Admin / Account など）
* 現在の画面 ID
* 操作対象（例：Workflow ノード、ステンシル、テナント設定）
* 現在のユーザのロール（下記「ロール定義」参照）

AI はこの情報を利用し、
「この画面では何ができるか」「どこを操作すればいいか」を説明する。

#### ロール定義（mirelplatform 実装準拠）

mirelplatform のロールは、**システムロール**と**アプリケーションロール**の2層構造で管理される。

| 種別 | ロール | Spring Security 表記 | 説明 |
|------|--------|---------------------|------|
| システム | ADMIN | `ROLE_ADMIN` | システム全体管理者。全テナント・全機能にアクセス可能 |
| システム | USER | `ROLE_USER` | 一般認証済みユーザ（デフォルト） |
| Studio | SystemAdmin | — | Studio 内の全権限（MODEL_EDIT, DEPLOY, USER_MANAGE 等） |
| Studio | Builder | — | モデル・フォーム・フロー編集（DRAFT_CREATE まで） |
| Studio | Operator | — | データ閲覧・ログ閲覧（DATA_VIEW, LOG_VIEW） |
| Studio | Viewer | — | 閲覧のみ（DATA_VIEW） |

> **参照**: [docs/studio/09_operations/rbac-model.md](../../studio/09_operations/rbac-model.md)

Mira のロールベース回答制御では、上記の組み合わせに応じて回答内容を最適化する。

### **F-3. エラー要約・原因推定・対処案提示**

以下の情報を Mires に渡すと、要約と対応を返す。

* バックエンドからのバリデーションメッセージ
* Workflow のログ
* Studio のステンシルエラー
* 管理画面の設定エラー

出力は以下の3要素を含むこと。

1. 要約（短文）
2. 考えられる原因
3. 推奨アクション（管理者/一般ユーザのロール別）

### **F-4. 設定ガイド（対話ベースのナビゲーション）**

ユーザの意図に基づいて、必要な設定手順を提示する。

例：

* 「テナントにユーザーを追加したい」
* 「Workflow の API 呼び出し設定をしたい」
* 「Studio に新しいカテゴリを追加したい」

Mira は「遷移すべき画面」と「操作手順」を分かりやすく案内する。

### **F-5. ロールベース回答制御**

ユーザのロールに応じて回答内容を制限する。

| ロール | 回答制御 |
|--------|----------|
| `ROLE_USER` + Viewer | 管理操作・設定変更を提示しない |
| `ROLE_USER` + Operator | データ閲覧・ログ関連の案内のみ |
| `ROLE_USER` + Builder | Studio 編集操作の案内可。Deploy は不可 |
| `ROLE_ADMIN` または SystemAdmin | 全体設定・Deploy・ユーザ管理含む説明が可能 |

ロール情報は `ExecutionContext` から取得し、Orchestration Layer で回答ポリシーに適用する。

### **F-6. Audit / Logging（MVPは軽量版）**

* AI へのプロンプトと応答内容を、セキュアな形式でサーバ側に記録。
* テナント ID とユーザ ID を紐付ける。

内容は暗号化または要約保存にする。

#### 監査ログエンティティ（MiraAuditLog）

| 項目名 | 型 | 説明 |
|--------|------|------|
| id | string | 監査ログID（UUID） |
| tenantId | string | テナントID |
| userId | string | ユーザID |
| conversationId | string | 会話セッションID |
| messageId | string | メッセージID |
| action | string | アクション種別（`chat`, `context_help`, `error_analyze` 等） |
| promptLength | number | プロンプト文字数（本文は保存しない） |
| responseLength | number | 応答文字数（本文は保存しない） |
| usedModel | string | 使用したモデル名 |
| latencyMs | number | レイテンシ（ミリ秒） |
| status | string | 成功/失敗 |
| errorCode | string (nullable) | エラー発生時のコード |
| createdAt | datetime | 記録日時 |

> **注**: プロンプト・応答の本文はテナント設定に応じて「フルテキスト保存」「要約保存」「保存なし」を選択可能。詳細は [api-spec.md](../03_functional/api-spec.md) の「9. ロギング・監査」を参照。

---

# **3. 非機能要件（MVP）**

### **N-1. セキュリティ**

* テナントごとにコンテキストを隔離。
* セッション情報はバックエンド上に保持し、フロントでは保持しない。
* API キーやシークレット情報は絶対に AI へ渡さない制御を実装。

### **N-2. レイテンシ**

* 1往復 2〜3秒以内を目標。
* バックエンドキャッシュにより高速化。

### **N-3. スケーラビリティ**

* **Azure OpenAI API を First Target** として実装。OpenAI API へのフォールバックも対応。
* モデル切り替えは設定画面で制御（デプロイメント名 / モデル名の選択）。
* テナントごとに利用モデル・上限を設定可能。

### **N-4. プライバシー**

* 学習には利用しない（Azure OpenAI / OpenAI の "no-train" モード前提）。
* 社内ログは暗号化ストレージに保存。

### **N-5. テスト容易性**

* 外部 AI API に依存しないモック機能を提供。
* CI/CD パイプラインでの自動テストを可能にする。
* モック応答のパターン定義により、各モード（`context_help` / `error_analyze` 等）のテストを実現。

詳細は [layer-design.md](../02_architecture/layer-design.md) の「4.5 モック機能」を参照。

---

# **4. 今後の拡張（Mira v0.2以降）**

以下は MVP 以降に実装すべき領域。

## 4.1 Studio / Workflow 連携の高度化

### ● Workflow ノードの自動生成

「○○する自動処理がほしい」と指示 → ノード提案

### ● Studio のステンシル生成案

項目説明に基づくテンプレート構築支援。

## 4.2 テナント初期設定の“自動化”

ヒアリング形式で必要な項目を特定し、
必要な設定変更・ライセンス割り当てを順に案内する。

## 4.3 アプリ横断のオペレーションブック生成

* テナント運用ガイド
* 開発ガイド
* ワークフロードキュメント
* API 呼び出し手順

などを自動生成する。

## 4.4 言語モデル切替機能

* 小型モデル（安価・高速）
* 高性能モデル（設計レビュー向け）
  を用途別に切り替え。

## 4.5 Workflow の「デバッグガイド」

エラー原因を時系列で整理し、改善案を提示する。

---

# **5. インターフェース要件**

## 5.1 フロントエンド

* React コンポーネントとして実装
* ポップアップ / モーダル / 右サイドパネルの 3 種類の表示方式
* 画面情報を JSON でバックエンドへ送信：

  ```
  {
    appId: "studio",
    screenId: "stencil-editor",
    role: "developer",
    context: { ... }
  }
  ```

## 5.2 バックエンド API

mirelplatformの標準に準拠した RESTful API を提供。

* POST /apps/mira/api/chat
* POST /apps/mira/api/context-snapshot
* POST /apps/mira/api/error-report

> **注**: mirelplatform の API パス規約（`/apps/{appId}/api/*`）に従う。詳細は [api-spec.md](../03_functional/api-spec.md) を参照。

---

# **6. 名称・表記基準**

* 正式名称：**Mira（mirel Assist）**
* UI 表記：**Mira**
* API 表記：`mira`
