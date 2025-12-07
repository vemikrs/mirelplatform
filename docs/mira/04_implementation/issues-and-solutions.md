# Mira 実装課題と対応方針

> **関連**: [implementation-plan.md](./implementation-plan.md)
> **作成日**: 2025-12-06

---

## 1. 課題一覧

### 1.1 技術的課題

| ID | 課題 | 重要度 | 影響範囲 |
|----|------|--------|----------|
| T-1 | Azure OpenAI SDK のバージョン互換性 | 高 | Phase 2 |
| T-2 | ストリーミング応答未対応 | 中 | UX |
| T-3 | 会話履歴のコンテキストウィンドウ管理 | 高 | Phase 3 |
| T-4 | マルチテナント環境での API キー管理 | 高 | Phase 1, 4 |
| T-5 | フロントエンド状態永続化 | 低 | Phase 6 |

### 1.2 アーキテクチャ課題

| ID | 課題 | 重要度 | 影響範囲 |
|----|------|--------|----------|
| A-1 | 既存 Studio パス規約との不整合可能性 | 中 | Phase 5 |
| A-2 | ExecutionContext への Mira 固有情報追加要否 | 中 | Phase 4 |
| A-3 | 監査ログの大量データ対策 | 中 | Phase 4 |
| A-4 | フロントエンド共通コンポーネントの配置場所 | 低 | Phase 6 |

### 1.3 運用課題

| ID | 課題 | 重要度 | 影響範囲 |
|----|------|--------|----------|
| O-1 | Azure OpenAI のコスト管理 | 高 | 運用 |
| O-2 | プロンプトテンプレートの運用更新 | 中 | 運用 |
| O-3 | モデルバージョンアップ対応 | 中 | 運用 |

### 1.4 セキュリティ課題

| ID | 課題 | 重要度 | 影響範囲 |
|----|------|--------|----------|
| S-1 | プロンプトインジェクション対策 | 高 | Phase 3 |
| S-2 | 機密情報のマスキング | 高 | Phase 3, 4 |
| S-3 | テナント間データ隔離の確認 | 高 | Phase 4 |

---

## 2. 対応方針

### 2.1 T-1: AI クライアントライブラリ選定

**課題詳細**:
- Java から Azure OpenAI を呼び出すライブラリの選定
- 安定性、保守性、将来の拡張性を考慮

**最新バージョン情報** (2025年12月時点):

| ライブラリ | バージョン | 状態 | 備考 |
|-----------|-----------|------|------|
| **Spring AI** | **1.1.0 GA** | **✅ GA** | **第一推奨** - 2025/11/12 リリース |
| Spring AI | 1.0.1 | GA | 2025/8/8 リリース、150以上の安定性改善 |
| `com.azure:azure-ai-openai` | 1.0.0-beta.16 | ⚠️ **Deprecated** | **非推奨** - 更新停止 |
| openai-java | 最新 | Community | Azure 推奨の移行先 |

> **⚠️ 重要**: Azure OpenAI Java SDK (`com.azure:azure-ai-openai`) は **2025年に非推奨（Deprecated）** となりました。
> Microsoft は `openai-java` コミュニティライブラリへの移行を推奨しています。
> Spring AI はこの問題を解決する最適な選択肢です。

**対応方針: Spring AI 1.1 GA を第一候補として採用**

> **⚠️ 実装時の注意**: Spring AI 1.1 は 2025年11月リリースのため、実装時には Web 検索（`vscode-websearchforcopilot_webSearch`）を活用し、最新のベストプラクティス・設定方法・サンプルコードを参照すること。公式ドキュメント（https://docs.spring.io/spring-ai/reference/）および Spring AI GitHub リポジトリを優先的に確認する。

1. **Spring AI 1.1 GA の採用理由**:
   - 2025年11月12日に GA リリース済み（850以上の改善）
   - Azure OpenAI SDK の非推奨問題を回避
   - Spring Boot との完全な統合
   - MCP (Model Context Protocol) ネイティブ対応（`@McpTool`, `@McpResource`, `@McpPrompt`）
   - ストリーミング、RAG、プロンプトキャッシュ対応
   - Chat Memory 機能内蔵（MongoDB, Oracle JDBC, Azure Cosmos DB）
   - Observability 対応（Micrometer, OpenTelemetry）

