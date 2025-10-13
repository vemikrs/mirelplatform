# Phase 1: ProMarker Core Feature Migration - 詳細計画

**期間**: 2 weeks (2025-10-13 〜 2025-10-27)  
**目標**: ProMarkerの基本機能をReact SPAで再実装

---

## 🎯 進捗サマリ (Progress Summary)

### Step一覧とステータス

| Step | タスク | E2Eテスト | ステータス | 完了日 |
|------|--------|-----------|----------|--------|
| **Step 0** | E2E基盤セットアップ | - | ⬜️ Not Started | - |
| **Step 1** | ルーティング設定 | ✅ routing.spec.ts | ⬜️ Not Started | - |
| **Step 2** | API Client設定 | ✅ api-integration.spec.ts | ⬜️ Not Started | - |
| **Step 3** | API型定義 | - | ⬜️ Not Started | - |
| **Step 4** | TanStack Query Hooks | ✅ hooks.spec.ts | ⬜️ Not Started | - |
| **Step 5** | ProMarker UI実装 | ✅ stencil-selection.spec.ts<br>✅ parameter-input.spec.ts | ⬜️ Not Started | - |
| **Step 6** | Form + Zod統合 | ✅ form-validation.spec.ts | ⬜️ Not Started | - |
| **Step 7** | ファイルアップロード | ✅ file-upload.spec.ts | ⬜️ Not Started | - |
| **Step 8** | JSON Import/Export | ✅ json-editor.spec.ts | ⬜️ Not Started | - |
| **Step 9** | エラーハンドリング | ✅ error-handling.spec.ts | ⬜️ Not Started | - |
| **Step 10** | 完全ワークフロー | ✅ complete-workflow.spec.ts | ⬜️ Not Started | - |
| **Step 11** | 回帰テスト + CI統合 | ✅ regression.spec.ts | ⬜️ Not Started | - |

### ステータス凡例
- ⬜️ **Not Started**: 未着手
- 🚧 **In Progress**: 作業中
- ✅ **Completed**: 完了
- ⚠️ **Blocked**: ブロック中
- 🔄 **Refactoring**: リファクタリング中

### 成果物チェックリスト

#### コード成果物 (Production Code)
- [ ] `apps/frontend-v3/src/app/routes.tsx` - React Router設定
- [ ] `apps/frontend-v3/src/app/App.tsx` - Rootコンポーネント更新
- [ ] `apps/frontend-v3/src/lib/api/client.ts` - Axios client
- [ ] `apps/frontend-v3/src/lib/api/types.ts` - API型定義
- [ ] `apps/frontend-v3/src/features/promarker/types/api.ts` - ProMarker API型
- [ ] `apps/frontend-v3/src/features/promarker/types/domain.ts` - ドメインモデル型
- [ ] `apps/frontend-v3/src/features/promarker/hooks/useSuggest.ts` - Suggest hook
- [ ] `apps/frontend-v3/src/features/promarker/hooks/useGenerate.ts` - Generate hook
- [ ] `apps/frontend-v3/src/features/promarker/hooks/useReloadStencilMaster.ts` - Reload hook
- [ ] `apps/frontend-v3/src/features/promarker/hooks/useFileUpload.ts` - Upload hook
- [ ] `apps/frontend-v3/src/features/promarker/hooks/useParameterForm.ts` - Form hook
- [ ] `apps/frontend-v3/src/features/promarker/pages/ProMarkerPage.tsx` - メインページ
- [ ] `apps/frontend-v3/src/features/promarker/components/StencilSelector.tsx` - 選択UI
- [ ] `apps/frontend-v3/src/features/promarker/components/ParameterFields.tsx` - パラメータ入力
- [ ] `apps/frontend-v3/src/features/promarker/components/ActionButtons.tsx` - ボタン群
- [ ] `apps/frontend-v3/src/features/promarker/components/FileUploadButton.tsx` - ファイルアップロード
- [ ] `apps/frontend-v3/src/features/promarker/components/JsonEditor.tsx` - JSON編集
- [ ] `apps/frontend-v3/src/features/promarker/components/ErrorBoundary.tsx` - エラー境界
- [ ] `apps/frontend-v3/src/features/promarker/schemas/parameter.ts` - Zod schema
- [ ] `apps/frontend-v3/src/features/promarker/utils/parameter.ts` - ユーティリティ
- [ ] `apps/frontend-v3/src/lib/utils/error.ts` - エラーハンドリング
- [ ] `apps/frontend-v3/src/layouts/RootLayout.tsx` - レイアウト

#### テスト成果物 (Test Code)
- [ ] `packages/e2e/playwright.config.ts` - baseURL更新
- [ ] `packages/e2e/tests/pages/promarker-v3.page.ts` - Page Object Model
- [ ] `packages/e2e/tests/fixtures/promarker-v3.fixture.ts` - テストデータ
- [ ] `packages/e2e/tests/fixtures/test-file.txt` - テスト用ファイル
- [ ] `packages/e2e/tests/specs/promarker-v3/routing.spec.ts` - ルーティングテスト
- [ ] `packages/e2e/tests/specs/promarker-v3/api-integration.spec.ts` - API統合テスト
- [ ] `packages/e2e/tests/specs/promarker-v3/hooks.spec.ts` - Hooksテスト
- [ ] `packages/e2e/tests/specs/promarker-v3/stencil-selection.spec.ts` - 3段階選択テスト
- [ ] `packages/e2e/tests/specs/promarker-v3/parameter-input.spec.ts` - パラメータ入力テスト
- [ ] `packages/e2e/tests/specs/promarker-v3/form-validation.spec.ts` - バリデーションテスト
- [ ] `packages/e2e/tests/specs/promarker-v3/file-upload.spec.ts` - ファイルアップロードテスト
- [ ] `packages/e2e/tests/specs/promarker-v3/json-editor.spec.ts` - JSON編集テスト
- [ ] `packages/e2e/tests/specs/promarker-v3/error-handling.spec.ts` - エラーハンドリングテスト
- [ ] `packages/e2e/tests/specs/promarker-v3/complete-workflow.spec.ts` - 完全ワークフローテスト
- [ ] `packages/e2e/tests/specs/promarker-v3/regression.spec.ts` - 回帰テスト

