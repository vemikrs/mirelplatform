# Issue #57: 管理者作成ユーザーのメール検証とログイン改善

> **Status**: In Progress  
> **Priority**: High  
> **Issue**: https://github.com/vemikrs/mirelplatform/issues/57  
> **PR**: https://github.com/vemikrs/mirelplatform/pull/59  
> **Branch**: `feature/57-v3-251210A`

---

## 概要

管理画面（Admin User Service）で作成されたユーザー（例：user11）がログインできない問題を解決し、SaaS基盤として適切なメールアドレス検証フローを確立する。

---

## 問題の背景

### 発生した事象

```
ユーザー: user11
メールアドレス: user11@example.com
作成方法: 管理画面（AdminUserService.createUser()）
状態: email_verified=false
結果: ログイン不可
```

添付画像：認証コード入力画面で「ユーザーが見つかりません」エラー

### 根本原因

#### 1. **SystemUser/User データ不整合**

| テーブル | 作成状況 | 問題 |
|---------|---------|-----|
| `user` | ✅ 作成済み | `system_user_id` が NULL |
| `system_user` | ❌ 未作成 | パスワードログイン時に参照するレコードが存在しない |

[AdminUserService.java#L194-L219](../../backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/AdminUserService.java) では `User` のみを作成し、`SystemUser` を作成していない。

#### 2. **メール検証チェックの欠如**

[AuthenticationServiceImpl.java#L70-L110](../../backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/service/AuthenticationServiceImpl.java) の `login()` メソッドに `emailVerified` チェックが存在しない。

```java
// 現状のログイン処理
public AuthenticationResponse login(LoginRequest request) {
    // ✅ isActiveチェック
    // ✅ accountLockedチェック
    // ✅ パスワード検証
    // ❌ emailVerifiedチェック → 存在しない
}
```

#### 3. **ユーザー作成フローの多様性**

| 作成方法 | `email_verified` | `SystemUser` 作成 | メール検証フロー |
|---------|-----------------|------------------|---------------|
| セルフサインアップ (`/auth/signup`) | `false` | ✅ | ❌ なし |
| OTPサインアップ (`/auth/signup-otp`) | `true` | ✅ | ✅ OTP検証済み |
| OAuth2サインアップ | `true` | ✅ | ✅ OAuth2検証済み |
| **管理者作成** | `false` | ❌ | ❌ なし |

---

## システムアーキテクチャの前提

### SystemUser vs User の役割分離

mirelplatform では、以下の理由により **SystemUser（アカウント）** と **User（アプリユーザー）** を分離している：

| 層 | エンティティ | 役割 | 責務 |
|----|------------|------|-----|
| **Foundation層** | `SystemUser` | 認証アカウント | ログイン、パスワード管理、OAuth2連携 |
| **Application層** | `User` | アプリケーションユーザー | テナント所属、ライセンス、業務データ紐付け |

**理由**：
- 1つの SystemUser（Google アカウント等）が複数 Application の User として存在しうる
- テナント分離、ライセンス管理は Application 層の責務
- 認証基盤の変更が Application に影響しないようにする

**結論**：完全統合は予定せず、適切な紐付けと同期処理で対応する。

---

## 設計方針

### 1. **メール検証ポリシー**

| 状況 | ログイン可否 | 対応 |
|-----|------------|-----|
| `email_verified=true` | ✅ 許可 | 通常ログイン |
| `email_verified=false` | ❌ 拒否 | 検証メール送信を促す専用エラー |

### 2. **管理者作成ユーザーの検証戦略**

**採用方針：初回ログイン時に検証メール送信**

```
[管理者] ユーザー作成
    ↓
[システム] アカウント作成通知メール送信（初期パスワード含む）
    ↓
[ユーザー] ログイン試行
    ↓
[システム] email_verified=false を検出
    ↓
[システム] 検証メール自動送信（OTP/Magic Link）
    ↓
[ユーザー] メール検証完了
    ↓
[システム] ログイン許可
```

**メリット**：
- ✅ セキュリティとUXのバランスが良い
- ✅ メールアドレスの誤入力を検出できる
- ✅ 既存のOTP基盤を活用できる

### 3. **再検証フローの提供**

ユーザー起点で検証メールを再送できる仕組みを提供：

```
GET /auth/resend-verification?email={email}
POST /auth/resend-verification
  {
    "email": "user11@example.com"
  }
```

**セキュリティ考慮**：
- レート制限: 1分間に3回まで
- 列挙攻撃対策: メールアドレスの存在有無を返さない
- 成功/失敗に関わらず同じレスポンス

---

## スコープ外（今回対応しない項目）

- ❌ SystemUser/User の完全統合
- ❌ 2FA（TOTP/SMS）実装
- ❌ パスワードポリシー強化（8文字以上、複雑性要件等）
- ❌ 初回パスワード変更の強制
- ❌ メール検証期限（7日以内に検証しない場合の自動無効化）

これらは別Issueで対応する。

---

## 成功基準

- ✅ 管理画面で作成したユーザーがログインできる
- ✅ `email_verified=false` のユーザーはログインを拒否される
- ✅ 検証メール再送APIが動作する
- ✅ 既存のセルフサインアップ/OTPサインアップが影響を受けない
- ✅ E2Eテストで全フローが検証される

---

## 関連ドキュメント

- [実装計画書](./implementation-plan.md)
- [AuthenticationServiceImpl.java](../../backend/src/main/java/jp/vemi/mirel/foundation/web/api/auth/service/AuthenticationServiceImpl.java)
- [AdminUserService.java](../../backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/AdminUserService.java)
- [OtpService.java](../../backend/src/main/java/jp/vemi/mirel/foundation/service/OtpService.java)

---

*Powered by Copilot 🤖*
