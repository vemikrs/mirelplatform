# Mira AI モデル選択機能の拡張計画

## 日付
2025-12-14

## 概要
ユーザーがプロバイダごとに利用可能なモデルを選択できるようにし、設定の優先順位を明確化する。モデル情報をデータベースで管理し、動的な選択を可能にする。

## 要件

### 1. モデル選択の優先順位
設定値の優先順位（高 → 低）:
1. **ユーザー入力メニュー** (Chat API リクエストの `forceModel` パラメータ)
2. **ユーザーコンテキスト設定** (`mira_user_context` テーブル)
3. **テナント設定** (`mira_tenant_setting` テーブル)
4. **システム設定** (`mira_system_setting` テーブル)
5. **プロパティファイル** (`application.yml` / `application-*.yml`)

### 2. モデル情報のデータベース管理
- 利用可能なモデル一覧を DB で管理
- プロバイダごとのモデル情報（機能、制限等）を保持
- 管理画面からモデルの有効/無効を切り替え可能

## アーキテクチャ設計

### データベーススキーマ

#### 新規テーブル: `mira_model_registry`
プロバイダごとの利用可能モデルを管理。

```sql
CREATE TABLE mira_model_registry (
    id VARCHAR(255) PRIMARY KEY,              -- モデルID (例: vertex-ai-gemini:gemini-2.5-flash)
    provider VARCHAR(100) NOT NULL,            -- プロバイダ名 (vertex-ai-gemini, github-models等)
    model_name VARCHAR(255) NOT NULL,          -- モデル名 (gemini-2.5-flash, gpt-4o等)
    display_name VARCHAR(255) NOT NULL,        -- 表示名 (Gemini 2.5 Flash)
    description TEXT,                          -- モデル説明
    capabilities JSONB,                        -- 機能配列 ["TOOL_CALLING", "STREAMING", ...]
    max_tokens INTEGER,                        -- 最大トークン数
    context_window INTEGER,                    -- コンテキストウィンドウサイズ
    is_active BOOLEAN DEFAULT true,            -- 有効/無効
    is_recommended BOOLEAN DEFAULT false,      -- 推奨モデルフラグ
    is_experimental BOOLEAN DEFAULT false,     -- 実験的モデルフラグ
    metadata JSONB,                            -- その他メタデータ
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, model_name)
);

-- インデックス
CREATE INDEX idx_model_registry_provider ON mira_model_registry(provider);
CREATE INDEX idx_model_registry_active ON mira_model_registry(is_active);
```

#### 既存テーブル拡張: `mira_user_context`
ユーザーコンテキストにモデル選択を追加。

```sql
ALTER TABLE mira_user_context 
ADD COLUMN preferred_provider VARCHAR(100),
ADD COLUMN preferred_model VARCHAR(255);

-- インデックス追加
CREATE INDEX idx_user_context_provider_model ON mira_user_context(preferred_provider, preferred_model);
```

### 設定取得フロー

```
┌─────────────────────────────────────────────────────────────┐
│ 1. ChatRequest (forceModel パラメータ)                      │
│    - 最高優先度                                              │
│    - 管理者またはユーザーが一時的にモデル変更               │
└──────────────────────┬──────────────────────────────────────┘
                       ↓ なし
┌─────────────────────────────────────────────────────────────┐
│ 2. MiraUserContext (user_id + context_id)                   │
│    - ユーザーが保存した設定                                 │
│    - プロファイル/ワークスペースごとに異なるモデルを使用   │
└──────────────────────┬──────────────────────────────────────┘
                       ↓ なし
┌─────────────────────────────────────────────────────────────┐
│ 3. MiraTenantSetting (tenant_id, "ai.model")                │
│    - テナント管理者が設定したデフォルト                     │
└──────────────────────┬──────────────────────────────────────┘
                       ↓ なし
┌─────────────────────────────────────────────────────────────┐
│ 4. MiraSystemSetting ("ai.model")                           │
│    - システム管理者が設定したデフォルト                     │
└──────────────────────┬──────────────────────────────────────┘
                       ↓ なし
┌─────────────────────────────────────────────────────────────┐
│ 5. application.yml (mira.ai.*.model)                        │
│    - プロパティファイルのデフォルト                         │
└─────────────────────────────────────────────────────────────┘
```

