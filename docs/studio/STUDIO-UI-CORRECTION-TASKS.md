# mirel Studio UI/UX 軌道修正 - 詳細作業計画

> **Version**: 1.0.0  
> **Status**: Draft  
> **Created**: 2025-12-05  
> **Parent**: [STUDIO-UI-CORRECTION-PLAN.md](./STUDIO-UI-CORRECTION-PLAN.md)

---

## 📋 Phase 1: レイアウト統一 + 文脈表示（2週間）

### Week 1: 基盤コンポーネント作成

#### Task 1.1: StudioLayout コンポーネント作成
**優先度**: 🔴 Critical  
**見積り**: 4h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/layouts/StudioLayout.tsx`

**作業内容**:
- [ ] `StudioLayout` コンポーネント新規作成
- [ ] 3ペイン構造の実装（Nav / Main / Property）
- [ ] レスポンシブ対応（モバイルでは折りたたみ）
- [ ] `children` / `explorer` / `properties` props 設計

**テスト**:
- [ ] レイアウト崩れがないこと（各ブレークポイント）
- [ ] ペイン幅のリサイズ動作

```tsx
// 想定インターフェース
interface StudioLayoutProps {
  children: React.ReactNode;           // Main Area
  navigation?: React.ReactNode;        // Left Nav (デフォルトあり)
  explorer?: React.ReactNode;          // 左側探索パネル（任意）
  properties?: React.ReactNode;        // Right Property Panel
  hideNavigation?: boolean;
  hideProperties?: boolean;
}
```

---

#### Task 1.2: StudioHeader コンポーネント作成
**優先度**: 🔴 Critical  
**見積り**: 3h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/StudioHeader.tsx`

**作業内容**:
- [ ] Workspace 名表示
- [ ] Draft バージョン / 保存状態表示
- [ ] 環境バッジ（Dev/Stg/Prod）
- [ ] ユーザーメニュー連携
- [ ] グローバル検索（Studio 内）

**テスト**:
- [ ] Workspace 名が正しく表示されること
- [ ] 保存状態のリアルタイム更新

---

#### Task 1.3: StudioContextBar コンポーネント作成
**優先度**: 🟡 High  
**見積り**: 2h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/StudioContextBar.tsx`

**作業内容**:
- [ ] パンくずナビゲーション
- [ ] 現在の編集対象表示
- [ ] クイックアクション（保存/プレビュー等）

---

#### Task 1.4: StudioContextProvider 作成
**優先度**: 🔴 Critical  
**見積り**: 3h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/contexts/StudioContext.tsx`

**作業内容**:
- [ ] `StudioContext` 定義（Workspace / Draft / Environment）
- [ ] `StudioContextProvider` 実装
- [ ] `useStudioContext` フック
- [ ] 永続化（localStorage / URL パラメータ）

```tsx
// 想定コンテキスト
interface StudioContextValue {
  workspace: {
    id: string;
    name: string;
  } | null;
  draft: {
    version: number;
    status: 'saved' | 'unsaved' | 'saving';
    lastSaved?: Date;
  };
  environment: 'dev' | 'stg' | 'prod';
  breadcrumbs: Array<{ label: string; path: string }>;
  setBreadcrumbs: (items: Array<{ label: string; path: string }>) => void;
  setDraftStatus: (status: 'saved' | 'unsaved' | 'saving') => void;
}
```

---

#### Task 1.5: StudioNavigation コンポーネント作成
**優先度**: 🟡 High  
**見積り**: 4h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/StudioNavigation.tsx`

**作業内容**:
- [ ] 統一されたナビゲーション構造
- [ ] アイコン + ラベル表示
- [ ] 展開/折りたたみ状態管理
- [ ] アクティブ状態のハイライト
- [ ] Home / Modeler / Form / Flow / Data / Release の6セクション

---

#### Task 1.6: StudioPropertyPanel コンポーネント作成
**優先度**: 🟡 High  
**見積り**: 2h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/StudioPropertyPanel.tsx`

**作業内容**:
- [ ] 汎用プロパティパネルコンテナ
- [ ] タブ切り替え機能
- [ ] 折りたたみセクション
- [ ] 「選択なし」状態の表示

---

### Week 2: 既存画面の移行

