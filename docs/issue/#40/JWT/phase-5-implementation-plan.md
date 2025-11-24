# Phase 5実装計画: E2Eテストと統合テスト

**作成日**: 2025年11月23日  
**Phase**: 5/6（E2Eテストと統合テスト）  
**前提**: Phase 1-4完了（OTP認証・GitHub OAuth2実装・単体テスト完了）

## 概要

Phase 5では、Phase 1-4で実装したOTP認証とGitHub OAuth2統合機能に対するE2Eテストと統合テストを実装します。

## 目標

1. ✅ **統合テスト実装**: Spring Boot統合テスト、データベーストランザクションテスト
2. ✅ **E2Eテスト実装**: Playwright によるエンドツーエンドテスト
3. ✅ **テストカバレッジ向上**: JaCoCo、Vitest Coverageで80%以上達成
4. ✅ **CI/CD統合**: GitHub Actionsでテスト自動実行

## Phase 5.1: バックエンド統合テスト実装

### 5.1.1 CustomOAuth2UserService統合テスト

**ファイル**: `backend/src/test/java/jp/vemi/mirel/foundation/service/oauth2/CustomOAuth2UserServiceIntegrationTest.java`

**テスト内容**:
1. **loadUser()統合テスト**
   - 実際のOAuth2UserRequestでloadUser()を呼び出し
   - SystemUserの作成・更新確認
   - アバター画像のダウンロード・保存確認

2. **データベーストランザクションテスト**
   - SystemUserRepositoryへの保存確認
   - OAuth2プロバイダー情報の正しい保存
   - トランザクションロールバック確認

3. **privateメソッドの間接テスト**
   - `processGitHubUser()`: loadUser()経由で検証
   - `createSystemUserFromGitHub()`: 新規ユーザー作成フローで検証
   - `downloadAndUpdateAvatar()`: アバター保存確認で検証

**技術スタック**:
- `@SpringBootTest`: Spring Boot統合テスト
- `@Transactional`: トランザクション管理
- `@DirtiesContext`: コンテキストクリーンアップ
- `TestRestTemplate`: HTTP通信モック
- H2 Database: インメモリデータベース（テスト用）

### 5.1.2 OtpService統合テスト

**ファイル**: `backend/src/test/java/jp/vemi/mirel/foundation/service/OtpServiceIntegrationTest.java`

**テスト内容**:
1. **OTPフルフロー統合テスト**
   - `requestOtp()` → `verifyOtp()` の一連のフロー
   - OtpTokenRepositoryへの保存確認
   - OtpAuditLogRepositoryへのログ記録確認

2. **メール送信統合テスト**
   - EmailServiceのモック確認
   - メールテンプレート変数の検証
   - メール送信エラーハンドリング

3. **レート制限統合テスト**
   - RateLimitServiceとの連携確認
   - Redisキャッシュ操作確認
   - クールダウン機能の動作確認

**技術スタック**:
- `@SpringBootTest`: Spring Boot統合テスト
- `@Transactional`: トランザクション管理
- `@MockBean`: EmailServiceのモック
- Redis Testcontainers: Redis統合テスト（オプション）

### 5.1.3 Spring Security統合テスト

**ファイル**: `backend/src/test/java/jp/vemi/mirel/security/SecurityIntegrationTest.java`

**テスト内容**:
1. **OAuth2ログインフロー統合テスト**
   - `/oauth2/authorization/github` エンドポイントテスト
   - OAuth2認証成功後のJWT発行確認
   - リダイレクトURL検証

2. **JWT認証統合テスト**
   - JWTトークンでの認証確認
   - 無効なトークンの拒否確認
   - 有効期限切れトークンの処理

3. **CORS・CSRF設定テスト**
   - CORS設定の動作確認
   - CSRF無効化の確認