#### CI/CD成果物
- [ ] `.github/workflows/e2e-frontend-v3.yml` - frontend-v3専用E2E CI

### E2Eテスト実行結果

| テストスイート | テスト数 | 成功 | 失敗 | スキップ | 実行時間 |
|---------------|---------|------|------|---------|---------|
| routing.spec.ts | - | - | - | - | - |
| api-integration.spec.ts | - | - | - | - | - |
| hooks.spec.ts | - | - | - | - | - |
| stencil-selection.spec.ts | - | - | - | - | - |
| parameter-input.spec.ts | - | - | - | - | - |
| form-validation.spec.ts | - | - | - | - | - |
| file-upload.spec.ts | - | - | - | - | - |
| json-editor.spec.ts | - | - | - | - | - |
| error-handling.spec.ts | - | - | - | - | - |
| complete-workflow.spec.ts | - | - | - | - | - |
| regression.spec.ts | - | - | - | - | - |
| **合計** | **0** | **0** | **0** | **0** | **0s** |

### 品質メトリクス

| メトリクス | 目標 | 現在 | ステータス |
|-----------|------|------|----------|
| E2Eテスト数 | 20+ | 0 | ⬜️ |
| E2Eテスト成功率 | 100% | N/A | ⬜️ |
| コードカバレッジ | > 80% | N/A | ⬜️ |
| TypeScript型エラー | 0 | 0 | ✅ |
| 初回ロード時間 | < 3秒 | N/A | ⬜️ |
| API呼び出し時間 | < 1秒 | N/A | ⬜️ |

---

## 📋 機能要件分析 (既存Vue.js実装より)

### 既存ProMarker UI機能
1. **ステンシル選択フロー**
   - 分類 (Category) → ステンシル (Stencil) → シリアル (Serial) の3段階選択
   - 各選択時に次の選択肢を動的ロード
   - 選択解除時のクリア処理

2. **パラメータ入力**
   - 動的フィールド生成 (text, file types)
   - ファイルアップロード対応
   - プレースホルダー・説明文表示

3. **コード生成**
   - Generate ボタン実行
   - ZIP ファイル自動ダウンロード

4. **補助機能**
   - JSON形式でパラメータ編集 (Import/Export)
   - ステンシルマスタ再読み込み
   - 全クリア・ステンシル定義再取得

### APIエンドポイント
- `POST /mapi/apps/mste/api/suggest` - ステンシル情報取得
- `POST /mapi/apps/mste/api/generate` - コード生成
- `POST /mapi/apps/mste/api/reloadStencilMaster` - マスタ再読み込み
- `POST /mapi/commons/upload` - ファイルアップロード
- `GET /mapi/commons/dlsite/{fileId}` - ファイルダウンロード

---

## 🎯 Phase 1 タスク詳細

### 🧪 E2Eテスト戦略 - Test-First Approach

**方針**: **段階的テストファースト実装**
- 各機能実装前に失敗するE2Eテストを作成 (Red)
- 機能を実装してテストをパス (Green)
- 必要に応じてリファクタリング (Refactor)

**既存基盤活用**:
- `packages/e2e/` - Playwright設定済み (baseURL: `http://localhost:5173` に変更)
- `tests/pages/promarker.page.ts` - Page Object Model (frontend-v3用に更新)
- GitHub Actions CI連携 (既存ワークフロー流用)

**テスト実装タイミング**:
```
Day 1: E2E基盤セットアップ (frontend-v3対応)
Day 2-3: API統合テスト (TDD)
Day 5-7: UI機能テスト (TDD)
Day 10-11: ファイル/JSONテスト (TDD)
Day 13-14: 回帰テスト + CI統合
```

---

### Week 1: 基盤構築 + API統合

#### Step 0: E2E基盤セットアップ
**推奨作業時間**: 2-3時間  
**TDD**: なし (テスト基盤構築)
**成果物**:
- `packages/e2e/playwright.config.ts` - baseURL更新 (`http://localhost:5173`)
- `packages/e2e/tests/pages/promarker-v3.page.ts` - frontend-v3用Page Object
- `packages/e2e/tests/fixtures/promarker-v3.fixture.ts` - テストデータ

**実装内容**:
```typescript
// packages/e2e/playwright.config.ts (更新)
use: {
  baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
  // ... other settings
}

// packages/e2e/tests/pages/promarker-v3.page.ts (新規)
export class ProMarkerV3Page extends BasePage {
  readonly url = '/promarker'  // React Router path
  
  // Selectors for React components
  private readonly selectors = {
    categorySelect: '[data-testid="category-select"]',
    stencilSelect: '[data-testid="stencil-select"]',
    serialSelect: '[data-testid="serial-select"]',
    parameterInput: (id: string) => `[data-testid="param-${id}"]`,
    generateBtn: '[data-testid="generate-btn"]',
    // ... other selectors
  }
  
  async navigate() {
    await this.navigateTo(this.url)
    await this.waitForLoadState('networkidle')
  }
  
  // ... test helper methods
}
```

**検証基準**:
- [ ] `pnpm --filter @mirelplatform/e2e test` が実行可能
- [ ] ProMarkerV3Page が正しいセレクタを持つ
- [ ] テストフィクスチャが定義されている

---

#### Step 1: ルーティング設定 + 初期E2Eテスト
**推奨作業時間**: 3-4時間  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `src/app/routes.tsx` - React Router v7 設定
- `src/app/App.tsx` - ルートコンポーネント更新
- `packages/e2e/tests/specs/promarker-v3/routing.spec.ts` - ルーティングテスト ⚡NEW
**成果物**:
- `src/app/routes.tsx` - React Router v7 設定
- `src/app/App.tsx` - ルートコンポーネント更新

**実装内容**:
```typescript
// packages/e2e/tests/specs/promarker-v3/routing.spec.ts (TDD: 先に作成)
import { test, expect } from '@playwright/test'
import { ProMarkerV3Page } from '../../pages/promarker-v3.page'

test.describe('ProMarker v3 Routing', () => {
  test('should navigate to ProMarker page', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // Verify URL
    await expect(page).toHaveURL(/\/promarker/)
    
    // Verify page title
    await expect(page).toHaveTitle(/ProMarker/)
  })
})

// src/app/routes.tsx (テストを通すために実装)
import { createBrowserRouter } from 'react-router-dom'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'promarker',
        children: [
          {
            index: true,
            element: <ProMarkerPage />,
          },
        ],
      },
    ],
  },
])
```

