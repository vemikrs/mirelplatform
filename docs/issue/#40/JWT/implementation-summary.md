# HTTPステータス401挙動改善 - 実装サマリー

## 概要

このドキュメントは、Issue #40で実装した401エラーハンドリング改善の実装サマリーです。
提案ドキュメント(`fix-httpstatus-401-behavior.md`)に基づき、Phase 1~3の実装を完了しました。

## 実装完了項目

### Phase 1: 基盤整備

#### 1.1 Axios Interceptorの401ハンドリング強化
- **ファイル**: `apps/frontend-v3/src/lib/api/client.ts`
- **実装内容**:
  - 401エラーを全域でキャッチし、`clearAuth()`を実行
  - 現在のパスを`returnUrl`としてログインページへリダイレクト
  - `/login`や`/auth/*`パスでは無限ループ防止のためリダイレクトをスキップ
  - 動的インポートで`authStore`を取得し、循環依存を回避

#### 1.2 エラーページ基盤の作成
- **ファイル**: 
  - `apps/frontend-v3/src/features/error/pages/ForbiddenPage.tsx` (403)
  - `apps/frontend-v3/src/features/error/pages/NotFoundPage.tsx` (404)
  - `apps/frontend-v3/src/features/error/pages/InternalServerErrorPage.tsx` (500)
  - `apps/frontend-v3/src/features/error/index.ts`
- **実装内容**:
  - 各HTTPステータスコードに対応した専用ページ
  - 統一されたデザインとUX（ホームへ戻る、前のページへ戻る）
  - 開発モードでのエラー詳細表示（スタックトレース含む）
  - アクセシビリティ対応（後述）

#### 1.3 React Router errorElement設定
- **ファイル**: `apps/frontend-v3/src/app/router.config.tsx`
- **実装内容**:
  - `/403`、`/500`を静的ルートとして追加
  - アプリケーションルート(`/`)に`errorElement`を設定し、予期しないエラーをキャッチ
  - Catch-allルート(`*`)で404ページを表示

### Phase 2: 認証フロー改善

#### 2.1 ProtectedRouteへトークン期限チェック追加
- **ファイル**: `apps/frontend-v3/src/components/auth/ProtectedRoute.tsx`
- **実装内容**:
  - JWTペイロードをデコードし、`exp`(有効期限)をチェック
  - 5秒のバッファを持たせてクロックスキュー対策
  - トークンが無効または期限切れの場合はログインページへリダイレクト
  - Base64URLデコード処理を正確に実装し、パースエラーを防止

**JWTペイロード構造**(バックエンド `JwtService.java` 準拠):
```json
{
  "iss": "self",
  "iat": 1700000000,
  "exp": 1700003600,
  "sub": "username",
  "roles": ["ROLE_USER", "ROLE_ADMIN"]
}
```

#### 2.2 authLoader実装とrouter.config.tsx適用
- **ファイル**: `apps/frontend-v3/src/app/router.config.tsx`
- **実装内容**:
  - React Router `loader`を使用し、ルート遷移前にサーバー側認証状態を検証
  - `/users/me`を呼び出し、プロフィール情報を取得
  - 5秒間のキャッシュで重複API呼び出しを削減
  - 401エラーの場合はログインページへリダイレクト
  - 取得したプロフィールを`authStore`に保存し、`RootLayout`での再取得を防ぐ

#### 2.3 RootLayoutエラーハンドリング改善
- **ファイル**: `apps/frontend-v3/src/layouts/RootLayout.tsx`
- **実装内容**:
  - `fetchProfile()`のエラーハンドリングを改善
  - 401エラーの場合、インターセプターで既にログアウト処理済みのため追加処理を行わない
  - 無限ループ防止

#### 2.4 logout関数の即時状態クリア対応
- **ファイル**: `apps/frontend-v3/src/stores/authStore.ts`
- **実装内容**:
  - 状態クリア → API呼び出し → リダイレクトの順で実行
  - バックエンドへのログアウト通知はベストエフォート
  - API呼び出しが失敗してもログアウト処理を継続

### Phase 3: UX向上

#### 3.1 returnUrl機構の実装
- **ファイル**: `apps/frontend-v3/src/features/auth/pages/LoginPage.tsx`
- **実装内容**:
  - `useSearchParams`で`returnUrl`パラメータを取得
  - ログイン成功後、`returnUrl` → `location.state.from` → `/`の優先順位でリダイレクト
  - 401インターセプターで設定した`returnUrl`を尊重

