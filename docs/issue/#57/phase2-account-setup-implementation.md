# Issue #57 Phase 2: アカウントセットアップ機能実装まとめ

## 実装日時
2025-12-11

## 実装内容

### Phase 2.1: SystemUser.created_by_admin カラム追加
- **コミット**: `c78adae`
- **実装内容**:
  - `SystemUser` エンティティに `created_by_admin` Boolean フィールド追加
  - `@Column(name = "created_by_admin", nullable = false)` で定義
  - `@PrePersist` で `false` にデフォルト設定
  
### Phase 2.2: AdminUserService で SystemUser 作成
- **コミット**: `26d663c`
- **実装内容**:
  - `AdminUserService.createUser()` を修正
  - SystemUser と User を同時に作成
  - `createdByAdmin = true` を設定
  - `emailVerified = false` を設定
  
### Phase 2.3: OtpService.createAccountSetupToken 実装
- **コミット**: `9ade00b`
- **実装内容**:
  - `OtpService.createAccountSetupToken(UUID, String)` メソッド追加
  - 32バイト（64文字hex）のセキュアトークン生成
  - 既存の `ACCOUNT_SETUP` トークンを無効化
  - 72時間の有効期限設定
  - `purpose = "ACCOUNT_SETUP"` で保存
  - `OtpTokenRepository.findBySystemUserIdAndPurposeAndIsVerifiedFalse()` 追加

### Phase 2.4: アカウントセットアップメールテンプレート作成
- **コミット**: `5bac9bb`
- **実装内容**:
  - `account-setup.ftl` Freemarker テンプレート作成
  - テンプレート変数:
    - `displayName`: ユーザーの表示名
    - `username`: ユーザー名
    - `email`: メールアドレス
    - `setupLink`: セットアップリンク（トークン含む）
  - レスポンシブHTMLデザイン
  - 72時間有効期限の注意書き
  - セキュリティ警告（リンク共有禁止など）
  - `AdminUserService.sendAccountSetupEmail()` private メソッド追加
  - `AdminUserService.createUser()` からメール送信

### Phase 2.5: verify-setup-token & setup-account API 実装
- **コミット**: `1362fca`
- **実装内容**:
  - **AuthenticationServiceImpl**:
    - `verifyAccountSetupToken(String token)` メソッド追加
      - トークン検証（purpose=ACCOUNT_SETUP, isVerified=false）
      - 有効期限チェック
      - ユーザー情報（email, username）返却
    - `setupAccount(String token, String newPassword)` メソッド追加
      - トークン検証
      - パスワードハッシュ化
      - SystemUser: `passwordHash`, `emailVerified=true` 更新
      - User: `emailVerified=true` 更新
      - OtpToken: `isVerified=true`, `verifiedAt` 設定
  - **DTO 作成**:
    - `VerifySetupTokenResponse`: email, username フィールド
    - `SetupAccountRequest`: token, newPassword フィールド（@NotBlank, @Size バリデーション）
  - **AuthenticationController**:
    - `GET /auth/verify-setup-token?token=xxx` エンドポイント追加
    - `POST /auth/setup-account` エンドポイント追加
  - **OtpTokenRepository**:
    - `findByMagicLinkTokenAndPurposeAndIsVerifiedFalse(String, String)` メソッド追加

### Phase 2.6: テスト実装
- **コミット**: `024aa0c`
- **実装内容**:
  - **OtpServiceTest** 単体テスト追加:
    - `testCreateAccountSetupToken_Success`: 
      - 64文字hexトークン生成確認
      - SystemUserId, purpose, 有効期限検証
    - `testCreateAccountSetupToken_InvalidatesPreviousTokens`:
      - 既存トークンの無効化確認
      - save()呼び出し回数検証（既存+新規）
  - 統合テストは DB スキーマ未更新のためスキップ
    - 手動テストで検証予定

## 技術的な決定事項

1. **トークン生成**: `SecureRandom` で32バイト（64文字hex）
2. **トークン有効期限**: 72時間（3日間）
3. **既存トークン無効化**: `isVerified=true` で無効化（削除しない）
4. **パスワード設定時の動作**:
   - SystemUser と User の両方で `email_verified=true` に設定
   - トークンは `isVerified=true` で無効化し、再利用不可
5. **エラーハンドリング**: RuntimeException で日本語メッセージ返却

## API エンドポイント

### GET /auth/verify-setup-token
- **パラメータ**: `?token=xxx`
- **レスポンス**:
  ```json
  {
    "email": "user@example.com",
    "username": "username"
  }
  ```
- **エラー**: 400 Bad Request（無効/期限切れトークン）

### POST /auth/setup-account
- **リクエスト**:
  ```json
  {
    "token": "64文字hexトークン",
    "newPassword": "NewPassword123!"
  }
  ```
- **レスポンス**: `200 OK` + "アカウントのセットアップが完了しました"
- **エラー**: 400 Bad Request + エラーメッセージ

## 残タスク

### Phase 3: 検証メール再送 API（推定1日）
- `/auth/resend-verification` エンドポイント実装
- フロントエンドページ作成

### Phase 4: 管理者作成ユーザーログイン時の自動メール送信（推定2日）
- ログイン時に `createdByAdmin=true` & `emailVerified=false` を検出
- 自動的に検証メール送信
- フロントエンドで適切なメッセージ表示

## 注意事項

### データベーススキーマ更新
- **手動マイグレーション必要**:
  ```sql
  ALTER TABLE mir_system_user 
  ADD COLUMN created_by_admin BOOLEAN NOT NULL DEFAULT false;
  ```
- JPA `ddl-auto: update` は本番環境では無効にすること
- 統合テスト実行前にスキーマ更新が必要

### テスト環境
- 単体テストは Mock ベースで完了
- 統合テストは DB スキーマ更新後に実施
- 手動テストシナリオ:
  1. 管理者がユーザー作成
  2. セットアップメール受信確認
  3. セットアップリンククリック → トークン検証
  4. パスワード設定 → ログイン成功

### セキュリティ考慮事項
- トークンは HTTPS 環境でのみ使用推奨
- トークンを URL に含むため、アクセスログに注意
- 有効期限は72時間（必要に応じて調整可能）
- パスワードは8文字以上のバリデーション（SetupAccountRequest）

## コミット履歴
1. `c78adae` - Phase 2.1: SystemUser.created_by_admin 追加
2. `26d663c` - Phase 2.2: AdminUserService で SystemUser 作成
3. `9ade00b` - Phase 2.3: OtpService.createAccountSetupToken 実装
4. `5bac9bb` - Phase 2.4: アカウントセットアップメール機能
5. `1362fca` - Phase 2.5: アカウントセットアップ API 実装
6. `024aa0c` - Phase 2.6: 単体テスト実装

**Powered by Copilot 🤖**