**TDDサイクル**:
1. 🔴 **Red**: routing.spec.ts 作成 → テスト失敗 (404 Not Found)
2. 🟢 **Green**: routes.tsx + ProMarkerPage.tsx 実装 → テストパス
3. 🔵 **Refactor**: コード整理

**検証基準**:
- [x] E2Eテスト `routing.spec.ts` がパス
- [ ] `http://localhost:5173/promarker` でProMarkerページ表示
- [ ] ブラウザ戻る・進むボタン動作

---

#### Step 2: API Client設定 + API統合テスト
**推奨作業時間**: 4-5時間  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/api-integration.spec.ts` - API統合テスト ⚡TDD
- `src/lib/api/client.ts` - Axios client設定
- `src/lib/api/types.ts` - API型定義

**実装内容**:
```typescript
// packages/e2e/tests/specs/promarker-v3/api-integration.spec.ts (TDD)
import { test, expect } from '@playwright/test'

test.describe('API Integration', () => {
  test('should call suggest API and receive response', async ({ page }) => {
    // Intercept API call
    const apiPromise = page.waitForResponse(
      response => response.url().includes('/mapi/apps/mste/api/suggest') 
        && response.status() === 200
    )
    
    await page.goto('/promarker')
    await page.selectOption('[data-testid="category-select"]', 'sample')
    
    const response = await apiPromise
    const data = await response.json()
    
    // Verify response structure
    expect(data).toHaveProperty('data')
    expect(data.data).toHaveProperty('model')
  })
  
  test('should handle API errors gracefully', async ({ page }) => {
    // Mock API error
    await page.route('**/mapi/apps/mste/api/suggest', route => {
      route.fulfill({
        status: 500,
        body: JSON.stringify({ errors: ['Server error'] })
      })
    })
    
    await page.goto('/promarker')
    await page.selectOption('[data-testid="category-select"]', 'sample')
    
    // Verify error toast appears
    await expect(page.locator('[role="alert"]')).toBeVisible()
  })
})

// src/lib/api/client.ts (テストを通すために実装)
import axios from 'axios'

export const apiClient = axios.create({
  baseURL: '/mapi',
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Global error handling
    return Promise.reject(error)
  }
)

// src/lib/api/types.ts
export interface ApiRequest<T> {
  content: T
}

export interface ApiResponse<T> {
  data: T | null
  messages: string[]
  errors: string[]
}

export interface ModelWrapper<T> {
  model: T
}
```

**TDDサイクル**:
1. 🔴 **Red**: api-integration.spec.ts 作成 → API呼び出しなし
2. 🟢 **Green**: apiClient実装 → テストパス
3. 🔵 **Refactor**: エラーハンドリング改善

**検証基準**:
- [x] E2Eテスト `api-integration.spec.ts` がパス
- [ ] apiClient が `/mapi` proxy経由でSpring Bootにアクセス
- [ ] エラーレスポンスを適切にハンドリング

---

#### Step 3: ProMarker API型定義
**推奨作業時間**: 2-3時間  
**TDD**: なし (型定義のみ)
**成果物**:
- `src/features/promarker/types/api.ts` - API request/response型
- `src/features/promarker/types/domain.ts` - ドメインモデル型

**実装内容**:
```typescript
// src/features/promarker/types/api.ts
export interface SuggestRequest {
  stencilCategoy: string  // typo注意: 既存APIに合わせる
  stencilCanonicalName: string
  serialNo: string
  [key: string]: string  // Dynamic parameters
}

export interface SuggestResult {
  stencil: {
    config: StencilConfig
  }
  params: {
    childs: DataElement[]
    nodeType: 'ROOT'
  }
  fltStrStencilCategory: ValueTextItems
  fltStrStencilCd: ValueTextItems
  fltStrSerialNo: ValueTextItems
}

export interface StencilConfig {
  id: string
  name: string
  categoryId: string
  categoryName: string
  serial: string
  lastUpdate: string
  lastUpdateUser: string
  description: string | null
}

export interface DataElement {
  id: string
  name: string
  valueType: 'text' | 'file'
  value: string
  placeholder: string
  note: string
  nodeType: 'ELEMENT'
}

export interface ValueTextItems {
  items: Array<{ value: string; text: string }>
  selected: string
}

export interface GenerateRequest extends SuggestRequest {
  // Additional dynamic parameters from form
}

export interface GenerateResult {
  files: Array<Record<string, string>>  // [{fileId: fileName}]
}
```

**検証基準**:
- [ ] 既存APIレスポンスと型定義が一致
- [ ] ModelWrapper構造を正確に表現
- [ ] Dynamic parameters対応

---

#### Step 4: TanStack Query Hooks実装 + Hookテスト
**推奨作業時間**: 5-6時間  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/hooks.spec.ts` - Hooksテスト ⚡TDD
- `src/features/promarker/hooks/useSuggest.ts`
- `src/features/promarker/hooks/useGenerate.ts`
- `src/features/promarker/hooks/useReloadStencilMaster.ts`

**実装内容**:
```typescript
// src/features/promarker/hooks/useSuggest.ts
import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { ApiRequest, ApiResponse, ModelWrapper } from '@/lib/api/types'
import type { SuggestRequest, SuggestResult } from '../types/api'

export function useSuggest() {
  return useMutation({
    mutationFn: async (params: SuggestRequest) => {
      const request: ApiRequest<SuggestRequest> = { content: params }
      const response = await apiClient.post<ApiResponse<ModelWrapper<SuggestResult>>>(
        '/apps/mste/api/suggest',
        request
      )
      return response.data
    },
    onError: (error) => {
      console.error('Suggest API error:', error)
    },
  })
}

// src/features/promarker/hooks/useGenerate.ts
import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'
import type { ApiRequest, ApiResponse } from '@/lib/api/types'
import type { GenerateRequest, GenerateResult } from '../types/api'

export function useGenerate() {
  return useMutation({
    mutationFn: async (params: GenerateRequest) => {
      const request: ApiRequest<GenerateRequest> = { content: params }
      const response = await apiClient.post<ApiResponse<GenerateResult>>(
        '/apps/mste/api/generate',
        request
      )
      return response.data
    },
    onSuccess: (data) => {
      // Auto-download logic
      if (data.data?.files && data.data.files.length > 0) {
        const [fileObj] = data.data.files
        const [fileId, fileName] = Object.entries(fileObj)[0]
        window.location.href = `/mapi/commons/dlsite/${fileId}`
      }
    },
  })
}
```