2. **artifact名の変更に注意**:
   - 旧: `spring-ai-azure-openai-spring-boot-starter`
   - 新: `spring-ai-starter-model-azure-openai`

**Spring AI 1.1 の主要機能**:

| 機能 | 説明 |
|------|------|
| MCP 統合 | @McpTool, @McpResource, @McpPrompt アノテーション |
| プロンプトキャッシュ | 最大90%のコスト削減（Anthropic Claude対応） |
| Chat Memory | 会話履歴の永続化（MongoDB, Oracle, Cosmos DB） |
| Observability | Micrometer, Prometheus, OpenTelemetry 対応 |
| ネットワーク自動リトライ | 分散環境での耐障害性 |

**実装タスク（Spring AI 1.1 方式）**:
```java
// Spring AI 1.1 の ChatModel を使用
@Configuration
public class MiraAiConfig {
    
    // Spring AI auto-configuration により自動注入
    // application.yml の設定のみで ChatModel が利用可能
}

// サービス実装
@Service
public class MiraChatService {
    private final ChatModel chatModel;
    
    public MiraChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    public AiResponse chat(AiRequest request) {
        Prompt prompt = new Prompt(request.toMessages());
        ChatResponse response = chatModel.call(prompt);
        return AiResponse.from(response);
    }
    
    // ストリーミング対応（Spring AI 標準機能）
    public Flux<String> chatStream(AiRequest request) {
        return chatModel.stream(request.toPrompt())
            .map(response -> response.getResult().getOutput().getText());
    }
}
```

**application.yml 設定（Spring AI 1.1）**:
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
```

**依存関係（build.gradle）**:
```groovy
// Spring AI BOM
dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.1.0"
    }
}

// Spring AI Azure OpenAI Starter（新artifact名）
implementation 'org.springframework.ai:spring-ai-starter-model-azure-openai'
```

**フォールバック戦略（REST API 直接呼び出し）**:
```java
// 万が一 Spring AI で問題が発生した場合のフォールバック
@ConditionalOnProperty(name = "mira.ai.fallback-to-rest", havingValue = "true")
@Service
public class AzureOpenAiRestClient implements AiProviderClient {
    private final RestClient restClient;
    
    public AiResponse chat(AiRequest request) {
        // Azure OpenAI REST API を直接呼び出し
        return restClient.post()
            .uri("/openai/deployments/{deployment}/chat/completions", deploymentName)
            .body(request.toAzureFormat())
            .retrieve()
            .body(AiResponse.class);
    }
}

---

### 2.2 T-2: ストリーミング応答未対応

**課題詳細**:
- MVP ではリクエスト/レスポンス方式のみ
- 長い応答の場合、UX が低下する

**対応方針**:
1. **MVP**: 非ストリーミングで実装。ローディングインジケータで対応
2. **v0.2 以降**: Server-Sent Events (SSE) によるストリーミング対応
3. **Spring AI 利用時**: `StreamingChatModel` が標準でストリーミング対応

**実装タスク（Spring AI 方式）**:
```java
// v0.1: 同期レスポンス
@Service
public class MiraChatService {
    private final ChatModel chatModel;
    
    public AiResponse chat(AiRequest request) {
        return AiResponse.from(chatModel.call(request.toPrompt()));
    }
}

// v0.2: ストリーミング対応（Spring AI 標準機能）
@Service  
public class MiraChatStreamService {
    private final StreamingChatModel streamingChatModel;
    
    public Flux<AiResponseChunk> chatStream(AiRequest request) {
        return streamingChatModel.stream(request.toPrompt())
            .map(AiResponseChunk::from);
    }
}
```

**実装タスク（Azure SDK 方式）**:
```java
// v0.1: 同期レスポンス
AiResponse chat(AiRequest request);

// v0.2 で追加予定: ストリーミング
Flux<AiResponseChunk> chatStream(AiRequest request);
```

---

### 2.3 T-3: 会話履歴のコンテキストウィンドウ管理

**課題詳細**:
- LLM のコンテキストウィンドウには上限がある（GPT-4o: 128K tokens）
- 長い会話履歴をそのまま送ると上限超過

