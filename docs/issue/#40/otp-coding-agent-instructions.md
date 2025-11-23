# OTP問題解消専用 Coding Agent 指示書 (Issue #40)

最終更新: 2025-11-23
担当: Cloud Coding Agent / Copilot Agent

## 目的
OTPログイン後 `/users/me` が 401 を返す問題を完全に解消し、再現性ある自動検証レイヤー (Integration / Smoke / E2E) を整備する。既に principal 不一致は修正済み (OtpController: principal = `User.userId`)。残タスクを段階的に実装し、信頼性・観測性・保守性を確保する。

## 現在の状態 (サマリ)
- 修正済: `OtpController` principal 変更 & SecurityContext セッション保存。
- 追加済: 統合テスト `OtpLoginIntegrationTest` (request → verify → /users/me 200)。
- 追加済: スモークテスト `scripts/e2e/otp-login-smoke.sh` (現状: メール取得ロジック脆弱)。
- 未対応: ロール付与、ログ構造化、メトリクス、スモーク改良、Playwright E2E、異常系テスト。

## スコープ (含む / 含まない)
含む:
1. OTPセッション認証フローの安定化
2. ロール `ROLE_USER` 付与 (最低限の権限モデル準備)
3. 観測性: 構造化ログ + メトリクス (Micrometer カウンター)
4. 自動テスト整備 (Smoke改善 / Playwrightシナリオ / Integration異常系)

含まない:
- JWTモード再実装・最適化
- WebAuthn/Magic Link 拡張
- 大規模 RBAC 導入
- 非OTP関連既存大量テスト失敗の包括的修復 (別Issue化)

## タスク一覧 (優先順 / 推奨コミット分割)
| No | タスク | 詳細 | 成果物/受入条件 | 推奨 Commit Type |
|----|--------|------|-----------------|------------------|
| 1 | ロール付与 | `OtpController` で `UsernamePasswordAuthenticationToken` に `ROLE_USER` を追加 | `/users/me` 実行後ログに roles=ROLE_USER | feat(backend) |
| 2 | ログ構造化 | OTP成功時 JSON1行ログ (userId, systemUserId, sessionId, requestId) / ExecutionContextFilter INFO強化 | backendログで検索可能 `otp.login.success` | feat(backend) |
| 3 | メトリクス | `otp.request.success/failed`, `otp.verify.success/failed` カウンター追加 | `/actuator/metrics` で値増加 (devのみ) | feat(backend) |
| 4 | スモーク改良 | 再送2回まで, 指数的待機, quoted-printable デコード, レート制限検知 | スクリプト2連続成功 (冷/温) | fix(e2e) or feat(e2e) |
| 5 | 異常系 Integration | 無効コード / 期限切れ / 最大試行超過 / 再送クールダウン | 各ケースで期待レスポンス & 状態検証 | test(backend) |
| 6 | Playwright E2E | `otp-login.spec.ts` 追加 (API発行→MailHog取得→フォーム入力→プロフィール表示) | `pnpm test:e2e` 成功レポート | test(e2e) |
| 7 | ドキュメント更新 | 戦略ファイル・指示書/README に結果反映 | チェックリスト更新 / 完了マーク | docs(backend) |

## 詳細実装ガイド
### 1. ロール付与
変更箇所: `OtpController.verifyOtp` 成功ブロック。
実装: `new SimpleGrantedAuthority("ROLE_USER")` を authorities リストに設定。既存空リストを差し替え。
影響調査: grep で `getAuthorities()` 使用箇所の権限依存がないことを確認 (読取のみ)。

### 2. 構造化ログ
形式: `{"event":"otp.login.success","userId":"...","systemUserId":"...","sessionId":"...","requestId":"..."}` を INFO。
位置: OTP検証成功後 SecurityContext 保存直後。
ExecutionContextFilter: 成功時 `{"event":"executionContext.resolved","userId":"...","tenantId":"..."}` / 失敗時 WARN。

