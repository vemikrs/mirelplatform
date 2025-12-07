# Mira Layer Design

## 1. 概要

Mira のレイヤ設計は、役割と変更頻度の異なる関心事を分離しつつ、  
過剰な多層化を避けることを目的とする。

- Interaction Layer  
- Orchestration Layer  
- AI Model Layer  
- Platform Integration Layer  

の 4 レイヤ構成とし、それぞれの責務とインタフェースを明確化する。

---

## 2. Interaction Layer

### 2.1 役割

- ユーザインタラクション（チャット・全画面・最小化）の管理
- 画面コンテキストの収集と送信
- ショートカットボタンやテンプレートの提供

### 2.2 主なコンポーネント

- `MiraChatWidget`  
  - 画面右下に表示されるチャットウィンドウ
  - 状態：`minimized` / `docked` / `fullscreen`
- `MiraFullscreenView`  
  - 全画面用レイアウトコンポーネント
  - 複数カラム（会話 / 補助情報 / 提案アクション 等）も検討余地あり
- `ContextCollector`  
  - appId / screenId / role / tenantId / contextPayload を収集し、バックエンドへ送信
- `PromptTemplateButtons`  
  - 「この画面の説明」「エラーを説明」「設定手順を教えて」など、モード切替ショートカット

### 2.3 インタフェース

Interaction Layer → Orchestration Layer へのリクエスト例：

```json
{
  "conversationId": "conv-123",
  "tenantId": "tenant-001",
  "userId": "user-001",
  "mode": "context_help",
  "context": {
    "appId": "studio",
    "screenId": "stencil-editor",
    "systemRole": "ROLE_USER",
    "appRole": "Builder",
    "payload": { "entityName": "Customer" }
  },
  "message": {
    "senderType": "user",
    "content": "この画面で何ができますか？"
  }
}
```

> **ロール構造**: `systemRole` は Spring Security のロール（`ROLE_ADMIN` / `ROLE_USER`）、`appRole` は Studio 等のアプリケーション固有ロール（`SystemAdmin` / `Builder` / `Operator` / `Viewer`）を表す。

---

## 3. Orchestration Layer

### 3.1 役割

* 入力メッセージと画面コンテキストの統合
* モード判定とプロンプト組み立て
* ロールベース回答ポリシーの適用
* AI Model Layer への問い合わせと結果の整形

### 3.2 主な機能

* **Mode Resolver**

  * `mode` 明示指定があれば優先
  * なければ Intent 推定により `general_chat` 等を決定

* **Prompt Builder**

  * system / context / user を組み立て
  * 例：

    * system: 「あなたは mirelplatform のヘルプエージェントです…」
    * context: appId, screenId, role, ErrorReport, 設定状態 等
    * user: ユーザ発話

* **Policy Enforcer**

  * ロールとテナント情報に応じて、回答内容を制約
  * 管理操作説明の有無等を制御

* **Response Formatter**

  * Markdown / プレーンテキスト / 構造化 JSON を整形
  * UI 側でレンダリングしやすい形式に変換

### 3.3 インタフェース

Orchestration Layer → AI Model Layer：

```json
{
  "model": "gpt-5.1",
  "prompt": {
    "system": "...",
    "context": "...",
    "user": "..."
  },
  "options": {
    "temperature": 0.3,
    "maxTokens": 1024
  }
}
```

AI Model Layer → Orchestration Layer：

```json
{
  "content": "この画面では、エンティティのフィールド定義を編集できます…",
  "metadata": {
    "finishReason": "stop"
  }
}
```

---

## 4. AI Model Layer

### 4.1 役割

* 実際の LLM 呼び出し処理
* プロバイダ（Azure OpenAI / OpenAI / Mock）の切り替え
* モデルごとの設定管理
* API エラーのラップ・再試行制御
* テスト用モック応答の提供

### 4.2 技術選定: Spring AI 1.1 GA

> **⚠️ 重要 (2025年12月時点)**: Azure OpenAI Java SDK (`com.azure:azure-ai-openai`) は**非推奨（Deprecated）** となりました。
> Microsoft は openai-java コミュニティライブラリへの移行を推奨しています。
> **Spring AI 1.1 GA を採用**することで、この問題を回避しつつ Spring Boot との完全な統合を実現します。

| 項目 | Spring AI 1.1 GA |
|------|------------------|
| リリース日 | 2025年11月12日 |
| 総改善数 | 850以上 |
| MCP サポート | @McpTool, @McpResource, @McpPrompt |
| Chat Memory | MongoDB, Oracle JDBC, Azure Cosmos DB |
| Observability | Micrometer, OpenTelemetry |
| ストリーミング | StreamingChatModel 標準対応 |

### 4.3 プロバイダ優先順位

| 優先度 | プロバイダ | 用途 | 備考 |
|--------|-----------|------|------|
| 1 | **Azure OpenAI (via Spring AI)** | 本番・開発環境 | First Target。エンタープライズ向け SLA・コンプライアンス対応 |
| 2 | OpenAI API (via Spring AI) | フォールバック・検証 | Azure 障害時または特定モデル利用時 |
| 3 | Mock Provider | テスト・CI/CD | 外部 API 非依存でのテスト実行 |

> **MVP では Azure OpenAI を First Target** とし、Spring AI の `spring-ai-starter-model-azure-openai` を使用。

### 4.4 主なコンポーネント

* `ChatModel`（Spring AI 標準）

  * Spring AI が提供する統一インタフェース
  * `AzureOpenAiChatModel` が自動構成される

* `ChatMemory`（Spring AI 標準）

  * 会話履歴の管理
  * 複数ストレージオプション（InMemory, JDBC, MongoDB, Cosmos DB）

