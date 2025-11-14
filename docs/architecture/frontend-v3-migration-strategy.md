# Frontend V3 構築方針 - React SPA による ProMarker 刷新

## 概要

**目的**: Vue2/Nuxt2 EOL スタックから、保守可能な React SPA への完全移行  
**対象**: mirelplatform 管理UI (frontend → frontend-v3)  
**方針**: SSR/SSG 不要。シンプル・高速・最小構成の**クライアントサイド SPA**

**関連 Issue**: [#28 frontend-v3: React + Vite + Tailwind + Radix + shadcn/ui SPA構築方針](https://github.com/vemikrs/mirelplatform/issues/28)

---

## 技術スタック

### ✅ 採用構成 (2025年10月)

```json
{
  "name": "frontend-v3",
  "runtime": {
    "node": "^22.0.0",
    "packageManager": "pnpm@^9.0.0"
  },
  "framework": {
    "primary": "React 18+ SPA",
    "bundler": "Vite 5+",
    "router": "React Router v6+",
    "reason": "SSR不要。軽量・高速・シンプル"
  },
  "core": {
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "react-router-dom": "^6.0.0",
    "vite": "^5.0.0"
  },
  "language": {
    "typescript": "^5.6.0",
    "strictMode": true
  },
  "styling": {
    "primary": "Tailwind CSS v3+",
    "primitives": "Radix UI",
    "components": "shadcn/ui (code-in, @mirel/ui でラップ)",
    "reason": "軽量・アクセシブル・カスタマイズ容易"
  },
  "state": {
    "server": "TanStack Query v5 (データキャッシュ)",
    "client": "Zustand (軽量グローバル状態)",
    "forms": "React Hook Form + Zod"
  },
  "data": {
    "fetching": "Axios (既存API互換)",
    "validation": "Zod",
    "types": "TypeScript strict"
  },
  "testing": {
    "unit": "Vitest",
    "component": "@testing-library/react",
    "e2e": "Playwright (既存)",
    "a11y": "@axe-core/react"
  },
  "tooling": {
    "linter": "ESLint + @typescript-eslint",
    "formatter": "Prettier",
    "preCommit": "lint + typecheck"
  }
}
```

### 🎯 なぜこの構成か

#### ❌ Next.js/Astro を採用しない理由

- **SSR/SSG 不要**: ProMarker は認証必須の管理画面。SEO/OGP 不要
- **複雑性回避**: Server Components, App Router は過剰
- **ビルド高速**: Vite の方がシンプルで開発体験良好
- **学習コスト**: React SPA パターンの方がチーム習熟度高い

#### ✅ Vite + React Router を選択する理由

1. **高速開発**: HMR 即反映、Turbopack 不要
2. **シンプル**: クライアントサイドルーティングのみ
3. **柔軟性**: 必要な機能だけ追加
4. **軽量**: バンドルサイズ最小化
5. **実績**: vemijp でも類似パターン (Astro Islands → React SPA 相当)

#### ✅ shadcn/ui + Radix Primitives の採用理由

1. **アクセシビリティ**: Radix が WAI-ARIA 完全対応
2. **カスタマイズ性**: コードコピーで完全制御
3. **デザインシステム化**: @mirel/ui でラップして統一
4. **vemijp 実績**: 同じ Radix ベースで成功事例あり

**既存との比較**:
```jsx
// 現在 (Bootstrap Vue)
<b-button variant="primary" size="lg">生成</b-button>

// 移行後 (@mirel/ui でラップした shadcn/ui)
<Button variant="default" size="lg">生成</Button>
```

#### ✅ TanStack Query の採用理由

1. **キャッシング**: 自動でAPIレスポンスをキャッシュ
2. **再取得**: stale-while-revalidate 戦略
3. **楽観的更新**: UX向上
4. **型安全**: TypeScript 完全対応
5. **DevTools**: デバッグが容易

**既存 Axios との統合**:
```typescript
// 既存バックエンドAPIとの連携
const { data, isLoading } = useQuery({
  queryKey: ['stencils', category],
  queryFn: () => axios.post('/mapi/apps/mste/api/suggest', {
    content: { stencilCategoy: category }
  }).then(res => res.data)
})
```

---

## monorepo 構成

### 🏗️ pnpm ワークスペース構造

```
mirelplatform/
├── backend/                  # Spring Boot (既存・変更なし)
├── apps/
│   └── frontend-v3/          # React SPA (Vite)
│       ├── src/
│       │   ├── app/          # ルーティング
│       │   ├── features/     # 機能モジュール
│       │   ├── lib/          # API/hooks/utils
│       │   └── main.tsx      # エントリーポイント
│       ├── index.html
│       ├── vite.config.ts
│       └── package.json
├── packages/
│   ├── ui/                   # @mirel/ui (デザインシステム)
│   │   ├── src/
│   │   │   ├── components/   # shadcn/ui ラップ
│   │   │   ├── styles/       # CSS変数・トークン
│   │   │   ├── theme/        # カラー・スペーシング
│   │   │   └── index.ts
│   │   ├── tailwind.config.ts
│   │   └── package.json
│   ├── configs/              # 共有設定
│   │   ├── eslint-config/
│   │   ├── tsconfig/
│   │   └── tailwind-config/
│   └── e2e/                  # Playwright (既存)
├── pnpm-workspace.yaml
└── package.json              # ルート
```

### 📦 pnpm ワークスペース設定

```yaml
# pnpm-workspace.yaml
packages:
  - 'apps/*'
  - 'packages/*'
```

```json
// package.json (ルート)
{
  "name": "mirelplatform",
  "private": true,
  "scripts": {
    "dev": "pnpm --filter frontend-v3 dev",
    "build": "pnpm --filter frontend-v3 build",
    "lint": "pnpm --filter frontend-v3 lint",
    "test": "pnpm --filter frontend-v3 test",
    "typecheck": "pnpm -r typecheck"
  },
  "devDependencies": {
    "prettier": "^3.0.0"
  }
}
```

---

## Phase 別実装計画

### **Phase 0: 基盤構築 (1週間)**

**ゴール**: 空の React SPA + @mirel/ui 基礎

**タスク**:
1. **pnpm ワークスペース初期化**
   ```bash
   cd /workspaces/mirelplatform
   pnpm init
   mkdir -p apps/frontend-v3 packages/ui packages/configs
   ```

2. **frontend-v3 スキャフォールド**
   ```bash
   cd apps/frontend-v3
   pnpm create vite@latest . -- --template react-ts
   pnpm add react-router-dom @tanstack/react-query axios zod
   pnpm add -D @types/node vitest @testing-library/react
   ```

3. **@mirel/ui 初期化**
   ```bash
   cd packages/ui
   pnpm init
   pnpm add @radix-ui/react-dialog @radix-ui/react-select
   pnpm add class-variance-authority clsx tailwind-merge
   npx shadcn@latest init
   ```

4. **shadcn/ui コンポーネント追加**
   ```bash
   npx shadcn@latest add button input select dialog dropdown-menu toast table
   ```

5. **共有設定パッケージ**
   ```bash
   cd packages/configs
   mkdir eslint-config tsconfig tailwind-config
   ```

**Definition of Done**:
- ✅ `pnpm dev` で Vite 起動
- ✅ ダミーページ表示
- ✅ @mirel/ui/Button インポート可能
- ✅ TypeScript strict モード
- ✅ ESLint エラー0件

### **Phase 1: ProMarker コア機能移行 (2週間)**

**ゴール**: `/promarker` ルート配下に List/Detail/Generate ページ

**1. ルーティング構造**
```typescript
// src/app/routes.tsx
import { createBrowserRouter } from 'react-router-dom'
import { RootLayout } from '@/layouts/RootLayout'
import { AuthGuard } from '@/lib/guards/AuthGuard'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    children: [
      {
        path: 'promarker',
        element: <AuthGuard><ProMarkerLayout /></AuthGuard>,
        children: [
          { index: true, element: <ProMarkerList /> },
          { path: 'detail/:id', element: <ProMarkerDetail /> },
          { path: 'generate', element: <ProMarkerGenerate /> }
        ]
      }
    ]
  }
])
```

**2. API 統合 (既存バックエンド互換)**
```typescript
// src/lib/api/client.ts
import axios from 'axios'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/mapi',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// インターセプター: エラーハンドリング
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)
```

```typescript
// src/lib/api/promarker.ts
import { z } from 'zod'
import { apiClient } from './client'

// Zodスキーマ: バックエンドレスポンス型定義
const SuggestResponseSchema = z.object({
  data: z.object({
    model: z.object({
      fltStrStencilCategory: z.object({
        items: z.array(z.object({
          value: z.string(),
          text: z.string()
        })),
        selected: z.string()
      }),
      fltStrStencilCd: z.object({
        items: z.array(z.object({
          value: z.string(),
          text: z.string()
        })),
        selected: z.string()
      }),
      params: z.object({
        childs: z.array(z.object({
          id: z.string(),
          name: z.string(),
          value: z.string().optional(),
          type: z.string()
        }))
      })
    })
  }),
  errors: z.array(z.string()).optional()
})

export type SuggestResponse = z.infer<typeof SuggestResponseSchema>

export async function fetchSuggest(params: {
  stencilCategoy: string
  stencilCanonicalName: string
  serialNo: string
}) {
  const response = await apiClient.post('/apps/mste/api/suggest', {
    content: params
  })
  return SuggestResponseSchema.parse(response.data)
}

export async function generateCode(params: Record<string, unknown>) {
  const response = await apiClient.post('/apps/mste/api/generate', {
    content: params
  })
  return response.data
}
```

**3. TanStack Query フック**
```typescript
// src/features/promarker/hooks/useSuggest.ts
import { useQuery } from '@tanstack/react-query'
import { fetchSuggest } from '@/lib/api/promarker'

export function useSuggest(params: {
  category: string
  stencil: string
  serial: string
}) {
  return useQuery({
    queryKey: ['promarker-suggest', params],
    queryFn: () => fetchSuggest({
      stencilCategoy: params.category,
      stencilCanonicalName: params.stencil,
      serialNo: params.serial
    }),
    staleTime: 5 * 60 * 1000, // 5分
    enabled: !!params.category // category 選択後のみ実行
  })
}
```

**4. ProMarker メインページ**
```typescript
// src/features/promarker/pages/ProMarkerGenerate.tsx
'use client'
import { useState } from 'react'
import { useSuggest } from '../hooks/useSuggest'
import { Button } from '@mirel/ui'
import { Select, SelectContent, SelectItem } from '@mirel/ui'
import { Input } from '@mirel/ui'

export function ProMarkerGenerate() {
  const [category, setCategory] = useState('*')
  const [stencil, setStencil] = useState('*')
  const [serial, setSerial] = useState('*')

  const { data, isLoading } = useSuggest({ category, stencil, serial })

  const handleGenerate = async () => {
    // 生成ロジック
  }

  if (isLoading) return <div>読み込み中...</div>

  return (
    <div className="container mx-auto p-6">
      <h1 className="text-2xl font-bold mb-4">ProMarker 生成</h1>
      
      <div className="space-y-4">
        <Select value={category} onValueChange={setCategory}>
          <SelectContent>
            {data?.data.model.fltStrStencilCategory.items.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.text}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {/* 他のフィールド... */}

        <Button onClick={handleGenerate} size="lg">
          生成
        </Button>
      </div>
    </div>
  )
}
```

**Definition of Done**:
- ✅ `/promarker` ルート表示
- ✅ Category/Stencil/Serial 選択動作
- ✅ API 連携（suggest）成功
- ✅ パラメータフォーム動的生成
- ✅ Generate ボタンクリック → API コール

### **Phase 2: @mirel/ui デザインシステム構築 (1週間)**

**ゴール**: shadcn/ui をラップした統一コンポーネントライブラリ

**1. @mirel/ui パッケージ構造**
```
packages/ui/
├── src/
│   ├── components/
│   │   ├── button.tsx          # shadcn/ui Button ラップ
│   │   ├── input.tsx
│   │   ├── select.tsx
│   │   ├── dialog.tsx
│   │   ├── dropdown-menu.tsx
│   │   ├── toast.tsx
│   │   └── table.tsx
│   ├── styles/
│   │   ├── globals.css         # Tailwind base + 共通スタイル
│   │   └── animations.css
│   ├── theme/
│   │   ├── colors.ts           # カラートークン
│   │   ├── spacing.ts          # スペーシング
│   │   └── tokens.ts           # CSS変数
│   └── index.ts                # エクスポート
├── tailwind.config.ts          # テーマ定義
├── tsconfig.json
└── package.json
```

**2. Button コンポーネント例**
```typescript
// packages/ui/src/components/button.tsx
import * as React from 'react'
import { Slot } from '@radix-ui/react-slot'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '../lib/utils'

// トークンベースの variant 定義
const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground hover:bg-primary/90',
        destructive: 'bg-destructive text-destructive-foreground hover:bg-destructive/90',
        outline: 'border border-input bg-background hover:bg-accent',
        ghost: 'hover:bg-accent hover:text-accent-foreground',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-9 rounded-md px-3',
        lg: 'h-11 rounded-md px-8',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button'
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    )
  }
)
Button.displayName = 'Button'
```

**3. テーマトークン定義**
```typescript
// packages/ui/src/theme/tokens.ts
export const tokens = {
  colors: {
    primary: {
      DEFAULT: 'hsl(222.2 47.4% 11.2%)',
      foreground: 'hsl(210 40% 98%)',
    },
    destructive: {
      DEFAULT: 'hsl(0 84.2% 60.2%)',
      foreground: 'hsl(210 40% 98%)',
    },
    // vemijp の Liquid Design 参考
    brand: {
      primary: '#4A90E2',
      secondary: '#7BB3F0',
    }
  },
  radius: {
    sm: '0.375rem',
    md: '0.5rem',
    lg: '0.75rem',
  },
  spacing: {
    xs: '0.5rem',
    sm: '1rem',
    md: '1.5rem',
    lg: '2rem',
  }
}
```

**4. Tailwind 設定連携**
```typescript
// packages/ui/tailwind.config.ts
import { tokens } from './src/theme/tokens'

export default {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: tokens.colors,
      borderRadius: tokens.radius,
      spacing: tokens.spacing,
    }
  }
}
```

**5. frontend-v3 での使用**
```typescript
// apps/frontend-v3/src/features/promarker/pages/ProMarkerGenerate.tsx
import { Button, Input, Select } from '@mirel/ui'

export function ProMarkerGenerate() {
  return (
    <div>
      <Button variant="default" size="lg">生成</Button>
      <Input placeholder="パラメータ入力" />
      <Select>...</Select>
    </div>
  )
}
```

**Definition of Done**:
- ✅ @mirel/ui パッケージ構築
- ✅ Button, Input, Select, Dialog コンポーネント実装
- ✅ トークンベーステーマ
- ✅ frontend-v3 からインポート可能
- ✅ Storybook または ドキュメントページ

### **Phase 3: レイアウト・ナビゲーション (3日)**

**ゴール**: 共通レイアウト、認証ガード、エラーページ

```typescript
// src/layouts/RootLayout.tsx
import { Outlet } from 'react-router-dom'
import { Header } from '@/components/common/Header'
import { Footer } from '@/components/common/Footer'

export function RootLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
```

```typescript
// src/lib/guards/AuthGuard.tsx
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/lib/hooks/useAuth'

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth()
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  
  return <>{children}</>
}
```

**Definition of Done**:
- ✅ Header/Footer 実装
- ✅ 認証ガード動作（ダミー）
- ✅ 404 ページ
- ✅ エラーバウンダリ

---

### **Phase 4: テスト・CI/CD (3日)**

**1. Vitest ユニットテスト**
```typescript
// src/lib/api/promarker.test.ts
import { describe, it, expect, vi } from 'vitest'
import { fetchSuggest } from './promarker'

describe('fetchSuggest', () => {
  it('should return parsed suggest response', async () => {
    const mockResponse = {
      data: {
        model: {
          fltStrStencilCategory: {
            items: [{ value: '/samples', text: 'Sample' }],
            selected: '/samples'
          }
        }
      }
    }
    
    // モック API
    vi.mock('./client', () => ({
      apiClient: {
        post: vi.fn().mockResolvedValue({ data: mockResponse })
      }
    }))
    
    const result = await fetchSuggest({
      stencilCategoy: '*',
      stencilCanonicalName: '*',
      serialNo: '*'
    })
    
    expect(result.data.model.fltStrStencilCategory.items).toHaveLength(1)
  })
})
```

**2. React Testing Library**
```typescript
// src/features/promarker/pages/ProMarkerGenerate.test.tsx
import { render, screen } from '@testing-library/react'
import { ProMarkerGenerate } from './ProMarkerGenerate'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

describe('ProMarkerGenerate', () => {
  it('should render category select', () => {
    const queryClient = new QueryClient()
    
    render(
      <QueryClientProvider client={queryClient}>
        <ProMarkerGenerate />
      </QueryClientProvider>
    )
    
    expect(screen.getByText('ProMarker 生成')).toBeInTheDocument()
  })
})
```

**3. E2E テスト (Playwright)**
```typescript
// packages/e2e/tests/promarker-v3.spec.ts
import { test, expect } from '@playwright/test'

test.describe('ProMarker V3', () => {
  test('should complete generation workflow', async ({ page }) => {
    await page.goto('http://localhost:5173/promarker/generate')
    
    // Category 選択
    await page.getByLabel('Stencil Category').click()
    await page.getByRole('option', { name: '/samples' }).click()
    
    // 生成実行
    await page.getByRole('button', { name: '生成' }).click()
    
    // ダウンロード確認
    const download = await page.waitForEvent('download')
    expect(download.suggestedFilename()).toMatch(/\.zip$/)
  })
})
```

**4. CI/CD 設定**
```yaml
# .github/workflows/frontend-v3-ci.yml
name: Frontend V3 CI

on:
  push:
    branches: [feature/frontend-v3-*]
  pull_request:
    paths:
      - 'apps/frontend-v3/**'
      - 'packages/ui/**'

jobs:
  test:
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
      
      - name: Type check
        run: pnpm typecheck
      
      - name: Lint
        run: pnpm --filter frontend-v3 lint
      
      - name: Unit tests
        run: pnpm --filter frontend-v3 test
      
      - name: Build
        run: pnpm --filter frontend-v3 build
      
      - name: E2E tests
        run: |
          pnpm --filter frontend-v3 preview &
          sleep 5
          pnpm --filter e2e test
```

**Definition of Done**:
- ✅ Vitest ユニットテスト実行
- ✅ React Testing Library コンポーネントテスト
- ✅ Playwright E2E テスト（新環境）
- ✅ CI/CD パイプライン構築
- ✅ カバレッジ > 70%

---

## ディレクトリ構造 (詳細)

```
apps/frontend-v3/
├── public/                       # 静的ファイル
│   ├── favicon.ico
│   └── images/
├── src/
│   ├── app/                      # ルーティング
│   │   ├── routes.tsx            # React Router 設定
│   │   └── App.tsx               # ルートコンポーネント
│   ├── features/                 # 機能モジュール
│   │   └── promarker/
│   │       ├── pages/            # ページコンポーネント
│   │       │   ├── ProMarkerList.tsx
│   │       │   ├── ProMarkerDetail.tsx
│   │       │   └── ProMarkerGenerate.tsx
│   │       ├── components/       # 機能固有コンポーネント
│   │       │   ├── CategorySelect.tsx
│   │       │   ├── StencilSelect.tsx
│   │       │   └── ParameterForm.tsx
│   │       └── hooks/            # カスタムフック
│   │           ├── useSuggest.ts
│   │           ├── useGenerate.ts
│   │           └── useFileUpload.ts
│   ├── components/               # 共通コンポーネント
│   │   └── common/
│   │       ├── Header.tsx
│   │       ├── Footer.tsx
│   │       └── Navigation.tsx
│   ├── layouts/                  # レイアウト
│   │   ├── RootLayout.tsx
│   │   └── ProMarkerLayout.tsx
│   ├── lib/                      # ライブラリ
│   │   ├── api/                  # APIクライアント
│   │   │   ├── client.ts
│   │   │   ├── promarker.ts
│   │   │   └── types.ts
│   │   ├── hooks/                # 汎用フック
│   │   │   ├── useAuth.ts
│   │   │   └── useToast.ts
│   │   ├── guards/               # ルートガード
│   │   │   └── AuthGuard.tsx
│   │   ├── utils/                # ヘルパー
│   │   │   ├── cn.ts
│   │   │   └── format.ts
│   │   └── constants/
│   │       └── config.ts
│   ├── styles/                   # スタイル
│   │   └── globals.css
│   ├── main.tsx                  # エントリーポイント
│   └── vite-env.d.ts
├── index.html
├── vite.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── vitest.config.ts
└── package.json

packages/ui/
├── src/
│   ├── components/
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── select.tsx
│   │   ├── dialog.tsx
│   │   ├── dropdown-menu.tsx
│   │   ├── toast.tsx
│   │   └── table.tsx
│   ├── styles/
│   │   ├── globals.css
│   │   └── animations.css
│   ├── theme/
│   │   ├── colors.ts
│   │   ├── spacing.ts
│   │   └── tokens.ts
│   ├── lib/
│   │   └── utils.ts
│   └── index.ts
├── tailwind.config.ts
├── tsconfig.json
└── package.json
```

---

## 技術的決定事項

### 1. **状態管理戦略**

```typescript
// ✅ Server State: TanStack Query (API キャッシュ)
const { data } = useQuery({
  queryKey: ['stencils'],
  queryFn: fetchStencils,
  staleTime: 5 * 60 * 1000
})

// ✅ Global Client State: Zustand (軽量)
import { create } from 'zustand'

interface AppState {
  selectedCategory: string
  setCategory: (cat: string) => void
}

export const useAppStore = create<AppState>((set) => ({
  selectedCategory: '',
  setCategory: (cat) => set({ selectedCategory: cat })
}))

// ✅ Local State: useState/useReducer
const [formData, setFormData] = useState({})

// ❌ Redux - 採用しない（オーバーキル）
```

### 2. **フォーム管理**

```typescript
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

const parameterSchema = z.object({
  message: z.string().min(1, '必須項目です'),
  language: z.enum(['ja', 'en'])
})

export function ParameterForm() {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(parameterSchema)
  })
  
  const onSubmit = (data: z.infer<typeof parameterSchema>) => {
    // バリデーション済みデータ
  }
  
  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input {...register('message')} />
      {errors.message && <span>{errors.message.message}</span>}
    </form>
  )
}
```

### 3. **スタイリング規約**

```typescript
// ✅ Tailwind Utility Classes (推奨)
<div className="flex items-center gap-4 p-6 bg-white rounded-lg shadow-md">

// ✅ CVA for Component Variants (@mirel/ui内部)
const buttonVariants = cva('base-classes', {
  variants: {
    variant: { primary: '...', secondary: '...' }
  }
})

// ✅ cn() でクラス結合
import { cn } from '@/lib/utils'
<div className={cn('base', isActive && 'active')} />

// ❌ Inline Styles - 避ける
<div style={{ display: 'flex' }}>
```

### 4. **パフォーマンス最適化**

```typescript
// ✅ React.lazy + Suspense
const HeavyComponent = lazy(() => import('./HeavyComponent'))

<Suspense fallback={<Skeleton />}>
  <HeavyComponent />
</Suspense>

// ✅ TanStack Query キャッシング
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      cacheTime: 10 * 60 * 1000,
      refetchOnWindowFocus: false
    }
  }
})

// ✅ useMemo/useCallback (必要時のみ)
const expensiveResult = useMemo(() => computeExpensive(data), [data])
```

---

## セキュリティ・ベストプラクティス

### 1. **環境変数管理**

```bash
# .env.local (Git 除外)
VITE_API_BASE_URL=http://localhost:3000/mapi
VITE_APP_NAME=ProMarker
```

```typescript
// src/lib/constants/config.ts
export const config = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,
  appName: import.meta.env.VITE_APP_NAME,
  isDevelopment: import.meta.env.DEV
}
```

### 2. **CSRF 対策**

```typescript
// src/lib/api/client.ts
export const apiClient = axios.create({
  baseURL: config.apiBaseUrl,
  withCredentials: true,
  headers: {
    'X-CSRF-Token': getCsrfToken()
  }
})
```

### 3. **XSS 対策**

```typescript
// ✅ React のデフォルトエスケープ
<div>{userInput}</div> // 自動エスケープ

// ❌ dangerouslySetInnerHTML - 避ける
<div dangerouslySetInnerHTML={{ __html: userInput }} />

// ✅ DOMPurify 使用（必要な場合）
import DOMPurify from 'dompurify'
<div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(html) }} />
```

---

## 開発環境セットアップ

### 初期セットアップコマンド

```bash
# Node.js 22 インストール (nvm)
nvm install 22
nvm use 22

# pnpm インストール
corepack enable
corepack prepare pnpm@9 --activate

# ワークスペース初期化
cd /workspaces/mirelplatform
pnpm init

# frontend-v3 作成
mkdir -p apps/frontend-v3
cd apps/frontend-v3
pnpm create vite@latest . -- --template react-ts

# 依存関係インストール
pnpm add react-router-dom @tanstack/react-query axios zod
pnpm add react-hook-form @hookform/resolvers/zod
pnpm add zustand class-variance-authority clsx tailwind-merge

# 開発ツール
pnpm add -D @types/node vitest @testing-library/react
pnpm add -D eslint @typescript-eslint/eslint-plugin
pnpm add -D prettier tailwindcss autoprefixer postcss

# @mirel/ui 初期化
cd ../../packages/ui
pnpm init
npx shadcn@latest init
npx shadcn@latest add button input select dialog dropdown-menu toast table
```

### DevContainer 更新

```json
// .devcontainer/devcontainer.json
{
  "name": "mirelplatform",
  "image": "mcr.microsoft.com/devcontainers/typescript-node:22",
  "features": {
    "ghcr.io/devcontainers-contrib/features/pnpm:2": {
      "version": "9"
    }
  },
  "customizations": {
    "vscode": {
      "extensions": [
        "bradlc.vscode-tailwindcss",
        "esbenp.prettier-vscode",
        "dbaeumer.vscode-eslint"
      ]
    }
  },
  "postCreateCommand": "pnpm install"
}
```

---

## マイルストーン

```
Week 1:     Phase 0 - 基盤構築 (pnpm + Vite + @mirel/ui)
Week 2-3:   Phase 1 - ProMarker コア機能移行
Week 4:     Phase 2 - @mirel/ui デザインシステム
Week 5:     Phase 3 - レイアウト・ナビゲーション
Week 6:     Phase 4 - テスト・CI/CD
Week 7+:    本番移行準備、旧 frontend 廃止
```

---

## 成功指標 (KPI)

- ✅ **ビルド時間**: < 10秒 (Vite HMR)
- ✅ **初期バンドルサイズ**: < 200KB (gzip)
- ✅ **Lighthouse Score**: Performance > 90, Accessibility > 95
- ✅ **型カバレッジ**: 100% (TypeScript strict)
- ✅ **テストカバレッジ**: > 70%
- ✅ **E2E テスト**: 全シナリオパス

---

## リスクと緩和策

| リスク | 影響 | 緩和策 |
|--------|------|--------|
| API インターフェース変更 | 高 | Zod スキーマで検証、E2E テスト |
| @mirel/ui コンポーネント不足 | 中 | shadcn/ui から段階的に追加 |
| Vite ビルド設定 | 低 | 公式ドキュメント参照、実績多数 |
| Node.js 22 互換性 | 低 | LTS 版、依存関係事前検証 |

---

## 参考リソース

- **Vite**: https://vitejs.dev/
- **React Router v6**: https://reactrouter.com/
- **TanStack Query**: https://tanstack.com/query/latest
- **shadcn/ui**: https://ui.shadcn.com/
- **Radix UI**: https://www.radix-ui.com/
- **Tailwind CSS**: https://tailwindcss.com/
- **vemijp (参考実装)**: https://github.com/vemikrs/vemijp

---

**作成日**: 2025-10-13  
**更新日**: 2025-10-13  
**作成者**: GitHub Copilot 🤖  
**関連 Issue**: [#28 frontend-v3: React + Vite + Tailwind + Radix + shadcn/ui SPA構築方針](https://github.com/vemikrs/mirelplatform/issues/28)  
**ステータス**: ✅ 承認済み

---

## 次のアクション

1. ✅ Issue #28 作成完了
2. ✅ ブランチ `feature/frontend-v3-spa-initial` 作成完了
3. ⏳ Phase 0 開始: pnpm ワークスペース + frontend-v3 スキャフォールド
4. ⏳ @mirel/ui パッケージ初期化
5. ⏳ 基本コンポーネント実装

**Powered by Copilot 🤖**
