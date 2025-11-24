# GitHub OAuth App 設定手順

**対象Issue**: #40  
**作成日**: 2025-01-XX  
**担当**: GitHub Copilot

## 概要

GitHub OAuth2認証を有効化するため、GitHub OAuth Appを登録し、mirelplatformに設定します。

---

## 1. GitHub OAuth Appの登録

### 1.1 開発環境用OAuth App登録

1. GitHub Settings にアクセス  
   [https://github.com/settings/developers](https://github.com/settings/developers)

2. **OAuth Apps** → **New OAuth App** をクリック

3. 以下の情報を入力

   | フィールド | 値 |
   |---|---|
   | **Application name** | `mirelplatform (Development)` |
   | **Homepage URL** | `http://localhost:5173` |
   | **Application description** | mirelplatform開発環境用OAuth App |
   | **Authorization callback URL** | `http://localhost:3000/mipla2/login/oauth2/code/github` |

4. **Register application** をクリック

5. 表示される **Client ID** と **Client Secret** をコピー

---

## 2. 環境変数設定

### 2.1 `.env` ファイルに追加

プロジェクトルートの `.env` ファイル（存在しない場合は新規作成）に以下を追加：

```bash
# GitHub OAuth2
GITHUB_CLIENT_ID=<あなたのClient ID>
GITHUB_CLIENT_SECRET=<あなたのClient Secret>
```

**注意**: `.env` は `.gitignore` に含まれているため、コミット不要です。

### 2.2 設定確認

`application.yml` では以下のように環境変数を参照しています：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID:}
            client-secret: ${GITHUB_CLIENT_SECRET:}
```

---

## 3. 本番環境用OAuth App登録（本番デプロイ時）

### 3.1 本番用OAuth App作成

1. 開発環境とは別に新規OAuth Appを作成

2. 以下の情報を入力

   | フィールド | 値 |
   |---|---|
   | **Application name** | `mirelplatform (Production)` |
   | **Homepage URL** | `https://your-domain.com` |
   | **Authorization callback URL** | `https://your-domain.com/mipla2/login/oauth2/code/github` |

3. 本番サーバーの環境変数に Client ID/Secret を設定

---

## 4. OAuth2フロー確認

### 4.1 リダイレクトURL構成

| ステップ | URL |
|---|---|
| 1. ログインボタンクリック | `http://localhost:3000/mipla2/oauth2/authorization/github` |
| 2. GitHub認証ページ遷移 | `https://github.com/login/oauth/authorize?client_id=...` |
| 3. 認可後コールバック | `http://localhost:3000/mipla2/login/oauth2/code/github` |
| 4. JWT発行・リダイレクト | `http://localhost:5173/auth/oauth2/success?token=...` |

### 4.2 Spring Security OAuth2のデフォルトパス

- Authorization Endpoint: `/oauth2/authorization/{registrationId}`
- Redirect Endpoint: `/login/oauth2/code/{registrationId}`

---

## 5. トラブルシューティング

### 5.1 "redirect_uri_mismatch" エラー

**原因**: GitHub OAuth Appに登録したコールバックURLが一致しない

**解決策**:
1. GitHub OAuth App設定画面でコールバックURLを確認
2. `http://localhost:3000/mipla2/login/oauth2/code/github` に正確に一致させる
3. ポート番号・コンテキストパス (`/mipla2`) を含めること

### 5.2 Client ID/Secretが読み込まれない

**原因**: 環境変数が設定されていない

**解決策**:
1. `.env` ファイルが存在するか確認
2. Spring Boot起動時に `--spring.config.import=optional:file:.env` オプションを付ける
3. または、環境変数を直接設定 `export GITHUB_CLIENT_ID=...`

### 5.3 CSRF トークンエラー

**原因**: OAuth2フローでCSRF保護が有効

**解決策**:
1. `WebSecurityConfig.java` で `/login/oauth2/code/**` をCSRF除外に追加
2. または、開発環境で `mipla2.security.csrfEnabled=false` に設定

---

## 6. セキュリティ考慮事項

### 6.1 Client Secretの管理

- **絶対にGitにコミットしない**
- `.env` ファイルは `.gitignore` に含まれていることを確認
- 本番環境では環境変数またはSecrets Managerを使用

### 6.2 Scopeの最小化

現在のScope:
- `read:user`: ユーザー情報（name, email, avatar_url）取得
- `user:email`: メールアドレス取得

**追加しないこと**:
- `repo`: リポジトリへのアクセス（不要）
- `write:*`: 書き込み権限（不要）

### 6.3 State パラメータ

Spring Security OAuth2は自動的にStateパラメータを生成・検証します（CSRF対策）。

---

## 7. 参考リンク

- [GitHub OAuth Apps 公式ドキュメント](https://docs.github.com/ja/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app)
- [Spring Security OAuth2 Client 公式ドキュメント](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)

---

**Powered by Copilot 🤖**
