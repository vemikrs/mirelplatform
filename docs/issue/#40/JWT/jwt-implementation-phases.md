# JWT 認証フロー実装フェーズ計画
作成日: 2024-06-10

---

本ドキュメントは `jwt-implementation-plan.md` を前提とし、**認証フローの検証が可能な単位**で実装フェーズを分割した計画である。
各フェーズごとに DoD（Definition of Done）と検証ポイントを定義し、
- Coding Agent による CLI レベルの動作確認
- ユーザによる UX 観点の打鍵確認
のどちらを DoD レビュー対象とするかを明示する。

進捗管理のため、以下のようにチェックボックス形式でも追跡できるようにする:

- [ ] フェーズ1: Backend JWT 基盤 & セキュリティ設定
- [ ] フェーズ2: パスワードログイン JWT 化 & `/users/me` 連携
- [ ] フェーズ3: フロント認証クライアント（`authStore` / `apiClient`）の JWT 対応
- [ ] フェーズ4: OTP ログインの JWT 対応
- [ ] フェーズ5: 自動リフレッシュ & UX 改善
- [ ] フェーズ6: ゲストモード・権限モデル・監査ログ整備
- [ ] フェーズ7: 統合テスト & E2E 検証完了

## フェーズ一覧（概要）

1. フェーズ1: Backend JWT 基盤 & セキュリティ設定
2. フェーズ2: パスワードログイン JWT 化 & `/users/me` 連携
3. フェーズ3: フロント認証クライアント（`authStore` / `apiClient`）の JWT 対応
4. フェーズ4: OTP ログインの JWT 対応
5. フェーズ5: 自動リフレッシュ & UX 改善
6. フェーズ6: ゲストモード・権限モデル・監査ログ整備
7. フェーズ7: 統合テスト & E2E 検証完了

---

## フェーズ1: Backend JWT 基盤 & セキュリティ設定

### フェーズ進捗チェックリスト

- [ ] `AuthenticationService` 実装が作成され、`WebSecurityConfig` から利用されている
- [ ] `WebSecurityConfig` の `/auth` 公開ポリシーが計画書どおりに反映されている
- [ ] `auth.method=jwt`, `auth.jwt.enabled=true` で正常起動する
- [ ] 設定不整合（鍵未設定など）の場合に起動が失敗する

### スコープ
- `AuthenticationService` 実装（JWT リソースサーバとトークン検証の接続）
- `WebSecurityConfig` の JWT/セッション切替の整理
- **CSRF 対策**: `CookieCsrfTokenRepository.withHttpOnlyFalse()` の設定（`XSRF-TOKEN` Cookie 配布）。
- `/auth` 公開ポリシーの反映（`/auth/**.permitAll()` の撤廃）
- 設定不整合時のフェイルセーフ（起動時チェック）
- **フィルタチェーン維持**: ゲストモード時もセキュリティフィルタを無効化せず、`permitAll()` で制御する構成の確立。

### DoD（Definition of Done）
- `auth.method=jwt`, `auth.jwt.enabled=true` 設定でアプリケーションが起動し、
  - `/auth/health` が 200 を返すこと
  - 本番想定プロファイルで致命的な設定不整合がある場合は起動に失敗すること
- `WebSecurityConfig` の認可設定が次を満たすこと:
  - `/auth/login`, `/auth/signup`, `/auth/otp/**`, パスワードリセット系, `/auth/health`, `/auth/logout` が `permitAll`
  - `/auth/me`, `/auth/switch-tenant`, `/auth/refresh` が `authenticated`
  - 上記以外のエンドポイントは本番プロファイルで `anyRequest().authenticated()`
- **CSRF**: `XSRF-TOKEN` Cookie がレスポンスに含まれ、JS から読み取り可能であること。
- **ゲストモード**: `securityProperties.isEnabled=false` でも `ExecutionContext` 構築ロジック（フィルタ）が動作すること。
- JWT リソースサーバ設定が `AuthenticationService.getJwtDecoder()` を通じて正しく動作すること（署名鍵を変えればトークンが無効になる）。

### 検証ポイント
- **CLI レベル（Coding Agent DoD 対象）**
  - Gradle による起動・停止確認:
    - `./gradlew :backend:bootRun --args='--spring.profiles.active=dev'` で起動成功 / 失敗条件を確認。
  - `/auth` 系エンドポイントの公開範囲確認:
    - `curl` or `httpie` 等で未ログイン状態から以下を確認:
      - `/mipla2/auth/health` → 200
      - `/mipla2/auth/login` → 400/422（バリデーションエラー）だが 401/403 にはならない
      - `/mipla2/auth/me` → 401
  - 不整合設定例（`auth.method=jwt` かつ 鍵未設定 or `auth.jwt.enabled=false`）で起動が失敗すること。
