## コンテキストインライン編集機能 - 実装方式検討

既存のMira実装を調査しました。以下、実装方式を検討します。

---

## 1. 既存実装との整合性

### 1.1 既存のコンテキスト管理機能

**Backend側**
- `MiraContextLayerService`: 階層的コンテキスト（System/Tenant/Organization/User）を管理
- `MiraContextLayer` エンティティ: カテゴリ別（terminology, style, workflow等）のコンテキスト永続化
- 優先度マージ機能: スコープ順（System < Tenant < Org < User）+ priority属性で柔軟な制御

**Frontend側**
- `MiraUserContextEditor`: ユーザーコンテキストの編集UI（terminology/style/workflow）
- `miraStore`: 会話（conversation）とメッセージ（message）を管理
- `ChatRequest.Context`: リクエスト時のコンテキスト送信構造

---

## 2. 実装方式の提案

### 2.1 アーキテクチャ概要

```
┌─────────────────────────────────────────────────────┐
│  MiraChatInput (メッセージ入力)                        │
│  └─ [Ctrl+Shift+M] → ContextSwitcherModal 起動      │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  ContextSwitcherModal (送信前コンテキスト編集)         │
│  ┌───────────────────────────────────────────────┐  │
│  │ A. AI推奨プリセット (LLMサジェスト)              │  │
│  │ B. 履歴スコープ (全履歴/直近N件/なし)            │  │
│  │ C. コンテキストON/OFF・優先度                   │  │
│  │ D. 追加プリセット                              │  │
│  │ E. 一時カスタムコンテキスト                     │  │
│  │ F. コマンドターミナル (Optional)               │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                      ↓
         [この設定で送信] → API呼び出し
```

### 2.2 データフロー

#### **送信時**

```typescript
// 1. ユーザーがモーダルで設定
interface MessageSubmitConfig {
  historyScope: 'auto' | 'recent' | 'none';
  recentCount?: number; // historyScope='recent'時
  contextOverrides: {
    [category: string]: {
      enabled: boolean;
      priority: 'normal' | 'high' | 'low';
    };
  };
  additionalPresets: string[]; // プリセットID配列
  temporaryContext?: string; // ワンショット追加指示
}

// 2. ChatRequest拡張
interface ChatRequest {
  conversationId?: string;
  mode: string;
  context: {
    // 既存フィールド
    snapshotId?: string;
    appId?: string;
    screenId?: string;
    // 新規フィールド
    messageConfig?: {
      historyScope: 'auto' | 'recent' | 'none';
      recentCount?: number;
      contextOverrides?: Record<string, { enabled: boolean; priority: number }>;
      additionalPresets?: string[];
      temporaryContext?: string;
    };
  };
  message: { content: string; contentType: string };
}
```

#### **Backend処理**

```java
// MiraChatService内での処理フロー
1. リクエストからmessageConfigを取得
2. historyScope に応じて会話履歴をフィルタリング
   - 'auto': 既存のトークン制限内で自動圧縮
   - 'recent': recentCount件のみ使用
   - 'none': 履歴なし（スタンドアロンプロンプト）
3. contextOverridesを元にMiraContextLayerServiceから取得したコンテキストを調整
   - enabled=false → 除外
   - priority → マージ順序に反映
4. additionalPresetsをコンテキストに追加マージ
5. temporaryContextを最後にマージ（最優先）
6. LLMにプロンプト送信
```

---

## 3. 実装コンポーネント設計

### 3.1 Frontend

#### **新規コンポーネント**

**`ContextSwitcherModal.tsx`**
```typescript
interface ContextSwitcherModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentMessage: string; // AI推奨に使用
  onSubmit: (config: MessageSubmitConfig, message: string) => void;
}

// 内部タブ構成
// - 履歴スコープタブ
// - コンテキスト制御タブ (MiraUserContextEditorを流用)
// - プリセット選択タブ
// - 一時追加タブ
// - コマンドターミナル（オプション）
```

**`AiPresetSuggestion.tsx`**
```typescript
// AI推奨プリセットカード
interface AiPresetSuggestionProps {
  messageContent: string;
  onApply: (suggestion: MessageSubmitConfig) => void;
}

// バックエンドAPI: POST /apps/mira/api/suggest-config
// リクエスト: { message: string }
// レスポンス: { recommendedConfig: MessageSubmitConfig }
```

#### **既存コンポーネント拡張**

**`MiraChatInput.tsx`**
- ショートカット `Ctrl+Shift+M` でContextSwitcherModalを開く
- アイコンボタンでも起動可能
- モーダルからsubmit時は設定込みでAPI呼び出し

