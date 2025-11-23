# OTP認証実装 現状報告と完全解消戦略 (2025-11-23)

## 1. 現状サマリ
| 項目 | 状態 |
|------|------|
| OTPコードメール送信 | 正常 (MailHogにHTMLメール到達) |
| OTPコード抽出 | 手動抽出は可能 / 自動スクリプトは失敗 (初期ポーリングで取得できず) |
| OTP検証API (`/auth/otp/verify`) | 改修後未確認 (スクリプト失敗のため到達せず) |
| セッションSecurityContext保持 | コード改修済 (principal=アプリ`User.userId`) |
| `/users/me` 401問題 | 改修後再検証未実施 / 旧実行では `principal=email` のためUser解決不可 |
| ExecutionContext解決 | Filterは `auth.getName()` を `UserRepository.findById` に使用する設計 |
| 自動化テスト | Playwright未対応 / bashスモークのみ暫定追加 |
| ログ観測 | OTPリクエスト/検証ログあり / ExecutionContext解決の詳細ログはあるが関連ID不足 |

## 2. これまでの改修過程
1. 既存実装: OTP検証成功時に `UsernamePasswordAuthenticationToken(principal = systemUser.email, authorities = empty)` を SecurityContext に保存 → `/users/me` は ExecutionContextFilter 内で `userRepository.findById(email)` を試みるため不一致で 401。
2. 問題分析: アプリ側 `mir_user.user_id` と SystemUser.email の概念ずれ。`ExecutionContext` はアプリケーションユーザーIDベースで解決する設計。principal の値が不適切。
3. 対応: `OtpController` を修正し、OTPログイン時 principal を `User.userId` に変更し `details` に `systemUserId` と `email` を付加。`SecurityContextRepository.saveContext` + セッション直書き維持。
4. 自動検証: bash スモークテスト `scripts/e2e/otp-login-smoke.sh` を作成。MailHogクリア→OTPリクエスト→メールポーリング→検証→`/users/me` 呼び出しの流れを設計。
5. 現在の失敗点: ポーリング終了後にメールが到着しているケースがあり、ポーリング戦略と抽出ロジックが脆弱。結果として OTP 検証・`/users/me` 到達前に終了。

## 3. 現時点の主な課題
| 課題 | 詳細 | 影響 |
|------|------|------|
| ポーリング不安定 | メール送信タイミング遅延 > 最大待機 (60s) 超過ケース | 自動検証が再現性を欠く |
| OTP抽出ロジック単純 | HTML quoted-printable / マルチパート構造考慮不足 | 稀なフォーマット差異で失敗リスク |
| `/auth/otp/verify` 未再検証 | principal 変更後の正常動作未確定 | 401解消確認未完了 |
| 権限/ロール空 | `Authentication` に roles がない (今後RBAC導入時の布石不足) | 将来機能追加時に再修正必要 |
| 観測性不足 | requestId ↔ SecurityContext ↔ ExecutionContext の相関ログ欠如 | 障害解析困難 |
| テスト形態不足 | 単体/統合/Playwright/E2E で階層的カバレッジ不足 | 回帰検知弱い |
| レート制限と再送の分岐未テスト | 増分/異常系シナリオ網羅なし | 本番挙動差異リスク |

## 4. 根本原因整理
1. 401の根本原因: SecurityContext principal とアプリ `User` のキー不一致。
2. 自動化失敗の根本原因: メール到着タイミングの非決定性 + ポーリング実装の早期終了条件過度。
3. 今後の潜在的リスク: ロール未設定で将来権限チェック導入時に追加改修が必要 / OTPメールフォーマット変更により抽出壊れる恐れ。

## 5. 完全解消に向けた戦略
### 5.1 検証レイヤー構築
| レイヤー | 目的 | 手段 |
|----------|------|------|
| Unit | OTP生成/ハッシュ/レート制限 | 既存 `OtpServiceTest` 拡張 (失敗/再送/最大試行) |
| Integration (MockMvc) | OTP検証→SecurityContext→`/users/me` | SpringBootTest + MockMvc セッション維持テスト |
| API Contract | `/auth/otp/*` スキーマ | OpenAPI 追加 or Spring REST Docs |
| E2E (Headless) | 実ブラウザ + MailHog | Playwright シナリオ: ログイン→プロフィール表示 |
| Smoke CI | 軽量Bash | 改良版 `otp-login-smoke.sh` (高速Fail/ロバスト再試行) |

### 5.2 改修ステップ詳細
1. Backend Integration Test 追加: 
   - テスト: `requestOtp` → MailHog API モックor InMemoryメール送信 → OTP取得 → `verifyOtp` → `GET /users/me` 期待200。
   - `@TestConfiguration` で EmailService Stub。SystemUser/User 初期化をテスト固有に実施。