#### Task 1.7: StudioPage への StudioLayout 適用
**優先度**: 🔴 Critical  
**見積り**: 4h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/pages/StudioPage.tsx`

**作業内容**:
- [ ] `StudioLayout` でラップ
- [ ] 既存ツールバーを `StudioContextBar` に統合
- [ ] FormDesigner を MainArea に配置
- [ ] Widget プロパティを PropertyPanel に移動
- [ ] モード切り替え（Form/Flow/Preview）の維持

**テスト**:
- [ ] 既存機能が動作すること
- [ ] E2E テストのパス

---

#### Task 1.8: ModelerHomePage への StudioLayout 適用
**優先度**: 🟡 High  
**見積り**: 2h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/modeler/pages/ModelerHomePage.tsx`

**作業内容**:
- [ ] `ModelerLayout` → `StudioLayout` 置換
- [ ] ナビゲーションを `StudioNavigation` に統合
- [ ] 3カード構造の維持（Dashboard として）

---

#### Task 1.9: ModelerModelDefinePage への StudioLayout 適用
**優先度**: 🟡 High  
**見積り**: 3h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/modeler/pages/ModelerModelDefinePage.tsx`

**作業内容**:
- [ ] `StudioLayout` 適用
- [ ] モデル一覧を Explorer に配置
- [ ] フィールド編集を PropertyPanel に配置

---

#### Task 1.10: ModelerRecordListPage / ModelerRecordDetailPage の移行
**優先度**: 🟢 Medium  
**見積り**: 3h  
**担当ファイル**: 
- `apps/frontend-v3/src/features/studio/modeler/pages/ModelerRecordListPage.tsx`
- `apps/frontend-v3/src/features/studio/modeler/pages/ModelerRecordDetailPage.tsx`

---

#### Task 1.11: ModelerCodeMasterPage の移行
**優先度**: 🟢 Medium  
**見積り**: 2h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/modeler/pages/ModelerCodeMasterPage.tsx`

---

#### Task 1.12: ModelerLayout / ModelerSidebar の非推奨化
**優先度**: 🟢 Medium  
**見積り**: 1h

**作業内容**:
- [ ] `@deprecated` コメント追加
- [ ] 移行完了後に削除（Phase 2）

---

### Week 1-2: Phase 1 完了チェック

| チェック項目 | 状態 |
|-------------|------|
| StudioLayout が全 Studio 画面で使用されている | ☐ |
| Workspace / Draft / Environment が常時表示されている | ☐ |
| パンくずが正しく表示されている | ☐ |
| 既存機能に regression がない | ☐ |
| E2E テストがパスする | ☐ |

---

## 📋 Phase 2: IA 再編成 + ルート整理（2週間）

### Week 3: ルート構造変更

#### Task 2.1: 新規ルート構造の定義
**優先度**: 🔴 Critical  
**見積り**: 2h  
**担当ファイル**: `apps/frontend-v3/src/app/router.config.tsx`

**作業内容**:
- [ ] 新 IA に基づくルート定義
- [ ] 旧ルートからのリダイレクト設定

```tsx
// 新ルート構造
{
  path: 'apps/studio',
  element: <StudioLayout />,
  children: [
    { index: true, element: <StudioHomePage /> },
    {
      path: 'modeler',
      children: [
        { path: 'entities', element: <EntityListPage /> },
        { path: 'entities/:entityId', element: <EntityEditPage /> },
        { path: 'relations', element: <RelationViewPage /> },
        { path: 'codes', element: <CodeSystemPage /> },
      ],
    },
    {
      path: 'forms',
      children: [
        { index: true, element: <FormListPage /> },
        { path: ':formId', element: <FormDesignerPage /> },
        { path: 'layouts', element: <LayoutSettingsPage /> },
      ],
    },
    // ... 以下同様
  ],
}
```

---

#### Task 2.2: 新規ページコンポーネント作成

**見積り**: 8h（計）

| ページ | ファイル | 見積り |
|--------|----------|--------|
| EntityListPage | `modeler/pages/EntityListPage.tsx` | 2h |
| EntityEditPage | `modeler/pages/EntityEditPage.tsx` | 3h |
| RelationViewPage | `modeler/pages/RelationViewPage.tsx` | 2h |
| FormListPage | `forms/pages/FormListPage.tsx` | 1h |

---

#### Task 2.3: 旧ページのリファクタリング

**見積り**: 4h

- [ ] `ModelerModelDefinePage` → `EntityListPage` / `EntityEditPage` に分割
- [ ] `StudioPage` → `FormDesignerPage` に改名・整理
- [ ] `ModelerRecordListPage` → `DataBrowserPage` に再配置

---

### Week 4: ナビゲーション統一

#### Task 2.4: navigation 設定の分離
**優先度**: 🟡 High  
**見積り**: 2h

