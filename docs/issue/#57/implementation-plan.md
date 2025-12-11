# Issue #57: 実装計画書

> **Last Updated**: 2025/12/10  
> **Assignee**: GitHub Copilot 🤖  
> **Estimated Effort**: 4.5日

---

## 実装フェーズ

### Phase 1: ログイン時のメール検証チェック【0.5日】

#### 目的

`email_verified=false` のユーザーがログインできないようにする。

#### 実装内容

##### 1.1 Backend: 専用例外クラスの作成

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/exception/EmailNotVerifiedException.java`

```java
package jp.vemi.mirel.foundation.exception;

public class EmailNotVerifiedException extends RuntimeException {
    private final String email;
    
    public EmailNotVerifiedException(String message, String email) {
        super(message);
        this.email = email;
    }
    
    public String getEmail() {
        return email;
    }
}
```

##### 1.2 Backend: ログイン処理の修正

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/service/AuthenticationServiceImpl.java`

**変更箇所**: `login()` メソッド

```java
// パスワード検証成功後に追加
if (systemUser.getEmailVerified() == null || !systemUser.getEmailVerified()) {
    logger.warn("Login attempt with unverified email: {}", systemUser.getEmail());
    throw new EmailNotVerifiedException(
        "メールアドレスが未検証です。受信ボックスを確認してください。",
        systemUser.getEmail()
    );
}
```

##### 1.3 Backend: グローバル例外ハンドラの追加

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/GlobalExceptionHandler.java`

```java
@ExceptionHandler(EmailNotVerifiedException.class)
public ResponseEntity<ApiResponse<Object>> handleEmailNotVerified(
        EmailNotVerifiedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(
            "EMAIL_NOT_VERIFIED",
            ex.getMessage(),
            Map.of("email", ex.getEmail())
        ));
}
```

##### 1.4 Frontend: エラーハンドリングの追加

**ファイル**: `apps/frontend-v3/src/features/promarker/auth/LoginPage.tsx`

```typescript
// ログインエラー時の処理
if (error.code === 'EMAIL_NOT_VERIFIED') {
  setError('メールアドレスが未検証です。')
  setShowResendButton(true)
  setUnverifiedEmail(error.data.email)
}
```

**UI追加**:
- 「認証メールを再送」ボタン
- `/auth/resend-verification` への遷移

##### 1.5 テスト

- ✅ `email_verified=false` でログイン拒否されることを確認
- ✅ エラーレスポンスに `email` が含まれることを確認
- ✅ フロントエンドでエラーメッセージが表示されることを確認

**影響範囲**: Backend, Frontend

---

### Phase 2: AdminUserService の SystemUser 作成対応【1日】

#### 目的

管理画面でユーザーを作成する際、`SystemUser` も同時に作成し、適切に紐付ける。

#### 実装内容

##### 2.1 Backend: AdminUserService.createUser() の修正

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/AdminUserService.java`

**変更前**:
```java
@Transactional
public AdminUserDto createUser(CreateUserRequest request) {
    // User のみ作成
    User user = new User();
    user.setUserId(UUID.randomUUID().toString());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setEmailVerified(false);
    userRepository.save(user);
    return convertToAdminUserDto(user);
}
```

**変更後**:
```java
@Transactional
public AdminUserDto createUser(CreateUserRequest request) {
    // 1. SystemUser 作成
    SystemUser systemUser = new SystemUser();
    systemUser.setId(UUID.randomUUID());
    systemUser.setUsername(request.getUsername());
    systemUser.setEmail(request.getEmail());
    systemUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    systemUser.setIsActive(true);
    systemUser.setEmailVerified(false);
    systemUser.setCreatedByAdmin(true); // 管理者作成フラグ（新規追加）
    systemUser = systemUserRepository.save(systemUser);
    
    // 2. User 作成（systemUserIdを紐付け）
    User user = new User();
    user.setUserId(UUID.randomUUID().toString());
    user.setSystemUserId(systemUser.getId()); // ✅ 紐付け
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPasswordHash(systemUser.getPasswordHash());
    user.setDisplayName(request.getDisplayName());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setEmailVerified(false);
    user.setIsActive(true);
    if (request.getRoles() != null) {
        user.setRoles(String.join(",", request.getRoles()));
    }
    user = userRepository.save(user);
    
    // 3. アカウント作成通知メール送信
    sendAccountCreationEmail(user, request.getPassword());
    
    return convertToAdminUserDto(user);
}
```

