# JWT 認証実装計画書
作成日: 2024-06-10

---

**1. 全体方針**

- **認証方式**
  - 本番・標準: `auth.method=jwt`（短命アクセストークン + リフレッシュトークン）
  - レガシー / デバッグ: `auth.method=session`（現在の OTP ログインや一部テスト向けに維持）
- **責務分離**
  - `AuthenticationService`: JWT発行・検証／セッション認証の抽象化
  - `ExecutionContext`: 「誰が・どのテナントで」動いているかを、JWT/セッションのどちらでも同じ形で提供
- **影響範囲**
  - Backend: 認証・発行・検証・設定 (`WebSecurityConfig`, `AuthenticationService`, Auth/OTP系Controller, ExecutionContext)
  - Frontend: `authStore`, `useAuth`, `apiClient`, `/users/me` まわり（User/Tenant/License取得）
  - E2E: ログインフロー、トークン更新、保護ルートのテスト

---

**2. バックエンド: JWT 基盤の整理**

2-1. **JWT 設計の確定**

- クレーム設計
  - `sub`: アプリケーションユーザーID (`User.userId`)
  - `tenantId`: 現在選択中のテナントID（必要に応じて）
  - `systemUserId`: システムユーザーID
  - `roles`: `ROLE_USER` / 管理者ロールなど
  - `jti`: トークンID（監査ログ・リフレッシュローテーション識別用、主にリフレッシュトークン側で使用）
- 有効期限
  - アクセストークン: 5〜15分程度
  - リフレッシュトークン: 数日〜数週間（DB管理）
  - 鍵ローテーション時に備え、`exp` とは別にアプリケーション側で「最長許容期間」を定義しておき、古い鍵で署名されたトークンをどこまで受け付けるかを制御可能にする。
- 署名方式
  - 開発: 対称鍵（HS256）で簡易
  - 本番: 非対称鍵（RS256 or ES256）＋ JWK / kid ローテーション設計（将来拡張を見据えてインターフェースを切る）
  - 署名鍵は `kid` 付きで管理し、検証側（`JwtDecoder`）では複数鍵を扱える前提で実装しておく（当面は単一キー想定でも、インターフェース上はローテーション対応可能な形にしておく）。

2-1-1. **トークンの保存場所とクライアント側の扱い**

- アクセストークン
  - ブラウザ上ではメモリまたは `sessionStorage` に保持し、ページリロード時には `/auth/refresh` による再発行を基本とする。
  - `localStorage` への長期保存は XSS リスクを踏まえ原則避ける。必要な場合でも暗号化や厳格な CSP を前提とした「暫定措置」としてドキュメント化する。
- リフレッシュトークン
  - 原則として `HttpOnly` / `Secure` / `SameSite=Lax`（or `Strict`）な Cookie に保存し、JavaScript からは参照しない。
  - API は Cookie によるリフレッシュトークン送信を前提に実装し、JSON ボディにリフレッシュトークンを載せる方式は「デバッグ・互換用」に限定する。
- サーバ側
  - リフレッシュトークンは DB にハッシュ値として保存し、照合時はハッシュ比較を行うことで漏洩リスクを軽減する。
  - Cookie 名やパス（例: `Path=/mipla2/auth`）は一元管理し、複数アプリケーションを跨ぐ場合の影響範囲を明示する。

2-2. **`AuthenticationService` / SecurityConfig の整理**

- `AuthenticationService` を JWT 中心のAPIに整理
  - `String generateAccessToken(authenticatedUser, currentTenant)`  
  - `String generateRefreshToken(authenticatedUser)`  
  - `JwtDecoder getJwtDecoder()`（既存利用を継続）
  - `Authentication authenticateFromJwt(Jwt jwt)`  
- `WebSecurityConfig` の `configureAuthentication` を整理
  - `auth.method=jwt`:
    - `oauth2ResourceServer().jwt(jwt -> jwt.decoder(authenticationService.getJwtDecoder()).jwtAuthenticationConverter(...))`
    - `SessionCreationPolicy.STATELESS`
  - `auth.method=session`:
    - 現行の `formLogin + SecurityContextRepository + sessionManagement` をレガシーモードとして温存