## 実装計画

### Phase 1: データベーススキーマと初期データ（2-3時間）

#### 1.1 既存テーブル定義更新
**ファイル**: `backend/src/main/java/jp/vemi/framework/config/system/TableDefinitions.java` または適切な場所

```java
// mira_model_registry テーブル定義追加
CREATE TABLE mira_model_registry (
    id VARCHAR(255) PRIMARY KEY,
    provider VARCHAR(100) NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    capabilities TEXT,  -- JSON文字列として保存
    max_tokens INTEGER,
    context_window INTEGER,
    is_active BOOLEAN DEFAULT true,
    is_recommended BOOLEAN DEFAULT false,
    is_experimental BOOLEAN DEFAULT false,
    metadata TEXT,  -- JSON文字列として保存
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, model_name)
);

CREATE INDEX idx_model_registry_provider ON mira_model_registry(provider);
CREATE INDEX idx_model_registry_active ON mira_model_registry(is_active);

// mira_user_context テーブル拡張
ALTER TABLE mira_user_context 
ADD COLUMN preferred_provider VARCHAR(100),
ADD COLUMN preferred_model VARCHAR(255);

CREATE INDEX idx_user_context_provider_model ON mira_user_context(preferred_provider, preferred_model);
```

#### 1.2 初期データ CSV 定義
**ファイル**: `backend/src/main/resources/data/system/mira_model_registry.csv` または `backend/src/main/resources/data/sample/mira_model_registry.csv`

```csv
id,provider,model_name,display_name,description,capabilities,max_tokens,context_window,is_active,is_recommended,is_experimental,metadata,created_at,updated_at
vertex-ai-gemini:gemini-2.5-flash,vertex-ai-gemini,gemini-2.5-flash,Gemini 2.5 Flash,高速・低コストの最新モデル,"[""TOOL_CALLING"",""MULTIMODAL_INPUT"",""STREAMING"",""LONG_CONTEXT""]",8192,1000000,true,true,false,{},2025-01-01 00:00:00,2025-01-01 00:00:00
vertex-ai-gemini:gemini-2.0-flash-exp,vertex-ai-gemini,gemini-2.0-flash-exp,Gemini 2.0 Flash (Experimental),実験的な次世代モデル,"[""TOOL_CALLING"",""MULTIMODAL_INPUT"",""STREAMING"",""LONG_CONTEXT""]",8192,1000000,true,false,true,{},2025-01-01 00:00:00,2025-01-01 00:00:00
vertex-ai-gemini:gemini-1.5-pro,vertex-ai-gemini,gemini-1.5-pro,Gemini 1.5 Pro,高精度な汎用モデル,"[""TOOL_CALLING"",""MULTIMODAL_INPUT"",""STREAMING"",""LONG_CONTEXT""]",8192,2000000,true,false,false,{},2025-01-01 00:00:00,2025-01-01 00:00:00
github-models:openai/gpt-4o,github-models,openai/gpt-4o,GPT-4o,OpenAI の最新マルチモーダルモデル,"[""TOOL_CALLING"",""MULTIMODAL_INPUT"",""STREAMING""]",16384,128000,true,true,false,{},2025-01-01 00:00:00,2025-01-01 00:00:00
github-models:openai/gpt-4o-mini,github-models,openai/gpt-4o-mini,GPT-4o Mini,高速・低コストの GPT-4 系モデル,"[""TOOL_CALLING"",""STREAMING""]",16384,128000,true,false,false,{},2025-01-01 00:00:00,2025-01-01 00:00:00
github-models:openai/o1-preview,github-models,openai/o1-preview,o1 Preview,推論特化型モデル（実験的）,"[""TOOL_CALLING""]",32768,128000,true,false,true,{},2025-01-01 00:00:00,2025-01-01 00:00:00
github-models:meta/llama-3.3-70b-instruct,github-models,meta/llama-3.3-70b-instruct,Llama 3.3 70B Instruct,Meta の最新オープンソースモデル（ツール呼び出し非対応）,"[""STREAMING"",""LONG_CONTEXT""]",8192,128000,true,false,false,{},2025-01-01 00:00:00,2025-01-01 00:00:00
azure-openai:gpt-4o,azure-openai,gpt-4o,GPT-4o,Azure OpenAI Service の GPT-4o,"[""TOOL_CALLING"",""MULTIMODAL_INPUT"",""STREAMING""]",16384,128000,true,true,false,{},2025-01-01 00:00:00,2025-01-01 00:00:00
azure-openai:gpt-4,azure-openai,gpt-4,GPT-4,Azure OpenAI Service の GPT-4,"[""TOOL_CALLING"",""STREAMING""]",8192,8192,true,false,false,{},2025-01-01 00:00:00,2025-01-01 00:00:00
```

