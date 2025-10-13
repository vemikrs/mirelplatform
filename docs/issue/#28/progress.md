# Issue #28: frontend-v3 React SPA構築 - 進捗管理

## Overview

**Issue**: [#28](https://github.com/vemikrs/mirelplatform/issues/28)  
**Branch**: `feature/frontend-v3-spa-initial`  
**Strategy**: React + Vite + Tailwind CSS + Radix Primitives + shadcn/ui SPA  
**Architecture Doc**: [frontend-v3-migration-strategy.md](../../architecture/frontend-v3-migration-strategy.md)

## Phase Status

### Phase 0: pnpm workspace + frontend-v3 初期化 ✅ COMPLETED

**期間**: 2025-10-13  
**コミット**: `2cfc200`

#### 完了タスク
- [x] pnpm workspace構成 (`pnpm-workspace.yaml`, root `package.json`)
- [x] Node.js 22.20.0 + pnpm 9.15.9環境構築
- [x] apps/frontend-v3 Viteプロジェクト作成 (React 19.2.0, TypeScript 5.9.3)
- [x] Core dependencies インストール (React Router, TanStack Query, Axios, Zod, etc.)
- [x] Tailwind CSS設定 (shadcn/ui互換トークン)
- [x] TypeScript strict mode + path mapping
- [x] Vite設定 (path alias, API proxy)
- [x] Vitest設定 (testing-library, happy-dom)
- [x] packages/ui (@mirel/ui) デザインシステムパッケージ作成
- [x] Radix UI primitives インストール
- [x] shadcn/ui inspired components実装 (Button, Input, Select, Dialog, Toast)
- [x] CVA variant system導入
- [x] テーマトークン定義
- [x] cn() utility + useToast() hook実装
- [x] 動作確認 (TypeScript型チェック、dev server起動、@mirel/ui import)

#### 成果物
- **ディレクトリ構造**: `apps/frontend-v3/`, `packages/ui/`
- **パッケージ設定**: `pnpm-workspace.yaml`, `package.json` (root, frontend-v3, ui)
- **設定ファイル**: `vite.config.ts`, `tailwind.config.js`, `tsconfig.json`, `vitest.config.ts`
- **コンポーネント**: Button, Input, Select, Dialog, Toast (+ Toaster, useToast)
- **テーマ**: `packages/ui/src/theme/tokens.ts` (colors, borderRadius, spacing)
- **ユーティリティ**: `cn()`, `useToast()`

#### 検証結果
```bash
✅ TypeScript型チェック成功 (tsc --noEmit)
✅ Vite dev server起動 (http://localhost:5173/)
✅ @mirel/ui workspace link動作
✅ テストページでコンポーネント表示確認
```

---

### Phase 1: ProMarker core feature migration ⏳ IN PROGRESS

**期間**: 2 weeks (予定)  
**目標**: ProMarkerの基本機能をReact SPAで再実装

#### タスクリスト
- [ ] **1.1 ルーティング設定**
  - [ ] React Router v7設定 (`src/app/routes.tsx`)
  - [ ] `/promarker` base route設定
  - [ ] List/Detail/Generate routes定義
  - [ ] ProtectedRoute guard実装

- [ ] **1.2 API統合**
  - [ ] Axios client設定 (`src/lib/api/client.ts`)
  - [ ] Spring Boot backend連携 (`/mapi` proxy経由)
  - [ ] ApiResponse<T> 型定義
  - [ ] Error handling pattern実装

- [ ] **1.3 データフェッチング**
  - [ ] TanStack Query設定 (`src/app/App.tsx`)
  - [ ] useSuggest() hook実装
  - [ ] useGenerate() hook実装
  - [ ] useReloadStencilMaster() hook実装

- [ ] **1.4 フォーム処理**
  - [ ] React Hook Form + Zod統合
  - [ ] Form validation schema定義
  - [ ] Dynamic form fields generation
  - [ ] File upload handling

- [ ] **1.5 ProMarker UI実装**
  - [ ] StencilList page (category/stencil/serial selection)
  - [ ] StencilDetail page (parameter input)
  - [ ] StencilGenerate page (code generation + download)
  - [ ] ErrorBoundary + Toast notifications

#### 成果物 (予定)
- **Routes**: `src/app/routes.tsx`, `src/features/promarker/routes.tsx`
- **API**: `src/lib/api/client.ts`, `src/features/promarker/api/`
- **Hooks**: `src/features/promarker/hooks/` (useSuggest, useGenerate, etc.)
- **Types**: `src/features/promarker/types/` (API response types)
- **Pages**: `src/features/promarker/pages/` (List, Detail, Generate)
- **Components**: `src/features/promarker/components/` (StencilForm, ParameterFields, etc.)

---

### Phase 2: @mirel/ui design system拡張 📋 PLANNED

**期間**: 1 week (予定)  
**目標**: ProMarker UIに必要な追加コンポーネント実装

#### タスクリスト
- [ ] **2.1 追加コンポーネント**
  - [ ] Table (data table with sorting/filtering)
  - [ ] Card (content container)
  - [ ] Badge (status indicator)
  - [ ] Alert (notification component)
  - [ ] Skeleton (loading placeholder)

- [ ] **2.2 Form components**
  - [ ] Textarea (multi-line input)
  - [ ] Checkbox (boolean input)
  - [ ] RadioGroup (single choice)
  - [ ] Label (form label)

- [ ] **2.3 ドキュメント**
  - [ ] Storybook setup
  - [ ] Component examples
  - [ ] Usage documentation
  - [ ] Design tokens documentation

---

### Phase 3: Layout & Navigation 📋 PLANNED

**期間**: 1 week (予定)

#### タスクリスト
- [ ] **3.1 レイアウト構築**
  - [ ] MainLayout component
  - [ ] Header with navigation
  - [ ] Sidebar (optional)
  - [ ] Footer

- [ ] **3.2 ナビゲーション**
  - [ ] Navigation menu
  - [ ] Breadcrumbs
  - [ ] User menu (auth integration)

---

### Phase 4: Testing & CI/CD 📋 PLANNED

**期間**: 1 week (予定)

#### タスクリスト
- [ ] **4.1 Unit Testing**
  - [ ] Component tests (Vitest + testing-library)
  - [ ] Hook tests
  - [ ] Utility tests
  - [ ] 80%+ test coverage

- [ ] **4.2 Integration Testing**
  - [ ] API integration tests
  - [ ] Router integration tests
  - [ ] Form submission tests

- [ ] **4.3 E2E Testing**
  - [ ] Playwright test setup
  - [ ] Critical user flows
  - [ ] Cross-browser testing

- [ ] **4.4 CI/CD**
  - [ ] GitHub Actions workflow (`.github/workflows/frontend-v3-ci.yml`)
  - [ ] Build verification
  - [ ] Test automation
  - [ ] Deployment to staging

---

## コミット履歴

### 2025-10-13
- `2cfc200` - feat(frontend-v3): Phase 0 完了 - pnpm workspace + Vite React + @mirel/ui (refs #28)
- `88268d4` - docs: frontend-v3 migration strategyをNext.jsからVite SPAに変更 (refs #28)
- 初回コミット - Issue #28作成、branch作成

---

## 参考ドキュメント

- [Architecture Document](../../architecture/frontend-v3-migration-strategy.md)
- [API Reference](../../api-reference.md)
- [E2E CI Error Analysis](../17/e2e-ci-error-analysis.md)

---

*Last Updated: 2025-10-13 by Copilot 🤖*