- **UX 打鍵（本フェーズの DoD レビュー対象外）**
  - 本フェーズでは画面遷移や UI 動作は対象外。API レベルでの挙動のみ確認。

---

## フェーズ2: パスワードログイン JWT 化 & `/users/me` 連携

### フェーズ進捗チェックリスト

- [ ] `/auth/login` が JWT 仕様のレスポンスを返す
- [ ] `/auth/me` が JWT ベースでユーザ情報を返す
- [ ] `/users/me` / `/users/me/tenants` / `/users/me/licenses` が JWT 認証で 200 となる
- [ ] 未認証または不正トークンで `/users/me/**` が 401 となる

### スコープ
- `/auth/login` を JWT 仕様に変更（`AuthenticationResponse` に access/refresh/token 有効期限を含める）。
- `/auth/me` 実装の整備（ExecutionContext 経由で User/Tenant を返却）。
- JWT 認証済みリクエストで `/users/me` / `/users/me/tenants` / `/users/me/licenses` が 200 となる状態を実現。

### DoD
- 正しい資格情報で `/auth/login` を叩くと、レスポンスに以下が含まれる:
  - `user`, `currentTenant`, `tokens.accessToken`, `tokens.refreshToken`, `tokens.expiresIn`
- 取得した `accessToken` を `Authorization: Bearer <token>` で付与すると、
  - `/users/me` が 200 + ログインユーザ情報を返す
  - `/users/me/tenants`, `/users/me/licenses` も 200 を返す
- 未ログイン、あるいは不正トークンで `/users/me/**` を叩くと 401 になる。

### 検証ポイント
- **CLI レベル（Coding Agent DoD 対象）**
  - `curl` ベースのシナリオテスト:
    1. `/auth/login` に対して既存テストユーザの ID/パスワードを POST → トークン取得。
    2. `Authorization: Bearer` 付きで `/users/me` `/users/me/tenants` `/users/me/licenses` を GET → 200 であることを確認。
    3. トークンを改ざんする / 期限切れテスト用設定で 401 を確認。
  - JUnit Integration Test を追加し、上記シナリオが CI で自動検証されること（最低限 `/auth/login` → `/users/me`）。
- **UX 打鍵（本フェーズの DoD レビュー対象外）**
  - まだフロントエンドは既存実装のままでもよく、ブラウザ経由のログイン UI は本フェーズの完了条件に含めない。

---

## フェーズ3: フロント認証クライアント（`authStore` / `apiClient`）の JWT 対応

### フェーズ進捗チェックリスト

- [ ] `authStore.login` が新レスポンス形式に対応している
- [ ] `apiClient` が自動で `Authorization: Bearer` ヘッダを付与する
- [ ] 初回ロード/リロード時に `/users/me` ベースの状態同期が行われる
- [ ] `getUserTenants` / `getUserLicenses` が accessToken 引数に依存しない

### スコープ
- `authStore.login` を新しい `/auth/login` レスポンス形式に追従させる。
- `apiClient` に Authorization ヘッダ付与ロジック（accessToken 自動付与）を実装。
- `/users/me` ベースの状態同期（初回ロード/リロード時）の最小実装。
- `getUserTenants` / `getUserLicenses` から accessToken 引数を削除し、`apiClient` ベースに統一。

### DoD
- ブラウザでフロントエンドを起動し（`pnpm --filter frontend-v3 dev`）、次が成立する:
  - 正しい ID/パスワードでログインすると、トップページが表示される。
  - リロード（F5）後も、トークンが有効な間はユーザ情報とテナント/ライセンス情報が表示され続ける。
- フロントエンドコード上で:
  - `authStore` が `tokens.accessToken` / `tokens.refreshToken` を保持し、ログイン直後に `/users/me` を呼んで state を同期している。
  - `TenantSwitcher` / `LicenseBadge` 等が accessToken 引数に依存せず、`apiClient` による Authorization に依存するようになっている。

### 検証ポイント
- **CLI レベル（Coding Agent DoD 対象）**
  - `pnpm --filter frontend-v3 lint` / `pnpm --filter frontend-v3 test` がグリーンであること。
  - `curl` / DevTools Network パネルで、フロントからの API 呼び出しが `Authorization: Bearer <token>` を付与していることを確認。