- `ExecutionContext` のバックエンドを JWT/セッション共通で動くように確認
  - `getCurrentUserId()`, `getCurrentTenantId()` が、JWT クレームからもセッションからも取得できるように調整

2-3. **リフレッシュトークン管理**

- `RefreshToken` エンティティ or テーブル設計
  - `id`, `userId`, `token`, `expiresAt`, `revokedAt`, `createdAt`, `createdByIp`, など
- Repository/Service
  - `createRefreshToken(userId, ip, userAgent)`
  - `rotateRefreshToken(oldToken, ip, userAgent)`  
  - `revokeRefreshToken(token or familyId)`
- ここでは「API表面」と「DB/モデル」のIFだけ先に決め、実装は段階的でもOK。

2-4. **運用フェイルセーフと鍵ローテーション**

- 起動時のフェイルセーフ
  - `auth.method=jwt` にもかかわらず署名鍵が読み込めない、あるいは `auth.jwt.enabled=false` となっている場合は、原則としてアプリケーション起動自体を失敗させる（ログに明確なエラーメッセージを出力）。
  - ゲストモード専用のプロファイルを設ける場合でも、「本番用プロファイルでは必ず JWT 設定が正しい前提で起動する」ことを保証する。
- 鍵ローテーション
  - 署名鍵には `kid` を付与し、検証側は複数の公開鍵を順次参照できる設計とする（JWK セット or アプリ設定）。
  - 古い鍵は「一定期間のみ受理する」などの方針を設け、監査ログと合わせて運用しやすい状態にする。
  - 現段階では単一鍵構成で実装し、将来的な複数鍵サポートを見据えたインターフェース・設定名にしておく。

---

**3. バックエンド: ログイン/OTP/APIのJWT対応**

3-1. **パスワードログイン (`/auth/login`) の JWT 化**

- 現行の `/auth/login` を以下に変更/拡張
  - 入力: username/email + password
  - 正常系:
    - ユーザ認証
    - `AuthenticationService.generateAccessToken(...)` で accessToken 発行
    - `AuthenticationService.generateRefreshToken(...)` + DB保存
    - レスポンス:  
      ```json
      {
        "user": { ... }, 
        "currentTenant": { ... },
        "tokens": {
          "accessToken": "...",
          "refreshToken": "...",
          "expiresIn": 900
        }
      }
      ```
  - `auth.method=session` 時のみ、現在のようなセッションベースの SecurityContext 設定（レガシーモード）を維持

3-2. **OTP ログイン (`/auth/otp/verify`) の JWT 対応**

- `LOGIN` 用OTP検証成功時の挙動を分岐
  - `auth.method=jwt`:
    - 現行の「セッションに SecurityContext 保存」ではなく、
    - パスワードログインと同様に accessToken + refreshToken を返す
  - `auth.method=session`:
    - 現在のセッションベース実装を維持
- 目的: パスワードログインとOTPログインが **同じ JWT スキーマ** のトークンを返すようにする。

3-3. **ユーザプロフィール系 (`/users/me/**`) の JWT/セッション両対応**

- 現行の `UserProfileController` + `ExecutionContext` は基本そのまま利用
  - JWT モード: `ExecutionContext` が JwtAuthentication から userId / tenantId を取得
  - セッションモード: 現行どおり SecurityContext から取得
- `/users/me` / `/users/me/tenants` / `/users/me/licenses` が、JWT を持つフロントから正常に動くか確認する。

3-4. **トークンリフレッシュ/ログアウト API**

- `POST /auth/refresh`
  - 入力: `refreshToken`
  - 動作: 
    - DB で refreshToken 検証（有効期限・revoke 状態）
    - 新しい accessToken / refreshToken 発行（ローテーション）
