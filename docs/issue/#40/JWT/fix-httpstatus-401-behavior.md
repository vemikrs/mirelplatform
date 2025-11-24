# HTTPステータス401の挙動改善

## 現状の問題整理と、あるべき姿の提案

調査結果を踏まえ、現在の問題点と解決策を以下に提示します。

---

## 📋 現状の問題整理

### 1. **401エラー発生箇所の洗い出し**
コンソールログから、以下3つのAPI呼び出しが同時に401を返しています:
- `GET /mapi/users/me` (RootLayoutの`fetchProfile`から)
- `GET /mapi/users/me/tenants` (同上 + UserMenuから)
- `GET /mapi/users/me/licenses` (同上 + UserMenuから)

### 2. **認証チェックの流れと問題**
現在の構成:
1. **React Router**: router.config.tsx でルート定義
2. **ProtectedRoute**: `isAuthenticated` フラグ(Zustand永続化)を使った**クライアント側チェックのみ**
3. **RootLayout**: マウント時に `fetchProfile()` を実行し、プロフィール情報を取得
4. **問題**: 
   - **`isAuthenticated`は過去のセッション情報を保持**しているため、サーバー側のセッション切れ(401)と整合しない
   - **401エラーをキャッチしてログアウト処理を実行していない**
   - **ProtectedRouteの判定が先に通過**し、コンポーネントが描画される→**その後**に`fetchProfile`が401で失敗
   - 結果: **一瞬画面が描画され、セキュリティホール**となる

---

## 🎯 あるべき姿の方式提案

### **方針: 多層防御によるセキュリティ強化 + エラーハンドリング基盤整備**

| 層 | 実装箇所 | 役割 |
|---|---|---|
| **1. グローバル401インターセプター** | client.ts | 全401エラーを捕捉し、強制ログアウト + `/login`へリダイレクト |
| **2. RootLayout loader** | router.config.tsx | ルート遷移前にサーバー側認証状態を検証 |
| **3. ProtectedRoute強化** | ProtectedRoute.tsx | サーバー検証失敗時のフォールバック表示 |
| **4. エラーページ基盤** | features/error | 403/404/500専用の明示的なエラー画面 |
| **5. React Router ErrorBoundary** | router.config.tsx | 予期しないエラーの包括的なハンドリング |

**エラーページ整備方針**:
- **401**: 専用ページ不要(Interceptorで即座に `/login` へリダイレクト)
- **403 Forbidden**: 権限エラー専用ページ
- **404 Not Found**: 存在しないルート用の基盤ページ
- **500 Internal Server Error**: 予期しないエラー用のフォールバック

---

## 🛠️ 具体的な実装方針

### **1. Axios Interceptorで401を全域キャッチ**

```typescript
// apps/frontend-v3/src/lib/api/client.ts

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Zustand storeをクリア
      const { clearAuth } = useAuthStore.getState();
      clearAuth();
      
      // ログイン画面へリダイレクト(現在のパスを保存)
      const currentPath = window.location.pathname;
      window.location.href = `/login?returnUrl=${encodeURIComponent(currentPath)}`;
      
      // エラーは伝播させない(画面描画を防ぐ)
      return Promise.reject(error);
    }
    return Promise.reject(error);
  }
);
```

**効果**: 
- どこで401が発生しても即座にログアウト&リダイレクト
- **描画前にキャッチされるため、一瞬の表示を防げる**

---

### **2. RootLayout loaderでサーバー側検証**

React Routerの`loader`は**ルート遷移前**に実行されるため、ここで`/users/me`を呼び出して認証状態を確認します。

```typescript
// apps/frontend-v3/src/app/router.config.tsx

// キャッシュ用の変数（同一セッション内での重複API呼び出しを防ぐ）
let cachedProfile: UserProfile | null = null;
let cacheTimestamp: number = 0;
const CACHE_DURATION = 5000; // 5秒

async function authLoader() {
  const { isAuthenticated, tokens } = useAuthStore.getState();
  
  if (!isAuthenticated || !tokens?.accessToken) {
    throw redirect('/login');
  }
  
  try {
    const now = Date.now();
    
    // キャッシュが有効ならスキップ
    if (cachedProfile && (now - cacheTimestamp) < CACHE_DURATION) {
      return loadNavigationConfig();
    }
    
    // /users/me で認証検証とプロフィール取得を同時実行
    const profile = await getUserProfile();
    cachedProfile = profile;
    cacheTimestamp = now;
    
    // プロフィールをストアに保存(RootLayoutでの再取得を防ぐ)
    useAuthStore.getState().updateUser(profile);
    
    return loadNavigationConfig();
  } catch (error) {
    // 401の場合、インターセプターで既にログアウト&リダイレクト済み
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      throw redirect('/login');
    }
    throw error;
  }
}

export const router = createBrowserRouter([
  {
    id: 'app-root',
    path: '/',
    element: <RootLayout />,
    loader: authLoader,
    children: [
      // ...
    ],
  },
]);
```

