# Phase 1: Backend JWT 基盤 & セキュリティ設定 - 作業ログ

## 概要
バックエンドの認証基盤を JWT (Stateless) と Session (Stateful) で切り替え可能にするための実装を行った。
また、CSRF対策およびゲストモード（セキュリティ無効化）の挙動を検証した。

## 実装内容

### 1. 設定プロパティの追加
- `jp.vemi.mirel.config.properties.AuthProperties` を作成。
- `auth.method` (jwt/session), `auth.jwt.*` プロパティを定義。

### 2. 認証サービスの抽象化
- `AuthenticationService` インターフェースに対し、`JwtAuthenticationService` と `SessionAuthenticationService` を実装。
- `@ConditionalOnProperty` により、起動時にどちらか一方の Bean が有効化されるように構成。

### 3. SecurityFilterChain の構成変更
- `WebSecurityConfig` にて `auth.method` の値を参照し、`oauth2ResourceServer` (JWT) または `formLogin` (Session) を切り替え。
- CSRFトークンを Cookie (`XSRF-TOKEN`) に書き込むための `CsrfCookieFilter` を追加（Spring Security 6 の遅延ロード対応）。

### 4. テスト実装
- `JwtAuthenticationTest`: JWTモード時の 401 応答、CSRFトークン生成を確認。
- `GuestModeTest`: `mipla2.security.api.enabled=false` 時のアクセス許可を確認。

## 検証結果

### JWTモード (`auth.method=jwt`)
- 未認証リクエストに対し `401 Unauthorized` が返却されることを確認。
- `/auth/health` などのパブリックエンドポイントで `XSRF-TOKEN` クッキーが発行されることを確認。

### Guestモード (`mipla2.security.api.enabled=false`)
- 保護リソース（存在しないパスで代用）に対し、認証エラー（401/403）にならず、404 Not Found が返ることを確認（Spring Security を通過）。

## 課題・備考
- 開発環境 (`application-dev.yml`) では `mipla2.security.api.csrf-enabled: false` がデフォルトとなっているため、CSRFテスト時は `@SpringBootTest(properties = "mipla2.security.api.csrf-enabled=true")` で上書きが必要。
- `CsrfCookieFilter` は SPA フロントエンドが初回アクセス時に CSRF トークンを取得するために必須。

## 次のステップ
- Phase 2: フロントエンド (React) 側の JWT 認証フロー実装。