- `POST /auth/logout`
  - 入力: 現在の `refreshToken` or トークンファミリID
  - 動作:
    - 「現在のクライアント」に紐づくトークンファミリのみを revoke することを基本とし、同一ユーザーの他デバイスには影響を与えない。
    - フロントエンドからは、通常は Cookie + 必要に応じてクライアント識別子（`deviceId` など）を送信し、サーバ側でそのクライアントに紐づくトークンレコードを revoke する。
    - 「全デバイスからログアウト」機能が必要な場合は、管理 UI 専用の別 API（例: `/admin/users/{id}/logout-all`）として実装し、本計画のスコープ外とする。

---

**4. フロントエンド: JWT クライアントへの移行**

4-1. **`apiClient` に JWT を付与**

- client.ts
  - `apiClient` 作成時に interceptors で `Authorization: Bearer <accessToken>` を自動付与
    - `useAuthStore.getState().tokens?.accessToken` から取得
  - セッションモードを残す場合は、「`auth.method` に応じて Authorization ヘッダ付与を ON/OFF する」仕組みをあとで追加してもよい（当面は JWT 前提でもOK）。

4-2. **`authStore` のログイン/OTP/API 呼び出し整理**

- `login`:
  - `/mapi/auth/login` のレスポンスを `user/currentTenant/tokens` として保存する形に確定（既に想定している形を厳密化）
  - 成功時: `set({ user, currentTenant, tokens, isAuthenticated: true })`
- OTP 確認系 (`OtpVerifyPage` など):
  - `/mapi/auth/otp/verify` が JWT を返すようになったら、`setAuth(user, tenant, tokens)` を呼び出して `tokens` に access/refresh を設定。
- `logout`:
  - `/mapi/auth/logout` に `refreshToken` を渡してサーバ側でも revoke
  - クライアント側 state はクリア

4-3. **`/users/me` ベースの状態同期**

- ログイン直後・リロード時などに `/mapi/users/me` を叩き、サーバ側状態を基準に `user/currentTenant` を再同期
  - 401 の場合は `clearAuth()`（トークン期限切れや不正トークン時の処理）
- これにより「ローカル state とサーバ state の乖離」を防ぐ。

4-4. **テナント/ライセンス取得部分のトークン依存簡素化**

- `getUserTenants`, `getUserLicenses` から `accessToken` 引数を削除し、`apiClient` の Authorization に依存させる
  - `TenantSwitcher`, `LicenseBadge` などでは `tokens.accessToken` を見なくても動くように変更
  - クエリの `enabled:` 判定は `isAuthenticated` + `!!tokens?.accessToken` 程度に単純化

---

**5. JWT / セッション モードの共存ストラテジ**

5-1. **設定レイヤ**

- `application.yml`: `auth.method: jwt`（デフォルト）
- `application-dev.yml`: 必要に応じて `auth.method: session` を許可
- テストプロファイルでは `@ActiveProfiles("test")` + `properties = "auth.method=session"` 等で、既存の OTP Integration Test を維持

5-2. **コード内の分岐の明示**

- `OtpController` や `AuthController` 内で
  - `if ("jwt".equals(authMethod)) { ...JWTパス... } else { ...セッションパス... }`
  - ログメッセージにもモードを出力しておく（運用時の切り分けを容易にする）

5-3. **ゲストモード（認証不要エンドポイント）の扱い**

- 現状:
  - `WebSecurityConfig.configureAuthorization` では、`securityProperties.isEnabled()` が `false` の場合に `/commons/**` や `/apps/*/api/**` を含む全リクエストを `permitAll()` とするゲストモードを提供している。
  - OTP を含む `/auth/**` は常に `permitAll()` としており、ログイン用エンドポイント群は認証不要でアクセス可能。
- 方針:
  - 本番運用では「原則ログイン必須」とし、ゲストモードは開発・デモ用途に限定する。
  - ゲストモード有効時は、JWT/セッションとは無関係に `/commons/**` や `/apps/*/api/**` を含む全てのエンドポイントを認証不要としつつ、「ゲストユーザー」としての一時的なコンテキスト（必要に応じて ExecutionContext に反映）を検討する。
  - `/auth/**` 系（ログイン・サインアップ・OTP 等）は、ゲストモードかどうかに関わらず常に `permitAll()` とし、未ログイン状態からのアクセスを許容する。
