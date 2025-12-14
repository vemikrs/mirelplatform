# SaaS環境でLlamaプロバイダが選択される問題の調査

## 日付
2025-12-14

## 症状
- Dev環境: Gemini (Vertex AI) が正常に使用される
- SaaS環境: Llama 3.3 (GitHub Models) が使用され、Web検索がエラーになる

## ログ分析

### 起動ログ (SaaS環境)
```
2025-12-14 17:22:26.295 [main] INFO  j.v.m.a.m.i.ai.VertexAiGeminiClient - Initializing VertexAiGeminiClient with project: august-cascade-481002-a2, location: us-central1, model: gemini-2.5-flash
2025-12-14 17:22:26.376 [main] INFO  j.v.m.a.m.i.ai.GitHubModelsClient - Initializing GitHubModelsClient with model: meta/llama-3.3-70b-instruct
2025-12-14 17:22:26.699 [main] WARN  j.v.m.a.m.i.ai.AzureOpenAiClient - Azure OpenAI config is missing. Client will be disabled.
2025-12-14 17:22:26.700 [main] INFO  j.v.m.a.m.i.ai.AzureOpenAiClient - AzureOpenAiClient initialized with deployment: gpt-4o
2025-12-14 17:22:26.702 [main] INFO  j.v.m.a.m.i.ai.AiProviderFactory - AiProviderFactory initialized with providers: [mock, azure-openai, vertex-ai-gemini, github-models]
```

**所見**:
- Vertex AI (Gemini) は正常に初期化されている
- GitHub Models (Llama 3.3) も初期化されている
- 4つのプロバイダがすべて登録されている

### チャットリクエストログ
```
2025-12-14 17:23:29.467 [http-nio-3000-exec-16] DEBUG o.s.web.servlet.DispatcherServlet - POST "/mipla2/apps/mira/api/stream/chat", parameters={}
2025-12-14 17:23:29.484 [http-nio-3000-exec-16] INFO  j.v.m.a.m.d.s.MiraStreamService - StreamChat called. ConversationID: 28f359fc-0f03-4d72-be65-2097a6490009, Mode: GENERAL_CHAT
```

**問題**:
- **プロバイダ選択のログが出力されていない**
- どのプロバイダが選択されたか明示的なログがない

## プロバイダ選択フロー

### コードパス
1. `MiraStreamController.chatStream()` → `MiraStreamService.streamChat()`
2. `MiraStreamService.executeStreamLoop()` 内で:
   ```java
   AiProviderClient client = aiProviderFactory.createClient(tenantId);
   ```
3. `AiProviderFactory.createClient(tenantId)`:
   ```java
   public AiProviderClient createClient(String tenantId) {
       String providerName = settingService.getAiProvider(tenantId);
       
       return getProvider(providerName)
               .orElseGet(() -> {
                   log.warn("Requested provider '{}' for tenant '{}' is not available. Falling back to default.",
                           providerName, tenantId);
                   return getProvider("github-models")
                           .orElseGet(() -> getProvider("mock")
                                   .orElseThrow(() -> new IllegalStateException(
                                           "No AI provider available. Requested: " + providerName)));
               });
   }
   ```
4. `MiraSettingService.getAiProvider(tenantId)`:
   ```java
   public String getAiProvider(String tenantId) {
       return getString(tenantId, KEY_AI_PROVIDER, miraAiProperties.getProvider());
   }
   ```

### 設定優先順位
1. **最優先**: テナント設定 (`mira_tenant_setting` テーブル)
2. **次点**: システム設定 (`mira_system_setting` テーブル)
3. **デフォルト**: `application.yml` の `mira.ai.provider`

## 設定ファイルの確認

### application.yml (ベース)
```yaml
mira:
  ai:
    provider: ${MIRA_AI_PROVIDER:mock}
```
デフォルト: `mock`

### application-dev.yml
```yaml
mira:
  ai:
    provider: ${MIRA_AI_PROVIDER:github-models}
```
デフォルト: `github-models`（Llama 3.3）

### application-it.yml
存在しない（ユーザーが提示したが、実際にはリポジトリに存在しない）

## 仮説

### 仮説1: SaaS環境で `dev` プロファイルが使用されている
- SaaS環境が `dev` プロファイルで起動している
- `application-dev.yml` のデフォルト `github-models` が適用されている
- 環境変数 `MIRA_AI_PROVIDER` が設定されていない

**確認方法**:
```bash
# SaaS環境のプロファイル確認
docker logs <container> | grep "spring.profiles.active"
# または
docker exec <container> env | grep SPRING_PROFILES_ACTIVE
```

### 仮説2: テナント設定で `github-models` が設定されている
- テナント `default` の設定テーブルに `ai.provider=github-models` が保存されている
- これがプロパティファイルの設定を上書きしている

**確認方法**:
```sql
-- テナント設定確認
SELECT * FROM mira_tenant_setting WHERE tenant_id = 'default' AND key = 'ai.provider';

-- システム設定確認
SELECT * FROM mira_system_setting WHERE key = 'ai.provider';
```

### 仮説3: 環境変数 `MIRA_AI_PROVIDER=github-models` が設定されている
- Docker Compose や環境変数で明示的に設定されている

**確認方法**:
```bash
docker exec <container> env | grep MIRA_AI_PROVIDER
```

## 推奨される対応

