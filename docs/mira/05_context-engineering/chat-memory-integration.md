# Mira ChatMemory 統合設計

> **Issue**: #50 Mira v1 実装  
> **作成日**: 2025-12-07  
> **関連**: [context-engineering-plan.md](context-engineering-plan.md)

---

## 1. 統合戦略

### 1.1 比較分析

| 観点 | Spring AI ChatMemory | 既存 Mira 実装 |
|------|---------------------|----------------|
| **テーブル** | `SPRING_AI_CHAT_MEMORY` | `mir_mira_conversation` / `mir_mira_message` |
| **セッション管理** | `conversationId` のみ | `tenantId`, `userId`, `mode` |
| **メタデータ** | 基本的なメッセージ情報 | `tokenCount`, `usedModel`, `contentType`, `contextSnapshotId` |
| **拡張性** | Dialect 実装で拡張可能 | 既にカスタム済み |
| **階層コンテキスト** | なし | 設計予定（Tenant/Org/User） |

### 1.2 選定: ハイブリッドアプローチ

**既存エンティティを維持しつつ、Spring AI の `ChatMemory` インターフェースを実装するアダプターを作成**

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring AI ChatClient                      │
│                  + MessageChatMemoryAdvisor                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              MiraChatMemoryAdapter                           │
│         implements ChatMemory, ChatMemoryRepository          │
├─────────────────────────────────────────────────────────────┤
│  - add(conversationId, messages)                            │
│  - get(conversationId, lastN)                               │
│  - clear(conversationId)                                    │
│  - findByConversationId(conversationId)                     │
│  - saveAll(conversationId, messages)                        │
│  - deleteByConversationId(conversationId)                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 既存 Mira エンティティ                        │
├─────────────────────────────────────────────────────────────┤
│  MiraConversation          │  MiraMessage                    │
│  - id (UUID)               │  - id (UUID)                    │
│  - tenantId                │  - conversationId (FK)          │
│  - userId                  │  - senderType (USER/ASSISTANT)  │
│  - mode                    │  - content                      │
│  - status                  │  - contextSnapshotId            │
│  - title                   │  - usedModel                    │
│  - lastActivityAt          │  - tokenCount                   │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 メリット

1. **既存投資の保護**: `mir_mira_*` テーブルをそのまま利用
2. **Spring AI 統合**: `MessageChatMemoryAdvisor` による自動メモリ管理
3. **拡張性維持**: 階層コンテキスト（Tenant/Org/User）を追加可能
4. **メタデータ保持**: `tokenCount`, `usedModel` 等の独自メタデータを維持

---

## 2. DB 階層コンテキスト設計

### 2.1 コンテキストレイヤー構造

```
┌─────────────────────────────────────────────────────────────┐
│                     System Context                           │
│              (グローバル設定・共通ナレッジ)                    │
├─────────────────────────────────────────────────────────────┤
│  - プラットフォーム全体の用語定義                             │
│  - 共通エラーパターン辞書                                    │
│  - デフォルト応答スタイル                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Tenant Context                           │
│              (テナント固有の設定・知識)                       │
├─────────────────────────────────────────────────────────────┤
│  - テナント固有の用語（業界用語等）                           │
│  - カスタムエンティティ定義                                  │
│  - テナント固有のワークフローパターン                         │
│  - Mira 応答スタイルのカスタマイズ                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Organization Context                       │
│              (組織固有の設定・知識)                           │
├─────────────────────────────────────────────────────────────┤
│  - 組織固有のビジネスルール                                  │
│  - 部署別のワークフロー設定                                  │
│  - 組織固有のアクセス制限ルール                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      User Context                            │
│              (ユーザー固有の設定・履歴)                       │
├─────────────────────────────────────────────────────────────┤
│  - ユーザー設定（言語、応答スタイル）                         │
│  - 直近の会話サマリー                                        │
│  - ユーザーのスキルレベル（初心者/上級者）                    │
│  - よく使う機能の統計                                        │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 新規エンティティ: `MiraContextLayer`

```java
@Entity
@Table(name = "mir_mira_context_layer")
public class MiraContextLayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ContextScope scope;  // SYSTEM, TENANT, ORGANIZATION, USER

    @Column(name = "scope_id")
    private String scopeId;  // null for SYSTEM, tenantId/orgId/userId for others

    @Column(nullable = false)
    private String category;  // terminology, workflow_patterns, style, etc.

    @Column(columnDefinition = "TEXT")
    private String content;  // Markdown or JSON content

    @Column(nullable = false)
    private Integer priority = 0;  // Higher = applied later (overrides)

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

