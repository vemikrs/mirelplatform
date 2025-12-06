# Mira 実装計画書 v0.1

> **Issue**: #50 Mira v1 実装
> **ブランチ**: `feature/50-mira-v1`
> **作成日**: 2025-12-06
> **最終更新**: 2025-12-06

---

## 1. 概要

本ドキュメントは、Mira（mirel Assist）MVP の実装計画を定義する。既存の Studio/MSTE アプリのアーキテクチャに準拠し、段階的に実装を進める。

### 1.1 実装方針

- **既存アーキテクチャ準拠**: `jp.vemi.mirel.apps.mira` パッケージ構成は MSTE/Studio と同様の `application/controller` + `domain/{api,dto,service,dao}` 構造
- **API パス規約**: `/apps/mira/api/*` （mirelplatform 標準に準拠）
- **DTO/Response 標準**: `ApiRequest<T>` / `ApiResponse<T>` を使用
- **テスト駆動**: モック機能を活用し、外部 AI API 非依存でテスト可能な設計

### 1.2 フェーズ構成

| フェーズ | 内容 | 目標期間 |
|---------|------|----------|
| Phase 1 | 基盤構築（パッケージ・エンティティ・設定） | 1-2日 |
| Phase 2 | AI Model Layer（Azure OpenAI + Mock） | 2-3日 |
| Phase 3 | Orchestration Layer（モード判定・プロンプト構築） | 2-3日 |
| Phase 4 | Platform Integration Layer（RBAC・監査ログ） | 1-2日 |
| Phase 5 | API エンドポイント実装 | 2-3日 |
| Phase 6 | フロントエンド（React コンポーネント） | 3-5日 |
| Phase 7 | E2E テスト・統合テスト | 2-3日 |

---

## 2. Phase 1: 基盤構築

### 2.1 パッケージ構造作成

```
backend/src/main/java/jp/vemi/mirel/apps/mira/
├── application/
│   └── controller/
│       └── MiraApiController.java
├── domain/
│   ├── api/
│   │   └── MiraApi.java (interface)
│   ├── dao/
│   │   ├── entity/
│   │   │   ├── MiraConversation.java
│   │   │   ├── MiraMessage.java
│   │   │   ├── MiraContextSnapshot.java
│   │   │   └── MiraAuditLog.java
│   │   └── repository/
│   │       ├── MiraConversationRepository.java
│   │       ├── MiraMessageRepository.java
│   │       └── MiraAuditLogRepository.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ChatRequest.java
│   │   │   ├── ContextSnapshotRequest.java
│   │   │   └── ErrorReportRequest.java
│   │   └── response/
│   │       ├── ChatResponse.java
│   │       └── ContextSnapshotResponse.java
│   └── service/
│       ├── MiraChatService.java
│       ├── MiraChatServiceImpl.java
│       ├── MiraContextService.java
│       └── MiraAuditService.java
└── infrastructure/
    ├── ai/
    │   ├── AiProviderClient.java (interface)
    │   ├── AzureOpenAiClient.java
    │   ├── OpenAiClient.java
    │   └── MockAiClient.java
    └── config/
        └── MiraConfiguration.java
```

#### チェックリスト

- [ ] **1.1** パッケージディレクトリ作成
  - `git commit -m "chore(backend): Mira パッケージ構造作成 (refs #50)"`

- [ ] **1.2** 設定クラス作成 `MiraConfiguration.java`
  - Azure OpenAI / OpenAI / Mock の設定プロパティ定義
  - `@ConfigurationProperties(prefix = "mira.ai")` 使用
  - `git commit -m "feat(backend): Mira 設定クラス追加 (refs #50)"`

- [ ] **1.3** application.yml への Mira 設定追加
  - `mira.ai.provider`, `mira.ai.azure-openai.*`, `mira.ai.mock.*`
  - `git commit -m "feat(backend): application.yml に Mira AI 設定追加 (refs #50)"`

### 2.2 エンティティ定義

