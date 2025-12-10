# エラーハンドリング改善 - システムエラーの漏洩防止

**作成日**: 2025-12-10  
**対応者**: Copilot  
**関連Issue**: なし（バグ修正）

## 問題の概要

デプロイ環境で以下の2つの問題が発生していました:

1. **システムエラー詳細のフロントエンド漏洩**
   - SMTPの詳細エラー (`SMTPSendFailedException: 501 5.1.7 The specified sender domain has not been linked.`) がフロントエンドに表示されていた
   - システムエラーの詳細はセキュリティリスクとなり、ログに記録してフロントエンドには最小限の情報のみ返すべき

2. **新規ユーザー作成で400エラー**
   - `/mapi/admin/users` へのPOSTリクエストが400エラーを返す
   - レスポンスの `data` が空文字列で、エラー内容が不明確

## 根本原因

### 1. グローバルエラーハンドラの欠如

バックエンドに `@RestControllerAdvice` によるグローバルエラーハンドラが存在せず、各コントローラで個別に `try-catch` を実装していました。これにより:
- 例外のメッセージがそのままフロントエンドに送信される
- エラーレスポンスの形式が統一されていない
- バリデーションエラーの処理が適切でない

### 2. バリデーション制約の欠如

`CreateUserRequest` にBeanValidation制約がなく、不正なデータでも受け入れていました。

### 3. 詳細なエラーメッセージの伝播

メール送信エラーで `RuntimeException("メール送信エラー", e)` のように原因例外を含めていたため、SMTPの詳細エラーがフロントエンドに漏洩していました。

### 4. パスワードハッシュ化の未実装 ⚠️ **重要**

`AdminUserService.createUser` でパスワードを平文のまま保存していました:
- `user.setPassword()` は **レガシーフィールド** でNOT NULL制約なし
- 実際に使うべきは `user.setPasswordHash()` 
- `PasswordEncoder` でハッシュ化する必要がある
- この問題により、ユーザー作成時に **400エラー** が発生していました

## 実装内容

### 1. グローバルエラーハンドラの作成

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // バリデーションエラー
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(...)
    
    // RuntimeException (業務エラー)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(...)
    
    // システムエラー (詳細を隠蔽)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(...)
}
```

**特徴**:
- すべての例外を一元的に処理
- `RuntimeException` のメッセージは業務エラーとしてクライアントに返す
- その他の `Exception` はログに記録し、一般的なメッセージのみ返す
- `ApiResponse<Void>` 形式で統一されたエラーレスポンスを返す

### 2. CreateUserRequest のバリデーション強化

**変更内容**:
```java
@Data
@NoArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 3, max = 50, message = "ユーザー名は3文字以上50文字以内で入力してください")
    private String username;
    
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください")
    private String password;
    
    // ...
}
```

### 3. AdminUserController の簡潔化

コントローラから `try-catch` を削除し、`@Valid` アノテーションを追加:
```java
@PostMapping
public ResponseEntity<AdminUserDto> createUser(
        @Valid @RequestBody CreateUserRequest request) {
    AdminUserDto user = adminUserService.createUser(request);
    return ResponseEntity.ok(user);
}
```

すべての例外は `GlobalExceptionHandler` で処理されるため、コントローラは簡潔になりました。

### 4. メール送信エラーの隠蔽

**SmtpEmailServiceImpl** と **AzureEmailServiceImpl** を修正:
```java
// Before
throw new RuntimeException("メール送信エラー", e);

// After
throw new RuntimeException("メール送信に失敗しました");
```

原因例外 `e` を含めないことで、SMTP詳細エラーがフロントエンドに漏洩しないようにしました。

### 5. フロントエンドのエラー表示改善

**UserManagementPage.tsx** で `getApiErrors` を使用してエラー配列を表示:
```typescript
import { getApiErrors } from '@/lib/api/client';

// mutation の onError
onError: (error) => {
  console.error("Failed to create user", error);
  const errors = getApiErrors(error);
  toast({

### 6. AdminUserService のパスワードハッシュ化 ⚠️ **重要**

**問題**: `user.setPassword()` で平文パスワードを保存していた（TODOコメントのまま放置）

**修正**:
```java
@Autowired
@Lazy
private PasswordEncoder passwordEncoder;

public AdminUserDto createUser(CreateUserRequest request) {
    // メールアドレスの重複チェックを追加
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
        throw new RuntimeException("Email already exists");
    }
    
    // パスワードをハッシュ化してpasswordHashに保存
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setEmailVerified(false); // デフォルトは未認証
    // ...
}
```

**重要ポイント**:
- `setPassword()` ではなく `setPasswordHash()` を使用
- `PasswordEncoder` で BCrypt ハッシュ化
- メールアドレスの重複チェックも追加
- `@Lazy` を使用して循環依存を回避（他のサービスと同じパターン）
    title: '作成失敗',
    description: errors.join('\n'),
    variant: 'destructive',
  });
}
```

これにより、バックエンドの `ApiResponse.errors` 配列がユーザーに適切に表示されます。

## 変更ファイル一覧

### Backend (Java)
1. ✅ **新規作成**: `backend/src/main/java/jp/vemi/mirel/foundation/web/exception/GlobalExceptionHandler.java`
2. ✅ **修正**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/dto/CreateUserRequest.java` - バリデーション追加
3. ✅ **修正**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/controller/AdminUserController.java` - try-catch削除、@Valid追加
4. ✅ **修正**: `backend/src/main/java/jp/vemi/mirel/foundation/service/impl/SmtpEmailServiceImpl.java` - エラーメッセージ一般化
5. ✅ **修正**: `backend/src/main/java/jp/vemi/mirel/foundation/service/impl/AzureEmailServiceImpl.java` - エラーメッセージ一般化

### Frontend (TypeScript)
1. ✅ **修正**: `apps/frontend-v3/src/features/admin/pages/UserManagementPage.tsx` - `getApiErrors` を使用

## テスト方法

### 1. ユーザー作成の正常系テスト ⚠️ **最重要**

```bash
# 正常なユーザー作成リクエスト
curl -X POST http://localhost:3000/mipla2/admin/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "username": "testuser123",
    "email": "test123@example.com",
    "password": "SecurePassword123!",
    "displayName": "テストユーザー",
    "firstName": "太郎",
    "lastName": "山田",
    "roles": ["USER"],
    "isActive": true
  }'