- 設計上の注意:
  - ゲストモード時にも将来の JWT 導入に備え、`auth.method` の設定自体は `jwt` としておき、`securityProperties.isEnabled()` フラグで「認証必須かどうか」だけを切り替える。
  - ゲストモードであっても、監査ログやレートリミット、重要な書き込み系 API に対する制限は別途検討する。
  - ゲストモードであっても、以下のカテゴリの API は**常に認証必須**とする:
    - ユーザー作成・削除、権限変更などのアカウント管理系
    - 課金・請求・プラン変更などの課金系
    - テナント設定変更やシステム管理系 API
  - ゲストリクエストであっても、少なくとも `ip` / `userAgent` / 匿名セッションID（Cookie など）をログに記録し、不正アクセス検知やレートリミットに活用する。

5-4. **/auth 系エンドポイントの公開ポリシー**

- 公開（常に `permitAll()`）とするエンドポイント:
  - `/auth/login`（パスワードログイン）
  - `/auth/signup`（新規登録）
  - `/auth/otp/**`（OTP ログイン/検証/再送）
  - `/auth/password-reset-request`, `/auth/password-reset`, `/auth/password-reset/verify`（パスワードリセット関連）
  - `/auth/health`（ヘルスチェック）
  - `/auth/logout`（ログアウト要求。未ログイン時に呼ばれても問題ないためパブリックで許容）
- 認証必須（`authenticated()`）とするエンドポイント:
  - `/auth/me`（現在のユーザコンテキスト取得）
  - `/auth/switch-tenant`（テナント切替）
  - `/auth/refresh`（リフレッシュトークンによるアクセストークン更新）※CSRF/JWT戦略に応じて保護レベルを精査
- リファクタ方針:
  - 現状の `requestMatchers("/auth/**").permitAll()` は削除し、上記パスごとに `permitAll()` / `authenticated()` を明示する。
  - 本番では `securityProperties.isEnabled() == true` を前提とし、`/auth` 以外の全エンドポイントは `anyRequest().authenticated()` とすることで「原則ログイン必須」を担保する。

5-5. **`/auth/refresh` と CSRF 対策**

- Cookie ベースのリフレッシュトークンを採用する場合:
  - `/auth/refresh` は `HttpOnly` Cookie によるリフレッシュトークン送信を前提とし、同時に CSRF トークン（例: `X-CSRF-Token` ヘッダ or Double Submit Cookie）を必須とする。
  - `SameSite=Lax` もしくは `Strict` を基本とし、クロスサイトからの自動送信を抑止する。
- アクセストークンのみを用いる簡易構成の場合:
  - `/auth/refresh` 自体を `Authorization: Bearer <accessToken>` で呼ぶ API とし、「アクセストークンがまだ有効な間にのみリフレッシュ可能」とする。
  - アクセストークンが完全に失効した場合は、再ログインを要求し、`/auth/refresh` の CSRF 考慮点を最小限に留める。
- いずれの方式を採用するかは、運用環境・クライアント構成に応じて 1 パターンに統一し、本ドキュメントおよび実装コードコメントに明示する。

---

**6. テスト戦略**

6-1. **ユニット/統合テスト**

- JWT モード用 Integration Test 追加
  - `/auth/login` → accessToken 取得 → `/users/me` が 200 になること
  - OTP ログイン（`/auth/otp/request` → `/auth/otp/verify`） → accessToken 取得 → `/users/me` が 200
  - `/auth/refresh` による accessToken ローテーション
- レガシー セッションモード既存テストは極力温存（`auth.method=session` を明示したテスト)

6-2. **E2E（Playwright）**

- シナリオ
  - パスワードログイン → ダッシュボード表示 → ヘッダの Tenant/Licence が正しく表示される
  - OTPログイン → 同上
  - トークン期限切れを疑似再現（モック or テスト専用短寿命設定）→ 自動リフレッシュ or 再ログインの UX 確認

