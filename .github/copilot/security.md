# Security & Authentication

## スコープ
認証・認可・暗号化に関わるバックエンド実装とフロントエンド連携。`backend/src/main/java/jp/vemi/mirel/security/` 配下と `foundation/web/api/auth/` が主な対象。

## 認証方式

本システムは **JWT (RS256)** と **セッション (OTP/OAuth2フォールバック)** のデュアルモードを持つ。`auth.method` プロパティで切り替え。

### JWT モード（推奨・本番用）
- **署名アルゴリズム**: RS256（HS256 フォールバックあり）
- **鍵管理**: `JwtKeyManagerService` が RSA 鍵ペアを生成し、DB (`mir_system_security_key`) に暗号化保存
- **暗号化**: `AesCryptoService` (AES-256-GCM) でマスターキー(KEK)により秘密鍵(DEK)を暗号化
- **マスターキー**: 環境変数 `MIREL_MASTER_KEY` (Base64, 32bytes以上)
- **トークン構造**: `iss=mirel`, `sub=userId`, `roles=[...]`, `kid` ヘッダ付き
- **有効期限**: アクセストークン `auth.jwt.expiration` (秒), リフレッシュトークン 7日 (rememberMe: 90日)
- **Cookie配信**: `accessToken` / `refreshToken` を HttpOnly Cookie で配信。`CookieOrHeaderBearerTokenResolver` で Cookie/Header 両方から解決

### セッションモード（OTP/OAuth2）
- `SessionCreationPolicy.IF_REQUIRED` で Spring Session 利用
- OTP 検証後に `SecurityContext` をセッションに保存
- OAuth2 (GitHub) 認証成功時は JWT を発行してフロントにリダイレクト

## 主要クラス構成

| クラス | パッケージ | 責務 |
|--------|-----------|------|
| `WebSecurityConfig` | `jp.vemi.mirel` | SecurityFilterChain 構築、CORS/CSRF/認可/認証設定 |
| `JwtService` | `security.jwt` | JWT 生成・検証。RS256/HS256 デュアルモード |
| `JwtKeyManagerService` | `security.jwt` | RSA 鍵ペア生成・DB保存・キャッシュ管理 |
| `AesCryptoService` | `jp.vemi.framework.crypto` | AES-256-GCM 暗号化/復号（マスターキーベース） |
| `AuthenticationController` | `foundation.web.api.auth.controller` | `/auth/*` エンドポイント群 |
| `AuthenticationServiceImpl` | `foundation.web.api.auth.service` | ログイン/サインアップ/リフレッシュ/ログアウト/テナント切替 |
| `PasswordResetService` | 同上 | SHA-256 トークンハッシュ + 1時間有効 + ワンタイム使用 |
| `ExecutionContextFilter` | `foundation.context` | リクエストごとにユーザー/テナント/ライセンスを解決 |
| `DeviceAuthService` | `security.device` | OAuth2 Device Authorization Grant (RFC 8628) CLI認証 |
| `JwtAuthoritiesConverter` | `security.jwt` | JWT `roles` クレーム → `GrantedAuthority` 変換 |

## 認可設定（permitAll エンドポイント）

```
/auth/login, /auth/signup, /auth/otp/**, /auth/health, /auth/logout, /auth/check
/auth/verify-setup-token, /auth/setup-account
/api/auth/device/code, /api/auth/device/token, /api/auth/device/verify
/login/oauth2/code/**, /oauth2/**, /api/users/*/avatar
/actuator/**, /v3/api-docs/**, /swagger-ui/**, /apps/mira/api/health
/api/bootstrap/**
```

それ以外は `authenticated()` 必須。`mipla2.security.api.enabled=false` でゲストモード。

## CSRF

- デフォルト有効 (`mipla2.security.api.csrf-enabled=true`)
- `/auth/login`, `/auth/refresh`, `/api/auth/device/**`, `/api/bootstrap/**`, OAuth2 コールバックは除外
- `CookieCsrfTokenRepository` + `CsrfCookieFilter` でSPA対応

## フロントエンド連携

- **401 ハンドリング**: Axios interceptor で `clearAuth()` → ログインページリダイレクト
- **トークン期限チェック**: `ProtectedRoute` で JWT `exp` をデコード確認（5秒バッファ）
- **returnUrl**: 401 時に現在パスを保存、ログイン成功後に復帰
- **ログアウト**: 状態クリア → API通知(ベストエフォート) → リダイレクト

## テナント解決優先順位

1. `X-Tenant-ID` ヘッダー
2. JWT `tenant_id` クレーム
3. User のデフォルトテナント
4. `"default"`

## 実装ポリシー

- 新規認証エンドポイント追加時は `WebSecurityConfig.configureAuthorization()` の permitAll リストを更新すること
- パスワードは `BCryptPasswordEncoder` でハッシュ。プレフィックス(`{bcrypt}`)不使用
- トークン（パスワードリセット、OTP）は必ず SHA-256 ハッシュで DB 保存し、平文を保持しない
- ユーザー列挙攻撃対策: メール存在有無に関わらず同一レスポンスを返す
- Cookie の `Secure` フラグは本番 (HTTPS) で `true` にすること（現在 dev は `false`）
- ログイン失敗5回でアカウントロック。`PasswordResetService` でロック解除可能