- **UX 打鍵（本フェーズの DoD レビュー対象）**
  - 開発用ユーザで実際にログインし、以下を手動確認:
    - ログイン → ヘッダのテナント/ライセンス情報が期待どおりに表示される。
    - ページリロードやタブ復帰後も、トークンが有効な間はログイン状態が維持される。
    - 明示的にログアウトすると、ログイン画面へ戻り、再度ログインできる。

---

## フェーズ4: OTP ログインの JWT 対応

### フェーズ進捗チェックリスト

- [ ] `OtpController.verifyOtp` に JWT/セッション分岐が実装されている
- [ ] JWT モードの OTP ログインで `AuthenticationResponse` が返る
- [ ] フロント OTP 画面が `setAuth` で JWT を反映する
- [ ] 既存セッションモードの OTP テストがグリーンである

### スコープ
- `OtpController.verifyOtp` に `auth.method` に応じた分岐を追加し、`jwt` モード時にパスワードログインと同じ JWT スキーマのレスポンスを返す。
- `OtpVerifyPage` で JWT モードのレスポンスを受け取り、`setAuth(user, tenant, tokens)` を実行するように修正。
- 既存のセッションモード OTP フローはテスト用途として維持。

### DoD
- JWT モードで:
  - `/auth/otp/request` → `/auth/otp/verify` のフローにより、パスワードログインと同様の `AuthenticationResponse` が得られる。
  - 取得した `accessToken` で `/users/me` 等が 200 となる。
- セッションモード（`auth.method=session`）の既存 OTP テストが引き続きグリーンであること。

### 検証ポイント
- **CLI レベル（Coding Agent DoD 対象）**
  - backend の OTP 関連 JUnit/Integration Test を拡張し、JWT モード時の OTP ログイン → `/users/me` までを自動検証。
  - `curl` or Postman 等で、OTP ログインの一連のリクエストを手動確認（テスト用固定コード or メールモックを利用）。
- **UX 打鍵（本フェーズの DoD レビュー対象）**
  - フロントエンドの OTP ログイン画面から:
    - OTP ログイン成功後にトップページへ遷移し、パスワードログインと同等の UI 状態になる。
    - 誤った OTP を入力した場合に適切なエラーメッセージが表示される。

---

## フェーズ5: 自動リフレッシュ & UX 改善

### フェーズ進捗チェックリスト

- [ ] `apiClient` のレスポンスインターセプタで自動リフレッシュが実装されている
- [ ] 初回ロード時の `/users/me` → `/auth/refresh` フローが実装されている
- [ ] マルチタブ同期（BroadcastChannel 等）が実装されている
- [ ] 短寿命トークン設定で自動リフレッシュ動作が確認されている

### スコープ
- `apiClient` のレスポンスインターセプタに自動リフレッシュロジックを実装。
  - 401/403 + 有効期限切れエラー時に `/auth/refresh` を 1 回だけ試行し、成功時に元リクエストを再送。
- 初回ロード時の `/users/me` → `/auth/refresh` フローの実装（7-4 節）。
- マルチタブ同期（`BroadcastChannel` 等）によるトークン状態共有（7-3 節）。
- **リフレッシュトークンの猶予期間 (Grace Period)** の実装と検証（マルチタブ競合対策）。

### DoD
- アクセストークンの有効期限を短め（例: 1 分）に設定した開発環境で:
  - ユーザが操作を続けている限り、バックグラウンドでトークンが自動リフレッシュされ、画面上は継続利用できる。
  - リフレッシュも失敗した場合のみ、明示的なメッセージ（例: 「セッションの有効期限が切れました。再度ログインしてください。」）が表示される。
- 2 つ以上のブラウザタブを開いた状態で:
  - **同時リフレッシュ**: ほぼ同時にリフレッシュが発生しても、猶予期間により両方のタブが生き残る（強制ログアウトされない）こと。
  - 片方でログアウトすると、他方も適切なタイミングでログアウト状態に遷移する。

### 検証ポイント
- **CLI レベル（Coding Agent DoD 対象）**
  - 短寿命トークン設定の下での Integration Test（モック時間 or 実時間待ち）を少なくとも 1 ケース用意し、自動リフレッシュの基本動作を検証。
  - `pnpm --filter frontend-v3 test` に、自動リフレッシュロジックのユニットテスト（store / client レベル）を追加。