#### チェックリスト

- [ ] **1.4** `MiraConversation.java` 作成
  - JPA エンティティ、テーブル名 `mir_conversation`
  - `git commit -m "feat(backend): MiraConversation エンティティ作成 (refs #50)"`

- [ ] **1.5** `MiraMessage.java` 作成
  - テーブル名 `mir_message`
  - `git commit -m "feat(backend): MiraMessage エンティティ作成 (refs #50)"`

- [ ] **1.6** `MiraContextSnapshot.java` 作成
  - テーブル名 `mir_context_snapshot`
  - `git commit -m "feat(backend): MiraContextSnapshot エンティティ作成 (refs #50)"`

- [ ] **1.7** `MiraAuditLog.java` 作成
  - テーブル名 `mir_audit_log`
  - `git commit -m "feat(backend): MiraAuditLog エンティティ作成 (refs #50)"`

- [ ] **1.8** Repository インタフェース作成
  - `MiraConversationRepository`, `MiraMessageRepository`, `MiraAuditLogRepository`
  - `git commit -m "feat(backend): Mira Repository インタフェース作成 (refs #50)"`

- [ ] **1.9** Flyway マイグレーション作成
  - `V20251206__create_mira_tables.sql`
  - `git commit -m "feat(backend): Mira テーブル作成マイグレーション (refs #50)"`

---

## 3. Phase 2: AI Model Layer

### 3.1 Provider インタフェース

#### チェックリスト

- [ ] **2.1** `AiProviderClient.java` インタフェース作成
  ```java
  public interface AiProviderClient {
      AiResponse chat(AiRequest request);
      boolean isAvailable();
      String getProviderName();
  }
  ```
  - `git commit -m "feat(backend): AiProviderClient インタフェース定義 (refs #50)"`

- [ ] **2.2** `AiRequest.java` / `AiResponse.java` DTO 作成
  - `git commit -m "feat(backend): AI 通信用 DTO 作成 (refs #50)"`

### 3.2 Azure OpenAI Client（Primary）

- [ ] **2.3** `AzureOpenAiClient.java` 実装
  - Spring AI `ChatModel` または `com.azure:azure-ai-openai:1.0.0-beta.16` SDK 使用
  - リトライ・タイムアウト制御
  - `git commit -m "feat(backend): AzureOpenAiClient 実装 (refs #50)"`

- [ ] **2.4** `AzureOpenAiClientTest.java` 単体テスト
  - WireMock 使用でモックサーバテスト
  - `git commit -m "test(backend): AzureOpenAiClient 単体テスト (refs #50)"`

### 3.3 Mock Provider（テスト用）

- [ ] **2.5** `MockAiClient.java` 実装
  - パターンマッチ応答
  - 設定ファイルベース応答
  - レイテンシシミュレーション
  - `git commit -m "feat(backend): MockAiClient 実装 (refs #50)"`

- [ ] **2.6** `@MiraWithMock` アノテーション作成
  - テスト用自動モック注入
  - `git commit -m "feat(backend): @MiraWithMock テストアノテーション (refs #50)"`

- [ ] **2.7** `mock-responses.yml` 作成
  - モード別応答パターン定義
  - `git commit -m "feat(backend): モック応答定義ファイル追加 (refs #50)"`

### 3.4 Provider 切り替え機構

- [ ] **2.8** `AiProviderFactory.java` 作成
  - 設定に基づくプロバイダ選択
  - フォールバック制御
  - `git commit -m "feat(backend): AiProviderFactory 実装 (refs #50)"`

---

## 4. Phase 3: Orchestration Layer

### 4.1 モード判定

- [ ] **3.1** `MiraMode.java` enum 作成
  - `GENERAL_CHAT`, `CONTEXT_HELP`, `ERROR_ANALYZE`, `STUDIO_AGENT`, `WORKFLOW_AGENT`
  - `git commit -m "feat(backend): MiraMode enum 定義 (refs #50)"`

