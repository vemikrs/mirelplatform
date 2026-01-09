# JWT 本番運用 実装計画 v3

**作成日**: 2026-01-09  
**基準**: `docs/issue/#40/JWT/jwt-implementation-phases.md` + 自律型鍵管理要件

---

## 概要

Phase 1-4（Backend JWT 基盤、ログイン、フロント対応、OTP）は完了済み。  
本計画は Phase 5 以降の残作業を定義する。

---

## 開発方針

### 段階的コミット戦略

- **1 コミット = 1 機能 or 1 ファイル単位**
- 各コミット後にコンパイル/テスト確認
- コミットメッセージ: `[Phase.Step] type(scope): description`

### 実装アプローチ

1. **RS256 直接実装**: HS256 からの移行ではなく、最初から RS256 で構築
2. **DB 内暗号化**: 外部 KMS 不要、マスターキー(KEK)で DEK を暗号化し DB 保存
3. **後方互換性不要**: 本番未稼働のため、既存トークンとの共存期間なし
4. **テスト駆動**: 各 Phase 完了時にユニット/統合テスト追加

---

## Phase 5: 自律型鍵管理基盤

### 5.1 暗号化サービス（CryptoService）

**目的**: マスターキー(KEK)で JWT 署名鍵(DEK)を暗号化し DB 保存

**コミット単位**:

```
[5.1.1] feat(security): add CryptoService with AES-256-GCM
[5.1.2] feat(config): add MIREL_MASTER_KEY env config
[5.1.3] test(security): add CryptoService unit tests
```

**タスク**:

- [ ] `CryptoService.java`（AES-256-GCM 暗号化/復号）
- [ ] `MIREL_MASTER_KEY`環境変数（32 文字以上必須）
- [ ] 起動時バリデーション

---

### 5.2 鍵管理テーブル

**コミット単位**:

```
[5.2.1] feat(db): add mir_system_security_key migration
[5.2.2] feat(entity): add SystemSecurityKey entity/repository
```

**テーブル設計**:

```sql
CREATE TABLE mir_system_security_key (
    key_id VARCHAR(64) PRIMARY KEY,
    algorithm VARCHAR(16) NOT NULL,      -- RS256
    public_key TEXT NOT NULL,            -- PEM形式
    private_key_enc TEXT NOT NULL,       -- KEKで暗号化
    status VARCHAR(16) NOT NULL,         -- CURRENT/VALID/EXPIRED
    use_purpose VARCHAR(32) NOT NULL,    -- ACCESS_TOKEN_SIGN
    activated_at TIMESTAMP,
    retired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
```

---

### 5.3 RS256 JWT 実装

**コミット単位**:

```
[5.3.1] feat(jwt): add RSA key generation utility
[5.3.2] feat(jwt): implement RS256 JwtService with kid header
[5.3.3] feat(jwt): add key cache with DB lookup
[5.3.4] test(jwt): add RS256 token tests
```

**タスク**:

- [ ] RSA 2048bit 鍵ペア生成
- [ ] `JwtService`改修: RS256 署名、`kid`ヘッダ
- [ ] 起動時: DB 鍵ロードまたは新規生成
- [ ] 検証時: `kid`で公開鍵ルックアップ

---

### 5.4 自動鍵ローテーション

**コミット単位**:

```
[5.4.1] feat(scheduler): add KeyRotationScheduler
[5.4.2] feat(config): add rotation period config (30 days)
[5.4.3] feat(jwt): implement key status transition
```

**タスク**:

- [ ] `KeyRotationScheduler`（毎日実行）
- [ ] 30 日経過でローテーション
- [ ] CURRENT→VALID→EXPIRED 遷移
- [ ] キャッシュリロード（DB ポーリング）

---

## Phase 6: 永続ログイン（Remember Me）

**コミット単位**:

```
[6.1] feat(auth): add rememberMe to LoginRequest
[6.2] feat(auth): extend RefreshTokenService for long-lived tokens
[6.3] feat(ui): add Remember Me checkbox
[6.4] feat(auth): add logout-all-devices API
```