### 3. メトリクス
Micrometer 使用。`MeterRegistry` インジェクト。`Counter counter = Counter.builder("otp.request.success").register(registry);` 等。
場所: OtpService.requestOtp, verifyOtp の分岐。成功/失敗毎にインクリメント。
devのみ: `@ConditionalOnProperty` で制御不要なら常時追加可。

### 4. スモークスクリプト改良
ファイル: `scripts/e2e/otp-login-smoke.sh`
改善点:
- 再送: 最初の待機失敗時 `/auth/otp/resend` 実行 (最大2回)。
- 指数退避: 1,2,3,5,8,... 秒 (上限 ~60s)。
- quoted-printable: Pythonで `quopri.decodestring`。
- エラー分類: レート制限の場合即終了コード 2、メール未取得は 3。
成功条件: 連続2回成功 (手動実行 log)。

### 5. 異常系 Integration テスト
新規ファイル例: `backend/src/test/java/jp/vemi/mirel/apps/auth/api/OtpLoginFailureIntegrationTest.java`
ケース:
- 無効コード: verify で false → HTTP 400 or data=false を検証。
- 期限切れ: トークンの expiresAt を過去に操作して verify false。
- 最大試行超過: attemptCount=maxAttempts 事前設定。
- 再送クールダウン: request → 即 resend → 429/エラーメッセージ。

### 6. Playwright E2E
パス: `packages/e2e/tests/specs/promarker-v3/otp-login.spec.ts`
流れ:
1. APIで OTP request。
2. MailHog RESTから OTP抽出。
3. UI 画面(仮: `/promarker/login`)へ入力 (メール + コード)。
4. 遷移後プロフィール表示 (userId or displayName) を `expect()`。
スキップ条件: UI 未整備なら仮要素待機をコメント化。

### 7. ドキュメント更新
更新対象:
- `otp-authentication-strategy-2025-11-23.md` チェックリスト更新。
- 本指示書 (完了欄チェック)。

## テスト & 検証コマンド
```bash
# Integration (変更後即確認)
./gradlew :backend:test --tests "*OtpLoginIntegrationTest"

# 全体ビルド (テスト通過確認)
./gradlew :backend:build

# Smoke (ローカル)
./scripts/e2e/otp-login-smoke.sh

# Playwright (E2E)
pnpm test:e2e
```

## コミット規約 (再掲)
`<type>(<scope>): <subject> (refs #40)` 例: `feat(backend): OTPログインにROLE_USER付与 (refs #40)`

## 受入条件一覧 (Acceptance Criteria)
- ロール: `/users/me` レスポンス内に roles 反映 (または後工程でDTO追加)。
- 統合テスト: 成功・失敗系全て緑。
- スモーク: 2回連続成功ログ添付可能。
- メトリクス: dev起動後 `curl localhost:3000/mipla2/actuator/metrics/otp.request.success` 取得可能。
- Playwright: レポートに OTP シナリオ成功記録。
- ログ: `otp.login.success` と `executionContext.resolved` が1回ずつ以上出力。

## 既知の注意点 / Pitfalls
- 既存失敗テスト多数: 本Issueでは OTP 関連以外へ手を広げない。
- レート制限キー衝突: メールアドレス再利用に注意 (テストは専用メール文字列使用)。
- トークン再送: 再送時は前トークンのハッシュが無効化される設計か確認 (必要なら仕様追記)。
- JSONログ: 既存ロガー設定が行全体を再成形しないか確認 (シンプルな `log.info(jsonString)` を使用)。

## 作業順序 (推奨)
1. ロール付与 (影響少 / 即可視化)。
2. 構造化ログ + メトリクス (観測基盤)。
3. スモーク改良 (素早いフィードバックループ確立)。
4. 異常系統合テスト (品質保証)。
5. Playwright E2E (UI回帰)。
6. ドキュメント最終更新。

## 完了後のフォローアップ (次Issue候補)
- ロールに応じた `/users/me` 拡張 (権限一覧返却)。
- OTPメールテンプレートへ `data-otp="XXXXXX"` 埋め込みで抽出単純化。
- WebAuthn 導入調査。

---
Generated for Coding Agent execution. Powered by Copilot 🤖