**技術スタック**:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`: ランダムポート起動
- `TestRestTemplate`: HTTP通信テスト
- `@WithMockUser`: モックユーザー認証

## Phase 5.2: Playwright E2Eテスト実装

### 5.2.1 GitHub OAuth2ログインフロー

**ファイル**: `packages/e2e/tests/specs/promarker-v3/auth/github-oauth2-login.spec.ts`

**テスト内容**:
1. **GitHubログインボタンクリック**
   - ログインページの「GitHubでログイン」ボタン表示確認
   - ボタンクリックで`/oauth2/authorization/github`へ遷移

2. **OAuth2認証フロー**
   - GitHubログインページ（モック）への遷移確認
   - 認証成功後のコールバックURL（`/auth/oauth2/success?token=...`）確認
   - JWTトークンの受け取り確認

3. **認証後の画面遷移**
   - ダッシュボード（`/`）への自動遷移確認
   - ユーザーメニューの表示確認
   - アバター画像の表示確認

**技術スタック**:
- Playwright 1.49+
- Page Object Model（`LoginPage`, `DashboardPage`）
- Fixture: GitHub OAuth2モックサーバー（オプション）

### 5.2.2 OTPメール認証フロー

**ファイル**: `packages/e2e/tests/specs/promarker-v3/auth/otp-email-login.spec.ts`

**テスト内容**:
1. **OTPリクエスト**
   - メールアドレス入力フォーム表示確認
   - 「認証コードを送信」ボタンクリック
   - 成功メッセージ表示確認

2. **OTP検証**
   - OTPコード入力フォーム表示確認
   - 6桁コード入力（テスト用固定コードまたはモック）
   - 「ログイン」ボタンクリック
   - 認証成功後のダッシュボード遷移確認

3. **エラーハンドリング**
   - 無効なOTPコード入力時のエラーメッセージ
   - レート制限超過時のエラーメッセージ
   - 有効期限切れ時のエラーメッセージ

**技術スタック**:
- Playwright 1.49+
- Page Object Model（`OtpLoginPage`, `OtpVerifyPage`）
- Fixture: OTPモックサービス（テスト用OTPコード生成）

### 5.2.3 アバター表示・更新フロー

**ファイル**: `packages/e2e/tests/specs/promarker-v3/user/avatar-display.spec.ts`

**テスト内容**:
1. **アバター表示確認**
   - ログイン後のユーザーメニューでアバター表示
   - アバター画像のURL確認（`/mapi/api/users/{userId}/avatar`）
   - フォールバック文字の表示確認（画像なし時）

2. **アバターサイズ確認**
   - sm/md/lg/xlサイズの表示確認
   - レスポンシブデザインの動作確認

3. **画像エラーハンドリング**
   - 画像読み込みエラー時のフォールバック表示
   - デフォルトアイコンの表示確認

**技術スタック**:
- Playwright 1.49+
- Visual Regression Testing（オプション: `@playwright/test`のスクリーンショット比較）

## Phase 5.3: テストカバレッジレポート

### 5.3.1 JaCoCo（バックエンド）

**設定ファイル**: `backend/build.gradle`

**追加設定**:
```gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.11"
}

test {
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
}
```

**カバレッジ目標**:
- 全体: 80%以上
- サービスクラス: 85%以上
- コントローラークラス: 75%以上

### 5.3.2 Vitest Coverage（フロントエンド）

**設定ファイル**: `apps/frontend-v3/vitest.config.ts`

**追加設定**:
```typescript
export default defineConfig({
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/**',
        'src/test/**',
        '**/*.d.ts',
        '**/*.config.*',
      ],
      thresholds: {
        lines: 80,
        functions: 80,
        branches: 70,
        statements: 80,
      },
    },
  },
});
```

**カバレッジ目標**:
- 全体: 80%以上
- コンポーネント: 75%以上
- ユーティリティ: 85%以上

## Phase 5.4: CI/CD統合

### 5.4.1 GitHub Actions設定

**ファイル**: `.github/workflows/test.yml`

**内容**:
```yaml
name: Test