##### 2.2 Backend: SystemUser エンティティの拡張

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/abst/dao/entity/SystemUser.java`

```java
@Column(name = "created_by_admin", columnDefinition = "boolean default false")
private Boolean createdByAdmin = false;
```

##### 2.3 Backend: マイグレーションスクリプト

**ファイル**: `backend/src/main/resources/db/migration/V{version}__add_created_by_admin.sql`

```sql
ALTER TABLE system_user 
ADD COLUMN created_by_admin BOOLEAN DEFAULT FALSE;

COMMENT ON COLUMN system_user.created_by_admin IS '管理者により作成されたアカウントか';
```

##### 2.4 Backend: アカウント設定トークン生成

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/service/OtpService.java`

```java
/**
 * アカウント設定用のワンタイムトークンを生成
 * 
 * @param systemUser SystemUserエンティティ
 * @param ipAddress リクエスト元IPアドレス
 * @param userAgent User Agent
 * @return マジックリンクトークン（64文字）
 */
public String createAccountSetupToken(SystemUser systemUser, String ipAddress, String userAgent) {
    logger.info("Creating account setup token for user: {}", systemUser.getEmail());
    
    // 既存の未検証トークンを無効化
    otpTokenRepository.invalidatePreviousTokens(systemUser.getId(), "ACCOUNT_SETUP");
    
    // マジックリンクトークン生成（32バイト = 64文字の16進数）
    String magicLinkToken = generateMagicLinkToken();
    
    // OTPトークン作成（OTPコードは不要なのでダミー値）
    OtpToken token = new OtpToken();
    token.setSystemUserId(systemUser.getId());
    token.setPurpose("ACCOUNT_SETUP"); // 新しい用途
    token.setMagicLinkToken(magicLinkToken);
    token.setOtpHash(hashOtp("dummy")); // ダミー値（Magic Link のみ使用）
    token.setExpiresAt(LocalDateTime.now().plusHours(24)); // 24時間有効
    token.setMaxAttempts(3); // パスワード設定試行は3回まで
    token.setRequestIp(ipAddress);
    token.setUserAgent(userAgent);
    otpTokenRepository.save(token);
    
    logger.info("Account setup token created: expires in 24 hours");
    return magicLinkToken;
}
```

##### 2.5 Backend: アカウント作成通知メール実装

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/AdminUserService.java`

```java
private void sendAccountCreationEmail(User user, String accountSetupToken) {
    String subject = "アカウントが作成されました";
    String templateName = "admin-account-creation";
    
    // アカウント設定リンク生成
    String setupLink = String.format("%s/auth/setup-account?token=%s",
        appProperties.getBaseUrl(), accountSetupToken);
    
    Map<String, Object> variables = Map.of(
        "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
        "username", user.getUsername(),
        "email", user.getEmail(),
        "setupLink", setupLink,
        "expirationHours", 24,
        "domain", appProperties.getDomain()
    );
    
    emailService.sendTemplateEmail(user.getEmail(), subject, templateName, variables);
}
```

**変更**: `AdminUserService.createUser()` の最後で呼び出し

```java
// 3. アカウント設定トークン生成
String setupToken = otpService.createAccountSetupToken(
    systemUser, 
    "admin-console", // IP（管理画面からの作成なので固定値）
    "Admin Console"
);