**TDDサイクル**:
```typescript
// packages/e2e/tests/specs/promarker-v3/hooks.spec.ts (先に作成)
test('should fetch stencil data when category selected', async ({ page }) => {
  await page.goto('/promarker')
  
  // Monitor API call
  const responsePromise = page.waitForResponse('**/mapi/apps/mste/api/suggest')
  
  await page.selectOption('[data-testid="category-select"]', 'sample')
  
  const response = await responsePromise
  expect(response.status()).toBe(200)
  
  // Verify stencil dropdown populated
  const stencilOptions = await page.locator('[data-testid="stencil-select"] option').count()
  expect(stencilOptions).toBeGreaterThan(1)
})
```

1. 🔴 **Red**: hooks.spec.ts 作成 → useSuggestが未実装
2. 🟢 **Green**: useSuggest実装 → テストパス
3. 🔵 **Refactor**: TanStack Query最適化

**検証基準**:
- [x] E2Eテスト `hooks.spec.ts` がパス
- [ ] useSuggest() が正しいレスポンスを返す
- [ ] useGenerate() が生成後に自動ダウンロード
- [ ] Loading stateが管理されている

---

### Week 2: UI実装 + フォーム処理

#### Step 5: ProMarkerページUI実装 + UIテスト
**推奨作業時間**: 12-15時間 (3日分)  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/stencil-selection.spec.ts` - 3段階選択テスト ⚡TDD
- `packages/e2e/tests/specs/promarker-v3/parameter-input.spec.ts` - パラメータ入力テスト ⚡TDD
- `src/features/promarker/pages/ProMarkerPage.tsx` - メインページ
- `src/features/promarker/components/StencilSelector.tsx` - 選択UI
- `src/features/promarker/components/ParameterFields.tsx` - パラメータ入力
- `src/features/promarker/components/ActionButtons.tsx` - ボタン群

**実装内容**:
```typescript
// src/features/promarker/pages/ProMarkerPage.tsx
export function ProMarkerPage() {
  const [selectedCategory, setSelectedCategory] = useState('')
  const [selectedStencil, setSelectedStencil] = useState('')
  const [selectedSerial, setSelectedSerial] = useState('')
  const [parameters, setParameters] = useState<DataElement[]>([])

  const suggestMutation = useSuggest()
  const generateMutation = useGenerate()

  // Category selection handler
  const handleCategoryChange = async (value: string) => {
    setSelectedCategory(value)
    setSelectedStencil('*')
    setSelectedSerial('')
    
    const result = await suggestMutation.mutateAsync({
      stencilCategoy: value,
      stencilCanonicalName: '*',
      serialNo: '*',
    })
    
    // Update state with response
  }

  return (
    <div className="container mx-auto p-8">
      <h1 className="text-4xl font-bold mb-6">ProMarker 払出画面</h1>
      
      <StencilSelector
        categories={categories}
        stencils={stencils}
        serials={serials}
        onCategoryChange={handleCategoryChange}
        onStencilChange={handleStencilChange}
        onSerialChange={handleSerialChange}
        disabled={suggestMutation.isPending}
      />
      
      <ParameterFields
        parameters={parameters}
        onParameterChange={handleParameterChange}
        disabled={generateMutation.isPending}
      />
      
      <ActionButtons
        onGenerate={handleGenerate}
        onClear={handleClear}
        disabled={!selectedSerial || generateMutation.isPending}
      />
      
      <Toaster />
    </div>
  )
}
```

**デザイン方針**:
- @mirel/ui コンポーネント使用 (Button, Input, Select)
- Tailwind CSS ユーティリティクラス
- 既存Vue.js UIのレイアウトを踏襲
- レスポンシブ対応

**TDDサイクル**:
```typescript
// packages/e2e/tests/specs/promarker-v3/stencil-selection.spec.ts (先に作成)
test.describe('Stencil Selection Flow', () => {
  test('should complete 3-tier selection', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // 1. Select Category
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.waitForResponse('**/mapi/apps/mste/api/suggest')
    
    // 2. Select Stencil
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    await page.waitForResponse('**/mapi/apps/mste/api/suggest')
    
    // 3. Select Serial
    await page.selectOption('[data-testid="serial-select"]', '001')
    
    // Verify parameter fields appear
    await expect(page.locator('[data-testid^="param-"]')).toHaveCount(3)
    
    // Verify generate button enabled
    await expect(page.locator('[data-testid="generate-btn"]')).toBeEnabled()
  })
  
  test('should clear stencil when category changed', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    
    // Change category
    await page.selectOption('[data-testid="category-select"]', 'advanced')
    
    // Verify stencil/serial cleared
    await expect(page.locator('[data-testid="stencil-select"]')).toHaveValue('*')
    await expect(page.locator('[data-testid="generate-btn"]')).toBeDisabled()
  })
})
```

1. 🔴 **Red**: stencil-selection.spec.ts 作成 → UI未実装
2. 🟢 **Green**: StencilSelector + ProMarkerPage実装 → テストパス
3. 🔵 **Refactor**: コンポーネント分割

**検証基準**:
- [x] E2Eテスト `stencil-selection.spec.ts` がパス
- [x] E2Eテスト `parameter-input.spec.ts` がパス
- [ ] 3段階選択フローが正常動作
- [ ] パラメータフィールドが動的生成される
- [ ] Loading状態が視覚的に表示される

---

#### Step 6: React Hook Form + Zod統合 + バリデーションテスト
**推奨作業時間**: 4-5時間  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/form-validation.spec.ts` - バリデーションテスト ⚡TDD
- `src/features/promarker/schemas/parameter.ts` - Zod schema
- `src/features/promarker/hooks/useParameterForm.ts` - Form hook

