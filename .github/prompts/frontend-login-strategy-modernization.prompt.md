# 実装計画: フロントエンドログイン戦略のモダン化（OTP優先表示 + GitHub OAuth統合）

## Overview

現在の LoginPage.tsx と OtpLoginPage.tsx を統合し、**OTP認証を優先的に表示しつつ、パスワード認証とGitHub OAuth2をすべて1画面で提供**するモダンなログイン体験を実現します。

**主要目的**:
1. **OTP認証を第一選択肢**として強調表示（パスワードレスファースト）
2. **GitHub OAuth2ボタン**をすべてのログイン方法で表示
3. パスワード認証は「代替手段」として折りたたみ表示
4. UX一貫性の向上（現在の2ページ構成を1ページに統合）
5. バックエンド設定（application.yml, application-dev.yml）との整合性確保

## Requirements

### 機能要件

#### FR-1: OTP優先UI
- **デフォルト表示**はOTPログイン（メールアドレス入力フィールド）
- 「認証コードを送信」ボタンを最上部に配置
- 新規登録リンク、GitHub OAuthボタンを同一画面に表示

#### FR-2: GitHub OAuth統合
- GitHubログインボタンを**すべての認証モード**で常時表示
- ボタン押下で `/mipla2/oauth2/authorization/github` へリダイレクト
- 成功後は `/auth/oauth2/success?token=xxx` で受け取り

#### FR-3: パスワードログインの非推奨化（オプション提供）
- 「パスワードでログイン」展開式リンクを下部に配置
- クリック時にパスワード入力フィールドを表示（初期状態は非表示）

#### FR-4: パスワードリセット・新規登録リンク
- `/password-reset`, `/signup` へのリンクを画面下部に配置

### 非機能要件

#### NFR-1: アクセシビリティ
- Radix UI + Tailwind 4の既存デザインシステムを踏襲
- キーボードナビゲーション対応
- スクリーンリーダー対応

#### NFR-2: パフォーマンス
- ページロード時のコンポーネント遅延読み込みなし
- TanStack Query の既存パターンを維持

#### NFR-3: セキュリティ
- OTP/OAuth2/パスワード認証すべてでCSRF保護
- JWT `auth.method=jwt` による認証統一

## Implementation Steps

### Step 1: 新しい統合ログインページコンポーネント作成

**ファイル**: `apps/frontend-v3/src/features/auth/pages/UnifiedLoginPage.tsx`

**コンポーネント構成**:
1. OTPログインフォーム（メインUI）
2. GitHub OAuthボタン（「または」セパレータ下）
3. 「パスワードでログイン」アコーディオン（下部、初期非表示）
4. エラーハンドリング
5. ロード中状態
6. テーマ対応

### Step 2: ルーティング設定変更

**ファイル**: `apps/frontend-v3/src/app/router.config.tsx`

- `/login` を UnifiedLoginPage に変更
- `/auth/otp-login` を削除

### Step 3: 既存ページのリンク更新

- PasswordResetRequestPage.tsx
- SignupPage.tsx
- `/auth/otp-login` へのリンクを `/login` に統一

### Step 4: レガシーページの削除

- LoginPage.tsx
- OtpLoginPage.tsx
- index.ts のエクスポート更新

### Step 5: UIデザイン微調整

1. OTPボタン: `variant="default"` （プライマリ）
2. GitHubボタン: `variant="outline"` （セカンダリ）
3. パスワードログイン: `variant="ghost"` （tertiary）
4. セパレータデザイン
5. レスポンシブ対応
6. ダークモード対応

### Step 6: エラーハンドリングとバリデーション

1. メールアドレス形式検証
2. パスワード長さ検証（8文字以上）
3. レート制限エラー表示
4. OAuth2エラー処理

### Step 7: 単体テスト実装

**ファイル**: `apps/frontend-v3/src/features/auth/pages/__tests__/UnifiedLoginPage.test.tsx`

**テストケース**:
1. レンダリングテスト
2. OTPフォーム送信テスト
3. パスワードログイン展開テスト
4. GitHubボタンクリックテスト
5. エラー表示テスト

### Step 8: E2Eテスト更新

**ファイル**: `packages/e2e/tests/specs/promarker-v3/auth-smoke.spec.ts`

### Step 9: ドキュメント更新

1. `apps/frontend-v3/README.md`
2. `docs/issue/#40/frontend-login-strategy-modernization.md`

### Step 10: 本番環境設定確認

1. `otp.enabled=true` 確認
2. `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` 設定確認
3. `app.base-url` 確認

## Validation & Testing

### 受け入れ基準

| ID | 基準 | 検証方法 |
|---|---|---|
| AC-1 | `/login` アクセス時、OTPフォームが最初に表示 | 手動テスト |
| AC-2 | GitHubログインボタンが常に表示 | E2E |
| AC-3 | パスワードログイン展開機能 | Vitest |
| AC-4 | OTP送信後 `/auth/otp-verify` 遷移 | E2E |
| AC-5 | パスワードログイン後 `/home` 遷移 | E2E |
| AC-6 | GitHubボタンでOAuth2リダイレクト | E2E |
| AC-7 | エラー時適切なメッセージ表示 | Vitest |
| AC-8 | ダークモード対応 | 手動テスト |

### テスト実行コマンド

```bash
pnpm --filter frontend-v3 test
pnpm test:e2e
pnpm --filter frontend-v3 lint
pnpm --filter frontend-v3 run typecheck
```

## Dependencies

### 内部依存
- @mirel/ui パッケージ
- apps/frontend-v3/src/lib/api/auth.ts
- apps/frontend-v3/src/lib/hooks/useOtp.ts
- apps/frontend-v3/src/stores/authStore.ts

### 外部依存
- Backend: `/mapi/auth/otp/request`, `/mapi/auth/login`, `/mapi/oauth2/authorization/github`
- Backend設定: `otp.enabled=true`, `spring.security.oauth2.client.registration.github.*`

### 前提条件
- Redis起動（レート制限用）
- MailHog または Azure Communication Services 設定済み
- GitHub OAuth App 登録済み（本番環境のみ）

## Risks

### 技術リスク

| リスク | 影響度 | 軽減策 |
|---|---|---|
| OTP優先UIの誤操作 | 中 | パスワード展開リンクを明確に表示 |
| GitHub OAuth未設定時のエラー | 高 | GITHUB_CLIENT_ID が空ならボタン非表示 |
| 既存ユーザーの混乱 | 中 | リリースノートで周知 |

## Open Questions

### 決定事項

1. **OTPボタンのラベル**: 「認証コードを送信」（技術的正確性重視）
2. **パスワードログインの初期表示**: 完全非表示 → クリックで展開（OTP優先を強調）
3. **GitHub OAuthボタンの配置**: OTPフォーム → セパレータ → GitHubボタン → パスワード展開
4. **既存ユーザーへの通知**: 必要に応じてバナー表示検討
5. **GitHub OAuth未設定時の挙動**: ボタン非表示

---

**作成日**: 2025年11月28日
**関連Issue**: #40
**実装ブランチ**: copilot/implement-saas-planning