**対応方針**:
1. **トークンカウント**: tiktoken 相当のライブラリでトークン数を推定
2. **履歴圧縮**: 古いメッセージを要約して圧縮
3. **スライディングウィンドウ**: 直近 N 件のメッセージのみ送信
4. **設定可能化**: テナント設定で最大履歴数を制御

**実装タスク**:
```java
public class ConversationContextManager {
    private static final int DEFAULT_MAX_TOKENS = 8000;
    private static final int DEFAULT_MAX_MESSAGES = 20;
    
    public List<Message> prepareContext(Conversation conversation, int maxTokens) {
        // 1. 直近メッセージを取得
        // 2. トークン数を計算
        // 3. 上限を超える場合は古いメッセージを要約 or 削除
        return optimizedMessages;
    }
}
```

---

### 2.4 T-4: マルチテナント環境での API キー管理

**課題詳細**:
- テナントごとに異なる Azure OpenAI リソースを使用する可能性
- API キーの安全な保管と取得

**対応方針**:
1. **MVP**: システム共通の API キー（環境変数）を使用
2. **v0.2 以降**: テナント別 API キー対応
   - `Tenant` エンティティに暗号化キー格納
   - Azure Key Vault 連携

**実装タスク**:
```yaml
# MVP: 環境変数から取得
mira:
  ai:
    azure-openai:
      api-key: ${AZURE_OPENAI_API_KEY}

# v0.2: テナント別設定（将来）
# mira:
#   ai:
#     tenant-specific: true
#     key-vault-enabled: true
```

---

### 2.5 A-1: 既存 Studio パス規約との不整合可能性

**課題詳細**:
- Studio: `/api/studio/*`
- MSTE: `apps/mste/api/*`
- Mira: `apps/mira/api/*` （仕様書定義）

**対応方針**:
1. **MSTE パターンに準拠**: `apps/mira/api/*` を採用
2. **理由**: 
   - 仕様書で定義済み
   - MSTE が最新のパス規約
   - `/api/*` は旧形式

**補足**:
- Studio の `/api/studio/*` は将来的に `apps/studio/api/*` へ移行を検討

---

### 2.6 A-2: ExecutionContext への Mira 固有情報追加要否

**課題詳細**:
- Mira 固有の情報（現在の会話ID、モード等）を ExecutionContext に追加すべきか

**対応方針**:
1. **追加しない**: ExecutionContext は汎用コンテキストとして維持
2. **Mira 専用コンテキスト**: `MiraSessionContext` を別途作成
3. **リクエストスコープ**: `@Scope(SCOPE_REQUEST)` で管理

**実装タスク**:
```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MiraSessionContext {
    private String conversationId;
    private MiraMode currentMode;
    private ContextSnapshot currentSnapshot;
    // ...
}
```

---

### 2.7 A-3: 監査ログの大量データ対策

**課題詳細**:
- チャットは頻繁に発生し、ログデータが急増する可能性

**対応方針**:
1. **保存ポリシー**: `METADATA_ONLY` をデフォルトに
2. **パーティショニング**: 月別パーティションテーブル
3. **TTL 設定**: デフォルト 90 日で自動削除
4. **非同期書き込み**: `@Async` で監査ログ書き込みを非同期化

**実装タスク**:
```java
@Async
public void logAsync(MiraAuditLog log) {
    auditLogRepository.save(log);
}
```

---

### 2.8 S-1: プロンプトインジェクション対策

**課題詳細**:
- ユーザー入力をそのままプロンプトに含めると、意図しない動作を引き起こす可能性

**対応方針**:
1. **入力サニタイズ**: 特殊文字・制御文字のエスケープ
2. **プロンプト構造化**: system / user の明確な分離
3. **出力バリデーション**: 応答内容の検証
4. **ロールベース制限**: 管理者向け情報の出力制御

**実装タスク**:
```java
public class PromptSanitizer {
    public String sanitize(String userInput) {
        // 1. 制御文字除去
        // 2. プロンプト注入パターンの検出・無害化
        // 3. 長さ制限
        return sanitized;
    }
}
```

---

### 2.9 S-2: 機密情報のマスキング

**課題詳細**:
- エラーメッセージやコンテキストに機密情報が含まれる可能性

**対応方針**:
1. **マスキングフィルタ**: API キー、パスワード、個人情報パターンを検出・マスク
2. **ホワイトリスト方式**: 送信可能な情報を明示的に定義
3. **ログ出力時マスク**: 監査ログへの保存時にも適用