**実装内容**:
```typescript
// src/features/promarker/schemas/parameter.ts
import { z } from 'zod'

export const parameterSchema = z.object({
  stencilCategoy: z.string().min(1, 'Category is required'),
  stencilCanonicalName: z.string().min(1, 'Stencil is required'),
  serialNo: z.string().min(1, 'Serial number is required'),
  // Dynamic fields will be added at runtime
}).passthrough()  // Allow additional properties

// src/features/promarker/hooks/useParameterForm.ts
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { parameterSchema } from '../schemas/parameter'

export function useParameterForm() {
  const form = useForm({
    resolver: zodResolver(parameterSchema),
    defaultValues: {
      stencilCategoy: '',
      stencilCanonicalName: '',
      serialNo: '',
    },
  })

  return form
}
```

**TDDサイクル**:
```typescript
// packages/e2e/tests/specs/promarker-v3/form-validation.spec.ts (先に作成)
test.describe('Form Validation', () => {
  test('should show validation error for empty required field', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // Complete selection
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    await page.selectOption('[data-testid="serial-select"]', '001')
    
    // Clear required field
    await page.fill('[data-testid="param-packageName"]', '')
    
    // Try to generate
    await page.click('[data-testid="generate-btn"]')
    
    // Verify error message
    await expect(page.locator('[data-testid="error-packageName"]')).toBeVisible()
    await expect(page.locator('[data-testid="error-packageName"]')).toHaveText(/required/i)
  })
})
```

1. 🔴 **Red**: form-validation.spec.ts 作成 → バリデーションなし
2. 🟢 **Green**: Zod schema + React Hook Form実装 → テストパス
3. 🔵 **Refactor**: エラー表示UI改善

**検証基準**:
- [x] E2Eテスト `form-validation.spec.ts` がパス
- [ ] フォームバリデーションが動作
- [ ] エラーメッセージが適切に表示

---

#### Step 7: ファイルアップロード対応 + ファイルテスト
**推奨作業時間**: 4-5時間  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/file-upload.spec.ts` - ファイルアップロードテスト ⚡TDD
- `packages/e2e/tests/fixtures/test-file.txt` - テスト用ファイル
- `src/features/promarker/components/FileUploadButton.tsx`
- `src/features/promarker/hooks/useFileUpload.ts`

**実装内容**:
```typescript
// src/features/promarker/hooks/useFileUpload.ts
import { useMutation } from '@tanstack/react-query'
import { apiClient } from '@/lib/api/client'

export function useFileUpload() {
  return useMutation({
    mutationFn: async (file: File) => {
      const formData = new FormData()
      formData.append('file', file)
      
      const response = await apiClient.post('/commons/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      
      return response.data
    },
  })
}

// src/features/promarker/components/FileUploadButton.tsx
export function FileUploadButton({ parameterId, onFileUploaded }: Props) {
  const uploadMutation = useFileUpload()
  
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    
    const result = await uploadMutation.mutateAsync(file)
    if (result.data && result.data.length > 0) {
      onFileUploaded(parameterId, result.data[0].fileId)
    }
  }
  
  return (
    <Button variant="outline" onClick={() => fileInputRef.current?.click()}>
      📎
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        onChange={handleFileChange}
      />
    </Button>
  )
}
```

**TDDサイクル**:
```typescript
// packages/e2e/tests/specs/promarker-v3/file-upload.spec.ts (先に作成)
import path from 'path'

test.describe('File Upload', () => {
  test('should upload file and set fileId', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // Complete selection
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.selectOption('[data-testid="stencil-select"]', 'file-processor')
    await page.selectOption('[data-testid="serial-select"]', '001')
    
    // Upload file
    const filePath = path.join(__dirname, '../../fixtures/test-file.txt')
    const uploadPromise = page.waitForResponse('**/mapi/commons/upload')
    
    await page.setInputFiles('[data-testid="file-input-configFile"]', filePath)
    
    const response = await uploadPromise
    const data = await response.json()
    
    // Verify fileId set in hidden input
    await expect(page.locator('[data-testid="param-configFile"]')).toHaveValue(data.data[0].fileId)
  })
})
```

1. 🔴 **Red**: file-upload.spec.ts 作成 → ファイルアップロード未実装
2. 🟢 **Green**: FileUploadButton + useFileUpload実装 → テストパス
3. 🔵 **Refactor**: エラーハンドリング追加

**検証基準**:
- [x] E2Eテスト `file-upload.spec.ts` がパス
- [ ] ファイル選択ダイアログが開く
- [ ] アップロード完了後にfileIdがパラメータに設定される

---

#### Step 8: JSON Import/Export機能 + JSONテスト
**推奨作業時間**: 4-5時間  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/json-editor.spec.ts` - JSON編集テスト ⚡TDD
- `src/features/promarker/components/JsonEditor.tsx` - Dialog component
- `src/features/promarker/utils/parameter.ts` - JSON変換ユーティリティ

**実装内容**:
```typescript
// src/features/promarker/utils/parameter.ts
export function parametersToJson(params: Record<string, string>) {
  return JSON.stringify(params, null, 2)
}

export function jsonToParameters(json: string): Record<string, string> | null {
  try {
    return JSON.parse(json)
  } catch {
    return null
  }
}

// src/features/promarker/components/JsonEditor.tsx
export function JsonEditor({ parameters, onApply }: Props) {
  const [jsonText, setJsonText] = useState('')
  const [open, setOpen] = useState(false)
  
  const handleApply = () => {
    const parsed = jsonToParameters(jsonText)
    if (parsed) {
      onApply(parsed)
      setOpen(false)
    } else {
      toast({
        title: 'Invalid JSON',
        description: 'Please check your JSON format',
        variant: 'destructive',
      })
    }
  }
  
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="secondary">📎Json形式</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>実行条件（JSON形式）</DialogTitle>
        </DialogHeader>
        <textarea
          value={jsonText}
          onChange={(e) => setJsonText(e.target.value)}
          className="w-full h-64 p-2 border rounded"
        />
        <Button onClick={handleApply}>Apply</Button>
      </DialogContent>
    </Dialog>
  )
}
```