// 4. アカウント作成通知メール送信
sendAccountCreationEmail(user, setupToken);
```

##### 2.6 Email Template の作成

**ファイル**: `backend/src/main/resources/templates/email/admin-account-creation.ftl`

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>アカウント作成のお知らせ</title>
</head>
<body style="font-family: sans-serif; line-height: 1.6; color: #333;">
    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
        <h2 style="color: #4F46E5;">アカウントが作成されました</h2>
        
        <p>${displayName} 様</p>
        
        <p>あなたのアカウントが管理者により作成されました。</p>
        
        <h3>アカウント情報</h3>
        <ul>
            <li><strong>ユーザー名</strong>: ${username}</li>
            <li><strong>メールアドレス</strong>: ${email}</li>
        </ul>
        
        <div style="background: #F3F4F6; border-left: 4px solid #4F46E5; padding: 16px; margin: 20px 0;">
            <p style="margin: 0;"><strong>⚠️ 重要</strong></p>
            <p style="margin: 8px 0 0 0;">
                以下のリンクからパスワードを設定してください。<br>
                このリンクは <strong>${expirationHours}時間</strong> 有効です。
            </p>
        </div>
        
        <p style="text-align: center; margin: 30px 0;">
            <a href="${setupLink}" 
               style="display:inline-block;
                      background:#4F46E5;
                      color:#fff;
                      padding:14px 28px;
                      text-decoration:none;
                      border-radius:6px;
                      font-weight:bold;">
                パスワードを設定する
            </a>
        </p>
        
        <p style="font-size: 14px; color: #666;">
            リンクをクリックできない場合は、以下のURLをブラウザにコピー＆ペーストしてください：<br>
            <code style="background: #F3F4F6; padding: 4px 8px; border-radius: 4px; display: inline-block; margin-top: 8px;">
                ${setupLink}
            </code>
        </p>
        
        <hr style="border: none; border-top: 1px solid #E5E7EB; margin: 30px 0;">
        
        <p style="font-size:12px; color:#666;">
            このメールは ${domain} から自動送信されています。<br>
            心当たりがない場合は、このメールを無視してください。
        </p>
    </div>
</body>
</html>
```

##### 2.7 Backend: アカウント設定API（パスワード設定）

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/controller/AuthenticationController.java`

```java
/**
 * アカウント設定トークン検証
 */
@PostMapping("/verify-setup-token")
public ResponseEntity<ApiResponse<Map<String, Object>>> verifySetupToken(
        @RequestBody @Valid VerifySetupTokenRequest request) {
    
    OtpToken token = otpService.verifyAccountSetupToken(request.getToken());
    SystemUser systemUser = systemUserRepository.findById(token.getSystemUserId())
        .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
    
    return ResponseEntity.ok(ApiResponse.success(Map.of(
        "email", systemUser.getEmail(),
        "username", systemUser.getUsername(),
        "expiresAt", token.getExpiresAt().toString()
    )));
}

/**
 * アカウント設定（パスワード設定）
 */