**効果**:
- ルート遷移前にサーバー側認証状態を検証し、401の場合はコンポーネント描画前にログイン画面へリダイレクト
- 5秒間のキャッシュにより、同一セッション内での重複API呼び出しを削減

---

### **3. ProtectedRouteの改善**

現状の`ProtectedRoute`は`isAuthenticated`のみチェックしているため、以下を追加:

**JWTペイロード構造**(バックエンド `JwtService.java` より):
```json
{
  "iss": "self",
  "iat": 1700000000,
  "exp": 1700003600,
  "sub": "username",
  "roles": ["ROLE_USER", "ROLE_ADMIN"]
}
```

- `iss`: トークン発行者 ("self")
- `iat`: 発行日時 (Unix timestamp)
- `exp`: 有効期限 (Unix timestamp、デフォルト3600秒=1時間)
- `sub`: ユーザー名
- `roles`: 権限リスト

```typescript
// apps/frontend-v3/src/components/auth/ProtectedRoute.tsx

interface JwtPayload {
  iss: string;
  iat: number;
  exp: number;
  sub: string;
  roles: string[];
}

export function ProtectedRoute({ children, redirectTo = '/login' }: ProtectedRouteProps) {
  const { isAuthenticated, tokens } = useAuth();
  const location = useLocation();

  // トークン期限切れチェック(JWTデコード)
  const isTokenValid = useMemo(() => {
    if (!tokens?.accessToken) return false;
    try {
      // JWTは "header.payload.signature" の3部構成
      const parts = tokens.accessToken.split('.');
      if (parts.length !== 3) return false;
      
      // Base64URL デコード (padding追加が必要な場合あり)
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      
      const payload: JwtPayload = JSON.parse(jsonPayload);
      
      // exp は秒単位、Date.now() はミリ秒単位
      return payload.exp * 1000 > Date.now();
    } catch (error) {
      console.error('Failed to decode JWT:', error);
      return false;
    }
  }, [tokens]);

  if (!isAuthenticated || !isTokenValid) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
```

**効果**:
- **クライアント側でもトークン有効期限を確認**し、期限切れトークンでの画面描画を防止
- loaderが走らない場合(直接URLアクセス等)のフォールバック
- Base64URLデコード処理を正確に実装し、パースエラーを防止

---

### **4. エラーページ基盤の作成**

**方針**: アプリケーション全体で統一されたエラーハンドリングUXを提供。

#### **4.1 Forbidden Page (403)**

権限エラー専用ページ。将来的なロールベースアクセス制御(RBAC)実装を見据えた設計。

```tsx
// apps/frontend-v3/src/features/error/pages/ForbiddenPage.tsx

import { useLocation, useNavigate } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { ShieldX } from 'lucide-react';

export function ForbiddenPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="max-w-md text-center space-y-6 px-4">
        <div className="flex justify-center">
          <ShieldX className="size-20 text-warning" />
        </div>
        
        <div className="space-y-2">
          <h1 className="text-7xl font-bold text-warning">403</h1>
          <h2 className="text-2xl font-semibold text-foreground">アクセス権限がありません</h2>
        </div>
        
        <p className="text-muted-foreground leading-relaxed">
          {location.state?.message || 
            'このページへのアクセス権限がありません。管理者にお問い合わせください。'}
        </p>
        
        {user && (
          <div className="rounded-lg bg-surface-subtle p-4 text-sm text-muted-foreground">
            <p>現在のアカウント: <span className="font-medium text-foreground">{user.displayName}</span></p>
            <p className="text-xs mt-1">異なるアカウントでログインする必要がある場合があります</p>
          </div>
        )}
        
        <div className="flex flex-col gap-3 pt-4">
          <Button 
            variant="outline" 
            className="w-full"
            onClick={() => navigate(-1)}
          >
            前のページに戻る
          </Button>
          <Button 
            variant="ghost" 
            className="w-full"
            onClick={() => navigate('/')}
          >
            ホームに戻る
          </Button>
        </div>
      </div>
    </div>
  );
}
```

#### **4.2 Not Found Page (404)**

ルーティングエラー専用ページ。存在しないパスへのアクセス時に表示。