**タスク**:

- [ ] `rememberMe: boolean`リクエストフィールド
- [ ] リフレッシュトークン有効期限分岐（true: 90 日, false: 24 時間）
- [ ] フロントエンドチェックボックス
- [ ] `DELETE /auth/sessions`（全デバイスログアウト）

---

## Phase 7: 自動リフレッシュ & UX 改善

### 7.1 apiClient 自動リフレッシュ **（必須）**

**コミット単位**:

```
[7.1.1] feat(api): add 401 interceptor with auto-refresh
[7.1.2] feat(auth): implement authStore.refresh()
[7.1.3] feat(auth): add refresh failure logout handling
```

**実装方針**:

```typescript
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      await authStore.getState().refresh();
      return apiClient(error.config);
    }
    return Promise.reject(error);
  }
);
```

---

### 7.2 初回ロード時リフレッシュ **（必須）**

**コミット単位**:

```
[7.2.1] feat(auth): add initial refresh on app load
[7.2.2] feat(auth): add redirect to login on refresh failure
```

---

### 7.3 マルチタブ同期 **（必須）**

**コミット単位**:

```
[7.3.1] feat(auth): add BroadcastChannel for token sync
[7.3.2] feat(auth): add logout broadcast to all tabs
[7.3.3] feat(auth): add token update broadcast
```

---

### 7.4 リフレッシュトークン猶予期間

**コミット単位**:

```
[7.4.1] feat(auth): add grace period config (30s)
[7.4.2] feat(auth): implement grace period in RefreshTokenService
```

---

## Phase 8: 権限モデル・監査ログ

### 8.1 ゲストモード制御

```
[8.1.1] fix(security): disable guest mode in production profile
```

### 8.2 権限モデル整備

```
[8.2.1] refactor(auth): verify roles claim with ExecutionContext
[8.2.2] audit(auth): review @PreAuthorize annotations
```

### 8.3 監査ログ

```
[8.3.1] feat(audit): add structured auth event logging
```

**出力イベント**: ログイン成功/失敗、OTP 検証、リフレッシュ、ログアウト

---

## Phase 9: 統合テスト & E2E

### 9.1 E2E テスト

```
[9.1.1] test(e2e): add password login flow
[9.1.2] test(e2e): add OTP login flow
[9.1.3] test(e2e): add token refresh flow
[9.1.4] test(e2e): add multi-tab logout sync
```

### 9.2 統合テスト

```
[9.2.1] test(integration): add AuthenticationService tests
[9.2.2] test(integration): add key rotation tests
```

### 9.3 ドキュメント

```
[9.3.1] docs: update jwt-implementation-phases.md
[9.3.2] docs: create operations guide
```

---

## 実装順序

| 順位 | Phase   | 内容                     | 推定工数 | 必須度 |
| ---- | ------- | ------------------------ | -------- | ------ |
| 1    | 5.1-5.2 | 暗号化基盤 + DB テーブル | 1 日     | 必須   |
| 2    | 5.3     | RS256 JWT 実装           | 1.5 日   | 必須   |
| 3    | 5.4     | 自動鍵ローテーション     | 0.5 日   | 必須   |
| 4    | 6       | Remember Me              | 0.5 日   | 必須   |
| 5    | 7.1-7.2 | 自動リフレッシュ         | 1 日     | 必須   |
| 6    | 7.3-7.4 | マルチタブ同期・猶予期間 | 1 日     | 必須   |
| 7    | 8       | 権限・監査ログ           | 1 日     | 必須   |
| 8    | 9       | 統合テスト・E2E          | 1.5 日   | 必須   |

**合計推定工数**: 8-9 日

---

## 完了条件

1. **Phase 5**: RS256 署名動作、鍵 DB 暗号化保存、ローテーション動作
2. **Phase 6**: Remember Me チェックボックス動作、長期トークン発行
3. **Phase 7**: 自動リフレッシュ動作、マルチタブ同期動作
4. **Phase 8**: 本番ゲストモード無効、監査ログ出力
5. **Phase 9**: E2E テストグリーン、ドキュメント更新