- [ ] **3.2** `ModeResolver.java` 実装
  - Intent 推定ロジック
  - `git commit -m "feat(backend): ModeResolver 実装 (refs #50)"`

### 4.2 プロンプト構築

- [ ] **3.3** `PromptTemplate.java` 作成
  - モード別システムプロンプト
  - `git commit -m "feat(backend): PromptTemplate 定義 (refs #50)"`

- [ ] **3.4** `PromptBuilder.java` 実装
  - system / context / user の統合
  - `git commit -m "feat(backend): PromptBuilder 実装 (refs #50)"`

- [ ] **3.5** `prompts/` リソースフォルダ作成
  - `system-general.txt`, `system-context-help.txt`, etc.
  - `git commit -m "feat(backend): プロンプトテンプレートファイル追加 (refs #50)"`

### 4.3 回答ポリシー

- [ ] **3.6** `PolicyEnforcer.java` 実装
  - ロールベース回答制御
  - `git commit -m "feat(backend): PolicyEnforcer 実装 (refs #50)"`

- [ ] **3.7** `ResponseFormatter.java` 実装
  - Markdown / プレーンテキスト変換
  - `git commit -m "feat(backend): ResponseFormatter 実装 (refs #50)"`

### 4.4 統合サービス

- [ ] **3.8** `MiraChatService.java` インタフェース定義
  - `git commit -m "feat(backend): MiraChatService インタフェース (refs #50)"`

- [ ] **3.9** `MiraChatServiceImpl.java` 実装
  - ModeResolver → PromptBuilder → AiProviderClient → PolicyEnforcer → ResponseFormatter の統合
  - `git commit -m "feat(backend): MiraChatServiceImpl 実装 (refs #50)"`

- [ ] **3.10** `MiraChatServiceTest.java` 単体テスト
  - モック使用
  - `git commit -m "test(backend): MiraChatService 単体テスト (refs #50)"`

---

## 5. Phase 4: Platform Integration Layer

### 5.1 RBAC Adapter

- [ ] **4.1** `MiraRbacAdapter.java` 実装
  - `ExecutionContext` からロール情報取得
  - `git commit -m "feat(backend): MiraRbacAdapter 実装 (refs #50)"`

### 5.2 監査ログ

- [ ] **4.2** `MiraAuditService.java` 実装
  - `MiraAuditLog` への記録
  - 保存ポリシー（FULL / SUMMARY / METADATA_ONLY）
  - `git commit -m "feat(backend): MiraAuditService 実装 (refs #50)"`

- [ ] **4.3** `MiraAuditServiceTest.java` 単体テスト
  - `git commit -m "test(backend): MiraAuditService 単体テスト (refs #50)"`

### 5.3 テナントコンテキスト

- [ ] **4.4** `MiraTenantContextManager.java` 実装
  - AI 利用可否、モデル上限取得
  - `git commit -m "feat(backend): MiraTenantContextManager 実装 (refs #50)"`

---

## 6. Phase 5: API エンドポイント

### 6.1 Controller 実装

- [ ] **5.1** `MiraApiController.java` 作成
  - `@RequestMapping("apps/mira/api")`
  - `git commit -m "feat(backend): MiraApiController 基盤作成 (refs #50)"`

- [ ] **5.2** `POST /chat` エンドポイント実装
  - `git commit -m "feat(backend): POST /apps/mira/api/chat 実装 (refs #50)"`

- [ ] **5.3** `POST /context-snapshot` エンドポイント実装
  - `git commit -m "feat(backend): POST /apps/mira/api/context-snapshot 実装 (refs #50)"`

- [ ] **5.4** `POST /error-report` エンドポイント実装
  - `git commit -m "feat(backend): POST /apps/mira/api/error-report 実装 (refs #50)"`

- [ ] **5.5** OpenAPI アノテーション追加
  - `@Tag`, `@Operation`, `@ApiResponses`
  - `git commit -m "docs(backend): Mira API OpenAPI ドキュメント (refs #50)"`

