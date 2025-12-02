# Phase 3: Frontend Authentication Client (authStore / apiClient) JWT Support

## 概要
Frontend (`apps/frontend-v3`) の認証クライアントとストアを更新し、Backend の JWT 認証基盤と連携できるようにしました。

## 変更内容

### 1. API Client (`apps/frontend-v3/src/lib/api/client.ts`)
- `setTokenProvider` を追加し、外部（Zustand Store）からトークン取得ロジックを注入可能にしました。
- Request Interceptor で `Authorization: Bearer ${token}` を自動付与するように変更しました。
- 循環参照を回避するため、Store の直接インポートを廃止しました。

### 2. API Wrappers
- **`apps/frontend-v3/src/lib/api/auth.ts` (新規作成)**
  - `login`, `register`, `logout`, `switchTenant`, `refreshToken` を実装。
  - Backend DTO に合わせた型定義 (`LoginRequest`, `AuthenticationResponse` 等) を追加。
- **`apps/frontend-v3/src/lib/api/userProfile.ts`**
  - `getUserProfile` 等の関数から `accessToken` 引数を削除（Interceptor が処理するため）。
  - `UserProfile` インターフェースに `username` を追加。

### 3. Auth Store (`apps/frontend-v3/src/stores/authStore.ts`)
- `authApi` を使用するように `login`, `signup`, `logout` アクションを更新。
- `switchTenant` を `authApi` 経由に変更。
- `fetchProfile` アクションを追加。
- 型定義を `api/auth.ts` および `api/userProfile.ts` のものに統一。

### 4. Backend DTO Alignment
- **`backend/.../UserProfileDto.java`**: `username` フィールドを追加。
- **`backend/.../UserProfileService.java`**: `username` をマッピングするように修正。

### 5. Component Updates
- **`apps/frontend-v3/src/main.tsx`**: アプリ起動時に `setTokenProvider` を初期化し、`useAuthStore` と連携。
- **`apps/frontend-v3/src/features/auth/pages/LoginPage.tsx`**: `login` アクションの呼び出し引数をオブジェクト形式に変更。
- **`apps/frontend-v3/src/hooks/useAuth.ts`**: 未使用変数を削除し、Lint エラーを修正。

### 6. トラブルシューティング: APIレスポンス形式の不整合修正
- **事象**: UX打鍵時にログインが失敗する（成功レスポンスなのにエラー扱いされる）。
- **原因**: バックエンドの認証系API（`AuthenticationController`, `UserProfileController`）が `ApiResponse` ラッパーを使用せず、直接 DTO を返していたのに対し、フロントエンドの API クライアントが `ApiResponse` ラッパーを期待していたため。
- **対応**: `apps/frontend-v3/src/lib/api/auth.ts` および `userProfile.ts` を修正し、`ApiResponse` ラッパーを介さずにレスポンスデータを直接読み取るように変更しました。

### 7. テストデータ修正
- **事象**: ログイン後のプロファイル画面で「ユーザー名」が表示されない。
- **原因**: 開発用初期データ投入処理 (`DatabaseUtil.java`) で `User` エンティティの `username` フィールドが設定されていなかった。
- **対応**: `DatabaseUtil.initializeSaasTestData` を修正し、`adminUser` に "admin"、`regularUser` に "user" を設定。H2 データベースをリセットして再投入を実施。

## 検証
- `pnpm --filter frontend-v3 lint` を実行し、関連する型エラーがないことを確認しました。
- 既存の Lint エラー（React Hooks 関連など）は今回の変更とは無関係であることを確認済み。

## 次のステップ
- E2E テストによるログインフローの動作確認。
- 実際のブラウザでの動作確認。
