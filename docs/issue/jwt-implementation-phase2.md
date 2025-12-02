# Phase 2: パスワードログイン JWT 化 & `/users/me` 連携 - 作業ログ

## 概要
パスワードログイン (`/auth/login`) のレスポンスを JWT 対応形式に変更し、
取得したトークンを用いて `/users/me` 系のエンドポイントからユーザ情報を取得できることを検証した。

## 実装内容

### 1. `AuthenticationServiceImpl` の改修
- `AuthProperties` を利用するように変更（`@Value` の廃止）。
- JWT 有効時 (`auth.method=jwt`) に `JwtService` を使用してアクセストークンを生成。
- `AuthenticationResponse` に `expiresIn` を動的に設定。

### 2. `UserProfileController` の確認
- `/users/me`, `/users/me/tenants`, `/users/me/licenses` が実装済みであることを確認。
- `ExecutionContext` 経由で認証済みユーザ情報を取得する実装となっている。

### 3. 統合テストの実装 (`JwtLoginIntegrationTest`)
- 以下のシナリオを検証するテストを作成:
    1. `/auth/login` に POST し、アクセストークンを取得。
    2. 取得したトークンを `Authorization: Bearer` ヘッダに付与して `/users/me` にアクセス → 200 OK & ユーザ情報取得。
    3. 同様に `/users/me/tenants` にアクセス → 200 OK & テナント一覧取得。
    4. 同様に `/users/me/licenses` にアクセス → 200 OK & ライセンス一覧取得。
    5. `/auth/me` (Legacy) にアクセス → 200 OK & ユーザコンテキスト取得。

## 検証結果
- `JwtLoginIntegrationTest` が **PASS** した。
- これにより、バックエンドは JWT を発行し、その JWT を用いて保護リソース (`/users/me` 等) にアクセス可能な状態となった。

## 次のステップ
- Phase 3: フロント認証クライアント（`authStore` / `apiClient`）の JWT 対応。