```tsx
// apps/frontend-v3/src/features/error/pages/NotFoundPage.tsx

import { useNavigate } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { SearchX } from 'lucide-react';

export function NotFoundPage() {
  const navigate = useNavigate();
  
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="max-w-md text-center space-y-6 px-4">
        <div className="flex justify-center">
          <SearchX className="size-20 text-muted-foreground" />
        </div>
        
        <div className="space-y-2">
          <h1 className="text-7xl font-bold text-muted-foreground">404</h1>
          <h2 className="text-2xl font-semibold text-foreground">ページが見つかりません</h2>
        </div>
        
        <p className="text-muted-foreground leading-relaxed">
          お探しのページは存在しないか、移動または削除された可能性があります。
        </p>
        
        <div className="flex flex-col gap-3 pt-4">
          <Button 
            className="w-full"
            onClick={() => navigate('/')}
          >
            ホームに戻る
          </Button>
          <Button 
            variant="outline" 
            className="w-full"
            onClick={() => navigate(-1)}
          >
            前のページに戻る
          </Button>
        </div>
      </div>
    </div>
  );
}
```

#### **4.3 Internal Server Error Page (500)**

予期しないエラーのフォールバック。React ErrorBoundaryやAPI 500エラー時に表示。

```tsx
// apps/frontend-v3/src/features/error/pages/InternalServerErrorPage.tsx

import { useNavigate, useRouteError } from 'react-router-dom';
import { Button } from '@mirel/ui';
import { AlertTriangle } from 'lucide-react';

export function InternalServerErrorPage() {
  const navigate = useNavigate();
  const error = useRouteError() as Error | null;
  
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="max-w-md text-center space-y-6 px-4">
        <div className="flex justify-center">
          <AlertTriangle className="size-20 text-destructive" />
        </div>
        
        <div className="space-y-2">
          <h1 className="text-7xl font-bold text-destructive">500</h1>
          <h2 className="text-2xl font-semibold text-foreground">サーバーエラー</h2>
        </div>
        
        <p className="text-muted-foreground leading-relaxed">
          申し訳ございません。予期しないエラーが発生しました。
          しばらく時間をおいて再度お試しください。
        </p>
        
        {import.meta.env.DEV && error && (
          <div className="rounded-lg bg-destructive/10 p-4 text-left">
            <p className="text-xs font-mono text-destructive break-all">
              {error.message}
            </p>
            {error.stack && (
              <pre className="mt-2 text-xs text-muted-foreground overflow-auto max-h-32">
                {error.stack}
              </pre>
            )}
          </div>
        )}
        
        <div className="flex flex-col gap-3 pt-4">
          <Button 
            className="w-full"
            onClick={() => window.location.reload()}
          >
            ページを再読み込み
          </Button>
          <Button 
            variant="outline" 
            className="w-full"
            onClick={() => navigate('/')}
          >
            ホームに戻る
          </Button>
        </div>
      </div>
    </div>
  );
}
```

#### **4.4 エクスポートとルート設定**

```typescript
// apps/frontend-v3/src/features/error/index.ts

export { ForbiddenPage } from './pages/ForbiddenPage';
export { NotFoundPage } from './pages/NotFoundPage';
export { InternalServerErrorPage } from './pages/InternalServerErrorPage';
```

**router.config.tsx**に追加:
```typescript
import { ForbiddenPage, NotFoundPage, InternalServerErrorPage } from '@/features/error';

export const router = createBrowserRouter([
  // エラーページ (静的ルート)
  {
    path: '/403',
    element: <ForbiddenPage />,
  },
  {
    path: '/500',
    element: <InternalServerErrorPage />,
  },
  
  // アプリケーションルート
  {
    id: 'app-root',
    path: '/',
    element: <RootLayout />,
    loader: authLoader,
    errorElement: <InternalServerErrorPage />, // React Router ErrorBoundary
    children: [
      // ... 既存のルート
      
      // 404 Catch-all (最後に配置)
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
```

**設計上の特徴**:
- **403**: 認証済みだが権限不足を示し、現在のアカウント情報を表示
- **404**: 存在しないルートへのアクセス時、Catch-allルート(`*`)で捕捉
- **500**: React Router `errorElement` で予期しないエラーを包括的にハンドリング
- **開発モード専用情報**: `import.meta.env.DEV` でエラー詳細とスタックトレースを表示
- **統一されたUX**: 全エラーページで一貫したレイアウトとアクション(ホームへ戻る等)

---

### **5. RootLayoutの`fetchProfile`エラーハンドリング改善**

現状は`console.error`のみで処理が続行されますが、以下に改善:

```typescript
// apps/frontend-v3/src/layouts/RootLayout.tsx

useEffect(() => {
  if (isAuthenticated) {
    fetchProfile().catch((error) => {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        // インターセプターで既にログアウト処理が実行されているはず
        // ここでは追加の処理は不要(無限ループ防止)
        return;
      }
      console.error('Failed to fetch profile', error);
    });
  }
}, [isAuthenticated, fetchProfile]);
```