---

**7. UX を損なわないための対策**

7-1. **自動リフレッシュ（サイレント更新）**

- アクセストークンは短命（例: 5〜15分）、リフレッシュトークンは数日〜数週間とし、通常の利用ではユーザーに期限切れを意識させない。
- フロントエンドでは `apiClient` のレスポンスインターセプタで以下を実施する:
  - 401/403 かつ「トークン期限切れ」を示すエラーコード/メッセージの場合にのみ `/auth/refresh` を 1 回だけ試行。
  - `refresh` 成功時は新しい `accessToken` / `refreshToken` を `authStore` に保存し、元リクエストを再送する。
  - `refresh` も失敗した場合のみログアウト処理またはログイン画面への遷移を行う。
- これにより、バックグラウンドでトークンを更新しつつ、ユーザーには極力エラーを見せない運用とする。

7-2. **アイドルタイムアウトとソフトログアウト**

- 一定時間ユーザー操作がない場合、即座に強制ログアウトするのではなく「ソフトログアウト」として扱う。
- 具体的には:
  - アイドル時間経過後もトークンは即削除せず、次回 API 呼び出し時に `/auth/refresh` を試行する。
  - それでも失敗した場合のみ、明示的な「セッションが切れました。再度ログインしてください。」というメッセージを表示し、ログイン画面へ遷移する。
- 必要に応じて、長時間操作がない前に「まもなくセッションが切れます」のようなトースト/モーダルを出すことも検討する（要件と負荷に応じて判断）。

7-3. **マルチタブ・マルチウィンドウ対応**

- `localStorage` または `BroadcastChannel` を利用し、トークン状態の変化（ログイン・リフレッシュ・ログアウト）をブラウザタブ間で同期する。
- これにより、
  - 片方のタブでログアウトしたのに、別タブではまだログイン中に見える、
  - 片方のタブだけトークンが更新されず 401 が発生する、
 という UX の不整合を防ぐ。

7-4. **初回ロード / リロード時の状態回復**

- ページロード時（F5 やタブ復帰時）のフローを統一する:
  1. `authStore` に永続化されている `tokens` があれば、まず `/users/me` を呼び出してサーバ側状態と同期する。
  2. `/users/me` が 401 かつ期限切れを示す場合は `/auth/refresh` を試みる。
  3. それでも失敗した場合のみ `clearAuth()` を実行してクリーンな未ログイン状態に戻す。
- これにより、「F5 したら急にログアウトされる」ケースを減らしつつ、不正/失効トークンは適切に破棄する。

7-5. **エラー表示ポリシーの明確化**

- 自動リフレッシュを試行できる状況では、ユーザーに 401/403 の技術的なエラーを直接見せない。
- リフレッシュも含めて復旧できなかった場合のみ、ユーザーに分かりやすい文言で再ログインを促す:
  - 例: 「セッションの有効期限が切れました。再度ログインしてください。」
- API レベルの細かいエラーコードはログ/監視に出力しつつ、UI では簡潔で理解しやすいメッセージにマッピングする。

7-6. **マルチロール / 権限モデルとの一貫性**

- JWT の `roles` クレームとアプリケーション内部の権限モデルの対応を明確にする:
  - 例: `ROLE_SYSTEM_ADMIN`（全テナントの管理操作が可能）、`ROLE_TENANT_ADMIN`（自テナント配下ユーザー・設定の管理が可能）、`ROLE_USER`（自身のプロファイル・ライセンスのみ操作可能）など。
- `ExecutionContext` も同じロール情報を保持し、サービス層では `hasSystemAdminRole()` や `hasTenantAdminRole()` 等のヘルパーを通じて権限チェックを行う。
- UI 側でも `roles` 情報を基に表示制御（管理メニューの表示/非表示など）を行い、「API の権限」と「画面上の見え方」がずれないようにする。

**8. 段階導入プラン（実装順）**

1. **Backend 基盤**
  - JWT クレーム設計・`AuthenticationService` 整理・`WebSecurityConfig` JWT モード完成