**miraStore.ts**
```typescript
interface MiraConversation {
  // 既存フィールド
  id: string;
  mode: MiraMode;
  title?: string;
  messages: MiraMessage[];
  context?: ChatContext;
  
  // 新規: メッセージごとの設定記録（監査用）
  messageConfigs?: Record<string, MessageSubmitConfig>;
}
```

### 3.2 Backend

#### **新規DTO**

**ChatRequest.java 拡張**
```java
public class ChatRequest {
    // 既存フィールド省略
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Context {
        // 既存フィールド
        private String snapshotId;
        private String appId;
        private String screenId;
        
        // 新規
        private MessageConfig messageConfig;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageConfig {
        @Builder.Default
        private String historyScope = "auto"; // auto, recent, none
        private Integer recentCount;
        private Map<String, ContextOverride> contextOverrides;
        private List<String> additionalPresets;
        private String temporaryContext;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContextOverride {
        @Builder.Default
        private Boolean enabled = true;
        @Builder.Default
        private Integer priority = 0; // 0=normal, 1=high, -1=low
    }
}
```

#### **新規サービス**

**`MiraContextMergeService.java`**
```java
@Service
public class MiraContextMergeService {
    
    /**
     * messageConfigに基づいてコンテキストをマージ
     */
    public String buildFinalContext(
        String tenantId,
        String orgId,
        String userId,
        MessageConfig messageConfig
    ) {
        // 1. 基本コンテキスト取得
        Map<String, String> baseContext = 
            contextLayerService.buildMergedContext(tenantId, orgId, userId);
        
        // 2. contextOverridesを適用
        Map<String, String> filteredContext = applyOverrides(
            baseContext, messageConfig.getContextOverrides());
        
        // 3. additionalPresetsを追加
        String withPresets = mergePresets(
            filteredContext, messageConfig.getAdditionalPresets());
        
        // 4. temporaryContextを最優先で追加
        return appendTemporaryContext(
            withPresets, messageConfig.getTemporaryContext());
    }
    
    /**
     * historyScope に応じてメッセージ履歴をフィルタリング
     */
    public List<Message> filterHistory(
        List<Message> fullHistory,
        MessageConfig messageConfig
    ) {
        String scope = messageConfig.getHistoryScope();
        if ("none".equals(scope)) {
            return Collections.emptyList();
        }
        if ("recent".equals(scope)) {
            int count = messageConfig.getRecentCount() != null 
                ? messageConfig.getRecentCount() : 5;
            return fullHistory.subList(
                Math.max(0, fullHistory.size() - count), 
                fullHistory.size()
            );
        }
        return fullHistory; // "auto"
    }
}
```

**`MiraPresetSuggestionService.java`** (AI推奨)
```java
@Service
public class MiraPresetSuggestionService {
    
    /**
     * メッセージ内容からAI推奨設定を生成
     */
    public MessageConfig suggestConfig(String messageContent) {
        // 軽量LLM（GPT-4o-miniなど）でヒューリスティック判定
        // または、ルールベースロジック
        
        // 例: "詳細レビュー" キーワード → review-strict プリセット推奨
        //     "簡潔に" → 超簡潔モード推奨
        
        return MessageConfig.builder()
            .historyScope("recent")
            .recentCount(5)
            .additionalPresets(List.of("review-strict"))
            .build();
    }
}
```

#### **API追加**

**MiraApiController.java**
```java
@PostMapping("/suggest-config")
@Operation(summary = "AI推奨設定を取得")
public ResponseEntity<ApiResponse<MessageConfig>> suggestConfig(
    @RequestBody ApiRequest<SuggestConfigRequest> request
) {
    String messageContent = request.getModel().getMessageContent();
    MessageConfig suggestion = presetSuggestionService.suggestConfig(messageContent);
    
    return ResponseEntity.ok(ApiResponse.<MessageConfig>builder()
        .data(suggestion)
        .build());
}
```

---

## 4. UI/UXフロー

### 4.1 モーダル起動

1. ユーザーが `MiraChatInput` でメッセージ入力
2. `Ctrl+Shift+M` または設定アイコンクリック
3. `ContextSwitcherModal` 表示

### 4.2 モーダル内UI構成