# 期待レスポンス (200):
{
  "data": null,
  "messages": [],
  "errors": []
}

# ユーザーが正しく作成され、passwordHashフィールドにBCryptハッシュが保存される
# 平文パスワードは保存されない
```

### 2. バリデーションエラーのテスト

```bash
# ユーザー名が短すぎる
curl -X POST http://localhost:3000/mipla2/admin/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "username": "ab",
    "email": "test@example.com",
    "password": "password123"
  }'

# 期待レスポンス (400):
{
  "data": null,
  "messages": [],
  "errors": ["username: ユーザー名は3文字以上50文字以内で入力してください"]
}
```

### 2. バリデーションエラーのテスト

```bash
# 無効なメールアドレスでOTP送信
curl -X POST http://localhost:3000/mipla2/api/auth/send-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid@nonexistent-domain.com"
  }'

# 期待レスポンス (400):
{
  "data": null,
  "messages": [],
  "errors": ["メール送信に失敗しました"]
}

# ログには詳細エラーが記録される (SMTPSendFailException等)
```

### 3. システムエラーのテスト

```bash
# 存在しないユーザーIDで取得
curl -X GET http://localhost:3000/mipla2/admin/users/nonexistent-id \
  -H "Authorization: Bearer <token>"

# 期待レスポンス (400):
{
  "data": null,
  "messages": [],
  "errors": ["User not found"]
}
```

### 4. フロントエンドでのテスト

1. ブラウザで `/admin/users` にアクセス
2. 「新規ユーザー」をクリック
1. ブラウザで `/admin/users` にアクセス
2. 「新規ユーザー」をクリック
3. 正常系をテスト:
   - すべてのフィールドを正しく入力 → ユーザーが作成され、一覧に表示される ✅
4. バリデーションエラーをテスト:
   - ユーザー名を2文字で入力 → 「ユーザー名は3文字以上...」と表示
   - 無効なメールアドレス → 「有効なメールアドレスを...」と表示
   - パスワードを7文字で入力 → 「パスワードは8文字以上...」と表示
5. 重複エラーをテスト:
   - 既存のユーザー名で作成 → 「Username already exists」と表示
   - 既存のメールアドレスで作成 → 「Email already exists」と表示

## セキュリティへの影響

### 改善点
✅ システムエラーの詳細がフロントエンドに漏洩しない  
✅ エラーログはサーバー側に記録され、監査可能  
✅ バリデーションにより不正なデータを事前に防止  
✅ 一般的なエラーメッセージで攻撃者に情報を与えない  
✅ **パスワードがBCryptでハッシュ化され、平文保存されない** ⚠️ 重要  
✅ メールアドレスの重複チェックで不正登録を防止

### 注意点
⚠️ `RuntimeException` のメッセージは業務エラーとしてクライアントに返されるため、業務ロジックで投げる `RuntimeException` のメッセージには機密情報を含めないこと

## 今後の課題

1. **カスタム例外クラスの導入**
   - 業務エラー用の `BusinessException`
   - バリデーション用の `ValidationException`
   - 権限エラー用の `PermissionDeniedException`
   
2. **エラーコードの標準化**
   - エラーメッセージだけでなく、エラーコード (`USER_NOT_FOUND`, `INVALID_EMAIL` 等) を追加
   - フロントエンドでエラーコードに基づいた処理が可能に

3. **監査ログの強化**
   - エラー発生時のリクエスト詳細を監査ログに記録
   - 異常なエラー頻度を検知するアラート機能

4. **他のコントローラの修正**
   - `AdminUserController` 以外のコントローラも同様に `try-catch` を削除
   - `@Valid` によるバリデーションを統一

## まとめ

- ✅ システムエラーの詳細がフロントエンドに漏洩しない仕組みを構築
- ✅ バリデーションエラーが適切に表示される
- ✅ エラーレスポンスの形式が `ApiResponse` で統一
- ✅ メール送信エラーの詳細が隠蔽される
- ✅ コントローラのコードが簡潔になり保守性が向上
- ✅ **パスワードハッシュ化の実装により、ユーザー作成が正常に動作** ⚠️ 最重要
- ✅ メールアドレスの重複チェックを追加

**Powered by Copilot 🤖**