2. **Auth API**
  - `/auth/login` を JWT 仕様に変更
  - `/auth/otp/verify` を JWT/セッション両対応に
  - `/auth/refresh` / `/auth/logout` を追加
3. **Frontend の最小改修**
  - `authStore.login` / OTP まわりを新レスポンスに追従
  - `apiClient` に Authorization 付与ロジック追加
  - `/users/me` ベースの状態同期処理追加
  - `getUserTenants` / `getUserLicenses` の accessToken 引数廃止
4. **動作確認 & テスト**
  - 開発環境で JWT モード起動 → ログイン〜 `/users/me/tenants` / `/users/me/licenses` が 200 になることを確認
  - 既存の OTP セッションモードテストが通ることを確認
5. **リファイン**
  - リフレッシュトークンのローテーション戦略・監査ログ出力などを強化
  - ドキュメント・図（認証フロー/トークンライフサイクル）更新

---

**9. 現状実装とのギャップと対応タスク**

9-1. **JWT リソースサーバ設定とトークン検証の接続**

- 現状:
  - `WebSecurityConfig` は `AuthenticationService` インタフェース（`isJwtSupported()`, `getJwtDecoder()`）を前提としている。
  - 一方でパスワードログイン等のトークン発行は `AuthenticationServiceImpl`（`foundation.web.api.auth.service`）が `JwtService` に直接依存して行っている。
- 課題:
  - 「発行された `accessToken` を Resource Server 側でどのようにデコードし、`Authentication` / `ExecutionContext` に反映するか」が明示されていない。
- 対応方針:
  - `jp.vemi.mirel.security.AuthenticationService` の実装クラスを整備し、`WebSecurityConfig` から利用する。
    - `isJwtSupported()` で `auth.method` / `auth.jwt.enabled` を参照。
    - `getJwtDecoder()` で `JwtService` あるいは `NimbusJwtDecoder` を返却。
  - JWT デコード後に `userId` / `tenantId` から `User` / `Tenant` をロードし、`ExecutionContext` を構築するコンポーネント（フィルタ or Resolver）を明示的に設計・実装する。

9-2. **`auth.method` と `auth.jwt.enabled` 設定の整合性**

- 現状:
  - `WebSecurityConfig` は `auth.method`（`jwt` / `session`）で挙動を切り替える。
  - `AuthenticationServiceImpl` は `auth.jwt.enabled` フラグでトークン発行方式を切り替え、無効時は `"session-based-auth-token"` というプレースホルダを返している。
- 課題:
  - `auth.method=jwt` かつ `auth.jwt.enabled=false` のような設定になると、SecurityConfig は JWT リソースサーバとして動作する一方で、発行されるトークンはダミー文字列となり整合性が取れない。
- 対応方針:
  - 設定ポリシーを文書化し、原則として本番では `auth.method=jwt` かつ `auth.jwt.enabled=true` を必須とする。
  - 将来的には `auth.method` に一本化し、`auth.jwt.enabled` は内部実装向け（あるいは削除候補）とする方向でリファクタリングする。
  - 起動時に設定不整合（`auth.method=jwt` なのに `auth.jwt.enabled=false` など）があればログに警告を出す、もしくは起動を失敗させるガードを入れる。

9-2-1. **設定不整合時の運用ポリシー**

- 本番プロファイルでは、致命的な設定不整合（`auth.method=jwt` だが鍵が無い、`auth.jwt.enabled=false` など）が検出された場合は、サービス起動を失敗させる（起動ログに理由を明記）。
- 開発・テストプロファイルでは、必要に応じて「警告ログを出しつつ自動的に `auth.method=session` にフォールバックする」などの挙動も検討するが、その場合でも本番とテストの挙動差をドキュメントに明示する。

9-3. **ExecutionContext への値セット経路の明示**

- 現状:
  - `ExecutionContext` は `currentUser` / `currentTenant` / `effectiveLicenses` を保持し、`isAuthenticated()` などを提供している。
  - しかし、どの層（フィルタ／ハンドラなど）が JWT / セッション / DB からこれらをセットしているかは計画書上で明示されていない。