@PostMapping("/setup-account")
public ResponseEntity<ApiResponse<AuthenticationResponse>> setupAccount(
        @RequestBody @Valid AccountSetupRequest request,
        HttpServletRequest httpRequest) {
    
    String ipAddress = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");
    
    AuthenticationResponse response = authenticationService.setupAccount(
        request.getToken(),
        request.getPassword(),
        ipAddress,
        userAgent
    );
    
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

**DTO**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/dto/`

```java
// VerifySetupTokenRequest.java
@Data
public class VerifySetupTokenRequest {
    @NotBlank(message = "トークンは必須です")
    private String token;
}

// AccountSetupRequest.java
@Data
public class AccountSetupRequest {
    @NotBlank(message = "トークンは必須です")
    private String token;
    
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上である必要があります")
    private String password;
}
```

##### 2.8 Backend: AuthenticationService 拡張

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/service/AuthenticationServiceImpl.java`

```java
@Transactional
public AuthenticationResponse setupAccount(String token, String password, 
                                          String ipAddress, String userAgent) {
    logger.info("Account setup attempt with token");
    
    // 1. トークン検証
    OtpToken otpToken = otpService.verifyAccountSetupToken(token);
    
    // 2. SystemUser 取得
    SystemUser systemUser = systemUserRepository.findById(otpToken.getSystemUserId())
        .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));
    
    // 3. パスワード設定
    String passwordHash = passwordEncoder.encode(password);
    systemUser.setPasswordHash(passwordHash);
    systemUser.setEmailVerified(true); // アカウント設定完了 = メール検証完了
    systemUserRepository.save(systemUser);
    
    // 4. User 更新
    User user = userRepository.findBySystemUserId(systemUser.getId())
        .orElseThrow(() -> new RuntimeException("ユーザープロフィールが見つかりません"));
    user.setPasswordHash(passwordHash);
    user.setEmailVerified(true);
    userRepository.save(user);
    
    // 5. トークンを使用済みに
    otpToken.setIsVerified(true);
    otpToken.setVerifiedAt(LocalDateTime.now());
    otpTokenRepository.save(otpToken);
    
    // 6. 自動ログイン
    logger.info("Account setup successful, auto-login: {}", user.getUserId());
    return loginWithUser(user);
}
```

##### 2.9 Backend: OtpService 拡張

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/service/OtpService.java`

```java
/**
 * アカウント設定トークンを検証
 */
@Transactional
public OtpToken verifyAccountSetupToken(String magicLinkToken) {
    logger.info("Verifying account setup token");
    
    // トークン検索
    OtpToken token = otpTokenRepository
        .findByMagicLinkTokenAndIsVerifiedAndExpiresAtAfter(
            magicLinkToken, false, LocalDateTime.now())
        .orElseThrow(() -> new RuntimeException("無効または期限切れのリンクです"));
    
    // 用途チェック
    if (!"ACCOUNT_SETUP".equals(token.getPurpose())) {
        throw new RuntimeException("このトークンはアカウント設定用ではありません");
    }
    
    // 試行回数チェック
    if (token.getAttemptCount() >= token.getMaxAttempts()) {
        throw new RuntimeException("最大試行回数を超過しました");
    }
    
    token.incrementAttemptCount();
    otpTokenRepository.save(token);
    
    logger.info("Account setup token verified successfully");
    return token;
}
```

##### 2.10 テスト

- ✅ 管理画面でユーザー作成時、SystemUser も作成される
- ✅ User.systemUserId が正しく紐付けられる
- ✅ アカウント設定トークンが生成される
- ✅ アカウント作成通知メールが送信される
- ✅ メール内のリンクが有効（24時間）
- ✅ トークン検証APIが動作する
- ✅ パスワード設定APIが動作し、自動ログインできる
- ✅ 使用済みトークンは再利用できない

**影響範囲**: Backend, Database, Email

---

### Phase 3: 再検証メール送信API【1日】

#### 目的

ユーザーが自分で検証メールを再送できるようにする。

#### 実装内容

##### 3.1 Backend: API エンドポイントの追加

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/controller/AuthenticationController.java`

```java
@PostMapping("/resend-verification")
public ResponseEntity<ApiResponse<Map<String, String>>> resendVerification(
        @RequestBody @Valid ResendVerificationRequest request,
        HttpServletRequest httpRequest) {
    
    String ipAddress = httpRequest.getRemoteAddr();
    String userAgent = httpRequest.getHeader("User-Agent");
    
    try {
        authenticationService.resendVerificationEmail(
            request.getEmail(), 
            ipAddress, 
            userAgent
        );
        
        // セキュリティ: 成功/失敗に関わらず同じレスポンス
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "検証メールを送信しました。受信ボックスを確認してください。")
        ));
    } catch (Exception e) {
        logger.warn("Verification email resend failed: {}", request.getEmail(), e);
        
        // セキュリティ: エラー詳細を返さない
        return ResponseEntity.ok(ApiResponse.success(
            Map.of("message", "検証メールを送信しました。受信ボックスを確認してください。")
        ));
    }
}
```

##### 3.2 Backend: DTO の追加

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/dto/ResendVerificationRequest.java`

```java
package jp.vemi.mirel.foundation.web.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationRequest {
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;
}
```

##### 3.3 Backend: サービス実装

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/service/AuthenticationServiceImpl.java`

```java
@Transactional
public void resendVerificationEmail(String email, String ipAddress, String userAgent) {
    logger.info("Resend verification email request: email={}", email);
    
    // レート制限チェック（OtpService 内で実施）
    // ユーザーが存在しなくてもエラーにしない（列挙攻撃対策）
    SystemUser systemUser = systemUserRepository.findByEmail(email).orElse(null);
    
    if (systemUser != null && !Boolean.TRUE.equals(systemUser.getEmailVerified())) {
        // 既存の OTP 基盤を活用
        otpService.requestOtp(email, "EMAIL_VERIFICATION", ipAddress, userAgent);
        logger.info("Verification email sent: email={}", email);
    } else {
        // ユーザーが存在しない、または既に検証済みの場合でもログのみ
        logger.info("Verification email request ignored: email={} (not found or already verified)", email);
    }
}
```

##### 3.4 Frontend: アカウント設定ページの実装

**ファイル**: `apps/frontend-v3/src/features/promarker/auth/AccountSetupPage.tsx`

```typescript
import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button, Input } from '@mirel/ui'
import { authApi } from '@/lib/api/auth'

export function AccountSetupPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')
  
  const [loading, setLoading] = useState(true)
  const [tokenValid, setTokenValid] = useState(false)
  const [userInfo, setUserInfo] = useState<{ email: string; username: string } | null>(null)
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  // トークン検証
  useEffect(() => {
    if (!token) {
      setError('無効なリンクです')
      setLoading(false)
      return
    }

    authApi.verifySetupToken({ token })
      .then((response) => {
        setTokenValid(true)
        setUserInfo({
          email: response.data.email,
          username: response.data.username,
        })
      })
      .catch((err) => {
        setError(err.response?.data?.message || '無効または期限切れのリンクです')
      })
      .finally(() => setLoading(false))
  }, [token])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    // パスワード検証
    if (password.length < 8) {
      setError('パスワードは8文字以上である必要があります')
      return
    }

    if (password !== passwordConfirm) {
      setError('パスワードが一致しません')
      return
    }

    setSubmitting(true)

    try {
      const response = await authApi.setupAccount({ token: token!, password })
      
      // 自動ログイン成功
      localStorage.setItem('accessToken', response.data.tokens.accessToken)
      navigate('/promarker')
    } catch (err: any) {
      setError(err.response?.data?.message || 'アカウント設定に失敗しました')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto" />
          <p className="mt-4 text-gray-600">検証中...</p>
        </div>
      </div>
    )
  }

  if (!tokenValid) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="max-w-md w-full space-y-6 p-8 bg-white rounded-lg shadow">
          <div className="text-center">
            <div className="text-red-600 text-5xl mb-4">⚠️</div>
            <h2 className="text-2xl font-bold text-gray-900">無効なリンク</h2>
            <p className="mt-2 text-gray-600">{error}</p>
            <Button 
              className="mt-6" 
              onClick={() => navigate('/promarker/login')}
            >
              ログイン画面へ
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-6 p-8 bg-white rounded-lg shadow">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">パスワードを設定</h2>
          <p className="mt-2 text-sm text-gray-600">
            {userInfo?.email} のアカウントにパスワードを設定してください。
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2">
              パスワード（8文字以上）
            </label>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full"
              required
              minLength={8}
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">
              パスワード（確認）
            </label>
            <Input
              type="password"
              value={passwordConfirm}
              onChange={(e) => setPasswordConfirm(e.target.value)}
              className="w-full"
              required
            />
          </div>

          {error && (
            <div className="p-3 bg-red-50 text-red-700 rounded-md text-sm">
              {error}
            </div>
          )}

          <Button 
            type="submit" 
            disabled={submitting} 
            className="w-full"
          >
            {submitting ? 'パスワード設定中...' : 'パスワードを設定してログイン'}
          </Button>
        </form>
      </div>
    </div>
  )
}
```

##### 3.5 Frontend: 再送ページの実装

**ファイル**: `apps/frontend-v3/src/features/promarker/auth/ResendVerificationPage.tsx`

```typescript
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@mirel/ui'
import { authApi } from '@/lib/api/auth'

export function ResendVerificationPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [email, setEmail] = useState(searchParams.get('email') || '')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setMessage('')

    try {
      await authApi.resendVerification({ email })
      setMessage('検証メールを送信しました。受信ボックスを確認してください。')
      
      // 3秒後に検証ページへ遷移
      setTimeout(() => {
        navigate(`/auth/email-verification?email=${encodeURIComponent(email)}`)
      }, 3000)
    } catch (error) {
      setMessage('検証メールを送信しました。受信ボックスを確認してください。')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="max-w-md w-full space-y-6 p-8 bg-white rounded-lg shadow">
        <h2 className="text-2xl font-bold">メールアドレス検証</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2">
              メールアドレス
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-3 py-2 border rounded-md"
              required
            />
          </div>
          
          <Button type="submit" disabled={loading} className="w-full">
            {loading ? '送信中...' : '検証メールを再送'}
          </Button>
          
          {message && (
            <div className="p-3 bg-blue-50 text-blue-700 rounded-md text-sm">
              {message}
            </div>
          )}
        </form>
      </div>
    </div>
  )
}
```

##### 3.6 Frontend: API クライアントの追加

**ファイル**: `apps/frontend-v3/src/lib/api/auth.ts`

```typescript
export const authApi = {
  // ... 既存メソッド
  
  resendVerification: async (data: { email: string }) => {
    const response = await apiClient.post('/auth/resend-verification', data)
    return response.data
  },
  
  verifySetupToken: async (data: { token: string }) => {
    const response = await apiClient.post('/auth/verify-setup-token', data)
    return response.data
  },
  
  setupAccount: async (data: { token: string; password: string }) => {
    const response = await apiClient.post('/auth/setup-account', data)
    return response.data
  },
}
```

##### 3.7 Frontend: ルーティングの追加

**ファイル**: `apps/frontend-v3/src/app/routes.tsx`

```typescript
{
  path: 'resend-verification',
  element: <ResendVerificationPage />,
},
{
  path: 'setup-account',
  element: <AccountSetupPage />,
}
```

##### 3.8 テスト

- ✅ `/auth/verify-setup-token` でトークン検証ができる
- ✅ `/auth/setup-account` でパスワード設定＆自動ログインができる
- ✅ 無効なトークンでエラーが返る
- ✅ 期限切れトークンでエラーが返る
- ✅ 使用済みトークンは再利用できない
- ✅ `/auth/resend-verification` にメールアドレスを送信すると検証メールが届く
- ✅ レート制限が機能する（1分間に3回まで）
- ✅ 存在しないメールアドレスでもエラーが返らない
- ✅ 既に検証済みのメールアドレスでもエラーが返らない

**影響範囲**: Backend, Frontend

---

### Phase 4: 初回ログイン時の検証フロー【2日】

#### 目的

管理者作成ユーザーが初回ログイン時に自動的にメール検証フローに入る。

#### 実装内容

##### 4.1 Backend: ログイン処理の拡張

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/service/AuthenticationServiceImpl.java`

**変更箇所**: `login()` メソッドの `EmailNotVerifiedException` スロー箇所

```java
if (systemUser.getEmailVerified() == null || !systemUser.getEmailVerified()) {
    logger.warn("Login attempt with unverified email: {}", systemUser.getEmail());
    
    // 管理者作成ユーザーの場合、自動的に検証メール送信
    if (Boolean.TRUE.equals(systemUser.getCreatedByAdmin())) {
        logger.info("Auto-sending verification email for admin-created user: {}", systemUser.getEmail());
        try {
            otpService.requestOtp(
                systemUser.getEmail(), 
                "EMAIL_VERIFICATION", 
                request.getIpAddress(), 
                request.getUserAgent()
            );
        } catch (Exception e) {
            logger.error("Failed to send verification email: {}", systemUser.getEmail(), e);
        }
    }
    
    throw new EmailNotVerifiedException(
        "メールアドレスが未検証です。検証コードを送信しました。受信ボックスを確認してください。",
        systemUser.getEmail()
    );
}
```

##### 4.2 Backend: LoginRequest の拡張

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/dto/LoginRequest.java`

```java
// フィールド追加（Controller から注入）
private String ipAddress;
private String userAgent;
```

**ファイル**: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/controller/AuthenticationController.java`

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
        @RequestBody @Valid LoginRequest request,
        HttpServletRequest httpRequest) {
    
    // IP と UserAgent を注入
    request.setIpAddress(httpRequest.getRemoteAddr());
    request.setUserAgent(httpRequest.getHeader("User-Agent"));
    
    AuthenticationResponse response = authenticationService.login(request);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

##### 4.3 Frontend: ログインページの改善

**ファイル**: `apps/frontend-v3/src/features/promarker/auth/LoginPage.tsx`

```typescript
// エラーハンドリング
if (error.code === 'EMAIL_NOT_VERIFIED') {
  setError('メールアドレスが未検証です。検証コードを送信しました。')
  
  // 自動的に検証ページへ遷移
  navigate(`/auth/email-verification?email=${encodeURIComponent(error.data.email)}`)
}
```

##### 4.4 Frontend: メール検証ページの改善

**ファイル**: `apps/frontend-v3/src/features/promarker/auth/OtpEmailVerificationPage.tsx`

既存ページを活用し、以下を追加：
- ログインから遷移した場合のメッセージ調整
- 検証成功後に自動ログイン（既存実装を活用）

##### 4.5 テスト

- ✅ 管理者作成ユーザーでログイン試行すると検証メールが送信される
- ✅ 検証メールのOTPコードでログインできる
- ✅ Magic Link でもログインできる
- ✅ 検証後は通常のパスワードログインができる

**影響範囲**: Backend, Frontend

---

## データマイグレーション

### 既存の管理者作成ユーザーの救済

**SQL スクリプト**: `backend/src/main/resources/db/migration/V{version}__fix_admin_created_users.sql`

```sql
-- 1. User に対応する SystemUser がない場合、作成する
INSERT INTO system_user (id, username, email, password_hash, is_active, email_verified, created_by_admin)
SELECT 
    gen_random_uuid(),
    u.username,
    u.email,
    u.password_hash,
    u.is_active,
    false, -- メール未検証
    true   -- 管理者作成フラグ
FROM user_table u
WHERE u.system_user_id IS NULL
ON CONFLICT DO NOTHING;

-- 2. User.system_user_id を更新
UPDATE user_table u
SET system_user_id = (
    SELECT su.id 
    FROM system_user su 
    WHERE su.email = u.email 
    LIMIT 1
)
WHERE u.system_user_id IS NULL;

-- 3. 検証
SELECT 
    u.user_id,
    u.username,
    u.email,
    u.system_user_id,
    su.email_verified,
    su.created_by_admin
FROM user_table u
LEFT JOIN system_user su ON u.system_user_id = su.id
WHERE u.system_user_id IS NOT NULL;
```

**実行タイミング**: Phase 2 完了後

---

## テスト計画

### 単体テスト

| 対象 | テストケース |
|-----|------------|
| `AuthenticationServiceImpl.login()` | `email_verified=false` でログイン拒否 |
| `AuthenticationServiceImpl.login()` | `created_by_admin=true` で検証メール自動送信 |
| `AdminUserService.createUser()` | SystemUser が作成される |
| `AdminUserService.createUser()` | User.systemUserId が紐付けられる |
| `authenticationService.resendVerificationEmail()` | レート制限が機能する |

### E2Eテスト

**ファイル**: `packages/e2e/tests/specs/promarker-v3/admin-created-user-verification.spec.ts`

```typescript
test.describe('管理者作成ユーザーのメール検証', () => {
  test('管理者作成ユーザーがログインして検証できる', async ({ page }) => {
    // 1. 管理者でログイン
    await loginAsAdmin(page)
    
    // 2. ユーザー作成
    await page.goto('/promarker/admin/users')
    await page.click('text=新規ユーザー作成')
    await page.fill('[name="username"]', 'testuser')
    await page.fill('[name="email"]', 'testuser@example.com')
    await page.fill('[name="password"]', 'TestPass123!')
    await page.click('text=作成')
    
    // 3. ログアウト
    await page.click('[data-testid="user-menu"]')
    await page.click('text=ログアウト')
    
    // 4. 作成したユーザーでログイン試行
    await page.goto('/promarker/login')
    await page.fill('[name="usernameOrEmail"]', 'testuser')
    await page.fill('[name="password"]', 'TestPass123!')
    await page.click('button[type="submit"]')
    
    // 5. メール検証ページに遷移
    await expect(page).toHaveURL(/\/auth\/email-verification/)
    
    // 6. OTP取得（メールモックから）
    const otpCode = await getOtpFromEmail('testuser@example.com')
    
    // 7. OTP入力
    await page.fill('[name="otpCode"]', otpCode)
    await page.click('text=認証')
    
    // 8. ログイン成功
    await expect(page).toHaveURL('/promarker')
  })
})
```

### 統合テスト

- ✅ セルフサインアップフローが影響を受けない
- ✅ OTPサインアップフローが影響を受けない
- ✅ OAuth2ログインが影響を受けない
- ✅ 既存の検証済みユーザーが通常ログインできる

---

## リリース手順

### 1. 開発環境でのテスト

```bash
# Backend 起動
./gradlew :backend:bootRun --args='--spring.profiles.active=dev'

# Frontend 起動
pnpm --filter frontend-v3 dev

# E2E テスト実行
pnpm test:e2e
```

### 2. マイグレーション実行

```bash
# DB マイグレーション（自動）
./gradlew :backend:bootRun
```

### 3. デプロイ

```bash
# ビルド
./scripts/build-services.sh

# デプロイ（環境に応じて）
# ...
```

### 4. 本番環境での動作確認

- ✅ 管理画面でテストユーザー作成
- ✅ 作成通知メールが届くことを確認
- ✅ ログイン試行で検証メールが届くことを確認
- ✅ 検証後にログインできることを確認

---

## ロールバック計画

Phase 1 のログイン時チェックで問題が発生した場合：

```java
// 一時的に無効化（コメントアウト）
// if (systemUser.getEmailVerified() == null || !systemUser.getEmailVerified()) {
//     throw new EmailNotVerifiedException(...);
// }
```

Phase 2 の SystemUser 作成で問題が発生した場合：

```sql
-- User.system_user_id を NULL に戻す
UPDATE user_table SET system_user_id = NULL WHERE ...;

-- 作成した SystemUser を削除
DELETE FROM system_user WHERE created_by_admin = true AND created_at > ...;
```

---

## 工数見積もり

| Phase | 内容 | 工数 |
|-------|-----|------|
| Phase 1 | ログイン時のメール検証チェック | 0.5日 |
| Phase 2 | AdminUserService の SystemUser 作成対応 | 1日 |
| Phase 3 | 再検証メール送信API | 1日 |
| Phase 4 | 初回ログイン時の検証フロー | 2日 |
| **合計** | | **4.5日** |

---

## 懸念事項とリスク

### 1. ワンタイムリンクのセキュリティ

**対策済み**:
- ✅ トークンは 256bit（64文字の16進数）で総当たり不可能
- ✅ 有効期限: 24時間
- ✅ 試行回数制限: 3回まで
- ✅ 使用後は無効化
- ✅ IP アドレス・User Agent を記録
- ✅ データベースで永続化（Redis障害の影響なし）

### 2. SystemUser/User の同期漏れ

**リスク**: データ不整合

**対策**:
- トランザクション境界を適切に設定
- 定期的な整合性チェックバッチを実装

### 3. レート制限の回避

**リスク**: スパム攻撃

**対策**:
- IP ベースのレート制限（既存の OtpService で実装済み）
- reCAPTCHA 導入（将来課題）

---

## 次のステップ（Issue #57 完了後）

- [ ] パスワードポリシー強化（8文字以上、複雑性要件）
- [ ] 初回パスワード変更の強制
- [ ] メール検証期限（7日以内）の実装
- [ ] 2FA（TOTP/SMS）対応
- [ ] セキュリティ監査ログの強化

---

*Powered by Copilot 🤖*