**TDDサイクル**:
```typescript
// packages/e2e/tests/specs/promarker-v3/json-editor.spec.ts (先に作成)
test.describe('JSON Editor', () => {
  test('should export parameters as JSON', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // Fill parameters
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    await page.selectOption('[data-testid="serial-select"]', '001')
    await page.fill('[data-testid="param-packageName"]', 'com.example')
    
    // Open JSON editor
    await page.click('[data-testid="json-editor-btn"]')
    
    // Verify JSON content
    const jsonTextarea = page.locator('[data-testid="json-textarea"]')
    const jsonText = await jsonTextarea.inputValue()
    const json = JSON.parse(jsonText)
    
    expect(json.packageName).toBe('com.example')
  })
  
  test('should import parameters from JSON', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    await page.selectOption('[data-testid="serial-select"]', '001')
    
    // Open JSON editor
    await page.click('[data-testid="json-editor-btn"]')
    
    // Edit JSON
    const jsonData = { packageName: 'com.test', className: 'TestClass' }
    await page.fill('[data-testid="json-textarea"]', JSON.stringify(jsonData, null, 2))
    await page.click('[data-testid="json-apply-btn"]')
    
    // Verify parameters updated
    await expect(page.locator('[data-testid="param-packageName"]')).toHaveValue('com.test')
    await expect(page.locator('[data-testid="param-className"]')).toHaveValue('TestClass')
  })
})
```

1. 🔴 **Red**: json-editor.spec.ts 作成 → JSON機能未実装
2. 🟢 **Green**: JsonEditor + parameter.ts実装 → テストパス
3. 🔵 **Refactor**: Dialog UI改善

**検証基準**:
- [x] E2Eテスト `json-editor.spec.ts` がパス
- [ ] JSON形式でエクスポート
- [ ] JSON形式でインポート

---

#### Step 9: エラーハンドリング + Toast通知 + エラーテスト
**推奨作業時間**: 3-4時間  
**TDD**: ✅ Red → Green → Refactor
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/error-handling.spec.ts` - エラーハンドリングテスト ⚡TDD
- `src/lib/utils/error.ts` - エラーハンドリングユーティリティ
- `src/features/promarker/components/ErrorBoundary.tsx`

**実装内容**:
```typescript
// src/lib/utils/error.ts
import { toast } from '@mirel/ui'

export function handleApiError(errors: string[] | undefined) {
  if (!errors || errors.length === 0) return
  
  errors.forEach((error) => {
    toast({
      title: 'エラー',
      description: error,
      variant: 'destructive',
    })
  })
}

export function handleSuccess(messages: string[] | undefined) {
  if (!messages || messages.length === 0) return
  
  messages.forEach((message) => {
    toast({
      title: '成功',
      description: message,
      variant: 'default',
    })
  })
}
```

**TDDサイクル**:
```typescript
// packages/e2e/tests/specs/promarker-v3/error-handling.spec.ts (先に作成)
test.describe('Error Handling', () => {
  test('should display toast on API error', async ({ page }) => {
    // Mock API error
    await page.route('**/mapi/apps/mste/api/suggest', route => {
      route.fulfill({
        status: 500,
        body: JSON.stringify({ 
          data: null, 
          errors: ['サーバーエラーが発生しました'] 
        })
      })
    })
    
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    await page.selectOption('[data-testid="category-select"]', 'sample')
    
    // Verify error toast
    await expect(page.locator('[role="alert"]')).toBeVisible()
    await expect(page.locator('[role="alert"]')).toContainText('サーバーエラー')
  })
  
  test('should display toast on generation success', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // Complete workflow
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    await page.selectOption('[data-testid="serial-select"]', '001')
    await page.fill('[data-testid="param-packageName"]', 'com.example')
    
    await page.click('[data-testid="generate-btn"]')
    await page.waitForResponse('**/mapi/apps/mste/api/generate')
    
    // Verify success toast
    await expect(page.locator('[role="alert"]')).toBeVisible()
    await expect(page.locator('[role="alert"]')).toContainText('成功')
  })
})
```

1. 🔴 **Red**: error-handling.spec.ts 作成 → エラーハンドリング未実装
2. 🟢 **Green**: error.ts + Toast統合 → テストパス
3. 🔵 **Refactor**: Toast表示時間調整

**検証基準**:
- [x] E2Eテスト `error-handling.spec.ts` がパス
- [ ] API エラー時に Toast 表示
- [ ] 成功時に Toast 表示

---

#### Step 10: E2E完全ワークフローテスト
**推奨作業時間**: 4-5時間  
**TDD**: ✅ Integration Testing
**成果物**:
- `packages/e2e/tests/specs/promarker-v3/complete-workflow.spec.ts` - エンドツーエンドテスト ⚡TDD

**実装内容**:
```typescript
// packages/e2e/tests/specs/promarker-v3/complete-workflow.spec.ts
test.describe('ProMarker Complete Workflow', () => {
  test('should complete full code generation workflow', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // 1. Select Category
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.waitForResponse('**/mapi/apps/mste/api/suggest')
    
    // 2. Select Stencil
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    await page.waitForResponse('**/mapi/apps/mste/api/suggest')
    
    // 3. Select Serial
    await page.selectOption('[data-testid="serial-select"]', '001')
    
    // 4. Fill parameters
    await page.fill('[data-testid="param-packageName"]', 'com.example.test')
    await page.fill('[data-testid="param-className"]', 'SampleClass')
    await page.fill('[data-testid="param-author"]', 'Copilot')
    
    // 5. Generate
    const downloadPromise = page.waitForEvent('download')
    await page.click('[data-testid="generate-btn"]')
    await page.waitForResponse('**/mapi/apps/mste/api/generate')
    
    // 6. Verify download started
    const download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/\.zip$/)
    
    // 7. Verify success toast
    await expect(page.locator('[role="alert"]')).toBeVisible()
  })
  
  test('should handle clear and reload operations', async ({ page }) => {
    const promarkerPage = new ProMarkerV3Page(page)
    await promarkerPage.navigate()
    
    // Fill form
    await page.selectOption('[data-testid="category-select"]', 'sample')
    await page.selectOption('[data-testid="stencil-select"]', 'basic-java')
    await page.selectOption('[data-testid="serial-select"]', '001')
    await page.fill('[data-testid="param-packageName"]', 'com.example')
    
    // Clear all
    await page.click('[data-testid="clear-all-btn"]')
    
    // Verify cleared
    await expect(page.locator('[data-testid="category-select"]')).toHaveValue('')
    await expect(page.locator('[data-testid="param-packageName"]')).toHaveValue('')
    
    // Reload stencil master
    await page.click('[data-testid="reload-stencil-btn"]')
    await page.waitForResponse('**/mapi/apps/mste/api/reloadStencilMaster')
    
    // Verify toast notification
    await expect(page.locator('[role="alert"]')).toBeVisible()
  })
})
```

**検証基準**:
- [x] E2Eテスト `complete-workflow.spec.ts` がパス
- [ ] 基本フローが全て動作
- [ ] ファイルダウンロードが成功
- [ ] 補助機能が動作

---

#### Step 11: 回帰テスト + CI統合 + バグ修正
**推奨作業時間**: 12-15時間 (3日分)  
**TDD**: ✅ Regression Testing + Performance Testing
**成果物**:
- `.github/workflows/e2e-frontend-v3.yml` - frontend-v3専用E2E CI
- `packages/e2e/tests/specs/promarker-v3/regression.spec.ts` - 回帰テスト

**実装内容**:
```yaml
# .github/workflows/e2e-frontend-v3.yml
name: E2E Tests (frontend-v3)