- 課題:
  - `/auth/me` や `/users/me/**` が想定どおり動作するには、「毎リクエストで `ExecutionContext` を構築する責務」が明確である必要がある。
- 対応方針:
  - 「ExecutionContext 構築レイヤ」を計画書上に定義し、JWT/セッション双方に共通のフローとして整理する:
    1. SecurityContext（`Authentication` or JWT クレーム）から `userId` / `tenantId` を取り出す。
    2. `User` / `Tenant` / 有効ライセンスをリポジトリから取得。
    3. `ExecutionContext` にセット。
  - 実装クラス（例: `ExecutionContextResolver` や ServletFilter）を設計し、テスト戦略に「ExecutionContext が正しく構築されること」の検証を追加する。

9-4. **フロントエンドの自動リフレッシュと Authorization ヘッダ付与**

- 現状:
  - `authStore` は `tokens` を保持し、`/mapi/auth/login` のレスポンス形式と整合している。
  - しかし `apiClient` は Authorization ヘッダ付与や `/auth/refresh` ベースの自動トークン更新をまだ実装していない。
- 課題:
  - 計画書 7章で定義した「自動リフレッシュ（サイレント更新）」および `/users/me` ベースの状態回復が未実装のため、トークン期限切れ時の UX が不完全。
- 対応方針:
  - `client.ts` の request interceptor で、`useAuthStore.getState().tokens?.accessToken` を参照して `Authorization: Bearer <accessToken>` を付与する。
  - response interceptor で 401/403 かつ期限切れエラーの場合に `/auth/refresh` を 1 回だけ試行し、成功時に元リクエストを再送するロジックを追加する。
  - アプリ初期化時に `tokens` が存在する場合は `/users/me` → 必要に応じて `/auth/refresh` を呼ぶ初期同期フローを `useAuth` などに実装する。

9-5. **OTP ログインと JWT モードの統合**

- 現状:
  - `OtpController.verifyOtp` は `LOGIN` 用の成功時にセッションベースの SecurityContext 設定とセッション保存を行っている（`auth.method=session` 前提）。
  - フロントの `OtpVerifyPage` は TODO コメントのまま、ログイン成功時に `setAuth` でトークン／ユーザー情報を反映していない。
- 課題:
  - `auth.method=jwt` のときにも OTP ログインが「パスワードログインと同じ JWT スキーマのトークン」を返し、同じ UX で動作するべき。
- 対応方針:
  - `OtpController.verifyOtp` に `auth.method` に応じた分岐を追加:
    - `jwt` モード: OTP 検証成功時に `AuthenticationServiceImpl` 相当のロジックで `accessToken` / `refreshToken` / `user` / `currentTenant` / `licenses` を構築し、`AuthenticationResponse` 互換のレスポンスを返す。
    - `session` モード: 現行のセッションベース実装を維持。
  - `OtpVerifyPage` では、JWT モード時のレスポンスを受け取り `setAuth(user, tenant, tokens)` を呼んでからトップページへ遷移するよう修正する。

9-6. **監査ログとトレースの整備**

- 現状:
  - ログイン成功/失敗、OTP 検証、リフレッシュ、ログアウトなどのイベントログは一部のみ、あるいはメッセージが統一されていない可能性がある。
- 対応方針:
  - 次のイベントを少なくとも構造化ログとして記録する:
    - ログイン成功/失敗（userId/email/IP/userAgent/原因）
    - OTP リクエスト・検証成功/失敗
    - リフレッシュ成功/失敗（トークンID or familyId 単位）
    - ログアウト（誰が・どのトークンファミリを revoke したか）
  - ログにはメールアドレスや IP をそのまま保存するのではなく、必要に応じてマスキングやハッシュ化を行い、プライバシー保護と運用上のトレーサビリティのバランスを取る。
  - 監査ログ出力は共通コンポーネント（例: `AuthAuditLogger`）に集約し、フォーマットや項目を統一する。