on:
  push:
    branches: [master, develop, 'copilot/**']
  pull_request:
    branches: [master, develop]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run backend tests
        run: ./gradlew :backend:test
      - name: Generate JaCoCo report
        run: ./gradlew :backend:jacocoTestReport
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./backend/build/reports/jacoco/test/jacocoTestReport.xml

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
        with:
          version: 9
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'pnpm'
      - run: pnpm install
      - run: pnpm --filter frontend-v3 test --coverage
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v4
        with:
          files: ./apps/frontend-v3/coverage/coverage-final.json

  e2e-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v3
      - uses: actions/setup-node@v4
      - run: pnpm install
      - run: pnpm playwright install --with-deps
      - run: pnpm test:e2e
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: packages/e2e/playwright-report/
```

## Phase 5.5: テストドキュメント作成

### 5.5.1 テスト実行ガイド

**ファイル**: `docs/issue/#40/testing-guide.md`

**内容**:
1. **ローカルテスト実行手順**
   - バックエンド単体テスト: `./gradlew :backend:test`
   - フロントエンド単体テスト: `pnpm --filter frontend-v3 test`
   - E2Eテスト: `pnpm test:e2e`

2. **カバレッジレポート確認**
   - JaCoCo: `backend/build/reports/jacoco/test/html/index.html`
   - Vitest: `apps/frontend-v3/coverage/index.html`

3. **トラブルシューティング**
   - テスト失敗時の対処法
   - 環境変数設定
   - データベース初期化

### 5.5.2 テスト結果レポート

**ファイル**: `docs/issue/#40/phase-5-test-results.md`

**内容**:
1. **単体テストサマリー**
   - バックエンド: テストケース数、成功率、カバレッジ
   - フロントエンド: テストケース数、成功率、カバレッジ

2. **統合テストサマリー**
   - Spring Boot統合テスト: テストケース数、成功率
   - データベーステスト: テストケース数、成功率

3. **E2Eテストサマリー**
   - Playwright テストケース数、成功率、実行時間
   - ビジュアルリグレッション結果（オプション）

## 実装順序

1. **Phase 5.1.1**: CustomOAuth2UserService統合テスト（1日）
2. **Phase 5.1.2**: OtpService統合テスト（1日）
3. **Phase 5.1.3**: Spring Security統合テスト（1日）
4. **Phase 5.2.1**: GitHub OAuth2ログインE2Eテスト（1日）
5. **Phase 5.2.2**: OTPメール認証E2Eテスト（1日）
6. **Phase 5.2.3**: アバター表示E2Eテスト（0.5日）
7. **Phase 5.3**: テストカバレッジレポート設定（0.5日）
8. **Phase 5.4**: CI/CD統合（0.5日）
9. **Phase 5.5**: テストドキュメント作成（0.5日）

**合計**: 約6-7日

## 成功基準

- ✅ 統合テスト: 10ケース以上
- ✅ E2Eテスト: 15ケース以上
- ✅ バックエンドカバレッジ: 80%以上（JaCoCo）
- ✅ フロントエンドカバレッジ: 80%以上（Vitest）
- ✅ CI/CD: GitHub Actionsで自動実行成功
- ✅ テストドキュメント: 完全かつ明確

## リスクと対策

### リスク1: GitHub OAuth2モックの複雑さ
- **対策**: Playwrightの`route()`でOAuth2フローをモック化

### リスク2: Redis統合テストの環境依存
- **対策**: TestcontainersまたはインメモリRedis使用

### リスク3: E2Eテストの不安定性
- **対策**: `waitForSelector()`, `retry()`で安定性向上

### リスク4: カバレッジ目標未達
- **対策**: Phase 5.1-5.2で段階的にカバレッジ向上、不足箇所を特定して追加テスト実装

## 次のステップ: Phase 6

Phase 6ではデプロイ準備とドキュメント整備を実施:
1. 本番環境設定（OAuth2クライアントID・シークレット）
2. Redis本番設定
3. 運用ドキュメント作成
4. リリースノート作成

---

**Powered by Copilot 🤖**