---

## 📊 提案方式のフロー図

```
ユーザー: /promarker にアクセス
  ↓
[React Router loader]
  → authLoader() 実行
  → Zustand: isAuthenticated = true?
    → NO → /login へリダイレクト
    → YES → GET /users/me でサーバー検証
      → 401 → Interceptorでキャッチ → clearAuth() → /login へリダイレクト
      → 200 → プロフィール情報をキャッシュ&store保存 → 次へ
  ↓
[ProtectedRoute]
  → isAuthenticated && isTokenValid?
    → NO → /login へ Navigate
    → YES → 次へ
  ↓
[RootLayout]
  → fetchProfile() 実行(loaderでキャッシュ済みならスキップ)
    → 401 → Interceptorでキャッチ → clearAuth() → /login へリダイレクト
    → 200 → プロフィール情報を state に保存
  ↓
画面描画 (/promarker)
```

---

## ✅ 実装時の注意点

### **1. 無限ループ防止**
- インターセプター内で`window.location.href`を使用(React Routerの`navigate`は使用不可)
- `/login`ルートは認証チェックをスキップ
- RootLayoutでは、インターセプターでログアウト済みの場合は追加処理を行わない

### **2. リダイレクトループ回避**
- ログイン画面で`returnUrl`パラメータを受け取り、ログイン成功後に元のページへ戻す
- ログアウト時は`returnUrl`をクリアしてホームページへ遷移

### **3. E2Eテスト影響**
- ログイン後のリダイレクト先変更に対応
- 401発生時の挙動変更(エラーメッセージ表示→即座にリダイレクト)
- テストアカウントのトークン有効期限を十分長く設定

### **4. ログアウト時の即時状態クリア**
`logout()`関数内で、状態クリア→API呼び出し→リダイレクトの順で実行:

```typescript
// authStore.ts
logout: async () => {
  const { tokens } = get();
  
  // 1. ローカル状態を即座にクリア
  set({ user: null, currentTenant: null, tokens: null, 
        tenants: [], licenses: [], isAuthenticated: false });
  
  // 2. バックエンドへログアウト通知(ベストエフォート)
  try {
    await authApi.logout(tokens?.refreshToken);
  } catch (error) {
    console.warn('Logout API call failed', error);
  }
  
  // 3. ログイン画面へリダイレクト
  window.location.href = '/login';
},
```

### **5. JWTペイロード検証の堅牢性**
- クロックスキュー対応: `exp`チェック時に5秒のバッファを持たせる
- 将来的な拡張: トークン期限が近い場合の自動リフレッシュ機構

### **6. セキュリティ考慮事項**
- JWTはZustand永続化機構(sessionStorage推奨)に保存
- CSRF対策はバックエンドで実装済み
- ログアウト時にrefreshTokenを無効化しトークン盗難時の被害を最小化

---

## 🎬 実装ロードマップ

以下の順序で段階的に実装:

### **Phase 1: 基盤整備** (優先度: 高)
1. ✅ JWTペイロード構造の確認(完了)
2. Axios Interceptorの401ハンドリング強化
3. エラーページ基盤の作成
   - `ForbiddenPage` (403)
   - `NotFoundPage` (404)
   - `InternalServerErrorPage` (500)
4. React Router `errorElement` 設定

### **Phase 2: 認証フロー改善** (優先度: 高)
5. `ProtectedRoute`へのトークン期限チェック追加
6. `authLoader`の実装と`router.config.tsx`への適用
7. `RootLayout`のエラーハンドリング改善
8. `logout()`関数の即時状態クリア対応

### **Phase 3: UX向上** (優先度: 中)
9. `returnUrl`機構の実装とログイン画面への統合
10. ログアウト確認ダイアログの削除(即座にリダイレクト)
11. エラーページのデザイン洗練化
    - アクセシビリティ対応(ARIA属性、フォーカス管理)
    - アニメーション追加(エラーアイコンのフェードイン等)
    - ダークモード対応の検証

### **Phase 4: テスト・検証** (優先度: 高)
12. E2Eテストの調整
    - ログインフロー、401エラーハンドリング
    - 404ページの表示確認(存在しないルートへのアクセス)
    - 500ページの表示確認(意図的にエラーを発生させる)
13. 手動テスト
    - セッション切れ、ログアウト、権限エラー
    - 各エラーページからの遷移動作
    - 開発モードでのエラー詳細表示確認
14. パフォーマンス検証(loaderのキャッシュ効果測定)
15. アクセシビリティ検証(スクリーンリーダー、キーボード操作)
