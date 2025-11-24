# 401エラーハンドリング - バグ修正分析

## 報告された問題

1. **ログアウトボタン押下で、ログイン画面がチラつく** - 描画後に再描画が意図せず走っている
2. **ログアウト時に「アクセスするにはログインが必要」メッセージが一瞬表示される**
3. **ログアウト状態でpromarkerを直叩きした場合、上記メッセージが表示されない**
4. **ProMarkerにアクセスしても users/me が呼ばれていない**

---

## 原因分析

### 問題1 & 2: ログアウト時のチラつきと不要なメッセージ

**根本原因**: 
- `authStore.logout()` が `window.location.href = '/login'` でリダイレクト
- しかし `ProtectedRoute` が状態クリア後も一瞬レンダリングされる
- `ProtectedRoute` が `<Navigate>` で `/login` へリダイレクトし、`state.message` を設定
- 結果: 2回のリダイレクト + 不要なメッセージ表示

**フロー**:
```
ログアウトボタン押下
↓
authStore.logout() → clearAuth()
↓
ProtectedRoute が再レンダリング（isAuthenticated=false）
↓
<Navigate to="/login" state={{ message: "..." }} />  ← 問題!
↓
window.location.href = '/login' （authStore.logout内）
↓
ログイン画面でメッセージ表示 → 即座に state クリア
↓
画面がチラつく
```

### 問題3: promarker直叩き時にメッセージが表示されない

**根本原因**:
- `authLoader` が先に実行され、`throw redirect('/login')` でリダイレクト
- `ProtectedRoute` はレンダリングされない
- `state.message` が設定されない

**フロー**:
```
/promarker に直接アクセス（未認証）
↓
authLoader 実行
↓
isAuthenticated=false → throw redirect('/login')  ← state なし
↓
ログイン画面表示（メッセージなし）
```

### 問題4: ProMarkerアクセス時に users/me が呼ばれない

**根本原因**:
- `authLoader` は **ルートレベル** (`/`) にのみ設定されている
- `/promarker` は `ProtectedRoute` でラップされた子ルートなので、親の loader が実行される
- しかし、親は既に実行済みでキャッシュヒット → API呼び出しなし

**router.config.tsx の構造**:
```tsx
{
  path: '/',
  loader: authLoader,  // ← ここでのみ実行
  children: [
    {
      element: <ProtectedRoute><Outlet /></ProtectedRoute>,
      children: [
        { path: 'promarker', element: <ProMarkerPage /> }  // ← loaderなし
      ]
    }
  ]
}
```

**問題点**:
- 初回アクセス時は `authLoader` が `/` で実行され、`users/me` が呼ばれる
- しかし `/promarker` に直接アクセスした場合、親の loader が実行されるべきだが、キャッシュが効いてスキップされる可能性がある
- または、`/` にアクセスしていないため loader 自体が実行されていない

---

## 修正方針

### 修正1: ログアウト時は location.state を使わない

**authStore.logout()**:
- `window.location.href = '/login'` のみでリダイレクト
- `state.message` を設定しない

**ProtectedRoute**:
- ログアウト操作からのリダイレクトは `authStore` が処理するため、`state.message` を設定しない
- **ただし**: 自然な未認証アクセス（直リンク等）では `state.message` を設定すべき
- 判別方法: `isAuthenticated` が `false` かつ `tokens` が存在しない場合は自然な未認証

### 修正2: authLoader を適切に配置

**Option A: ProtectedRoute 内で loader を使用**
- `ProtectedRoute` を `loader` 付きのルートに変更
- 問題: `ProtectedRoute` はコンポーネントであり、ルート定義ではない

**Option B: 各保護ルートに個別に loader を設定**
- `/promarker`, `/settings/*` 等に個別に `authLoader` を設定
- 問題: 重複コードが増える

**Option C: 親ルート (`/`) の loader を保持し、キャッシュロジックを改善**
- 現状のまま、キャッシュキーに URL を含める
- ページ遷移時に必ず `authLoader` が実行されることを保証

### 修正3: ログアウト時の状態クリアタイミングを調整

**現状**:
```typescript
logout: async () => {
  // 1. 状態クリア
  set({ isAuthenticated: false, ... });
  // 2. API呼び出し
  // 3. リダイレクト
  window.location.href = '/login';
}
```

**問題**: 状態クリア後、React が再レンダリングし、`ProtectedRoute` が `<Navigate>` を返す

**解決策**: リダイレクトを同期的に行い、React の再レンダリングをスキップ
```typescript
logout: async () => {
  const { tokens } = get();
  
  // 1. 即座にリダイレクト（Reactの再レンダリングを防ぐ）
  window.location.href = '/login';
  
  // 2. ローカル状態をクリア（リダイレクト後に実行されるが、念のため）
  set({ isAuthenticated: false, ... });
  
  // 3. バックエンドへログアウト通知（ベストエフォート）
  try {
    await authApi.logout(tokens?.refreshToken);
  } catch (error) {
    console.warn('Logout API call failed', error);
  }
}
```

ただし、これでは `window.location.href` の設定後もコードが実行され続けるため、完全ではない。

**より良い解決策**: `window.location.replace()` を使用し、履歴を残さない
```typescript
logout: () => {
  const { tokens } = get();
  
  // 1. 状態クリア
  set({ isAuthenticated: false, ... });
  
  // 2. バックエンドへログアウト通知（非同期だが待たない）
  authApi.logout(tokens?.refreshToken).catch(() => {});
  
  // 3. 即座にリダイレクト
  window.location.replace('/login');
}
```

---