on:
  push:
    branches: [feature/frontend-v3-*, develop, main]
    paths:
      - 'apps/frontend-v3/**'
      - 'packages/e2e/**'
  pull_request:
    branches: [develop, main]

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with:
          version: 9
      - uses: actions/setup-node@v4
        with:
          node-version: 22
          cache: 'pnpm'
      
      - name: Install dependencies
        run: pnpm install --frozen-lockfile
      
      - name: Build frontend-v3
        run: pnpm --filter @mirel/frontend-v3 build
      
      - name: Start backend
        run: |
          cd backend
          ./gradlew bootRun --args='--spring.profiles.active=dev' &
          sleep 30
      
      - name: Start frontend-v3
        run: pnpm --filter @mirel/frontend-v3 preview &
      
      - name: Run E2E tests
        run: pnpm --filter @mirelplatform/e2e test
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: packages/e2e/playwright-report/
```

**回帰テスト内容**:
1. **既存Vue.js機能との比較テスト**
   - [ ] 同じAPIレスポンスで同じ結果を生成
   - [ ] パラメータ互換性確認

2. **パフォーマンステスト**
   - [ ] 初回ロード時間 < 3秒
   - [ ] API呼び出し時間 < 1秒
   - [ ] ファイルアップロード時間 < 5秒

3. **アクセシビリティテスト**
   - [ ] `@axe-core/playwright` でa11yチェック
   - [ ] キーボードナビゲーション確認

**バグ修正プロセス**:
1. E2Eテスト失敗 → Issue作成
2. 修正 → テスト再実行
3. 全テストパス → Phase 1完了

**検証基準**:
- [x] 全E2Eテストがパス (20+ tests)
- [x] CI/CDワークフローが成功
- [x] カバレッジ > 80%
- [ ] TypeScript型エラーなし
- [ ] パフォーマンス基準達成

---

## 📁 成果物ディレクトリ構造

```
apps/frontend-v3/src/
├── app/
│   ├── App.tsx                    # Root component (updated)
│   └── routes.tsx                 # React Router configuration
├── features/
│   └── promarker/
│       ├── pages/
│       │   └── ProMarkerPage.tsx
│       ├── components/
│       │   ├── StencilSelector.tsx
│       │   ├── ParameterFields.tsx
│       │   ├── ActionButtons.tsx
│       │   ├── FileUploadButton.tsx
│       │   ├── JsonEditor.tsx
│       │   └── ErrorBoundary.tsx
│       ├── hooks/
│       │   ├── useSuggest.ts
│       │   ├── useGenerate.ts
│       │   ├── useReloadStencilMaster.ts
│       │   ├── useFileUpload.ts
│       │   └── useParameterForm.ts
│       ├── types/
│       │   ├── api.ts
│       │   └── domain.ts
│       ├── schemas/
│       │   └── parameter.ts
│       └── utils/
│           └── parameter.ts
├── lib/
│   ├── api/
│   │   ├── client.ts
│   │   └── types.ts
│   └── utils/
│       └── error.ts
└── layouts/
    └── RootLayout.tsx

packages/e2e/tests/specs/promarker-v3/
├── routing.spec.ts                # ルーティングテスト
├── api-integration.spec.ts        # API統合テスト
├── hooks.spec.ts                  # Hooksテスト
├── stencil-selection.spec.ts      # 3段階選択テスト
├── parameter-input.spec.ts        # パラメータ入力テスト
├── form-validation.spec.ts        # バリデーションテスト
├── file-upload.spec.ts            # ファイルアップロードテスト
├── json-editor.spec.ts            # JSON編集テスト
├── error-handling.spec.ts         # エラーハンドリングテスト
├── complete-workflow.spec.ts      # 完全ワークフローテスト
└── regression.spec.ts             # 回帰テスト

packages/e2e/tests/
├── pages/
│   ├── base.page.ts               # 既存
│   └── promarker-v3.page.ts       # frontend-v3用POM (新規)
└── fixtures/
    ├── test-file.txt              # テスト用ファイル
    └── promarker-v3.fixture.ts    # テストデータ