### 6.2 DTO 作成

- [ ] **5.6** `ChatRequest.java` / `ChatResponse.java` 作成
  - `git commit -m "feat(backend): Chat DTO 作成 (refs #50)"`

- [ ] **5.7** `ContextSnapshotRequest.java` / `ContextSnapshotResponse.java` 作成
  - `git commit -m "feat(backend): ContextSnapshot DTO 作成 (refs #50)"`

- [ ] **5.8** `ErrorReportRequest.java` 作成
  - `git commit -m "feat(backend): ErrorReport DTO 作成 (refs #50)"`

### 6.3 統合テスト

- [ ] **5.9** `MiraApiControllerTest.java` 作成
  - `@WebMvcTest` + MockAiClient
  - `git commit -m "test(backend): MiraApiController 統合テスト (refs #50)"`

---

## 7. Phase 6: フロントエンド

### 7.1 共通コンポーネント

```
apps/frontend-v3/src/features/mira/
├── components/
│   ├── MiraChatWidget.tsx
│   ├── MiraFullscreenView.tsx
│   ├── MiraMessageList.tsx
│   ├── MiraInput.tsx
│   └── MiraPromptButtons.tsx
├── hooks/
│   ├── useMiraChat.ts
│   └── useMiraContext.ts
├── stores/
│   └── miraStore.ts
├── api/
│   └── miraApi.ts
└── types/
    └── mira.ts
```

#### チェックリスト

- [ ] **6.1** `types/mira.ts` 型定義
  - `git commit -m "feat(frontend): Mira 型定義追加 (refs #50)"`

- [ ] **6.2** `api/miraApi.ts` API クライアント
  - TanStack Query 使用
  - `git commit -m "feat(frontend): Mira API クライアント (refs #50)"`

- [ ] **6.3** `stores/miraStore.ts` Zustand ストア
  - チャット状態管理
  - `git commit -m "feat(frontend): Mira Zustand ストア (refs #50)"`

### 7.2 UI コンポーネント

- [ ] **6.4** `MiraInput.tsx` 入力コンポーネント
  - `git commit -m "feat(frontend): MiraInput コンポーネント (refs #50)"`

- [ ] **6.5** `MiraMessageList.tsx` メッセージ表示
  - `git commit -m "feat(frontend): MiraMessageList コンポーネント (refs #50)"`

- [ ] **6.6** `MiraPromptButtons.tsx` ショートカットボタン
  - `git commit -m "feat(frontend): MiraPromptButtons コンポーネント (refs #50)"`

- [ ] **6.7** `MiraChatWidget.tsx` チャットウィジェット
  - minimized / docked 状態
  - `git commit -m "feat(frontend): MiraChatWidget コンポーネント (refs #50)"`

- [ ] **6.8** `MiraFullscreenView.tsx` 全画面ビュー
  - `git commit -m "feat(frontend): MiraFullscreenView コンポーネント (refs #50)"`

### 7.3 フック

- [ ] **6.9** `useMiraChat.ts` チャットフック
  - `git commit -m "feat(frontend): useMiraChat フック (refs #50)"`

- [ ] **6.10** `useMiraContext.ts` コンテキスト収集フック
  - `git commit -m "feat(frontend): useMiraContext フック (refs #50)"`

### 7.4 アプリ統合

- [ ] **6.11** レイアウトへの MiraChatWidget 組み込み
  - `git commit -m "feat(frontend): MiraChatWidget をレイアウトに統合 (refs #50)"`

- [ ] **6.12** ルーティング追加（全画面ビュー用）
  - `git commit -m "feat(frontend): Mira 全画面ビュールート追加 (refs #50)"`

---

## 8. Phase 7: E2E テスト・統合テスト

### 8.1 E2E テスト

- [ ] **7.1** `packages/e2e/tests/specs/mira/` ディレクトリ作成
  - `git commit -m "chore(e2e): Mira テストディレクトリ作成 (refs #50)"`

