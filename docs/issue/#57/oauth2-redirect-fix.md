# OAuth2意図しないリダイレクト問題の根本修正

## 問題の本質

認証不要のエンドポイント（例: `/auth/setup-account`）にアクセスした際、なぜかGitHub OAuth2にリダイレクトされていた。

### 根本原因

Spring Securityの`oauth2Login()`が有効な場合、**デフォルトですべての未認証アクセスをOAuth2フローにリダイレクトする**設定になっていた。

```java
// 修正前
http.oauth2Login(oauth2 -> oauth2
        .loginPage("/oauth2/authorization/github") // これが問題
        ...
)
.exceptionHandling(handling -> handling
        .defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                new AntPathRequestMatcher("/users/**")) // 一部のパスのみ401
        ...
);
```

問題点:
1. `exceptionHandling()` で一部のパス（`/users/**`, `/api/**`）のみ401を返す設定
2. **`/auth/**` パスは設定されていない** → デフォルトでOAuth2リダイレクト
3. `permitAll()` に追加しても、根本的な設計が間違っている

## 修正内容

**設計方針**:
- **未認証アクセスはデフォルトで401を返す**（OAuth2リダイレクトしない）
- OAuth2は `/oauth2/authorization/github` への**明示的なアクセス**のみ有効

```java
// 修正後
http.oauth2Login(oauth2 -> oauth2
        // loginPage() を削除 - デフォルトの振る舞いを使用
        .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService))
        .successHandler(oauth2SuccessHandler)
        .failureHandler(oauth2FailureHandler));

// すべての未認証アクセスに対してデフォルトで401を返す
http.exceptionHandling(handling -> handling
        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
```

## 効果

### Before (修正前)

```bash
$ curl -i http://localhost:3000/mipla2/auth/verify-setup-token?token=xxx
HTTP/1.1 302
Location: http://localhost:3000/mipla2/oauth2/authorization/github
```

### After (修正後)

```bash
$ curl -i http://localhost:3000/mipla2/auth/verify-setup-token?token=xxx
HTTP/1.1 200
Content-Type: application/json
{"success":true,"email":"admin@example.com"}
```

## ユーザー体験の改善

| シナリオ | 修正前 | 修正後 |
|---|---|---|
| ユーザーがパスワードログインを選択 | 正常動作 | 正常動作 |
| ユーザーがGitHub OAuth2を選択 | 正常動作 | 正常動作 |
| **アカウントセットアップリンクをクリック** | GitHub OAuth2に飛ばされる ❌ | セットアップページ表示 ✅ |
| 未認証でAPI呼び出し | GitHub OAuth2に飛ばされる ❌ | 401エラー返却 ✅ |

## テスト確認

```bash
# 1. トークン検証エンドポイント（認証不要）
$ curl -i http://localhost:3000/mipla2/auth/verify-setup-token?token=valid-token
HTTP/1.1 200

# 2. アカウントセットアップ（認証不要）
$ curl -i -X POST http://localhost:3000/mipla2/auth/setup-account \
  -H "Content-Type: application/json" \
  -d '{"model":{"token":"xxx","password":"newpass123"}}'
HTTP/1.1 200

# 3. 認証必須エンドポイント（401返却）
$ curl -i http://localhost:3000/mipla2/auth/me
HTTP/1.1 401

# 4. GitHub OAuth2（明示的アクセス時のみ有効）
# ユーザーがボタンクリック → /oauth2/authorization/github → GitHub認証フロー
```

## Commit

```
2dbeda9 - fix(security): prevent unintended OAuth2 redirects - default to 401 for unauthenticated access
ae4d1a0 - fix(issue-57): add setup-account endpoints to permitAll (不要になったが記録)
```

## 参考

- [Spring Security - OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html)
- [Spring Security - Exception Handling](https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-exceptiontranslationfilter)

---

**Powered by Copilot 🤖**