* カスタムコンポーネント

  * `MiraChatService` — ビジネスロジックを含むサービス層
  * `MockChatModel` — テスト用モック実装

### 4.5 Spring AI 設定（application.yml）

```yaml
spring:
  ai:
    azure:
      openai:
        endpoint: ${AZURE_OPENAI_ENDPOINT}
        api-key: ${AZURE_OPENAI_API_KEY}
        chat:
          options:
            deployment-name: ${AZURE_OPENAI_DEPLOYMENT_NAME:gpt-4o}
            temperature: 0.7
            max-tokens: 4096
    # OpenAI フォールバック用（オプション）
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o

# Mira 固有設定
mira:
  ai:
    provider: azure-openai  # azure-openai | openai | mock
    mock:
      enabled: false  # テスト時に true
      response-delay-ms: 500
```

### 4.6 モック機能（テスト支援）

テスト・CI/CD 環境で外部 API に依存せずに動作確認を行うため、モック機能を組み込む。

#### 4.6.1 MockChatModel

* Spring AI の `ChatModel` インタフェースを実装
* 設定ファイルまたはコードで定義されたモック応答を返却
* レイテンシのシミュレーション機能（`response-delay-ms`）

#### 4.6.2 モック応答の定義方式

| 方式 | 用途 | 設定方法 |
|------|------|----------|
| **固定応答** | 単純なスモークテスト | YAML/JSON で定義 |
| **パターンマッチ応答** | モード別テスト | プロンプト内容に応じた応答切替 |
| **シナリオベース応答** | E2E テスト | 会話フロー全体のシナリオ定義 |

#### 4.6.3 モック設定例

```yaml
mira:
  ai:
    mock:
      enabled: true
      response-delay-ms: 200
      responses:
        - pattern: "この画面.*説明"
          response: "この画面では、エンティティのフィールド定義を編集できます。"
        - pattern: "エラー.*原因"
          response: "エラーの原因は入力値の形式が不正であることが考えられます。"
        - default:
          response: "ご質問ありがとうございます。詳細をお知らせください。"
```

#### 4.6.4 テスト用アノテーション（Java）

```java
@MiraWithMock  // モックプロバイダを自動注入
@SpringBootTest
class MiraChatServiceTest {
    @Autowired
    private MiraChatService chatService;

    @Test
    void contextHelp_shouldReturnScreenDescription() {
        // モック応答が自動的に使用される
    }
}
```

### 4.6 設計ポイント

* Platform Integration Layer とは疎結合を維持し、
  モデル差し替え時の影響範囲を Orchestration Layer 以下に限定する。
* 将来的なオンプレモデル（社内 LLM 等）の導入も視野に、プロバイダ抽象化を行う。
* **テスト容易性**: モック機能により、外部 API 非依存でのユニットテスト・統合テストを実現。

---

## 5. Platform Integration Layer

### 5.1 役割

* mirelplatform のコアサービスとの連携
* RBAC / テナント / ログ / エラー情報との橋渡し
* Studio / Workflow / Admin など各アプリケーション API との連携

### 5.2 主な機能

* **RBAC Adapter**

  * `ExecutionContext` から `currentUser` のロール情報を取得
  * システムロール（`ROLE_ADMIN` / `ROLE_USER`）とアプリロール（`SystemAdmin` / `Builder` 等）を解決
  * Orchestration Layer にロール情報を提供
  * 参照: [docs/studio/09_operations/rbac-model.md](../../studio/09_operations/rbac-model.md)

* **TenantContext Manager**

  * `ExecutionContext.currentTenant` からテナント設定を取得
  * AI 利用可否、モデル上限、監査ログポリシー等の取得
  * 料金・利用制限ポリシーの適用

* **Error / Log Connector**

  * バックエンドのエラー情報を構造化して Orchestration Layer に渡す
  * 必要に応じて ErrorReport を保存

* **Audit Logger**

  * `MiraAuditLog` への記録を担当
  * テナント設定に応じた保存ポリシー（`FULL` / `SUMMARY` / `METADATA_ONLY`）の適用
  * 参照: [data-model.md](./data-model.md) の「2.6 MiraAuditLog」

* **App API Connector**

  * Studio / Workflow などからメタ情報を取得（例：エンティティ定義、Workflow ノード情報 等）
  * Mira の回答に利用するための軽量 API を提供

### 5.3 データフローの一例（エラー解析）

1. フロントエンドが API エラー情報を受信
2. ErrorReport と ContextSnapshot を作成
3. Interaction Layer から Orchestration Layer へ `mode=error_analyze` で送信
4. Orchestration Layer が Platform Integration Layer から必要な追加情報（ロール、テナント設定等）を取得
5. AI Model Layer にプロンプトを送信
6. 結果を整形してフロントエンドへ返却
7. **Audit Logger が `MiraAuditLog` に記録を保存**

---

## 6. 変更容易性と責務分離

* **UI / UX の変更**
  → Interaction Layer に閉じる（チャットウィンドウや全画面 UI の改修）

* **プロンプト設計・回答ポリシーの変更**
  → Orchestration Layer を中心に改修

* **モデルの切り替え・増減**
  → AI Model Layer のみを改修

* **mirelplatform 側仕様変更（RBAC / API 変更等）**
  → Platform Integration Layer のみを改修

---

## 7. 関連ドキュメント

- [data-model.md](./data-model.md) — エンティティ定義、MiraAuditLog 詳細
- [overview.md](./overview.md) — アーキテクチャ全体像
- [docs/studio/09_operations/rbac-model.md](../../studio/09_operations/rbac-model.md) — RBAC モデル詳細
- [api-spec.md](../03_functional/api-spec.md) — API エンドポイント仕様
