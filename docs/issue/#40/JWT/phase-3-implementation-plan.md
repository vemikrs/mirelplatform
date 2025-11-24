# Phase 3: GitHub OAuth2統合 実装計画

**開始日**: 2025-01-XX  
**担当**: GitHub Copilot  
**対象Issue**: #40

## 目標

GitHub OAuth2によるソーシャルログインを実装し、ユーザーアバター機能を統合する。

## 実装ステップ

### Phase 3.1: Spring Security OAuth2設定とGitHub App設定

**目的**: Spring Security OAuth2クライアント設定、GitHub OAuth App登録

**タスク**:
1. `build.gradle` に OAuth2依存関係追加
2. `application.yml` に GitHub OAuth2設定追加
3. GitHub OAuth App 登録手順ドキュメント作成
4. `SecurityConfig.java` でOAuth2ログイン有効化

**成果物**:
- `build.gradle` - `spring-boot-starter-oauth2-client` 追加
- `application.yml` - `spring.security.oauth2.client.*` 設定
- `docs/issue/#40/github-oauth-setup.md` - GitHub App設定手順

---

### Phase 3.2: OAuth2ユーザー情報取得サービス実装

**目的**: GitHubユーザー情報を取得し、SystemUserと紐付ける

**タスク**:
1. `GitHubOAuth2UserInfo.java` - GitHub APIレスポンス型定義
2. `OAuth2UserService.java` - ユーザー情報取得・SystemUser作成/更新
3. SystemUserエンティティに `avatarUrl` フィールド追加
4. SystemUserエンティティに `oauth2Provider`, `oauth2ProviderId` フィールド追加

**成果物**:
- `GitHubOAuth2UserInfo.java` - login, email, name, avatar_url, id
- `CustomOAuth2UserService.java` - `DefaultOAuth2UserService` 拡張
- SystemUser migration - avatar_url, oauth2_provider, oauth2_provider_id

---

### Phase 3.3: アバター保存サービス実装

**目的**: GitHub アバター画像をダウンロードして保存する

**タスク**:
1. `AvatarService.java` - アバター画像ダウンロード・保存・URL生成
2. `data/storage/avatars/` ディレクトリ作成
3. `/api/users/{userId}/avatar` エンドポイント実装（画像提供）
4. デフォルトアバター画像用意

**成果物**:
- `AvatarService.java` - downloadAvatar(), saveAvatar(), getAvatarUrl()
- `UserAvatarController.java` - GET /api/users/{userId}/avatar
- `data/storage/avatars/` - アバター画像保存先

---

### Phase 3.4: OAuth2ログインハンドラー実装

**目的**: OAuth2認証成功後の処理（JWT発行、リダイレクト）

**タスク**:
1. `OAuth2AuthenticationSuccessHandler.java` - 認証成功ハンドラー
2. `OAuth2AuthenticationFailureHandler.java` - 認証失敗ハンドラー
3. JWT発行ロジック統合
4. フロントエンドへのリダイレクト設定

**成果物**:
- `OAuth2AuthenticationSuccessHandler.java` - JWT発行、`/auth/oauth2/success` リダイレクト
- `OAuth2AuthenticationFailureHandler.java` - エラーログ、`/login?error=oauth2` リダイレクト
- SecurityConfig - ハンドラー登録

---

### Phase 3.5: フロントエンドGitHubログインボタン実装

**目的**: GitHubログインボタンとOAuth2フロー統合

**タスク**:
1. `LoginPage.tsx` に「GitHubでログイン」ボタン追加
2. OAuth2リダイレクト処理
3. JWT受け取り処理（`/auth/oauth2/success?token=xxx`）
4. authStore へのトークン保存

**成果物**:
- `LoginPage.tsx` - GitHubロゴ付きボタン
- `OAuthCallbackPage.tsx` - `/auth/oauth2/success` でトークン受け取り

---

### Phase 3.6: アバター表示コンポーネント実装

**目的**: ユーザーアバター画像を各所に表示

**タスク**:
1. `Avatar.tsx` コンポーネント作成（@mirel/ui）
2. ヘッダー・サイドバーにアバター表示
3. プロフィール設定画面でアバター更新機能
4. フォールバック画像（デフォルトアバター）

**成果物**:
- `packages/ui/src/components/Avatar.tsx` - アバターコンポーネント
- `RootLayout.tsx` - ヘッダーにアバター表示
- `ProfilePage.tsx` - アバター変更機能

---

## 技術スペック

### GitHub OAuth2設定

```yaml
# application.yml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope:
              - read:user
              - user:email
        provider:
          github:
            user-name-attribute: login
```

### SystemUserエンティティ拡張

```java
@Entity
@Table(name = "mir_system_user")
public class SystemUser {
    // 既存フィールド...
    
    /**
     * アバター画像URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    /**
     * OAuth2プロバイダー (github, google, etc)
     */
    @Column(name = "oauth2_provider", length = 50)
    private String oauth2Provider;
    
    /**
     * OAuth2プロバイダー固有ID
     */
    @Column(name = "oauth2_provider_id", length = 255)
    private String oauth2ProviderId;
}
```

### GitHub OAuth App設定

1. GitHub Settings → Developer settings → OAuth Apps → New OAuth App
2. Application name: `mirelplatform (Development)`
3. Homepage URL: `http://localhost:5173`
4. Authorization callback URL: `http://localhost:3000/login/oauth2/code/github`
5. Client ID と Client Secret を `.env` に設定

---

## セキュリティ考慮事項

1. **CSRF対策**: Spring Security のデフォルトCSRF保護を有効化
2. **State パラメータ**: OAuth2フローでstate検証（Spring Securityが自動処理）
3. **アバター画像検証**: ファイルサイズ制限（5MB）、MIME type検証（image/*)
4. **OAuth2プロバイダーID保存**: 既存メール重複時の紐付けロジック

---

## エラーハンドリング

- **GitHub API失敗**: デフォルトアバターを使用、ログ記録
- **既存ユーザー重複**: メールアドレスで既存SystemUserに紐付け、oauth2ProviderId更新
- **アバター保存失敗**: ログ記録、デフォルトアバター使用
- **JWT発行失敗**: OAuth2失敗ハンドラーでエラーページにリダイレクト

---

## テスト計画

### 単体テスト
- `CustomOAuth2UserService` - ユーザー情報取得・作成ロジック
- `AvatarService` - 画像ダウンロード・保存・URL生成
- `OAuth2AuthenticationSuccessHandler` - JWT発行・リダイレクト

### 統合テスト
- OAuth2ログインフロー（モックGitHub API）
- アバター画像アップロード・取得
- 既存ユーザーとの紐付け

### E2Eテスト
- GitHubログインボタンクリック → 認証 → ダッシュボード遷移
- アバター表示確認（ヘッダー・プロフィール）

---

## マイルストーン

- **Phase 3.1-3.2**: バックエンドOAuth2基盤（2コミット）
- **Phase 3.3-3.4**: アバター・ハンドラー実装（2コミット）
- **Phase 3.5-3.6**: フロントエンド統合（2コミット）
- **合計**: 6コミット、推定2-3時間

---

**Powered by Copilot 🤖**