#### 3.2 エラーページのアクセシビリティ対応
- **ファイル**: `apps/frontend-v3/src/features/error/pages/*.tsx`
- **実装内容**:
  - `role="main"`でメインコンテンツを明示
  - `tabIndex={-1}`と`useRef`で見出しにフォーカス管理
  - `aria-label`でボタンの役割を明示
  - `aria-hidden="true"`でアイコンを支援技術から隠蔽
  - `role="status"`/`role="alert"`でステータス情報を伝達
  - `aria-live="polite"`でエラー詳細の動的更新を通知
  - ナビゲーション領域に`<nav>`タグと`aria-label`を追加

## テスト結果

### 型チェック
- `npx tsc --noEmit`: ✅ エラーなし

### ESLint
- `pnpm --filter frontend-v3 lint`: ⚠️ 既存の軽微な警告のみ（実装に影響なし）

### E2Eテスト
- `pnpm --filter e2e test auth-smoke`: ✅ 14件パス、9件スキップ（意図的）
- 認証フロー、ページ遷移、OAuth2コールバックが正常に動作

### 手動テスト（実施予定）
- [ ] セッション切れ時の401エラーハンドリング
- [ ] ログアウト後の即時リダイレクト
- [ ] returnUrl経由のログイン後リダイレクト
- [ ] 各エラーページ(403/404/500)の表示とナビゲーション
- [ ] アクセシビリティ(スクリーンリーダー、キーボード操作)

## アーキテクチャ上の決定事項

### 1. 多層防御アプローチ
以下の5層で認証とエラーハンドリングを実施:
1. **Axios Interceptor**: 全401エラーを捕捉し、強制ログアウト
2. **authLoader**: ルート遷移前にサーバー側認証状態を検証
3. **ProtectedRoute**: クライアント側トークン有効期限チェック
4. **エラーページ**: 403/404/500専用の明示的なエラー画面
5. **ErrorBoundary**: 予期しないエラーの包括的なハンドリング

### 2. 無限ループ防止策
- Interceptor内で`window.location.pathname`をチェックし、`/login`や`/auth/*`では再リダイレクトしない
- `RootLayout`では、401エラー時にInterceptorで既に処理済みと判断し、追加処理を行わない

### 3. キャッシュ戦略
- `authLoader`内で5秒間のプロフィールキャッシュを実装
- 同一セッション内での重複API呼び出しを削減し、パフォーマンスを向上

### 4. エラーページの設計哲学
- **401**: 専用ページ不要（Interceptorで即座にリダイレクト）
- **403**: 権限エラー専用、現在のアカウント情報を表示
- **404**: 存在しないルート用、Catch-allルートで捕捉
- **500**: 予期しないエラー用、開発モードでスタックトレース表示

## セキュリティ考慮事項

1. **JWTの保存先**: Zustand永続化機構（sessionStorage推奨）
2. **CSRF対策**: バックエンドで実装済み
3. **トークン無効化**: ログアウト時にrefreshTokenを無効化
4. **クロックスキュー対策**: JWT有効期限チェック時に5秒のバッファ

## 既知の制限事項

1. **トークン自動リフレッシュ**: 未実装（将来的な拡張予定）
2. **RBAC権限チェック**: ProtectedRouteでコメントアウト（Phase 6以降で実装予定）
3. **OTP認証フローのE2E**: 一部スキップ（Zustand store直接操作が必要）

## コミット履歴

1. `82e5406`: feat(frontend): Phase 1 - 401インターセプター、エラーページ基盤、ErrorBoundary設定 (refs #40)
2. `de9fe24`: feat(frontend): Phase 2 - JWT期限チェック、authLoader、エラーハンドリング改善 (refs #40)
3. `29a636d`: feat(frontend): Phase 3 - returnUrl機構、エラーページアクセシビリティ改善 (refs #40)
4. `a2a6c7e`: fix(frontend): ESLintエラー修正 - JWT decode型チェック、Date.now()呼び出し位置 (refs #40)

## 次のステップ（Phase 4以降）

### Phase 4: テスト・検証（本文書作成時点で進行中）
- [x] E2Eテストの調整と実行
- [ ] 手動テスト（ブラウザでの動作確認）
- [ ] パフォーマンス検証（loaderキャッシュ効果測定）
- [ ] アクセシビリティ検証（スクリーンリーダー、キーボード操作）

### Phase 5以降（提案ドキュメント外）
- トークン自動リフレッシュ機構
- RBAC権限チェックの実装
- エラーページのアニメーション追加
- ダークモード対応の検証

## 参考資料

- 提案ドキュメント: `docs/issue/#40/JWT/fix-httpstatus-401-behavior.md`
- React Router v6 Loaders: https://reactrouter.com/en/main/route/loader
- JWT Best Practices: https://tools.ietf.org/html/rfc8725
- WCAG 2.1 Level AA: https://www.w3.org/WAI/WCAG21/quickref/

---

**作成日**: 2025-11-24  
**作成者**: GitHub Copilot  
**ステータス**: Phase 1~3 実装完了、Phase 4 進行中