- [ ] **7.2** `mira-chat.spec.ts` チャット機能テスト
  - `git commit -m "test(e2e): Mira チャット E2E テスト (refs #50)"`

- [ ] **7.3** `mira-context-help.spec.ts` コンテキストヘルプテスト
  - `git commit -m "test(e2e): Mira コンテキストヘルプ E2E テスト (refs #50)"`

### 8.2 統合テスト

- [ ] **7.4** バックエンド統合テスト（Docker Compose 使用）
  - `git commit -m "test(backend): Mira 統合テスト (refs #50)"`

---

## 9. 依存ライブラリ

### 9.1 Backend (build.gradle)

#### Spring AI 1.1 GA（推奨・唯一の安定オプション）

> **⚠️ 実装時の注意**: Spring AI 1.1 は 2025年11月リリースのため、実装時には必ず Web 検索で最新のベストプラクティス・設定方法・サンプルコードを確認すること。
> - 公式ドキュメント: https://docs.spring.io/spring-ai/reference/
> - Spring AI GitHub: https://github.com/spring-projects/spring-ai
> - Spring AI Examples: https://github.com/spring-projects/spring-ai-examples

```groovy
// Spring AI BOM（実装時に最新バージョンを確認）
dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.1.0"
    }
}

// Spring AI Azure OpenAI Starter（新artifact名）
implementation 'org.springframework.ai:spring-ai-starter-model-azure-openai'

// WireMock（テスト用）
testImplementation 'org.wiremock:wiremock-standalone:3.3.1'
```

> **⚠️ 重要 (2025年12月時点)**:
> - **Spring AI 1.1.0 GA** が 2025年11月12日にリリース済み（850以上の改善）
> - **Azure OpenAI Java SDK は非推奨（Deprecated）** となり、更新停止
> - **artifact名変更**: `spring-ai-azure-openai-spring-boot-starter` → `spring-ai-starter-model-azure-openai`
> - 詳細は [issues-and-solutions.md T-1](./issues-and-solutions.md#21-t-1-ai-クライアントライブラリ選定) を参照

#### Spring AI 1.1 の主要機能

| 機能 | 説明 |
|------|------|
| MCP 統合 | @McpTool, @McpResource, @McpPrompt アノテーション |
| Chat Memory | MongoDB, Oracle JDBC, Azure Cosmos DB 対応 |
| Observability | Micrometer, OpenTelemetry 対応 |
| ネットワークリトライ | 分散環境での自動リトライ |

### 9.2 Frontend (package.json)

```json
{
  "dependencies": {
    // 既存の依存関係で対応可能
  }
}
```

---

## 10. リスクと対策

| リスク | 影響 | 対策 |
|--------|------|------|
| Azure OpenAI API レート制限 | 本番運用時のスロットリング | テナント別利用制限、キャッシュ導入 |
| レスポンス遅延 | UX 低下 | ストリーミング応答検討（v0.2） |
| プロンプト品質 | 回答精度低下 | プロンプトテンプレートの継続改善 |
| セキュリティ | 機密情報漏洩 | PolicyEnforcer による回答制御強化 |

---

## 11. 完了条件

- [ ] 全 Phase のチェックリスト完了
- [ ] `./gradlew :backend:check` 成功
- [ ] `pnpm --filter frontend-v3 lint` 成功
- [ ] `pnpm test:e2e` 成功（Mira 関連テスト）
- [ ] PR レビュー完了
- [ ] `master` ブランチへのマージ

---

## 12. 参照ドキュメント

- [requirement.md](../01_concept/requirement.md) — 要件定義
- [layer-design.md](../02_architecture/layer-design.md) — レイヤ設計
- [api-spec.md](../03_functional/api-spec.md) — API 仕様
- [data-model.md](../02_architecture/data-model.md) — データモデル
- [ui-design.md](../03_functional/ui-design.md) — UI 設計