**作業内容**:
- [ ] `studio-navigation.json` 作成
- [ ] Studio 専用ナビゲーション設定
- [ ] 動的読み込み対応

---

#### Task 2.5: StudioNavigation の完成
**優先度**: 🟡 High  
**見積り**: 3h

**作業内容**:
- [ ] 新 IA に基づくナビゲーション項目
- [ ] バッジ表示（Draft 数、未読通知等）
- [ ] キーボードナビゲーション

---

#### Task 2.6: ModelerSidebar の完全廃止
**優先度**: 🟢 Medium  
**見積り**: 1h

**作業内容**:
- [ ] 全参照を `StudioNavigation` に置換
- [ ] ファイル削除
- [ ] 関連 import の整理

---

#### Task 2.7: E2E テストの更新
**優先度**: 🔴 Critical  
**見積り**: 4h  
**担当ファイル**: `packages/e2e/tests/specs/studio/**`

**作業内容**:
- [ ] 新ルートへのパス更新
- [ ] セレクタの更新
- [ ] 新規画面のテスト追加

---

### Week 3-4: Phase 2 完了チェック

| チェック項目 | 状態 |
|-------------|------|
| 新 IA に基づくルート構造が実装されている | ☐ |
| 旧ルートからリダイレクトされる | ☐ |
| 統一された StudioNavigation が全画面で使用されている | ☐ |
| ModelerSidebar が廃止されている | ☐ |
| E2E テストがパスする | ☐ |

---

## 📋 Phase 3: 作業フロー可視化 + Home 改善（1週間）

### Week 5

#### Task 3.1: WorkspaceDashboard コンポーネント作成
**優先度**: 🟡 High  
**見積り**: 4h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/WorkspaceDashboard.tsx`

**作業内容**:
- [ ] 概況パネル（Entity/Form/Flow/Draft/Release 数）
- [ ] 状態インジケーター
- [ ] API 連携（統計情報取得）

```tsx
// 想定インターフェース
interface WorkspaceSummary {
  entities: { total: number; hasErrors: number };
  forms: { total: number; draft: number; published: number };
  flows: { total: number; active: number };
  releases: { latest: string; pending: number };
}
```

---

#### Task 3.2: QuickActions コンポーネント作成
**優先度**: 🟡 High  
**見積り**: 2h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/QuickActions.tsx`

**作業内容**:
- [ ] アクションボタン群
- [ ] [Open Modeler] [New Form] [New Flow] [Create Release]
- [ ] 権限に応じた表示制御

---

#### Task 3.3: RecentWorkList コンポーネント作成
**優先度**: 🟢 Medium  
**見積り**: 2h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/RecentWorkList.tsx`

**作業内容**:
- [ ] 最近の作業一覧
- [ ] 作業種別アイコン
- [ ] 更新日時表示
- [ ] クリックで該当画面へ遷移

---

#### Task 3.4: WorkflowStepper コンポーネント作成
**優先度**: 🟢 Medium  
**見積り**: 3h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/components/WorkflowStepper.tsx`

**作業内容**:
- [ ] M→F→F→R フローの視覚化
- [ ] 現在のステップハイライト
- [ ] 各ステップへのリンク
- [ ] 完了状態表示

---

