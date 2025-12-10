# サインアップフローOTP化とユーザー削除機能の実装

## 実装概要

### 1. サインアップフローのOTP方式への変更

#### フロントエンド変更
- **SignupPage.tsx**: パスワード入力フィールドを削除し、OTPリクエストボタンに変更
  - ユーザーがメールアドレスとプロフィール情報を入力
  - 「認証コードを送信」ボタンをクリックすると、OTPリクエストAPI(`/auth/otp/request`)を呼び出し
  - OTP検証ページ(`/auth/otp-email-verification`)へ遷移し、サインアップデータを`location.state`で渡す

- **OtpEmailVerificationPage.tsx**: OTP検証成功後のサインアップフローを追加
  - 6桁のOTPコードを入力・検証
  - 検証成功後、`signupData`が存在する場合は新規ユーザー作成API(`/auth/signup/otp`)を呼び出し
  - ユーザー作成成功後、自動的にログインし、`/home`へリダイレクト

#### バックエンド変更
- **OtpSignupRequest.java**: OTPベースサインアップ用の新しいDTO
  - `username`, `email`, `displayName`, `firstName`, `lastName`, `emailVerified`フィールド
  - パスワードフィールドは不要（OTP認証のため）

- **AuthenticationServiceImpl.signupWithOtp()**: OTPベースサインアップメソッド
  - メールアドレス検証済みのユーザーを作成
  - パスワードはランダムなUUIDで生成（使用されない）
  - **重要**: `user.setLastLoginAt(Instant.now())`を設定し、初回サインアップ時にも最終ログイン時刻を記録
  - デフォルトテナントとライセンス（FREE tier）を自動付与
  - JWTトークンを生成しログイン状態にする

- **AuthenticationController**: `/auth/signup/otp`エンドポイントを追加

#### 最終ログイン記録の不具合修正
OTP方式に変更することで、以下の問題が解決されます：
- 従来のパスワードベースサインアップでは、ユーザー作成時に`lastLoginAt`が設定されず、`null`のままだった
- OTPベースサインアップでは、`signupWithOtp()`メソッド内で`user.setLastLoginAt(Instant.now())`を明示的に設定
- これにより、初回サインアップ時から最終ログイン時刻が正しく記録される

### 2. ユーザー管理画面での削除機能追加

#### フロントエンド変更
- **UserFormDialog.tsx**: ユーザー編集ダイアログに削除ボタンを追加
  - `onDelete`プロップを追加
  - ダイアログフッターに削除ボタンを配置（左寄せ、赤色）
  - 削除ボタンクリック時に確認ダイアログを表示
  - Trash2アイコンを使用

- **UserManagementPage.tsx**: 削除ハンドラーを追加
  - `handleDeleteFromDialog()`メソッドを追加し、ダイアログから削除を実行
  - ドロップダウンメニューの削除機能も維持（確認ダイアログ付き）

#### バックエンド
- 既存のAPI `DELETE /admin/users/{id}` を使用（変更なし）

## 検証方法

### サインアップフローの検証

1. `/signup`ページにアクセス
2. 以下の情報を入力：
   - ユーザー名（3-50文字）
   - メールアドレス
   - 表示名
   - 名・姓（オプション）
3. 「認証コードを送信」ボタンをクリック
4. メールで受信した6桁のOTPコードを入力
5. 「アカウントを作成」ボタンをクリック
6. 自動的にログインし、`/home`へリダイレクトされる
7. ユーザー管理画面で、新規ユーザーの「最終ログイン」カラムに時刻が記録されていることを確認

### ユーザー削除機能の検証

1. 管理者アカウントでログイン
2. ユーザー管理画面（`/admin/users`）にアクセス
3. ユーザーの編集アイコンをクリック
4. ユーザー編集ダイアログが開く
5. 左下に赤色の「削除」ボタンが表示されることを確認
6. 「削除」ボタンをクリック
7. 確認ダイアログが表示される
8. 「OK」をクリックすると、ユーザーが削除され、リストから消える

または、ドロップダウンメニューからも削除可能：
1. ユーザー行の「...」メニューをクリック
2. 「削除」を選択
3. 確認ダイアログで「OK」をクリック

## API仕様

### POST /auth/signup/otp
OTPベースサインアップエンドポイント

**リクエストボディ:**
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "displayName": "Test User",
  "firstName": "太郎",
  "lastName": "山田",
  "emailVerified": true
}
```

**レスポンス:**
```json
{
  "user": {
    "userId": "...",
    "username": "testuser",
    "email": "test@example.com",
    "displayName": "Test User",
    "emailVerified": true,
    "lastLoginAt": "2025-12-10T04:42:49.607Z",
    ...
  },
  "tokens": {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 3600
  },
  "currentTenant": {
    "tenantId": "default",
    "tenantName": "Default Tenant",
    ...
  }
}
```

## 注意事項

1. OTP機能は`otp.enabled=true`の設定が必要（デフォルトで有効）
2. メール送信機能が正しく設定されている必要がある
3. パスワードベースのサインアップは残っているが、通常は使用しない（OAuth2サインアップは引き続き利用可能）
4. ユーザー削除は管理者権限（`ADMIN`ロール）が必要