public enum ContextScope {
    SYSTEM,       // グローバル（全テナント共通）
    TENANT,       // テナント固有
    ORGANIZATION, // 組織固有
    USER          // ユーザー固有
}
```

### 2.3 コンテキストマージロジック

```java
@Service
@RequiredArgsConstructor
public class MiraContextLayerService {

    private final MiraContextLayerRepository repository;

    /**
     * 階層コンテキストをマージして最終コンテキストを生成
     * 優先度: SYSTEM < TENANT < ORGANIZATION < USER
     */
    public String buildMergedContext(String tenantId, String orgId, String userId, String category) {
        List<MiraContextLayer> layers = repository.findByScopesAndCategory(
            List.of(
                new ScopeKey(ContextScope.SYSTEM, null),
                new ScopeKey(ContextScope.TENANT, tenantId),
                new ScopeKey(ContextScope.ORGANIZATION, orgId),
                new ScopeKey(ContextScope.USER, userId)
            ),
            category
        );

        // 優先度順にソートしてマージ
        return layers.stream()
            .filter(MiraContextLayer::getEnabled)
            .sorted(Comparator.comparing(l -> l.getScope().ordinal() * 1000 + l.getPriority()))
            .map(MiraContextLayer::getContent)
            .collect(Collectors.joining("\n\n"));
    }
}
```

### 2.4 State Layer への統合

```java
public class StateContext {

    // 既存フィールド
    private String screenId;
    private String systemRole;
    private String appRole;
    private String tenantId;
    private String locale;
    private String selectedEntity;
    private List<String> recentActions;
    private ErrorContext errorContext;

    // 階層コンテキスト（新規）
    private HierarchicalContext hierarchicalContext;

    @Data
    public static class HierarchicalContext {
        private Map<String, String> systemContext;  // category -> content
        private Map<String, String> tenantContext;
        private Map<String, String> orgContext;
        private Map<String, String> userContext;
    }
}
```

---

## 3. ChatMemory アダプター実装

### 3.1 インターフェース実装

```java
@Component
@RequiredArgsConstructor
public class MiraChatMemoryAdapter implements ChatMemory, ChatMemoryRepository {

    private final MiraConversationRepository conversationRepository;
    private final MiraMessageRepository messageRepository;
    private final MiraContextLayerService contextLayerService;

    // ========== ChatMemory Interface ==========

    @Override
    public void add(String conversationId, List<Message> messages) {
        UUID convId = UUID.fromString(conversationId);
        MiraConversation conversation = conversationRepository.findById(convId)
            .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        for (Message message : messages) {
            MiraMessage entity = toEntity(convId, message);
            messageRepository.save(entity);
        }

        conversation.setLastActivityAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        UUID convId = UUID.fromString(conversationId);
        List<MiraMessage> messages = messageRepository
            .findByConversationIdOrderByCreatedAtDesc(convId, PageRequest.of(0, lastN));

        Collections.reverse(messages);  // 古い順に戻す
        return messages.stream()
            .map(this::toSpringAiMessage)
            .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        UUID convId = UUID.fromString(conversationId);
        messageRepository.deleteByConversationId(convId);
    }

    // ========== ChatMemoryRepository Interface ==========