## 推奨修正内容

### 1. authStore.logout() の修正

```typescript
logout: () => {
  const { tokens } = get();
  
  // 1. ローカル状態を即座にクリア
  set({ 
    user: null, 
    currentTenant: null, 
    tokens: null, 
    tenants: [],
    licenses: [],
    isAuthenticated: false 
  });
  
  // 2. バックエンドへログアウト通知(非同期・待たない)
  authApi.logout(tokens?.refreshToken).catch((error) => {
    console.warn('Logout API call failed', error);
  });
  
  // 3. ログイン画面へリダイレクト（履歴を残さない）
  window.location.replace('/login');
},
```

**変更点**:
- `async` を削除（同期関数に）
- `await` を削除し、ログアウトAPIは非同期で実行（待たない）
- `window.location.href` → `window.location.replace()` に変更

### 2. ProtectedRoute の修正

**現状の問題**: すべての未認証アクセスで `state.message` を設定
**修正**: ログアウト操作からのリダイレクトでは message を設定しない

```typescript
export function ProtectedRoute({ children, redirectTo = '/login' }: ProtectedRouteProps) {
  const { isAuthenticated, tokens } = useAuth();
  const location = useLocation();

  const isTokenValid = useMemo(() => {
    // ... 既存のロジック
  }, [tokens]);

  if (!isAuthenticated || !isTokenValid) {
    // ログアウト操作からのリダイレクトかどうかを判別
    // ログアウト時は window.location.replace で直接遷移するため、
    // このコンポーネントは実行されないはず
    // しかし念のため、message は常に設定しない（LoginPage側で判断）
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
```

**変更点**:
- `state.message` を削除
- `state.from` のみを保持（returnUrl機構のため）

### 3. LoginPage の修正

**メッセージ表示ロジックの改善**:
- `returnUrl` パラメータがある場合のみメッセージを表示
- ログアウト操作からの遷移（URLパラメータなし）ではメッセージを表示しない

```typescript
useEffect(() => {
  const returnUrl = searchParams.get('returnUrl');
  
  // returnUrlがある場合のみメッセージを表示
  if (returnUrl && !toastShownRef.current) {
    toast({
      variant: "destructive",
      title: "認証が必要です",
      description: "このページにアクセスするにはログインが必要です。",
    });
    toastShownRef.current = true;
  }
}, [searchParams, toast]);
```

### 4. authLoader の修正

**キャッシュキーに URL を含める**:

```typescript
let cachedData: { profile: unknown; navigation: NavigationConfig } | null = null;
let cacheKey = '';
let cacheTimestamp = 0;
const CACHE_DURATION = 5000; // 5秒

async function authLoader(): Promise<NavigationConfig> {
  const { isAuthenticated, tokens, updateUser } = useAuthStore.getState();
  
  if (!isAuthenticated || !tokens?.accessToken) {
    // 現在のパスをreturnUrlとしてリダイレクト
    const currentPath = window.location.pathname + window.location.search;
    if (currentPath !== '/' && currentPath !== '/login') {
      throw redirect(`/login?returnUrl=${encodeURIComponent(currentPath)}`);
    }
    throw redirect('/login');
  }
  
  try {
    const now = Date.now();
    const currentKey = window.location.pathname;
    
    // キャッシュが有効かつ同じURLならスキップ
    if (cachedData && cacheKey === currentKey && (now - cacheTimestamp) < CACHE_DURATION) {
      return cachedData.navigation;
    }
    
    // /users/me で認証検証とプロフィール取得を同時実行
    const profile = await getUserProfile();
    const navigation = await loadNavigationConfig();
    
    cachedData = { profile, navigation };
    cacheKey = currentKey;
    cacheTimestamp = now;
    
    // プロフィールをストアに保存
    updateUser(profile);
    
    return navigation;
  } catch (error) {
    // 401の場合、インターセプターで既にログアウト&リダイレクト済み
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      throw redirect('/login');
    }
    throw error;
  }
}
```

---

## デバッグスクリプトの使い方

1. ブラウザで `http://localhost:5173` を開く
2. 開発者ツール（F12）→ Console タブ
3. `/docs/issue/#40/JWT/debug-console-script.js` の内容をコピー&ペースト
4. 以下の操作を順に実行:
   - ログイン
   - ProMarkerにアクセス
   - ログアウト
   - 再度ProMarkerに直接アクセス
5. コンソールログを確認し、以下を検証:
   - `[API Call] /users/me` が適切なタイミングで呼ばれているか
   - `[Navigation]` のログで不要なリダイレクトが発生していないか
   - `[AuthStore State]` でタイミングよく状態がクリアされているか

---

## テスト手順

### テスト1: ログアウト操作
1. ログイン
2. ログアウトボタンをクリック
3. **期待結果**:
   - チラつきなし
   - メッセージ表示なし
   - `/login` に即座にリダイレクト

### テスト2: 未認証でProMarkerアクセス
1. ログアウト状態
2. `/promarker` に直接アクセス
3. **期待結果**:
   - `/login?returnUrl=%2Fpromarker` にリダイレクト
   - 「認証が必要です」メッセージが表示される
   - ログイン後、`/promarker` にリダイレクト

### テスト3: ProMarkerでのusers/me呼び出し
1. ログイン
2. `/promarker` にアクセス
3. DevTools Network タブで確認
4. **期待結果**:
   - `GET /mapi/users/me` が呼ばれる（初回）
   - 5秒以内の再アクセスではキャッシュが使用される

---

**作成日**: 2025-11-24  
**ステータス**: 修正案作成完了、実装待ち