2. スモークスクリプト改良:
   - (a) 初期リクエスト後 3秒猶予 (メール送信排他処理待ち)
   - (b) ポーリング上限を `POLL_MAX * POLL_INTERVAL` で表示し、指数退避 (2,3,5,...秒)
   - (c) メール未到着時は自動再送 (最大2回) → 再送時はレート制限エラー検知して即Fail。
   - (d) OTP抽出: マルチパート各 part の text/html を正規化 (`quoted-printable` デコード) → 数字6桁最初ヒット採用。
3. Playwright テスト追加:
   - `packages/e2e/tests/specs/promarker-v3/otp-login.spec.ts`
   - API経由でOTPリクエスト→MailHog REST経由コード抽出→フォーム入力→プロフィール画面がユーザー名表示確認。
4. ログ強化:
   - `OtpController` 成功時ログに `userId`,`systemUserId`,`sessionId`,`requestId` を JSON 形式で出力。
   - `ExecutionContextFilter` で `resolvedUserId`, `tenantId`, `licensesCount` を INFO。失敗時 WARN。
5. ロール付与 (将来互換性): OTPログイン時 `ROLE_USER` を付加し認可ポリシー基盤統一。
6. メトリクス: OTPリクエスト/検証成功/失敗を Micrometer カウンター登録 (後でダッシュボード化)。
7. レート制限・再送テスト: 増分的に `verify` 多回失敗 → 最大試行超過 → 正常リセット を Integration Test 化。

### 5.3 フェーズ計画 (優先順)
| フェーズ | 内容 | 成果物 |
|----------|------|--------|
| Phase 1 | Integration Test / ログ強化 / ROLE_USER 付与 | 安定した /users/me 200 確認 |
| Phase 2 | スモークスクリプト改良 & CI組込 | `scripts/e2e/otp-login-smoke.sh` 改訂 + GitHub Actions job |
| Phase 3 | Playwright E2E OTP シナリオ | `otp-login.spec.ts` + レポート |
| Phase 4 | 異常系/レート制限/再送テスト拡充 | 失敗パターン網羅テスト |
| Phase 5 | メトリクス & アラート設計 | Micrometer + ダッシュボード要件定義 |

## 6. 追加改修案 (Optional / 将来)
- 短期: OTPコード再利用防止 (成功後即無効化確認 API)。
- 中期: `tenantId` 明示指定 (ヘッダ `X-Tenant-ID`) の UI 導線追加 & そのテスト。
- 長期: WebAuthn / Magic Link へ拡張する抽象化レイヤ (AuthenticatorStrategy)。

## 7. リスクと緩和策
| リスク | 内容 | 緩和 |
|--------|------|------|
| メール遅延 | メールサーバ負荷/IO遅延 | 再送・指数待機・メトリクス監視 |
| principal 変更副作用 | 既存他処理が `email` 前提 | 全検索 (grep) / 影響箇所レビュー / 回帰テスト |
| レート制限衝突 | CI並列実行で制限超過 | テスト用メールアドレス分離 / Mock化 |
| HTMLテンプレ変更 | 抽出正規表現破損 | `data-otp="XXXXXX"` 属性追加設計 |

## 8. 確認用チェックリスト
- [x] Integration Test で 200 + 正しい UserProfileDto (/users/me が 200 を返却し userId を含む)  [OtpLoginIntegrationTest 追加]
- [ ] スモークスクリプト 2連続成功 (冷/温スタート)。
- [ ] Playwright E2E 成功レポート保存。
- [ ] ログに userId/systemUserId/tenantId/roles 出力確認。
- [ ] メトリクス `otp.request.success` / `otp.verify.success` 増加確認。
- [ ] 失敗系 (誤OTP / 期限切れ / 最大試行超過) のレスポンス定義とテスト整備。

## 9. 今後の実装タスク (Issue #40 継続)
1. `OtpController` ロール付与変更。
2. (完了) Integration Test 追加済み (`OtpLoginIntegrationTest`).
3. スモークスクリプト改良 (再送/指数待機/quoted-printable 正規化)。
4. Playwright シナリオ追加。
5. ログ構造化 (JSONラインフォーマット)。
6. Micrometer カウンター追加。
7. 異常系テスト拡充。

## 10. まとめ
現時点で根本的 401 の原因（principal 不一致）は改修済みだが、再検証が自動化失敗により未完了。上記戦略に従い検証レイヤーを段階的に整備することで、OTPログイン機能を再現性・保守性・拡張性の面で安定化できる。まずは Integration Test を最優先で追加し、成功パスを確定させた上で周辺自動化と観測性を高める。

---
更新者: Copilot Agent
PR: #40
Powered by Copilot 🤖