#### Task 3.5: StudioHomePage のリニューアル
**優先度**: 🔴 Critical  
**見積り**: 4h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/pages/StudioHomePage.tsx`

**作業内容**:
- [ ] 既存のスキーマカード一覧を廃止
- [ ] WorkspaceDashboard 配置
- [ ] QuickActions 配置
- [ ] RecentWorkList 配置
- [ ] Getting Started セクション（新規ユーザー向け）

---

### Week 5: Phase 3 完了チェック

| チェック項目 | 状態 |
|-------------|------|
| Studio Home に概況が表示されている | ☐ |
| Quick Actions が機能している | ☐ |
| 最近の作業一覧が表示されている | ☐ |
| M→F→F→R フローが視覚化されている | ☐ |

---

## 📋 Phase 4: 権限境界の明確化（1週間）

### Week 6

#### Task 4.1: Studio ワークベンチの視覚的独立性
**優先度**: 🟢 Medium  
**見積り**: 2h

**作業内容**:
- [ ] Studio 専用のカラースキーム（微調整）
- [ ] ヘッダーの差別化
- [ ] 「Studio」ブランディング

---

#### Task 4.2: Builder 権限チェックの組み込み
**優先度**: 🟢 Medium  
**見積り**: 3h  
**担当ファイル**: `apps/frontend-v3/src/features/studio/guards/StudioGuard.tsx`

**作業内容**:
- [ ] `StudioGuard` コンポーネント作成
- [ ] Builder 権限チェック
- [ ] 権限不足時のリダイレクト/表示制限

---

#### Task 4.3: 危険操作の制限 UI
**優先度**: 🟢 Medium  
**見積り**: 2h

**作業内容**:
- [ ] 削除操作の確認ダイアログ強化
- [ ] Release override の警告表示
- [ ] 操作ログの表示

---

#### Task 4.4: Admin / Operations への分離準備
**優先度**: 🟢 Low  
**見積り**: 2h

**作業内容**:
- [ ] Admin ワークベンチの IA 設計（ドキュメント）
- [ ] Operations ワークベンチの IA 設計（ドキュメント）
- [ ] 将来実装のための TODO コメント

---

### Week 6: Phase 4 完了チェック

| チェック項目 | 状態 |
|-------------|------|
| Studio が視覚的に独立したワークベンチに見える | ☐ |
| Builder 権限がチェックされている | ☐ |
| 危険操作に適切な警告が表示される | ☐ |

---

## 📅 全体スケジュール

```
Week 1  [Phase 1] 基盤コンポーネント作成
        ├─ StudioLayout
        ├─ StudioHeader  
        ├─ StudioContextProvider
        └─ StudioNavigation

Week 2  [Phase 1] 既存画面の移行
        ├─ StudioPage 移行
        ├─ ModelerHomePage 移行
        └─ 各 Modeler ページ移行

Week 3  [Phase 2] ルート構造変更
        ├─ 新ルート定義
        ├─ 新ページ作成
        └─ 旧ページリファクタリング

Week 4  [Phase 2] ナビゲーション統一
        ├─ navigation 設定分離
        ├─ StudioNavigation 完成
        └─ E2E テスト更新

Week 5  [Phase 3] Home 改善 + 作業フロー
        ├─ WorkspaceDashboard
        ├─ QuickActions
        ├─ RecentWorkList
        └─ StudioHomePage リニューアル

Week 6  [Phase 4] 権限境界
        ├─ 視覚的独立性
        ├─ Builder 権限チェック
        └─ 危険操作の制限
```

---

## 📊 成果物一覧

### 新規ファイル

| ファイル | Phase |
|----------|-------|
| `features/studio/layouts/StudioLayout.tsx` | 1 |
| `features/studio/components/StudioHeader.tsx` | 1 |
| `features/studio/components/StudioContextBar.tsx` | 1 |
| `features/studio/components/StudioNavigation.tsx` | 1 |
| `features/studio/components/StudioPropertyPanel.tsx` | 1 |
| `features/studio/components/StudioStatusBar.tsx` | 1 |
| `features/studio/contexts/StudioContext.tsx` | 1 |
| `features/studio/modeler/pages/EntityListPage.tsx` | 2 |
| `features/studio/modeler/pages/EntityEditPage.tsx` | 2 |
| `features/studio/modeler/pages/RelationViewPage.tsx` | 2 |
| `features/studio/forms/pages/FormListPage.tsx` | 2 |
| `features/studio/components/WorkspaceDashboard.tsx` | 3 |
| `features/studio/components/QuickActions.tsx` | 3 |
| `features/studio/components/RecentWorkList.tsx` | 3 |
| `features/studio/components/WorkflowStepper.tsx` | 3 |
| `features/studio/guards/StudioGuard.tsx` | 4 |

### 変更ファイル

| ファイル | Phase | 変更内容 |
|----------|-------|---------|
| `app/router.config.tsx` | 2 | ルート構造変更 |
| `features/studio/pages/StudioPage.tsx` | 1 | StudioLayout 適用 |
| `features/studio/pages/StudioHomePage.tsx` | 3 | Dashboard UI |
| `features/studio/modeler/pages/*.tsx` | 1, 2 | StudioLayout 適用 |

### 削除ファイル

| ファイル | Phase |
|----------|-------|
| `features/studio/modeler/components/layout/ModelerLayout.tsx` | 2 |
| `features/studio/modeler/components/layout/ModelerSidebar.tsx` | 2 |

---

## 🚀 次のアクション

1. **Phase 1 開始**: `StudioLayout` コンポーネントの作成から着手
2. **Issue 作成**: 各 Task を GitHub Issue として登録
3. **ブランチ戦略**: `feature/studio-ui-correction-phase1` から開始

---

*Powered by Copilot 🤖*
