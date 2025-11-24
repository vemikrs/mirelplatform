# Phase 3: GitHub OAuth2統合 実装完了レポート

**開始日**: 2025-01-XX  
**完了日**: 2025-01-XX  
**担当**: GitHub Copilot  
**対象Issue**: #40

## 概要

GitHub OAuth2によるソーシャルログインとユーザーアバター機能を実装しました。

---

## 実装内容

### Phase 3.1: Spring Security OAuth2設定とGitHub App設定

**コミット**: `4140bdf` - feat(auth): Phase 3.1 - Spring Security OAuth2とGitHub App設定追加

**実装ファイル**:
- `application.yml` - GitHub OAuth2クライアント設定追加
- `WebSecurityConfig.java` - `oauth2Login()` 設定、CSRF除外追加
- `docs/issue/#40/github-oauth-setup.md` - GitHub OAuth App登録手順書

**技術詳細**:
```yaml
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
```

**CSRF除外**:
- `/login/oauth2/code/**` - OAuth2コールバックURL
- `/oauth2/**` - OAuth2認証エンドポイント

---

### Phase 3.2: OAuth2ユーザー情報取得サービス実装

**コミット**: `b769ea3` - feat(auth): Phase 3.2 - OAuth2ユーザー情報取得サービス実装

**実装ファイル**:
- `SystemUser.java` - `avatarUrl`, `oauth2Provider`, `oauth2ProviderId` フィールド追加
- `SystemUserRepository.java` - `findByOauth2ProviderAndOauth2ProviderId()` メソッド追加
- `GitHubOAuth2UserInfo.java` - GitHub API属性を格納するDTO
- `CustomOAuth2UserService.java` - OAuth2認証後のユーザー作成/更新処理

**処理フロー**:
1. GitHubからユーザー情報取得 (login, email, avatar_url, id)
2. OAuth2プロバイダーIDで既存ユーザー検索
3. 既存ユーザーがいれば更新、いなければメールアドレスで検索
4. メールアドレスで既存ユーザーがいればOAuth2情報を紐付け
5. 新規ユーザーの場合は作成（ユーザー名=GitHubログイン名）

**主要メソッド**:
- `processGitHubUser()` - GitHubユーザー情報処理
- `createSystemUserFromGitHub()` - 新規SystemUser作成
- `updateSystemUserFromGitHub()` - 既存SystemUser更新

---

### Phase 3.3: アバター保存サービス実装

**コミット**: `88d4c9b` - feat(auth): Phase 3.3 - アバター保存サービス実装

**実装ファイル**:
- `AvatarService.java` - アバター画像ダウンロード・保存・取得
- `UserAvatarController.java` - GET `/api/users/{userId}/avatar` エンドポイント
- `CustomOAuth2UserService.java` - アバターダウンロード統合

**主要機能**:
- `downloadAndSaveAvatar()` - GitHub画像を `data/storage/avatars` に保存
- `getAvatar()` - ユーザーIDでアバター画像取得
- `deleteAvatar()` - アバター画像削除
- 最大サイズ5MB、拡張子自動判定 (.jpg/.png/.gif/.jpeg)

**APIエンドポイント**:
```
GET /mipla2/api/users/{userId}/avatar
Content-Type: image/jpeg
Cache-Control: public, max-age=3600
```

---

### Phase 3.4: OAuth2ログインハンドラー実装

**コミット**: `991ff3e` - feat(auth): Phase 3.4 - OAuth2ログインハンドラー実装

**実装ファイル**:
- `OAuth2AuthenticationSuccessHandler.java` - 認証成功ハンドラー
- `OAuth2AuthenticationFailureHandler.java` - 認証失敗ハンドラー
- `WebSecurityConfig.java` - ハンドラー登録

**成功時処理**:
1. OAuth2UserからGitHub IDを取得
2. SystemUserをOAuth2プロバイダーIDで検索
3. `JwtService.generateToken()` でJWTトークン生成
4. `/auth/oauth2/success?token={jwt}` にリダイレクト

**失敗時処理**:
- エラーログ記録
- `/login?error=oauth2` にリダイレクト

---

### Phase 3.5: フロントエンドGitHubログインボタン実装

**コミット**: `d31f5c4` - feat(auth): Phase 3.5 - フロントエンドGitHubログインボタン実装

**実装ファイル**:
- `LoginPage.tsx` - GitHubログインボタン追加
- `OAuthCallbackPage.tsx` - OAuth2コールバック処理
- `router.config.tsx` - `/auth/oauth2/success` ルート追加

**GitHubログインボタン**:
```tsx
<Button
  type="button"
  variant="outline"
  onClick={() => window.location.href = 'http://localhost:3000/mipla2/oauth2/authorization/github'}
>
  <GitHubIcon />
  GitHubでログイン
</Button>
```

**コールバック処理**:
1. クエリパラメータから `token` 取得
2. `authStore.setToken(token)` でトークン保存
3. `navigate('/')` でダッシュボードへ遷移

---

### Phase 3.6: アバター表示コンポーネント実装

**コミット**: `1b4aeb4` - feat(ui): Phase 3.6 - アバター表示コンポーネント実装

**実装ファイル**:
- `packages/ui/src/components/Avatar.tsx` - アバターコンポーネント
- `packages/ui/src/index.ts` - Avatar エクスポート
- `UserMenu.tsx` - アバター表示統合
- `authStore.ts` - User型に `avatarUrl` 追加

**Avatarコンポーネント機能**:
- サイズ: `sm` (32px), `md` (40px), `lg` (48px), `xl` (64px)
- フォールバック文字（イニシャル）
- 画像読み込みエラーハンドリング
- デフォルトアバターアイコン（SVG）

