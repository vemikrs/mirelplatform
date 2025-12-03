Created 4 todos

Searched for regex `jwt|JWT|JwtToken|JwtAuth` (`**/backend/src/**/*.java`), 20 results

Searched for regex `security|SecurityConfig|WebSecurityConfig` (`**/backend/src/**/*.java`), 20 results

Searched for regex `OAuth|oauth|device|DeviceAuth` (`**/backend/src/**/*.java`), 20 results

Searched for files matching `**/jwt/**/*.java`, 6 matches

Searched for files matching `**/security/**/*.java`, 20 matches

Searched for files matching `**/auth/**/*.java`, 20 matches

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/WebSecurityConfig.java)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/security/jwt/JwtService.java)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/controller/AuthenticationController.java)

Starting: *JWT/セキュリティ設定の確認* (2/4)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/config/properties/AuthProperties.java)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/security/AuthenticationService.java)

Searched for regex `auth|jwt|security` (`**/backend/src/**/*.yml`), 20 results

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/resources/config/application.yml)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/resources/config/application-dev.yml)

Starting: *既存API構造の把握* (3/4)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/security/JwtAuthenticationService.java)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/security/oauth2/OAuth2AuthenticationSuccessHandler.java)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/security/CookieOrHeaderBearerTokenResolver.java)

Searched for regex `device|DeviceCode|DeviceAuth|cli` (`**/backend/src/**/*.java`), 20 results

