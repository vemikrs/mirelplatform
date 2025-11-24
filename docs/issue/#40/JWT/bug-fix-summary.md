# 401エラーハンドリング - バグ修正サマリー

## 修正内容

以下の4つの問題を修正しました:

### 問題1 & 2: ログアウト時のチラつきと不要なメッセージ

**原因**: 
- `logout()` が `async` で `window.location.href` を使用
- 状態クリア後にReactが再レンダリングし、`ProtectedRoute` が `<Navigate>` を返す
- 結果として2回のリダイレクトが発生

**修正**:
- `logout()` を同期関数に変更
- `window.location.replace()` を使用（履歴を残さない）
- ログアウトAPIは非同期で実行（待たない）

**変更ファイル**: `apps/frontend-v3/src/stores/authStore.ts`

```typescript
// 修正前
logout: async () => {
  // ...
  await authApi.logout(tokens?.refreshToken);
  window.location.href = '/login';
}

// 修正後
logout: () => {
  // ...
  authApi.logout(tokens?.refreshToken).catch(() => {});
  window.location.replace('/login');
}
```

### 問題3: 未認証でpromarker直叩き時にメッセージが表示されない

**原因**:
- `ProtectedRoute` が常に `state.message` を設定
- しかし `authLoader` が先に実行され、`state` なしでリダイレクト
- LoginPageでは `location.state.message` を確認していたため表示されない

**修正**:
- `ProtectedRoute` から `state.message` を削除
- `authLoader` で `returnUrl` パラメータを設定
- LoginPageで `returnUrl` の有無でメッセージ表示を判断

**変更ファイル**:
- `apps/frontend-v3/src/components/auth/ProtectedRoute.tsx`
- `apps/frontend-v3/src/app/router.config.tsx`
- `apps/frontend-v3/src/features/auth/pages/LoginPage.tsx`

```typescript
// ProtectedRoute: state.message削除
return <Navigate to={redirectTo} state={{ from: location }} replace />;

// authLoader: returnUrlパラメータを設定
if (currentPath !== '/' && currentPath !== '/login') {
  throw redirect(`/login?returnUrl=${encodeURIComponent(currentPath)}`);
}

// LoginPage: returnUrlの有無で判断
useEffect(() => {
  const returnUrl = searchParams.get('returnUrl');
  if (returnUrl && !toastShownRef.current) {
    toast({ ... });
  }
}, [searchParams, toast]);
```

### 問題4: ProMarkerアクセス時にusers/meが呼ばれない

**原因**:
- キャッシュキーがURLを含んでいない
- 異なるページ間でキャッシュが共有され、API呼び出しがスキップされる

**修正**:
- キャッシュキーに `window.location.pathname` を含める
- ページ遷移時に必ず `authLoader` が実行され、URLが変わればAPIを再呼び出し

**変更ファイル**: `apps/frontend-v3/src/app/router.config.tsx`

```typescript
// 修正前
let cachedProfile: unknown = null;
if (cachedProfile && (now - cacheTimestamp) < CACHE_DURATION) {
  return loadNavigationConfig();
}

// 修正後
let cachedData: { profile: unknown; navigation: NavigationConfig } | null = null;
let cacheKey = '';

const currentKey = window.location.pathname;
if (cachedData && cacheKey === currentKey && (now - cacheTimestamp) < CACHE_DURATION) {
  return cachedData.navigation;
}
```

---

## テスト確認ポイント

### 1. ログアウト操作
- [ ] ログアウトボタンクリック時にチラつかない
- [ ] 「アクセスするにはログインが必要」メッセージが表示されない
- [ ] `/login` に即座にリダイレクトされる

### 2. 未認証でProMarkerアクセス
- [ ] `/login?returnUrl=%2Fpromarker` にリダイレクトされる
- [ ] 「認証が必要です」メッセージが表示される
- [ ] ログイン後、`/promarker` にリダイレクトされる

### 3. ProMarkerでのAPI呼び出し
- [ ] `/promarker` アクセス時に `GET /mapi/users/me` が呼ばれる
- [ ] DevTools Network タブで確認できる
- [ ] 5秒以内の同一ページ再アクセスではキャッシュが使用される

### 4. ページ遷移時のAPI呼び出し
- [ ] `/` → `/promarker` の遷移時に `users/me` が呼ばれる
- [ ] `/promarker` → `/settings/profile` の遷移時も呼ばれる
- [ ] キャッシュが効いている場合（5秒以内、同一URL）は呼ばれない

---

## デバッグ方法

### 開発者ツールコンソールでデバッグスクリプトを実行

1. ブラウザで `http://localhost:5173` を開く
2. F12で開発者ツールを開く
3. Consoleタブを選択
4. `/docs/issue/#40/JWT/debug-console-script.js` の内容をコピー&ペースト
5. 以下の操作を実行:
   - ログイン → ProMarkerアクセス → ログアウト → 再度ProMarker直アクセス
6. コンソールログを確認:
   ```
   [API Call] /users/me requested
   [Navigation] pushState/replaceState
   [AuthStore State] isAuthenticated: true/false
   ```

### 停止方法
```javascript
stopDebug()
```

---

## 関連ドキュメント

- 問題分析: `/docs/issue/#40/JWT/bug-fix-analysis.md`
- デバッグスクリプト: `/docs/issue/#40/JWT/debug-console-script.js`
- 実装サマリー: `/docs/issue/#40/JWT/implementation-summary.md`
- 手動テストガイド: `/docs/issue/#40/JWT/manual-test-guide.md`

---

**修正日**: 2025-11-24  
**ステータス**: 実装完了、動作確認待ち  
**コミット**: 未実施（承認待ち）