### 即座の対応（暫定措置）
1. テナント設定を確認し、`vertex-ai-gemini` に変更:
   ```sql
   INSERT INTO mira_tenant_setting (tenant_id, key, value, updated_at) 
   VALUES ('default', 'ai.provider', 'vertex-ai-gemini', CURRENT_TIMESTAMP)
   ON CONFLICT (tenant_id, key) 
   DO UPDATE SET value = 'vertex-ai-gemini', updated_at = CURRENT_TIMESTAMP;
   ```

2. または、環境変数で設定:
   ```bash
   export MIRA_AI_PROVIDER=vertex-ai-gemini
   ```

### 恒久的な対応
1. **ログ改善**: プロバイダ選択時に必ずログ出力
   ```java
   AiProviderClient client = aiProviderFactory.createClient(tenantId);
   log.info("Selected AI Provider: {} for tenant: {}", client.getProviderName(), tenantId);
   ```

2. **設定の明示化**: SaaS環境用のプロファイル作成
   - `application-prod.yml` または `application-saas.yml` を作成
   - デフォルトプロバイダを `vertex-ai-gemini` に設定

3. **管理画面の改善**: 
   - UI にプロバイダ選択肢として Vertex AI を追加（ドキュメント #57/mira-ai-provider-issues.md 参照）

## 確認結果（2025-12-14）

```bash
# 1. プロファイル確認
$ docker exec mirelplatform-it-app env | grep SPRING_PROFILES_ACTIVE
SPRING_PROFILES_ACTIVE=it

# 2. 環境変数確認
$ docker exec mirelplatform-it-app env | grep MIRA_AI_PROVIDER
MIRA_AI_PROVIDER=vertex-ai-gemini  # ✅ 正しく設定されている

# 3. DB設定確認
テーブル確認: レコード 0 件（システム・テナント設定ともになし）
```

**結論**: すべての仮説が外れ。設定は正しいのに Llama が使われる原因は別にあった。

## 真の原因

**`MiraSettingService.getAiModel()` の実装不備**

[MiraSettingService.java#L129-L137](../../backend/src/main/java/jp/vemi/mirel/apps/mira/domain/service/MiraSettingService.java#L129-L137)

```java
public String getAiModel(String tenantId) {
    String defaultModel = miraAiProperties.getGithubModels().getModel(); // ← 常にGitHub Models
    if ("azure-openai".equals(getAiProvider(tenantId))) {
        defaultModel = miraAiProperties.getAzureOpenai().getDeploymentName();
    }
    return getString(tenantId, KEY_AI_MODEL, defaultModel);
}
```

**問題点**:
- `vertex-ai-gemini` プロバイダの分岐が存在しない
- プロバイダが `vertex-ai-gemini` でも、GitHub Models のモデル名 `meta/llama-3.3-70b-instruct` が返される
- `ModelCapabilityValidator` がモデル名をチェックし、Llama はツール呼び出し非対応と判定

**実際の挙動**:
1. ✅ プロバイダ選択: `vertex-ai-gemini` が正しく選択される
2. ❌ モデル名取得: `meta/llama-3.3-70b-instruct` (GitHub Models) が返される
3. ❌ 機能チェック: Llama 3.3 はツール呼び出し非対応 → Web検索エラー

## 実施した修正

### 1. `MiraSettingService.getAiModel()` の修正
プロバイダごとのデフォルトモデルを正しく取得するように修正。

```java
public String getAiModel(String tenantId) {
    String provider = getAiProvider(tenantId);
    String defaultModel;
    
    switch (provider) {
        case MiraAiProperties.PROVIDER_VERTEX_AI_GEMINI:
            defaultModel = miraAiProperties.getVertexAi().getModel();
            break;
        case MiraAiProperties.PROVIDER_AZURE_OPENAI:
            defaultModel = miraAiProperties.getAzureOpenai().getDeploymentName();
            break;
        case MiraAiProperties.PROVIDER_GITHUB_MODELS:
            defaultModel = miraAiProperties.getGithubModels().getModel();
            break;
        default:
            log.warn("Unknown provider: {}, falling back to github-models default", provider);
            defaultModel = miraAiProperties.getGithubModels().getModel();
    }
    
    return getString(tenantId, KEY_AI_MODEL, defaultModel);
}
```

### 2. `MiraSettingService.getAiTemperature()` / `getAiMaxTokens()` も同様に修正
各プロバイダごとの設定値を正しく取得するように修正。

### 3. `AiProviderFactory.createClient()` にログ追加
プロバイダ選択時に必ずログ出力。

```java
log.info("Selecting AI provider: '{}' for tenant: '{}'", providerName, tenantId);
```

## 検証方法

修正後、以下を確認：

```bash
# 1. 再ビルド・再起動
docker-compose down
docker-compose up -d --build

# 2. ログ確認
docker logs mirelplatform-it-app | grep "Selecting AI provider"
# 期待: "Selecting AI provider: 'vertex-ai-gemini' for tenant: 'default'"

# 3. Web検索テスト
# Mira AI で Web検索を有効化してメッセージ送信
# 期待: エラーが出ず、Gemini が使用される
```

## 次のアクション
1. [x] 真の原因を特定
2. [x] コード修正を実施
3. [ ] 修正をテスト（SaaS環境で検証）
4. [ ] ドキュメント [mira-ai-provider-issues.md](./mira-ai-provider-issues.md) の対応を別途実施