**実装タスク**:
```java
public class SensitiveDataMasker {
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(api[_-]?key|secret|password)\\s*[:=]\\s*\\S+");
    
    public String mask(String content) {
        return API_KEY_PATTERN.matcher(content).replaceAll("$1=***MASKED***");
    }
}
```

---

### 2.10 O-1: Azure OpenAI のコスト管理

**課題詳細**:
- 従量課金のため、使用量に応じてコストが増大

**対応方針**:
1. **テナント別上限**: 月間トークン数上限を設定
2. **使用量トラッキング**: `MiraAuditLog` でトークン使用量を記録
3. **アラート**: 閾値超過時に管理者通知
4. **モデル選択**: コスト効率の良いモデルの選択肢提供

**実装タスク**:
```java
public class UsageTracker {
    public void trackUsage(String tenantId, int promptTokens, int completionTokens) {
        // Redis or DB でカウント
    }
    
    public boolean isWithinLimit(String tenantId) {
        // テナント設定の上限と比較
    }
}
```

---

## 3. 優先度マトリクス

```
          高影響
             │
    S-1 ●    │    ● T-3
    S-2 ●    │    ● T-4
    T-1 ●    │
             │
低緊急 ──────┼────── 高緊急
             │
    A-3 ○    │    ○ O-1
    A-4 ○    │
    T-5 ○    │
             │
          低影響
```

**凡例**: ● MVP で対応必須、○ v0.2 以降で対応可

---

## 4. 推奨実装順序

1. **Phase 1-2 と並行**: T-1（SDK 互換性）、T-4（API キー管理 MVP 版）
2. **Phase 3 で対応**: T-3（コンテキスト管理）、S-1（プロンプトインジェクション）、S-2（マスキング）
3. **Phase 4 で対応**: A-2（セッションコンテキスト）、A-3（監査ログ最適化）、S-3（テナント隔離）
4. **Phase 5 で対応**: A-1（パス規約確認）
5. **運用開始後**: O-1（コスト管理）、O-2（プロンプト更新）、O-3（モデル更新）
6. **v0.2 以降**: T-2（ストリーミング）、T-5（状態永続化）

---

## 5. 追加検討事項

### 5.1 未定義事項

| 項目 | 現状 | 要決定 |
|------|------|--------|
| デフォルトモデル | GPT-4o | 確定 or 設定可能に |
| 最大応答トークン | 未定義 | 4096 推奨 |
| レート制限 | 未定義 | テナント別 or 全体 |
| キャッシュ戦略 | 未定義 | プロンプトキャッシュ（Spring AI 1.1 標準対応） |

### 5.2 将来検討（v0.2 以降）

> **Note**: 以下は Spring AI 1.1 で基盤機能が提供されているが、**Mira アプリケーション層での設計・実装が別途必要**。

- [ ] RAG（Retrieval-Augmented Generation）対応 — Spring AI VectorStore 活用、Mira 用ドキュメント取り込み設計が必要
- [ ] Function Calling / Tool Use 対応 — Spring AI Tool 機能活用、Studio 連携用ツール設計が必要
- [ ] MCP (Model Context Protocol) 連携 — Spring AI @McpTool 等活用、mirelplatform 向け MCP サーバ設計が必要
- [ ] Chat Memory（会話履歴永続化）— Spring AI ChatMemory 活用、Mira 用ストレージ選定・スキーマ設計が必要
- [ ] プロンプトキャッシュ — コスト最適化検討、キャッシュ戦略設計が必要
- [ ] Observability — Micrometer/OpenTelemetry 連携、Mira 用メトリクス・トレース設計が必要
- [ ] マルチモーダル対応（画像入力）— Spring AI MultimodalMessage API 活用
- [ ] 音声入力対応
- [ ] オンプレミスモデル対応（Ollama 等）— Spring AI で対応可能

---

## 6. 次のアクション

1. [x] 課題 T-1 の方針決定 → Spring AI 1.1 GA 採用
2. [ ] Spring AI 1.1 の依存関係追加（build.gradle）— 実装時に最新ドキュメント参照
3. [ ] Phase 1 の実装開始
4. [ ] Azure OpenAI リソースの準備確認
5. [ ] 開発環境用 API キーの発行依頼