**使用例**:
```tsx
<Avatar 
  src={user.avatarUrl}
  alt={user.displayName}
  fallback={user.displayName?.charAt(0).toUpperCase()}
  size="md"
/>
```

---

## 技術スタック

### バックエンド
- **Spring Security OAuth2 Client 3.3.0** - OAuth2認証
- **Spring Security 6.4.0-RC1** - セキュリティフレームワーク
- **JWT (Nimbus JOSE+JWT)** - トークン生成
- **RestTemplate** - アバター画像ダウンロード

### フロントエンド
- **React 19** - UIフレームワーク
- **React Router 7** - ルーティング
- **Zustand** - 状態管理
- **@mirel/ui** - デザインシステム

---

## フロー図

### OAuth2ログインフロー

```
[User] → [LoginPage: GitHubボタンクリック]
  ↓
[/mipla2/oauth2/authorization/github]
  ↓
[GitHub OAuth2認証ページ]
  ↓ (ユーザー認可)
[/mipla2/login/oauth2/code/github] (Spring Security)
  ↓
[CustomOAuth2UserService.loadUser()]
  ↓ (SystemUser作成/更新)
[OAuth2AuthenticationSuccessHandler]
  ↓ (JWT発行)
[/auth/oauth2/success?token={jwt}] (フロントエンド)
  ↓
[OAuthCallbackPage: トークン保存]
  ↓
[Dashboard]
```

### アバター保存フロー

```
[GitHub OAuth2認証成功]
  ↓
[CustomOAuth2UserService.processGitHubUser()]
  ↓
[SystemUser作成/更新]
  ↓
[downloadAndUpdateAvatar()]
  ↓
[AvatarService.downloadAndSaveAvatar()]
  ↓ (RestTemplate)
[GitHub avatar_url から画像ダウンロード]
  ↓
[data/storage/avatars/{userId}.{ext}] に保存
  ↓
[SystemUser.avatarUrl = /api/users/{userId}/avatar]
```

---

## データベーススキーマ変更

```sql
ALTER TABLE mir_system_user
ADD COLUMN avatar_url VARCHAR(500),
ADD COLUMN oauth2_provider VARCHAR(50),
ADD COLUMN oauth2_provider_id VARCHAR(255);

CREATE INDEX idx_oauth2_provider ON mir_system_user(oauth2_provider, oauth2_provider_id);
```

---

## テスト手順

### 1. GitHub OAuth App設定

1. GitHub Settings → Developer settings → OAuth Apps → New OAuth App
2. Application name: `mirelplatform (Development)`
3. Homepage URL: `http://localhost:5173`
4. Authorization callback URL: `http://localhost:3000/mipla2/login/oauth2/code/github`
5. Client ID と Client Secret を `.env` に設定:
   ```bash
   GITHUB_CLIENT_ID=<your_client_id>
   GITHUB_CLIENT_SECRET=<your_client_secret>
   ```

### 2. ローカル起動

```bash
# バックエンド起動
./gradlew :backend:bootRun --args='--spring.profiles.active=dev'

# フロントエンド起動
pnpm --filter frontend-v3 dev
```

### 3. テストフロー

1. http://localhost:5173/login にアクセス
2. 「GitHubでログイン」ボタンをクリック
3. GitHubログインページで認証
4. アプリケーションを認可
5. 自動的に http://localhost:5173/ にリダイレクト
6. ヘッダー右上にGitHubアバター画像が表示される

---

## セキュリティ考慮事項

### 1. CSRF保護

- OAuth2コールバックURL (`/login/oauth2/code/**`) をCSRF除外
- Spring Securityが自動的にStateパラメータを生成・検証

### 2. Scope最小化

- `read:user` - ユーザー情報取得のみ
- `user:email` - メールアドレス取得のみ
- 不要な権限（`repo`, `write:*`）は要求しない

### 3. アバター画像検証

- ファイルサイズ制限: 5MB
- MIME type検証: `image/*`
- 拡張子ホワイトリスト: `.jpg`, `.png`, `.gif`, `.jpeg`

### 4. JWT発行

- 有効期限: 1時間
- 署名アルゴリズム: HS256
- Claims: `sub` (username), `roles`, `iss`, `iat`, `exp`

---

## 既知の制限事項

### 1. プロフィール画像アップロード未実装

- 現在はOAuth2プロバイダー（GitHub）からのアバターのみ対応
- ユーザー自身によるアバター画像アップロードは Phase 4 で実装予定

### 2. メールアドレス未公開のGitHubユーザー

- GitHubでメールアドレスを非公開にしている場合、`{login}@github.oauth2.local` を仮のメールアドレスとして使用

### 3. OAuth2プロバイダーの拡張

- 現在はGitHubのみ対応
- Google, Microsoft等の追加は将来対応予定

---

## 次のステップ（Phase 4以降）

### Phase 4: 単体テスト

- `CustomOAuth2UserService` のテスト
- `AvatarService` のテスト
- `OAuth2AuthenticationSuccessHandler` のテスト

### Phase 5: E2Eテスト

- Playwright でOAuth2ログインフローをテスト
- アバター表示の確認

### Phase 6: プロフィール画面

- ユーザー自身によるアバター画像アップロード
- プロフィール情報編集

---

## 参考資料

- [Spring Security OAuth2 Client 公式ドキュメント](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [GitHub OAuth Apps 公式ドキュメント](https://docs.github.com/ja/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app)
- [Radix UI Avatar](https://www.radix-ui.com/primitives/docs/components/avatar)

---

**Powered by Copilot 🤖**