**注**: CSV は `system` または `sample` ディレクトリに配置し、起動時のデータロード機能（`mirel.data.load-mode`）で自動投入。

#### 1.3 Entity クラス作成
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/domain/dao/entity/MiraModelRegistry.java`

```java
@Entity
@Table(name = "mira_model_registry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiraModelRegistry {
    @Id
    private String id;
    
    @Column(nullable = false)
    private String provider;
    
    @Column(name = "model_name", nullable = false)
    private String modelName;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String capabilities;  // JSON文字列 (例: ["TOOL_CALLING", "STREAMING"])
    
    @Column(name = "max_tokens")
    private Integer maxTokens;
    
    @Column(name = "context_window")
    private Integer contextWindow;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "is_recommended")
    private Boolean isRecommended = false;
    
    @Column(name = "is_experimental")
    private Boolean isExperimental = false;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;  // JSON文字列
    
    // ヘルパーメソッド: JSON文字列をリストに変換
    @Transient
    public List<String> getCapabilitiesList() {
        if (capabilities == null) return Collections.emptyList();
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(capabilities, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    @Transient
    public void setCapabilitiesList(List<String> list) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.capabilities = mapper.writeValueAsString(list);
        } catch (Exception e) {
            this.capabilities = "[]";
        }
    }
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### 1.4 Repository 作成
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/domain/dao/repository/MiraModelRegistryRepository.java`

```java
public interface MiraModelRegistryRepository extends JpaRepository<MiraModelRegistry, String> {
    List<MiraModelRegistry> findByProviderAndIsActiveTrue(String provider);
    List<MiraModelRegistry> findByIsActiveTrue();
    Optional<MiraModelRegistry> findByProviderAndModelName(String provider, String modelName);
}
```

### Phase 2: バックエンドロジック実装（4-5時間）

#### 2.1 モデル選択サービス
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/domain/service/ModelSelectionService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelSelectionService {
    
    private final MiraModelRegistryRepository modelRegistryRepository;
    private final MiraUserContextRepository userContextRepository;
    private final MiraSettingService settingService;
    private final MiraAiProperties aiProperties;
    
    /**
     * 優先順位に基づいてモデルを選択.
     * 
     * @param tenantId テナントID
     * @param userId ユーザーID
     * @param contextId コンテキストID（任意）
     * @param forceModel リクエストで指定されたモデル（最優先）
     * @return 選択されたモデル名
     */
    public String resolveModel(String tenantId, String userId, String contextId, String forceModel) {
        // 1. リクエストで明示的に指定されている場合（最優先）
        if (forceModel != null && !forceModel.isEmpty()) {
            log.info("Using force-specified model: {} (tenant={}, user={})", forceModel, tenantId, userId);
            return forceModel;
        }
        
        // 2. ユーザーコンテキスト設定
        if (contextId != null && !contextId.isEmpty()) {
            Optional<MiraUserContext> context = userContextRepository.findByUserIdAndContextId(userId, contextId);
            if (context.isPresent() && context.get().getPreferredModel() != null) {
                log.info("Using user context model: {} (tenant={}, user={}, context={})", 
                    context.get().getPreferredModel(), tenantId, userId, contextId);
                return context.get().getPreferredModel();
            }
        }
        
        // 3. テナント設定
        String tenantModel = settingService.getString(tenantId, MiraSettingService.KEY_AI_MODEL, null);
        if (tenantModel != null && !tenantModel.isEmpty()) {
            log.info("Using tenant model: {} (tenant={})", tenantModel, tenantId);
            return tenantModel;
        }
        
        // 4. システム設定
        String systemModel = settingService.getString(null, MiraSettingService.KEY_AI_MODEL, null);
        if (systemModel != null && !systemModel.isEmpty()) {
            log.info("Using system model: {}", systemModel);
            return systemModel;
        }
        
        // 5. プロパティファイルのデフォルト（プロバイダに応じた値）
        String provider = settingService.getAiProvider(tenantId);
        String defaultModel = getDefaultModelForProvider(provider);
        log.info("Using default model from properties: {} (provider={})", defaultModel, provider);
        return defaultModel;
    }
    
    /**
     * プロバイダに応じたデフォルトモデルを取得.
     */
    private String getDefaultModelForProvider(String provider) {
        switch (provider) {
            case MiraAiProperties.PROVIDER_VERTEX_AI_GEMINI:
                return aiProperties.getVertexAi().getModel();
            case MiraAiProperties.PROVIDER_AZURE_OPENAI:
                return aiProperties.getAzureOpenai().getDeploymentName();
            case MiraAiProperties.PROVIDER_GITHUB_MODELS:
                return aiProperties.getGithubModels().getModel();
            default:
                return aiProperties.getGithubModels().getModel();
        }
    }
    
    /**List() != null && 
               model.get().getCapabilitiesList
     */
    public List<MiraModelRegistry> getAvailableModels(String provider) {
        return modelRegistryRepository.findByProviderAndIsActiveTrue(provider);
    }
    
    /**
     * すべての有効なモデル一覧を取得.
     */
    public List<MiraModelRegistry> getAllAvailableModels() {
        return modelRegistryRepository.findByIsActiveTrue();
    }
    
    /**
     * モデルの機能をチェック.
     */
    public boolean supportsCapability(String provider, String modelName, String capability) {
        Optional<MiraModelRegistry> model = modelRegistryRepository.findByProviderAndModelName(provider, modelName);
        return model.isPresent() && 
               model.get().getCapabilities() != null && 
               model.get().getCapabilities().contains(capability);
    }
}
```

