# Mira Architecture Overview

## 1. 概要

Mira（mirel Assist）は、mirelplatform の共通 AI アシスタント機能として動作する。  
ユーザは以下の 2 つの UX 形態で Mira を利用できる。

- 画面右下（もしくは右側）に表示される **チャットウィンドウ**
- 画面全体を占有する **全画面ビュー**

チャットウィンドウと全画面ビューは、以下を満たす。

- シームレスな切り替え（チャット → 全画面 → チャット）
- 最小化・再表示が可能
- 画面コンテキスト（アプリ、画面ID、ロール等）を共有

Mira は単体のアプリケーションではなく、mirelplatform の **Unified Assistance Layer（統合支援レイヤ）** として、各アプリケーション（Studio / Workflow / Admin 等）の上に重なる形で動作する。

---

## 2. 全体アーキテクチャ

Mira のアーキテクチャは、次の 4 レイヤで構成する。

1. **Interaction Layer（インタラクションレイヤ）**
2. **Orchestration Layer（オーケストレーションレイヤ）**
3. **AI Model Layer（AI モデルレイヤ）**
4. **Platform Integration Layer（プラットフォーム統合レイヤ）**

### 2.1 Interaction Layer

- チャットウィンドウ / 全画面ビューの UI コンポーネント
- 画面コンテキスト（appId, screenId, systemRole, appRole, tenantId, contextPayload）の収集と送信
- 最小化・復元・全画面切替の状態管理
- プロンプトテンプレート UI（「エラーを説明」「この画面の説明」などのショートカット）

### 2.2 Orchestration Layer

- ユーザ入力と画面コンテキストの統合
- 利用モードの判定
  - 汎用チャット
  - コンテキストヘルプ
  - エラー解析
  - Studio / Workflow Agent
- ロール・テナント情報を用いた回答制御（[RBAC モデル](../../studio/09_operations/rbac-model.md) 準拠）
- プロンプト構築（system / context / user の統合）

### 2.3 AI Model Layer

- **LLM 推論の実行**
  - マルチプロバイダー対応: Vertex AI Gemini（推奨）, Azure OpenAI, OpenAI
  - モデル切替（fast / standard / high-precision など）
  - プロンプトバージョン管理
- **RAG パイプライン**
  - ハイブリッド検索（ベクトル検索 + キーワード検索）
  - Reciprocal Rank Fusion (RRF) による統合ランキング
  - **Reranker**: Vertex AI Discovery Engine による二次リランキング
- 生成結果のポストプロセス（要約、構造化、箇条書き変換など）

### 2.4 Platform Integration Layer

- RBAC（ロールベースアクセス制御）連携
  - システムロール: `ROLE_ADMIN` / `ROLE_USER`（Spring Security）
  - アプリロール: `SystemAdmin` / `Builder` / `Operator` / `Viewer`（Studio 等）
- テナントコンテキスト管理（`ExecutionContext` 経由）
- エラー / ログとの連携（バックエンドからの構造化エラー）
- 会話履歴・監査ログの保存（`MiraAuditLog`、テナント設定に応じた保存ポリシー）
- Studio / Workflow / Admin など各アプリケーションの API 連携

---

## 3. API パス規約

Mira の API は mirelplatform の標準パス規約に従う。

| エンドポイント                         | 用途                     |
| -------------------------------------- | ------------------------ |
| `POST /apps/mira/api/chat`             | チャットメッセージ送受信 |
| `POST /apps/mira/api/context-snapshot` | 画面コンテキスト登録     |
| `POST /apps/mira/api/error-report`     | エラー情報登録           |
| `GET /apps/mira/api/conversation/{id}` | 会話履歴取得（将来拡張） |

> **注**: フロントエンドからは Vite プロキシ経由で `/mapi/apps/mira/api/*` でアクセス。詳細は [api-spec.md](../03_functional/api-spec.md) を参照。

---

## 4. UX とアーキテクチャの対応

### 4.1 チャットウィンドウ

- Interaction Layer が管理するコンポーネントとして、各画面共通で埋め込む。
- 現在の画面に応じたコンテキスト（appId, screenId, role 等）を、毎回 Orchestration Layer に渡す。
- ユーザは軽量な Q&A や簡易ヘルプに利用する想定。

### 4.2 全画面ビュー

- チャットウィンドウを基点としつつ、全画面モード用のレイアウトコンポーネントに切り替える。
- 大量のテキスト、複数ステップの手順案内、Studio / Workflow の設計相談など、情報量の多いやり取りに最適化する。
- 画面切替時も、Conversation ID を共有し、会話コンテキストを維持する。

### 4.3 状態遷移

- `minimized` / `docked` / `fullscreen` の 3 状態を Interaction Layer で管理する。
- 状態はフロントエンドのみで完結し、バックエンド側は Conversation 状態のみを持つ。

---

## 5. モードとフロー

Mira は内部的に「モード」を持ち、Orchestration Layer が切り替える。

- `general_chat`
- `context_help`
- `error_analyze`
- `studio_agent`
- `workflow_agent`

各モードごとに：

- 利用するコンテキスト
- プロンプトテンプレート
- 出力整形ルール

を切り替える。  
モード指定は、ユーザ操作（ボタン）および画面コンテキスト（エラー情報の有無等）から決定する。

---

## 6. 今後の拡張前提

- Workflow からの「自動修正案生成」や、Studio からの「項目定義案生成」など、高度な連携は Platform Integration Layer と Orchestration Layer の拡張で対応する。
- モデルの差し替え・追加（オンプレモデル等）は AI Model Layer を差し替えることで対応する。

---

## 7. 関連ドキュメント

- [layer-design.md](./layer-design.md) — レイヤ設計詳細
- [data-model.md](./data-model.md) — エンティティ定義
- [api-spec.md](../03_functional/api-spec.md) — API エンドポイント仕様
- [ui-design.md](../03_functional/ui-design.md) — UI 設計
- [docs/studio/09_operations/rbac-model.md](../../studio/09_operations/rbac-model.md) — RBAC モデル