    @Override
    public List<Message> findByConversationId(String conversationId) {
        UUID convId = UUID.fromString(conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(convId)
            .stream()
            .map(this::toSpringAiMessage)
            .collect(Collectors.toList());
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        clear(conversationId);
        add(conversationId, messages);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        clear(conversationId);
    }

    // ========== Conversion Methods ==========

    private MiraMessage toEntity(UUID conversationId, Message message) {
        MiraMessage entity = new MiraMessage();
        entity.setConversationId(conversationId);
        entity.setSenderType(mapMessageType(message.getMessageType()));
        entity.setContent(message.getText());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private Message toSpringAiMessage(MiraMessage entity) {
        return switch (entity.getSenderType()) {
            case USER -> new UserMessage(entity.getContent());
            case ASSISTANT -> new AssistantMessage(entity.getContent());
            case SYSTEM -> new SystemMessage(entity.getContent());
        };
    }

    private MiraMessageSenderType mapMessageType(MessageType type) {
        return switch (type) {
            case USER -> MiraMessageSenderType.USER;
            case ASSISTANT -> MiraMessageSenderType.ASSISTANT;
            case SYSTEM -> MiraMessageSenderType.SYSTEM;
            default -> MiraMessageSenderType.SYSTEM;
        };
    }
}
```

### 3.2 ChatClient 統合

```java
@Configuration
public class MiraChatClientConfiguration {

    @Bean
    public ChatClient miraChatClient(
            ChatModel chatModel,
            MiraChatMemoryAdapter chatMemoryAdapter) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryAdapter)
            .maxMessages(20)
            .build();

        return ChatClient.builder(chatModel)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
    }
}
```

### 3.3 使用例

```java
@Service
@RequiredArgsConstructor
public class MiraChatService {

    private final ChatClient miraChatClient;
    private final MiraPromptService promptService;
    private final MiraContextLayerService contextLayerService;

    public String chat(MiraContext context, String userMessage) {
        // 階層コンテキストを取得
        String terminology = contextLayerService.buildMergedContext(
            context.getTenantId(),
            context.getOrgId(),
            context.getUserId(),
            "terminology"
        );

        // System Prompt に階層コンテキストを追加
        String systemPrompt = promptService.buildSystemPrompt(context);
        if (terminology != null && !terminology.isEmpty()) {
            systemPrompt += "\n\n# Additional Terminology\n" + terminology;
        }

        return miraChatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, context.getConversationId()))
            .call()
            .content();
    }
}
```

---

## 4. マイグレーション

### 4.1 新規テーブル DDL

```sql
-- 階層コンテキストレイヤー
CREATE TABLE mir_mira_context_layer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope VARCHAR(20) NOT NULL,
    scope_id VARCHAR(255),
    category VARCHAR(100) NOT NULL,
    content TEXT,
    priority INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- インデックス
CREATE INDEX idx_context_layer_scope ON mir_mira_context_layer(scope, scope_id);
CREATE INDEX idx_context_layer_category ON mir_mira_context_layer(category);

-- 初期データ: システムレベルの用語定義
INSERT INTO mir_mira_context_layer (scope, scope_id, category, content, priority) VALUES
('SYSTEM', NULL, 'terminology', '
## Platform Terms
- Mira: AI Assistant (keep in English, never translate to ミラ)
- mirelplatform: Platform name (keep in English)
- ProMarker: Sample application (keep in English)
- Studio: Development environment (keep in English)
', 0);
```

---

## 5. 今後の拡張

### 5.1 Phase 2: 会話サマリー

長期記憶として、過去の会話をサマリー化して保存:

```java
@Entity
@Table(name = "mir_mira_conversation_summary")
public class MiraConversationSummary {
    @Id
    private UUID id;
    private UUID conversationId;
    private String summary;  // LLM生成のサマリー
    private Integer messageCount;  // サマリー対象のメッセージ数
    private LocalDateTime summarizedAt;
}
```

### 5.2 Phase 3: Vector Store 統合

RAG (Retrieval Augmented Generation) のための知識ベース:

```java
// VectorStoreChatMemoryAdvisor との併用
ChatClient chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        MessageChatMemoryAdvisor.builder(chatMemory).build(),
        VectorStoreChatMemoryAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.defaults().withTopK(5))
            .build()
    )
    .build();
```

---

## 6. 参考資料

- [Spring AI Chat Memory Reference](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
- [MessageChatMemoryAdvisor](https://docs.spring.io/spring-ai/reference/api/chat-memory.html#memory-in-chat-client)
- [Custom ChatMemoryRepository](https://www.baeldung.com/spring-ai-chat-memory)