#### 2.2 MiraSettingService 修正
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/domain/service/MiraSettingService.java`

```java
// getAiModel() を ModelSelectionService に委譲する形に変更
@Deprecated
public String getAiModel(String tenantId) {
    log.warn("getAiModel() is deprecated. Use ModelSelectionService.resolveModel() instead.");
    // 後方互換性のため残す
    String provider = getAiProvider(tenantId);
    // ... 既存のロジック
}
```

#### 2.3 MiraChatService / MiraStreamService 修正
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/domain/service/MiraChatService.java`

```java
@RequiredArgsConstructor
public class MiraChatService {
    // 新規追加
    private final ModelSelectionService modelSelectionService;
    
    public ChatResponse chat(ChatRequest request, String tenantId, String userId) {
        // モデル選択ロジック追加
        String selectedModel = modelSelectionService.resolveModel(
            tenantId, 
            userId, 
            request.getContext() != null ? request.getContext().getContextId() : null,
            request.getForceModel()  // 新規フィールド
        );
        
        // AiRequest にモデル名をセット
        aiRequest.setModelName(selectedModel);
        
        // ... 既存のロジック
    }
}
```

### Phase 3: API エンドポイント実装（3-4時間）

#### 3.1 管理者API（モデル管理）
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/application/controller/MiraAdminController.java`

```java
@RestController
@RequestMapping("apps/mira/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class MiraAdminController {
    
    private final ModelSelectionService modelSelectionService;
    
    // プロバイダ一覧取得
    @GetMapping("/providers")
    @Operation(summary = "利用可能なプロバイダ一覧取得")
    public ResponseEntity<List<ProviderInfo>> getProviders() {
        // AiProviderFactory.getAvailableProviders() を使用
        List<ProviderInfo> providers = aiProviderFactory.getAvailableProviders()
            .stream()
            .map(name -> {
                AiProviderClient client = aiProviderFactory.getProvider(name).orElse(null);
                return ProviderInfo.builder()
                    .name(name)
                    .displayName(getProviderDisplayName(name))
                    .available(client != null && client.isAvailable())
                    .build();
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(providers);
    }
    
    // モデル一覧取得
    @GetMapping("/models")
    @Operation(summary = "プロバイダ別モデル一覧取得")
    public ResponseEntity<List<MiraModelRegistry>> getModels(
            @RequestParam(required = false) String provider) {
        
        if (provider != null) {
            return ResponseEntity.ok(modelSelectionService.getAvailableModels(provider));
        } else {
            return ResponseEntity.ok(modelSelectionService.getAllAvailableModels());
        }
    }
    
    // モデル情報更新
    @PutMapping("/models/{id}")
    @Operation(summary = "モデル情報更新")
    public ResponseEntity<MiraModelRegistry> updateModel(
            @PathVariable String id,
            @RequestBody MiraModelRegistry model) {
        // 実装
        return ResponseEntity.ok(modelRegistryRepository.save(model));
    }
}
```

#### 3.2 ユーザーAPI（モデル選択）
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/application/controller/MiraApiController.java`

```java
@RestController
@RequestMapping("apps/mira/api")
public class MiraApiController {
    
    private final ModelSelectionService modelSelectionService;
    
    // 利用可能モデル取得（ユーザー向け）
    @GetMapping("/available-models")
    @Operation(summary = "利用可能なモデル一覧取得")
    public ResponseEntity<List<MiraModelRegistry>> getAvailableModels() {
        String tenantId = tenantContextManager.getCurrentTenantId();
        String provider = settingService.getAiProvider(tenantId);
        
        return ResponseEntity.ok(modelSelectionService.getAvailableModels(provider));
    }
    
    // ユーザーコンテキストのモデル設定更新
    @PutMapping("/user-context/model")
    @Operation(summary = "ユーザーコンテキストのモデル設定更新")
    public ResponseEntity<Void> updateUserContextModel(
            @RequestBody UpdateModelRequest request) {
        
        String userId = tenantContextManager.getCurrentUserId();
        
        // MiraUserContext の preferred_model を更新
        // 実装
        
        return ResponseEntity.ok().build();
    }
}
```

#### 3.3 ChatRequest DTO 拡張
**ファイル**: `backend/src/main/java/jp/vemi/mirel/apps/mira/domain/dto/request/ChatRequest.java`

```java
@Data
public class ChatRequest {
    // 既存フィールド
    private String conversationId;
    private String mode;
    private Message message;
    private Context context;
    
    // 新規追加
    private String forceModel;      // ユーザーが一時的にモデル変更
    private String forceProvider;   // ユーザーが一時的にプロバイダ変更
    
    // ... その他
}
```

### Phase 4: フロントエンド実装（5-6時間）

#### 4.1 API クライアント更新
**ファイル**: `apps/frontend-v3/src/lib/api/mira.ts`

```typescript
// プロバイダ一覧取得
export async function getProviders(): Promise<ProviderInfo[]> {
  const response = await apiClient.get<ApiResponse<ProviderInfo[]>>(
    '/apps/mira/api/admin/providers'
  );
  return response.data.data || [];
}

// モデル一覧取得
export async function getModels(provider?: string): Promise<ModelInfo[]> {
  const params = provider ? { provider } : {};
  const response = await apiClient.get<ApiResponse<ModelInfo[]>>(
    '/apps/mira/api/admin/models',
    { params }
  );
  return response.data.data || [];
}

// 利用可能モデル取得（ユーザー向け）
export async function getAvailableModels(): Promise<ModelInfo[]> {
  const response = await apiClient.get<ApiResponse<ModelInfo[]>>(
    '/apps/mira/api/available-models'
  );
  return response.data.data || [];
}
```

#### 4.2 Mira 管理画面の改修
**ファイル**: `apps/frontend-v3/src/features/admin/pages/MiraAdminPage.tsx`

```typescript
// プロバイダ選択時にモデル一覧を動的取得
const [providers, setProviders] = useState<ProviderInfo[]>([]);
const [models, setModels] = useState<ModelInfo[]>([]);

useEffect(() => {
  loadProviders();
}, []);

useEffect(() => {
  if (aiConfig.provider) {
    loadModelsForProvider(aiConfig.provider);
  }
}, [aiConfig.provider]);

const loadProviders = async () => {
  const data = await getProviders();
  setProviders(data);
};

const loadModelsForProvider = async (provider: string) => {
  const data = await getModels(provider);
  setModels(data);
};

// UI
<Combobox 
  options={providers.map(p => ({ 
    value: p.name, 
    label: p.displayName,
    disabled: !p.available
  }))}
  value={aiConfig.provider || ""}
  onValueChange={(v) => {
    setAiConfig({...aiConfig, provider: v, model: ""});
  }}
  placeholder="プロバイダを選択"
/>

<Combobox 
  options={models.map(m => ({ 
    value: m.modelName, 
    label: m.displayName,
    description: m.description,
    badge: m.isRecommended ? "推奨" : undefined
  }))}
  value={aiConfig.model || ""}
  onValueChange={(v) => setAiConfig({...aiConfig, model: v})}
  placeholder="モデルを選択"
  disabled={!aiConfig.provider}
/>
```

#### 4.3 Mira チャット画面にモデル選択追加
**ファイル**: `apps/frontend-v3/src/features/promarker/pages/v3/MiraAssistantPage.tsx`

```typescript
// メッセージ送信時にモデル選択を追加
const [selectedModel, setSelectedModel] = useState<string | null>(null);
const [availableModels, setAvailableModels] = useState<ModelInfo[]>([]);

const handleSendMessage = async () => {
  await sendMessage({
    // ... 既存のパラメータ
    forceModel: selectedModel || undefined,  // ユーザーが選択したモデル
  });
};

// UI: モデル選択ドロップダウン（オプション）
<Select value={selectedModel || ""} onValueChange={setSelectedModel}>
  <SelectTrigger>
    <SelectValue placeholder="モデルを選択（任意）" />
  </SelectTrigger>
  <SelectContent>
    {availableModels.map(m => (
      <SelectItem key={m.id} value={m.modelName}>
        {m.displayName}
        {m.isRecommended && <Badge>推奨</Badge>}
      </SelectItem>
    ))}
  </SelectContent>
</Select>
```

### Phase 5: テストとドキュメント（2-3時間）

#### 5.1 単体テスト
- `ModelSelectionServiceTest.java` - 優先順位ロジックのテスト
- `MiraAdminControllerTest.java` - API エンドポイントのテスト

#### 5.2 統合テスト
- モデル選択フローの E2E テスト
- プロバイダ切り替えのテスト

#### 5.3 ドキュメント更新
- API ドキュメント（Swagger）更新
- ユーザーガイド作成

## 工数見積もり

| Phase | タスク | 工数 |
|---|---|---|
| Phase 1 | DB スキーマ・マイグレーション | 2-3h |
| Phase 2 | バックエンドロジック実装 | 4-5h |
| Phase 3 | API エンドポイント実装 | 3-4h |
| Phase 4 | フロントエンド実装 | 5-6h |
| Phase 5 | テスト・ドキュメント | 2-3h |
| **合計** | | **16-21h** |

## リスクと対策

### リスク1: 既存設定との互換性
**対策**: 
- 段階的な移行を実施
- 既存の `getAiModel()` を `@Deprecated` にし、警告ログを出力
- 一定期間は両方のロジックをサポート

### リスク2: モデル一覧の同期
**対策**:
- プロバイダ追加時にモデル一覧を自動生成する機能を検討
- 管理画面でモデルの有効/無効を簡単に切り替え可能に

### リスク3: パフォーマンス
**対策**:
- モデル一覧のキャッシュ（Redis または Spring Cache）
- DB インデックスの最適化

## 実装順序の推奨

1. **Phase 1**: DB スキーマ定義、CSV初期データ作成、Entity/Repository実装
2. **Phase 2**: `ModelSelectionService` 実装
3. **Phase 3**: 管理者 API 実装
4. **Phase 4**: フロントエンド（管理画面）
5. **Phase 3 続き**: ユーザー API 実装
6. **Phase 4 続き**: フロントエンド（チャット画面）
7. **Phase 5**: テストとドキュメント

## 次のアクション

- [ ] テーブル定義を既存システムに追加
- [ ] CSV初期データ作成（`data/system/` または `data/sample/`）
- [ ] Entity クラスと Repository 実装
- [ ] データロード機能で CSV を投入し動作確認
- [ ] `ModelSelectionService` のコア実装
- [ ] 管理者 API のプロトタイプ作成
- [ ] フロントエンド UI のモックアップ作成

## 参考

- [mira-ai-provider-issues.md](./mira-ai-provider-issues.md) - 既存の問題点
- [saas-llama-provider-investigation.md](./saas-llama-provider-investigation.md) - 調査結果
- Spring AI Documentation: https://docs.spring.io/spring-ai/reference/
