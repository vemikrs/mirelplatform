# Mira AI プロバイダ/モデル選択の実装課題

## 日付
2025-12-14

## 概要
Mira AI 管理画面におけるプロバイダ・モデル選択機能の実装に複数の不備が存在することが判明。バックエンドとフロントエンドの整合性を含めた改修が必要。

## 特定された問題点

### 1. プロバイダ一覧がフロントエンドにハードコード
**場所**: [apps/frontend-v3/src/features/admin/pages/MiraAdminPage.tsx#L223-L227](../../apps/frontend-v3/src/features/admin/pages/MiraAdminPage.tsx#L223-L227)

```tsx
options={[
    { value: "github-models", label: "GitHub Models" },
    { value: "azure-openai", label: "Azure OpenAI" },
    { value: "mock", label: "Mock Provider" }
]}
```

**問題**:
- Vertex AI (Gemini) が選択肢に含まれていない
- バックエンドの `AiProviderFactory.getAvailableProviders()` メソッドを利用していない
- 新しいプロバイダを追加する際にフロントエンドも変更が必要

**影響**:
- ユーザーが Vertex AI (Gemini) を選択できない
- 実装済みのプロバイダと UI の選択肢が乖離

### 2. モデル一覧もフロントエンドにハードコード
**場所**: [apps/frontend-v3/src/features/admin/pages/MiraAdminPage.tsx#L233-L238](../../apps/frontend-v3/src/features/admin/pages/MiraAdminPage.tsx#L233-L238)

```tsx
options={[
    { value: "gpt-4o", label: "GPT-4o" },
    { value: "gpt-4o-mini", label: "GPT-4o Mini" },
    { value: "o1-preview", label: "o1 Preview" }, 
    { value: "o1-mini", label: "o1 Mini" },
    { value: "Phi-3.5-mini-instruct", label: "Phi 3.5 Mini" }
]}
```

**問題**:
- 選択されたプロバイダに応じた利用可能モデル一覧を動的に取得していない
- Gemini モデル（gemini-2.5-flash 等）や Llama モデルが選択肢にない
- プロバイダごとのモデル命名規則（OpenAI: `gpt-4o`, GitHub Models: `openai/gpt-4o`, Vertex AI: `gemini-2.5-flash`）が考慮されていない

**影響**:
- ユーザーが適切なモデルを選択できない
- カスタム入力に依存せざるを得ない

### 3. バックエンドAPIの不足
**不足しているエンドポイント**:

#### 3.1 プロバイダ一覧取得API
```
GET /apps/mira/api/admin/providers
```
**期待されるレスポンス**:
```json
{
  "data": [
    {
      "name": "vertex-ai-gemini",
      "displayName": "Vertex AI (Gemini)",
      "available": true,
      "configured": true
    },
    {
      "name": "github-models",
      "displayName": "GitHub Models",
      "available": true,
      "configured": true
    },
    {
      "name": "azure-openai",
      "displayName": "Azure OpenAI",
      "available": false,
      "configured": false,
      "reason": "API key not configured"
    },
    {
      "name": "mock",
      "displayName": "Mock Provider",
      "available": true,
      "configured": true
    }
  ]
}
```

#### 3.2 モデル一覧取得API
```
GET /apps/mira/api/admin/models?provider=vertex-ai-gemini
```
**期待されるレスポンス**:
```json
{
  "data": {
    "provider": "vertex-ai-gemini",
    "models": [
      {
        "id": "gemini-2.5-flash",
        "displayName": "Gemini 2.5 Flash",
        "capabilities": ["TOOL_CALLING", "MULTIMODAL_INPUT", "STREAMING", "LONG_CONTEXT"],
        "recommended": true
      },
      {
        "id": "gemini-2.0-flash-exp",
        "displayName": "Gemini 2.0 Flash (Experimental)",
        "capabilities": ["TOOL_CALLING", "MULTIMODAL_INPUT", "STREAMING", "LONG_CONTEXT"],
        "experimental": true
      }
    ]
  }
}
```

### 4. MiraSettingService のモデル名取得ロジックの不備
**場所**: [backend/src/main/java/jp/vemi/mirel/apps/mira/domain/service/MiraSettingService.java#L129-L137](../../backend/src/main/java/jp/vemi/mirel/apps/mira/domain/service/MiraSettingService.java#L129-L137)

```java
public String getAiModel(String tenantId) {
    String defaultModel = miraAiProperties.getGithubModels().getModel();
    if ("azure-openai".equals(getAiProvider(tenantId))) {
        defaultModel = miraAiProperties.getAzureOpenai().getDeploymentName();
    }
    return getString(tenantId, KEY_AI_MODEL, defaultModel);
}
```

**問題**:
- `vertex-ai-gemini` プロバイダの分岐が存在しない
- プロバイダが `vertex-ai-gemini` の場合でも GitHub Models のモデル名が返される
- プロバイダ追加時に毎回このメソッドを修正する必要がある

**修正案**:
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

## 改修計画

### Phase 1: バックエンドAPI実装
1. `MiraAdminController` にプロバイダ一覧取得エンドポイントを追加
2. `MiraAdminController` にモデル一覧取得エンドポイントを追加
3. `ModelCapabilityRegistry` を活用してモデル機能情報を提供
4. `MiraSettingService.getAiModel()` を修正

### Phase 2: フロントエンド改修
1. プロバイダ一覧を API から取得するように変更
2. プロバイダ選択時にモデル一覧を動的に取得
3. モデルの機能（ツール呼び出し対応等）を表示

### Phase 3: 統合テスト
1. 各プロバイダの選択・保存が正常に動作することを確認
2. モデル選択が適切にバックエンドに反映されることを確認
3. E2E テストの追加

## 補足: 現在のプロバイダ実装状況

| プロバイダ名 | 実装クラス | デフォルトモデル | ツール呼び出し対応 |
|---|---|---|---|
| `vertex-ai-gemini` | `VertexAiGeminiClient` | `gemini-2.5-flash` | ✅ |
| `github-models` | `GitHubModelsClient` | `meta/llama-3.3-70b-instruct` | モデル依存 |
| `azure-openai` | `AzureOpenAiClient` | `gpt-4o` | ✅ |
| `mock` | `MockAiProviderClient` | N/A | ✅ |

## 関連Issue
- #57: ProMarker v3 フロントエンド移行
- （今後作成予定）Mira AI 管理画面改善

## 備考
この問題は機能追加の過程で段階的に発生したもので、初期実装時のプロバイダが限定的だったため顕在化しなかった。Vertex AI (Gemini) の追加により、設計の不備が明らかになった。