Read [](file:///home/nimaz/dev/mirelplatform/docs/issue)

Read [](file:///home/nimaz/dev/mirelplatform/docs/issue/jwt-implementation-phase1.md)

Read [](file:///home/nimaz/dev/mirelplatform/docs/issue/jwt-implementation-phase2.md)

Read [](file:///home/nimaz/dev/mirelplatform/backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/service/AuthenticationServiceImpl.java)

Starting: *CLI認証要件・機能設計まとめ* (4/4)

調査結果と機能設計をまとめます。

---

# mirelplatform-cli 認証機能設計書

## 1. 現状の認証システム分析

### 1.1 既存の認証基盤

mirelplatform は以下の認証方式を既にサポートしています:

| 認証方式 | 実装状況 | 用途 |
|---------|---------|------|
| **JWT認証** | ✅ 実装済み | `auth.method=jwt` で有効化。HS256アルゴリズム使用 |
| **セッション認証** | ✅ 実装済み | `auth.method=session` で有効化 |
| **OTP認証** | ✅ 実装済み | メールOTPベースの認証 |
| **OAuth2 (GitHub)** | ✅ 実装済み | `spring.security.oauth2.client` で設定 |
| **デバイスフロー認証** | ❌ 未実装 | CLI向けに新規実装が必要 |

### 1.2 主要コンポーネント

| ファイル | 役割 |
|----------|------|
| WebSecurityConfig.java | Spring Securityフィルタチェーン設定、CORS、CSRF、認可ルール定義 |
| JwtService.java | JWT生成・検証（HS256、Nimbus JOSE+JWT使用） |
| AuthenticationController.java | `/auth/*` エンドポイント（login, signup, refresh, logout等） |
| AuthenticationServiceImpl.java | 認証ビジネスロジック、トークン発行 |
| CookieOrHeaderBearerTokenResolver.java | JWT取得（Authorization headerまたはCookie） |

### 1.3 既存の設定値

```yaml
# application.yml
auth:
  method: jwt
  jwt:
    enabled: true
    secret: ${JWT_SECRET:...}     # 32文字以上のHMAC鍵
    expiration: 3600              # アクセストークン有効期限（秒）
    refresh-expiration: 86400     # リフレッシュトークン有効期限（秒）
```

---

## 2. CLI認証の要件定義

### 2.1 OAuth2 デバイスフロー概要

CLI環境ではブラウザ認証を直接行えないため、RFC 8628「OAuth 2.0 Device Authorization Grant」に準拠したフローを実装します。

```
┌─────────────────────────────────────────────────────────────────────┐
│                        デバイスフロー認証                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. CLI → Server: POST /api/auth/device/code                        │
│     ← device_code, user_code, verification_uri                      │
│                                                                      │
│  2. CLI: ユーザーにuser_codeとURLを表示                               │
│     「ブラウザで http://localhost:3000/cli/auth を開き                 │
│      コード WDJB-MJHT を入力してください」                             │
│                                                                      │
│  3. User → Browser: /cli/auth?code=WDJB-MJHT                        │
│     → ログイン → 承認確認 → 承認/拒否                                │
│                                                                      │
│  4. CLI → Server: POST /api/auth/device/token (ポーリング)           │
│     ← pending / authorized (+ JWT) / denied / expired               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 機能要件

#### Phase 1: 認証API（優先度: 高）

| No | エンドポイント | メソッド | 説明 |
|----|--------------|---------|------|
| 1 | `/api/auth/device/code` | POST | デバイスコード・ユーザーコード発行 |
| 2 | `/api/auth/device/token` | POST | 認証状態ポーリング・JWT取得 |
| 3 | `/cli/auth` | GET | ブラウザ認証ページ（React） |
| 4 | `/cli/auth/authorize` | POST | ユーザーコード承認 |
| 5 | `/cli/auth/deny` | POST | ユーザーコード拒否 |

---

## 3. 機能設計

### 3.1 デバイスコード発行 API

**エンドポイント**: `POST /api/auth/device/code`

**リクエスト**:
```json
{
  "client_id": "mirel-cli",
  "scope": "api:read api:write"
}
```

**レスポンス（成功）**:
```json
{
  "device_code": "3c9a8f6b-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "user_code": "WDJB-MJHT",
  "verification_uri": "http://localhost:3000/mipla2/cli/auth",
  "expires_in": 900,
  "interval": 5
}
```

**実装要件**:
- `device_code`: UUID形式、サーバー側管理用
- `user_code`: 8文字（XXXX-XXXX形式）、紛らわしい文字除外（0/O, 1/I/l）
- セッションストレージ: Redisまたはインメモリ（TTL: 15分）
- ステータス: `pending` | `authorized` | `denied`

### 3.2 トークンポーリング API

**エンドポイント**: `POST /api/auth/device/token`

**リクエスト**:
```json
{
  "client_id": "mirel-cli",
  "device_code": "3c9a8f6b-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

**レスポンス（認証待ち）**:
```json
{
  "status": "pending"
}
```

**レスポンス（認証完了）**:
```json
{
  "status": "authorized",
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "user": {
    "email": "user@example.com",
    "name": "山田太郎"
  }
}
```

**実装要件**:
- ポーリング間隔: 5秒以上を強制（429エラー）
- JWT有効期限: 24時間（86400秒）推奨
- 認証完了後、device_codeセッションを即時削除

### 3.3 ブラウザ認証ページ

**エンドポイント**: `GET /cli/auth?code=WDJB-MJHT`

**画面フロー**:
1. ユーザーコード入力（URLパラメータから自動入力可）
2. ログイン確認（未認証の場合）
3. 承認確認ダイアログ
4. 完了メッセージ

**フロントエンド実装場所**: `apps/frontend-v3/src/app/cli/auth/`

### 3.4 データモデル

```java
@Data
@Builder
public class DeviceAuthSession {
    private String deviceCode;      // UUID
    private String userCode;        // 8文字コード (XXXX-XXXX)
    private String clientId;        // "mirel-cli"
    private String scope;           // "api:read api:write"
    private DeviceAuthStatus status; // PENDING, AUTHORIZED, DENIED
    private String userId;          // 承認したユーザーID（認証完了後）
    private Instant createdAt;
    private Instant expiresAt;      // 15分後
    private Instant lastPolledAt;   // レート制限用
}

public enum DeviceAuthStatus {
    PENDING,
    AUTHORIZED,
    DENIED
}
```

---

## 4. セキュリティ要件

### 4.1 JWT拡張

CLI向けJWTには以下のクレームを追加:

```json
{
  "sub": "user@example.com",
  "name": "山田太郎",
  "scope": "api:read api:write",
  "client_id": "mirel-cli",
  "iat": 1701518400,
  "exp": 1701604800
}
```

### 4.2 レート制限

| 対象 | 制限 |
|------|------|
| デバイスコードポーリング | 同一device_codeに対し5秒間隔 |
| 一般API | 同一ユーザー60リクエスト/分 |

### 4.3 CSRF対策

- `/api/auth/device/*` エンドポイントをCSRF除外対象に追加
- ブラウザ認証ページ（`/cli/auth/*`）はCSRF保護を維持

### 4.4 CORS設定

```java
// 追加が必要な設定
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:5173",    // 既存
    "http://localhost:3000"     // CLI認証ページ用
));
```

---

## 5. 実装計画

### Phase 1: バックエンド実装

| タスク | ファイル | 優先度 |
|--------|----------|--------|
| DeviceAuthSession エンティティ作成 | `jp.vemi.mirel.security.device.DeviceAuthSession` | 高 |
| デバイス認証サービス実装 | `jp.vemi.mirel.security.device.DeviceAuthService` | 高 |
| デバイス認証コントローラ実装 | `jp.vemi.mirel.security.device.DeviceAuthController` | 高 |
| WebSecurityConfig更新（CSRF除外） | WebSecurityConfig.java | 高 |
| レート制限フィルター追加 | `jp.vemi.mirel.security.device.DeviceAuthRateLimitFilter` | 中 |

### Phase 2: フロントエンド実装

| タスク | ファイル | 優先度 |
|--------|----------|--------|
| CLI認証ページ作成 | `apps/frontend-v3/src/app/cli/auth/page.tsx` | 高 |
| ユーザーコード入力コンポーネント | `apps/frontend-v3/src/features/cli-auth/` | 高 |
| 承認確認ダイアログ | 同上 | 中 |

### Phase 3: 追加機能

| タスク | 優先度 |
|--------|--------|
| APIトークン管理UI（Webで手動発行） | 低 |
| リフレッシュトークン機構 | 低 |
| 監査ログ | 低 |

---

## 6. 既存コードとの統合ポイント

### 6.1 JwtService 拡張

既存の `JwtService.generateToken(Authentication)` を拡張し、CLI向けトークン生成メソッドを追加:

```java
// 追加メソッド
public String generateCliToken(String userId, String scope, String clientId) {
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("self")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(86400)) // 24時間
        .subject(userId)
        .claim("scope", scope)
        .claim("client_id", clientId)
        .build();
    // ...
}
```

### 6.2 WebSecurityConfig 更新

```java
// configureCsrf メソッドへの追加
csrf.ignoringRequestMatchers(
    "/auth/login",
    "/auth/refresh",
    "/api/auth/device/**",  // ← 追加
    "/login/oauth2/code/**",
    "/oauth2/**"
)

// configureAuthorization メソッドへの追加
authz.requestMatchers(
    "/api/auth/device/code",
    "/api/auth/device/token"
).permitAll()
```

---

## 7. エラーハンドリング

| HTTPステータス | エラーコード | 説明 |
|---------------|-------------|------|
| 400 | `invalid_request` | パラメータ不足・不正 |
| 401 | `unauthorized` | 認証エラー |
| 403 | `access_denied` | ユーザーが拒否 |
| 404 | `not_found` | device_code不存在 |
| 410 | `expired_token` | device_code期限切れ |
| 429 | `slow_down` | ポーリング間隔違反 |

---

## 8. まとめ

### 新規実装が必要なもの

1. **バックエンド**
   - `DeviceAuthController` - デバイスフロー用APIコントローラ
   - `DeviceAuthService` - デバイス認証ビジネスロジック
   - `DeviceAuthSession` - セッション管理エンティティ
   - `DeviceAuthRateLimitFilter` - ポーリングレート制限

2. **フロントエンド**
   - `/cli/auth` ページ（React）
   - ユーザーコード入力・承認UIコンポーネント

### 既存コードへの変更

1. WebSecurityConfig.java - CSRF除外、認可ルール追加
2. JwtService.java - CLI向けトークン生成メソッド追加
3. application.yml - CLI関連設定追加

### 再利用可能な既存コンポーネント

- JWT生成・検証: `JwtService`
- ユーザー情報取得: `SystemUserRepository`, `UserRepository`
- パスワードエンコーダー: `PasswordEncoder`
- レート制限: 既存OTPレート制限の仕組みを参考に実装 

Completed: *CLI認証要件・機能設計まとめ* (4/4)

---

調査と設計を完了しました。上記の設計書に基づいて、mirelplatform-cli 向けの OAuth2 デバイスフロー認証を実装できます。既存の JWT 基盤（`JwtService`）をそのまま活用し、新規にデバイス認証用のコントローラ・サービス・フロントエンドページを追加する形となります。