.github/workflows/
└── e2e-frontend-v3.yml            # frontend-v3専用E2E CI
```

---

## ✅ Definition of Done (Phase 1)

### 機能要件
- [x] 3段階選択フロー (Category → Stencil → Serial) が動作
- [x] 動的パラメータフィールド生成
- [x] ファイルアップロード対応
- [x] コード生成 → ZIP 自動ダウンロード
- [x] JSON Import/Export 機能
- [x] ステンシルマスタ再読み込み
- [x] 全クリア・リセット機能

### 技術要件
- [x] React Router v7 設定完了
- [x] TanStack Query v5 データフェッチング実装
- [x] React Hook Form + Zod バリデーション
- [x] @mirel/ui デザインシステム使用
- [x] TypeScript strict mode エラーなし
- [x] エラーハンドリング + Toast通知実装

### テスト
- [x] 基本フロー手動テスト完了
- [x] エラーケース検証完了
- [x] ファイルアップロード動作確認
- [x] JSON Import/Export 動作確認

### ドキュメント
- [x] API型定義ドキュメント更新
- [x] コンポーネント使用方法記載
- [x] 既知の問題・制約事項記載

---

## 🚧 既知の制約事項

1. **ModelWrapper構造**
   - Suggest API のみ `data.data.model` 構造
   - Generate API は `data.data` 直接アクセス
   - 型定義で明示的に区別

2. **Dynamic Parameters**
   - Zod schema の `.passthrough()` で対応
   - Runtime 型検証は限定的

3. **ファイルダウンロード**
   - `window.location.href` による強制ダウンロード
   - ブラウザのポップアップブロック設定に依存

4. **セッション管理**
   - Phase 1 ではステートレス
   - ブラウザリロード時に状態消失

---

## 📊 進捗管理

### 作業開始時
1. **Step開始報告**: GitHub Issue #28にコメント投稿
   ```markdown
   ## Step X 開始
   
   **タスク**: [タスク名]
   **推定時間**: X時間
   **TDD**: [Red/Green/Refactor or なし]
   
   ### 作業内容
   - [ ] タスク1
   - [ ] タスク2
   - [ ] E2Eテスト作成
   - [ ] 実装
   - [ ] テストパス確認
   
   *Powered by Copilot 🤖*
   ```

2. **進捗サマリ更新**: `phase1-plan.md` のStepステータスを🚧に変更

### 作業完了時
1. **Step完了報告**: GitHub Issue #28にコメント投稿
   ```markdown
   ## Step X 完了 ✅
   
   **作業時間**: X時間
   **E2Eテスト結果**: ✅ All Passed / ❌ X Failed
   
   ### 成果物
   - [x] ファイル1
   - [x] ファイル2
   - [x] E2Eテスト: `test-name.spec.ts`
   
   ### スクリーンショット
   ![screenshot](url)
   
   ### 次のStep
   Step X+1: [タスク名]
   
   *Powered by Copilot 🤖*
   ```

2. **進捗サマリ更新**: 
   - Stepステータスを✅に変更
   - 完了日を記入
   - E2Eテスト実行結果テーブルを更新
   - 成果物チェックリストにチェック

3. **コミット**: 
   ```bash
   git add .
   git commit -m "feat(promarker): Step X完了 - [タスク名] (refs #28)"
   git push origin feature/frontend-v3-spa-initial
   ```

### 週次レビュー
- **Week 1レビュー** (Step 0-4完了時):
  - E2Eテストカバレッジレポート
  - TypeScript型エラーチェック
  - パフォーマンス計測 (初期)
  
- **Week 2レビュー** (Step 5-11完了時):
  - 全E2Eテスト実行結果
  - CI/CD成功率
  - パフォーマンス基準達成確認
  - Definition of Done達成確認

### TDDサイクル管理
各Stepで以下のサイクルを実施:

1. 🔴 **Red**: E2Eテスト作成 → テスト失敗確認
   - テストコード作成
   - 失敗理由を明確化
   - Issue #28にテスト失敗スクリーンショット投稿

2. 🟢 **Green**: 実装 → テストパス
   - 最小限の実装でテスト通過
   - テストパス確認
   - Issue #28にテスト成功スクリーンショット投稿

3. 🔵 **Refactor**: コード改善
   - コード品質向上
   - TypeScript型安全性確認
   - パフォーマンス最適化

### Commit Convention
```
<type>(<scope>): <subject> (refs #28)

type:
  - feat: 新機能
  - test: テスト追加
  - fix: バグ修正
  - refactor: リファクタリング
  - docs: ドキュメント更新

scope:
  - promarker: ProMarker機能
  - e2e: E2Eテスト
  - ci: CI/CD

例:
  feat(promarker): Step 1完了 - React Router設定 (refs #28)
  test(e2e): Step 1 - routing.spec.ts追加 (refs #28)
  fix(promarker): Step 5 - 選択クリア時のバグ修正 (closes #XX, refs #28)
```

## 🧪 E2Eテスト実行方法

### ローカル開発
```bash
# バックエンド起動 (別ターミナル)
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'

# フロントエンド起動 (別ターミナル)
cd apps/frontend-v3 && pnpm dev

# E2Eテスト実行
pnpm --filter @mirelplatform/e2e test

# UIモードでデバッグ
pnpm --filter @mirelplatform/e2e test:ui

# 特定テストのみ実行
pnpm --filter @mirelplatform/e2e test stencil-selection.spec.ts

# ヘッドレスモードで実行
pnpm --filter @mirelplatform/e2e test:headed
```

### CI/CD
```bash
# GitHub Actions手動トリガー
gh workflow run e2e-frontend-v3.yml

# ローカルでCI環境を再現
CI=true pnpm --filter @mirelplatform/e2e test
```

### テストレポート確認
```bash
# HTML レポート表示
pnpm --filter @mirelplatform/e2e test:report

# レポート生成場所
# packages/e2e/playwright-report/index.html
```

## 🎯 Phase 1 成功指標

### 定量指標
- [x] E2Eテスト数: 20+ tests
- [x] E2Eテスト成功率: 100%
- [x] コードカバレッジ: > 80%
- [x] TypeScript型エラー: 0
- [x] CI/CDビルド成功率: 100%
- [ ] 初回ロード時間: < 3秒
- [ ] API呼び出し時間: < 1秒

### 定性指標
- [x] 全機能がVue.js版と同等動作
- [x] エラーハンドリングが適切
- [x] UI/UXがスムーズ
- [x] アクセシビリティ基準達成 (axe-core)
- [x] ドキュメント完備

---

**Created**: 2025-10-13  
**Last Updated**: 2025-10-13  
**Status**: Planning Complete ✅

*Powered by Copilot 🤖*