```
┌─────────────────────────────────────────────────┐
│ このメッセージの送信設定                           │
├─────────────────────────────────────────────────┤
│ 【A】 AI推奨プリセット (カード表示)                 │
│   ┌─────────────────────────────────────────┐   │
│   │ おすすめ設定があります                      │   │
│   │ ・履歴スコープ: 直近5件                    │   │
│   │ ・追加コンテキスト: レビュー強化モード       │   │
│   │ [このおすすめを適用]                       │   │
│   └─────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│ 【B】 履歴スコープ                                │
│   ( ) 通常（自動圧縮＋全履歴）                   │
│   (•) 直近 [5▼] 件のみ                         │
│   ( ) 履歴を送らない                            │
├─────────────────────────────────────────────────┤
│ 【C】 コンテキスト管理 (Tabs)                     │
│   [専門用語] [スタイル] [ワークフロー]            │
│   ┌─────────────────────────────────────────┐   │
│   │ ☑ 有効    優先度: [通常▼]                  │   │
│   │ ☐ セーフティポリシー (編集不可)             │   │
│   └─────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│ 【D】 追加プリセット                              │
│   ☐ 詳細レビュー強化モード                       │
│   ☐ 仕様書フォーマット強制モード                  │
│   ☐ 超簡潔モード                                │
├─────────────────────────────────────────────────┤
│ 【E】 一時カスタムコンテキスト                     │
│   ┌─────────────────────────────────────────┐   │
│   │ 今回だけ、敬語＋箇条書きで                  │   │
│   └─────────────────────────────────────────┘   │
├─────────────────────────────────────────────────┤
│ 【F】 コマンドターミナル (任意)                    │
│   > ctx use review-strict                       │
└─────────────────────────────────────────────────┘
       [この設定で送信]   [キャンセル]
```

### 4.3 コマンドターミナル (Optional)

**対応コマンド例**
```
> history 5          // 履歴スコープを直近5件に
> history none       // 履歴なし
> ctx off style      // スタイル設定を無効化
> ctx high terminology // terminology優先度を高に
> preset review-strict  // プリセット追加
> apply ai           // AI推奨を一括適用
```

---

## 5. 技術的考慮点

### 5.1 データ整合性

- **会話履歴の一貫性**: 履歴スコープを変更しても、サーバー側の会話履歴は保持（フィルタリングは送信時のみ）
- **監査ログ**: `messageConfigs` に設定記録を保存し、トレーサビリティ確保

### 5.2 パフォーマンス

- **AI推奨API**: 軽量モデル使用でレイテンシ100ms以下目標
- **プリセットキャッシュ**: Tenant/User単位でプリセット定義をキャッシュ

### 5.3 セキュリティ

- **セーフティコンテキスト**: System scopeで固定、ユーザーは無効化不可
- **一時コンテキスト**: サニタイズ処理を実施（プロンプトインジェクション対策）

---

## 6. 実装優先度

### Phase 1 (MVP)
- [ ] `ContextSwitcherModal` 基本UI（B,C,E）
- [ ] `ChatRequest.MessageConfig` DTO拡張
- [ ] `MiraContextMergeService` 実装
- [ ] 履歴スコープフィルタリング

### Phase 2
- [ ] AI推奨プリセット（A）
- [ ] プリセット選択機能（D）
- [ ] 監査ログ保存

### Phase 3
- [ ] コマンドターミナル（F）
- [ ] キーボードショートカット拡張
- [ ] チーム共有機能

--- 

Completed: *実装方式の文書化* (5/5)

## 7. まとめ

既存のMira実装（`MiraContextLayerService`、`miraStore`、`MiraUserContextEditor`）を最大限活用しながら、以下の方針で「コンテキストインライン編集機能」を実装できます。

### ✅ 既存機能との統合ポイント

1. **階層コンテキスト**: `MiraContextLayerService` のスコープ・優先度機能を活用
2. **Zustand Store**: `miraStore` に `messageConfigs` フィールドを追加して監査対応
3. **UI再利用**: `MiraUserContextEditor` のタブ構造を `ContextSwitcherModal` 内に組み込み

### ✅ 主要な新規実装

**Frontend**
- `ContextSwitcherModal.tsx`: メッセージ送信前の統合設定UI
- `AiPresetSuggestion.tsx`: AI推奨プリセットカード
- `MiraChatInput` へのショートカット `Ctrl+Shift+M` 追加

**Backend**
- `ChatRequest.MessageConfig`: 送信時設定を表現するDTO
- `MiraContextMergeService`: messageConfigに基づくコンテキストマージロジック
- `MiraPresetSuggestionService`: AI推奨設定生成サービス
- `/apps/mira/api/suggest-config` エンドポイント追加

### ✅ 実装方針の妥当性

- **モノレポ規約準拠**: `/mapi/*` 経由のAPI呼び出し、Radix UIベースのコンポーネント設計
- **Mira既存設計尊重**: コンテキストレイヤー、会話管理、メッセージ構造を拡張
- **段階的実装**: Phase 1(基本UI)→Phase 2(AI推奨)→Phase 3(コマンドターミナル)