- **UX 打鍵（本フェーズの DoD レビュー対象）**
  - 開発環境で実際に 5〜10 分程度操作しながら:
    - バックグラウンドでリフレッシュが動作していること（Network パネルで `/auth/refresh` 呼び出しを確認）。
    - 長時間放置後の復帰時に「ソフトログアウト」挙動が期待どおりかを確認。
    - マルチタブでログアウト同期が行われることを確認。

---

## フェーズ6: ゲストモード・権限モデル・監査ログ整備

### フェーズ進捗チェックリスト

- [ ] 本番プロファイルでゲストモード無効がデフォルトになっている
- [ ] ゲストモード時でも禁止カテゴリ API が認証必須になっている
- [ ] `roles` クレームと権限モデル（ExecutionContext/サービス層）の整合が取れている
- [ ] 認証/トークンイベントの監査ログが共通フォーマットで出力されている

### スコープ
- ゲストモード時の開放範囲と禁止カテゴリの実装（5-3 節）。
- JWT `roles` クレームとアプリ内権限モデル（ExecutionContext, サービス層ヘルパー）の整合（7-6 節）。
- 認証/トークンイベントの監査ログ整備（9-6 節）。

### DoD
- 本番プロファイルではゲストモード無効（`securityProperties.isEnabled()==true`）がデフォルトとなっている。
- 開発/デモ用にゲストモードを有効にした際も:
  - 書き込み系 API（アカウント管理・課金・テナント/システム設定）は常に認証必須。
- ログ出力:
  - ログイン成功/失敗、OTP 検証、リフレッシュ成功/失敗、ログアウトイベントが、共通フォーマットで構造化ログに出力される。

### 検証ポイント
- **CLI レベル（Coding Agent DoD 対象）**
  - プロファイルごとの起動（本番相当・ゲストモード相当）で、許可/拒否される API が期待どおりであることを Integration Test またはスモークスクリプトで確認。
  - ログファイル（もしくはコンソール）を確認し、イベントログが想定の形式で出力されていることを確認。
- **UX 打鍵（本フェーズの DoD レビュー対象外）**
  - UX というより運用/監査寄りのため、基本は CLI / ログ確認を中心とし、UI からの操作は任意確認レベルとする。

---

## フェーズ7: 統合テスト & E2E 検証完了

### フェーズ進捗チェックリスト

- [ ] JWT モード前提の主要シナリオが Playwright E2E テストでカバーされている
- [ ] `pnpm test:e2e` がローカル/CI ともに安定してグリーンである
- [ ] backend/frontend のユニット/統合テストが CI でグリーンである
- [ ] JWT 関連ドキュメント（plan/phases）が最終版として更新されている

### スコープ
- JWT モードを前提にした E2E テスト（Playwright）の拡充。
- 主要な認証フロー（パスワードログイン・OTP ログイン・テナント切替・自動リフレッシュ・ログアウト）のシナリオ化。
- ドキュメント更新（アーキテクチャ図・運用手順・トラブルシュート）。

### DoD
- `pnpm test:e2e` がローカルおよび CI で安定してグリーンになる。
- JWT 認証まわりの主要シナリオ（少なくとも以下）が E2E テストとしてカバーされている:
  - パスワードログイン → ダッシュボード → `/users/me/tenants` / `/users/me/licenses` 表示。
  - OTP ログイン → 同上。
  - トークン期限切れ疑似再現 → 自動リフレッシュ動作確認。
  - ログアウト → 他タブ含めログアウト状態になる。
- `docs/issue/#40/JWT/*.md`（本計画書含む）が最終状態に更新され、PR #40 から参照可能になっている。

### 検証ポイント
- **CLI レベル（Coding Agent DoD 対象）**
  - `pnpm test:e2e` 実行ログの確認と、失敗時のレポート分析までを含めた手順が整理されている。
  - backend/frontend 両方のユニット/統合テストが CI 上でグリーンであること（`./gradlew :backend:check`、`pnpm --filter frontend-v3 lint,test`）。
- **UX 打鍵（本フェーズの DoD レビュー対象）**
  - 実際のブラウザで代表的なユーザストーリーを一通り打鍵し、E2E テストでは拾いきれない文言・レイアウト・体感速度などの UX をレビューする。
  - レビュー観点:
    - 認証エラー時のメッセージが分かりやすいか。
    - ログイン後の画面遷移が自然で、迷いがないか。
    - 長時間利用時にもストレスなく使い続けられるか（自動リフレッシュ挙動含む）。
