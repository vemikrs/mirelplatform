# Phase 4: 単体テスト実装計画

**開始日**: 2025-01-XX  
**担当**: GitHub Copilot  
**対象Issue**: #40

## 目標

Phase 1-3で実装したOTP認証とGitHub OAuth2統合の単体テストを実装し、コードカバレッジを確保します。

## 実装ステップ

### Phase 4.1: CustomOAuth2UserService単体テスト実装

**目的**: OAuth2ユーザー情報取得・作成・更新ロジックのテスト

**テストケース**:
1. **新規ユーザー作成**: GitHubから初めてログインするユーザー
2. **既存ユーザー更新**: OAuth2プロバイダーIDで既存ユーザーを検索・更新
3. **メールアドレス紐付け**: メールアドレスで既存ユーザーにOAuth2情報を紐付け
4. **アバターダウンロード成功**: GitHubアバター画像のダウンロード・保存
5. **アバターダウンロード失敗**: 画像ダウンロード失敗時のフォールバック

**モック対象**:
- `SystemUserRepository`
- `AvatarService`
- `PasswordEncoder`

**成果物**:
- `CustomOAuth2UserServiceTest.java`

---

### Phase 4.2: AvatarService単体テスト実装

**目的**: アバター画像ダウンロード・保存・取得ロジックのテスト

**テストケース**:
1. **画像ダウンロード成功**: 正常な画像URLからダウンロード
2. **画像ダウンロード失敗**: 無効なURLまたはネットワークエラー
3. **ファイルサイズ超過**: 5MBを超える画像の拒否
4. **拡張子抽出**: URLから正しい拡張子を抽出
5. **画像取得**: 保存済みアバター画像の取得
6. **画像削除**: アバター画像の削除

**モック対象**:
- `RestTemplate`
- ファイルシステム（`Files.write`, `Files.readAllBytes`）

**成果物**:
- `AvatarServiceTest.java`

---

### Phase 4.3: OAuth2ハンドラー単体テスト実装

**目的**: OAuth2認証成功/失敗ハンドラーのテスト

**テストケース**:

#### OAuth2AuthenticationSuccessHandler
1. **JWT発行成功**: SystemUserが存在する場合
2. **ユーザー未登録**: SystemUserが存在しない場合
3. **JWT発行失敗**: JwtServiceがエラーをスローする場合

#### OAuth2AuthenticationFailureHandler
1. **認証失敗**: エラーメッセージ付きでリダイレクト

**モック対象**:
- `JwtService`
- `SystemUserRepository`
- `HttpServletRequest`, `HttpServletResponse`

**成果物**:
- `OAuth2AuthenticationSuccessHandlerTest.java`
- `OAuth2AuthenticationFailureHandlerTest.java`

---

### Phase 4.4: OtpService単体テスト実装

**目的**: OTP生成・検証・再送信ロジックのテスト

**テストケース**:
1. **OTP生成**: 6桁のランダムOTP生成
2. **OTP検証成功**: 正しいOTPコードで検証成功
3. **OTP検証失敗**: 間違ったOTPコードで検証失敗
4. **試行回数超過**: 最大試行回数（3回）を超えた場合
5. **有効期限切れ**: 有効期限（5分）を過ぎたOTP
6. **OTP再送信**: クールダウン時間（60秒）経過後の再送信
7. **再送信失敗**: クールダウン時間内の再送信拒否

**モック対象**:
- `OtpTokenRepository`
- `EmailService`
- `RateLimitService`

**成果物**:
- `OtpServiceTest.java`

---

### Phase 4.5: フロントエンドOAuthコンポーネントテスト実装

**目的**: React OAuth2ログインボタンとコールバックページのテスト

**テストケース**:

#### LoginPage
1. **GitHubログインボタン表示**: ボタンが正しくレンダリングされる
2. **GitHubログインボタンクリック**: OAuth2エンドポイントにリダイレクト

#### OAuthCallbackPage
1. **トークン受け取り成功**: クエリパラメータからトークン取得・保存
2. **エラーハンドリング**: errorパラメータがある場合
3. **トークンなし**: tokenパラメータがない場合

**テストツール**:
- Vitest
- React Testing Library

**成果物**:
- `LoginPage.test.tsx`
- `OAuthCallbackPage.test.tsx`

---

## テストツール・フレームワーク

### バックエンド
- **JUnit 5** - テストフレームワーク
- **Mockito** - モックライブラリ
- **Spring Boot Test** - Spring統合テスト
- **AssertJ** - アサーションライブラリ

### フロントエンド
- **Vitest** - テストランナー
- **React Testing Library** - Reactコンポーネントテスト
- **@testing-library/user-event** - ユーザーインタラクションシミュレーション

---

## テストカバレッジ目標

- **CustomOAuth2UserService**: 90%以上
- **AvatarService**: 85%以上
- **OAuth2ハンドラー**: 80%以上
- **OtpService**: 90%以上
- **Reactコンポーネント**: 70%以上

---

## 実装順序

1. **Phase 4.1**: CustomOAuth2UserService（最も複雑）
2. **Phase 4.2**: AvatarService（ファイルI/O依存）
3. **Phase 4.3**: OAuth2ハンドラー（HTTPレスポンス依存）
4. **Phase 4.4**: OtpService（Phase 1の補完）
5. **Phase 4.5**: フロントエンドテスト（UIコンポーネント）

---

## マイルストーン

- **Phase 4.1-4.3**: バックエンドOAuth2テスト（3コミット）
- **Phase 4.4**: OTPサービステスト（1コミット）
- **Phase 4.5**: フロントエンドテスト（1コミット）
- **合計**: 5コミット、推定2-3時間

---

**Powered by Copilot 🤖**
