# ライセンス管理システム 鳥瞰図

**作成日:** 2025年11月28日  
**バージョン:** 1.0  
**対象:** mirelplatform 統合ライセンス管理基盤

---

## 1. システム概要

mirelplatform のライセンス管理システムは、**マルチテナント SaaS 環境**における柔軟で堅牢なライセンス制御を実現する統合基盤です。ユーザー単位・テナント単位の両方でライセンスを管理し、アプリケーション機能へのアクセス制御、課金連携、監査ログの統合を提供します。

### 主要機能
- **階層的ライセンス管理**: USER / TENANT スコープでのライセンス付与
- **ティア制御**: FREE, TRIAL, PRO, MAX の4段階ライセンス
- **フィーチャーフラグ統合**: 機能単位の詳細なアクセス制御
- **有効期限管理**: TRIAL の自動期限切れ、PRO/MAX の契約更新管理
- **監査ログ**: すべてのライセンス操作を記録
- **課金システム連携**: 将来的な Stripe / PayPal 等との統合を考慮した設計

---

## 2. アーキテクチャ全体図

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Frontend (React 19)                          │
├────────────────┬──────────────────┬──────────────────┬───────────────┤
│  User Dashboard│  Admin Console   │  Billing Portal  │  Feature UI   │
│  - My Licenses │  - User Mgmt     │  - Upgrade Plans │  - Conditional│
│  - Features    │  - License Mgmt  │  - Payment Hist  │    Display    │
│  - Upgrade CTA │  - Tenant Mgmt   │  - Invoice DL    │  - Lock Icons │
└────────────────┴──────────────────┴──────────────────┴───────────────┘
                                 ▼ HTTPS/REST API
┌──────────────────────────────────────────────────────────────────────┐
│                    API Gateway / Spring Security                     │
│  - JWT Authentication                                                │
│  - Rate Limiting (License Tier別)                                   │
│  - CORS, CSRF Protection                                             │
└──────────────────────────────────────────────────────────────────────┘
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      ExecutionContext (Request Scope)                │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ Current User, Tenant, Effective Licenses, Feature Flags       │ │
│  │ - hasLicense(app, tier): boolean                              │ │
│  │ - hasFeature(featureKey): boolean                             │ │
│  │ - getEffectiveLicenses(): List<ApplicationLicense>            │ │
│  │ - resolveStrategy: TENANT_PRIORITY / USER_PRIORITY / EITHER   │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                       Service Layer (Business Logic)                 │
├────────────────┬──────────────────┬──────────────────┬───────────────┤
│ LicenseService │FeatureFlagService│ BillingService   │ AuditService  │
│ - Grant        │ - CRUD           │ - Create Sub     │ - Record      │
│ - Revoke       │ - Evaluate       │ - Cancel Sub     │ - Query       │
│ - Upgrade      │ - Toggle         │ - Invoice Gen    │ - Export      │
│ - Downgrade    │ - A/B Test (将来)│ - Payment Webhook│ - Compliance  │
└────────────────┴──────────────────┴──────────────────┴───────────────┘
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                         Data Access Layer (JPA)                      │
├────────────────┬──────────────────┬──────────────────┬───────────────┤
│ ApplicationLic │ FeatureFlag      │ Subscription     │ AuditLog      │
│ enseRepository │ Repository       │ Repository       │ Repository    │
└────────────────┴──────────────────┴──────────────────┴───────────────┘
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      Database (PostgreSQL / H2)                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────────┐ │
│  │ mir_application_ │  │ mir_feature_flag │  │ mir_subscription  │ │
│  │ license          │  │                  │  │                   │ │
│  ├──────────────────┤  ├──────────────────┤  ├───────────────────┤ │
│  │ mir_audit_log    │  │ mir_user         │  │ mir_tenant        │ │
│  └──────────────────┘  └──────────────────┘  └───────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
                                 ▲
                    ┌────────────┴────────────┐
                    ▼                         ▼
          ┌──────────────────┐      ┌──────────────────┐
          │  External Billing│      │  Analytics &     │
          │  Provider (将来) │      │  Monitoring      │
          │  - Stripe        │      │  - Prometheus    │
          │  - PayPal        │      │  - Grafana       │
          └──────────────────┘      └──────────────────┘
```

---

## 3. データモデル詳細

### 3.1 コアエンティティ

```sql
-- ApplicationLicense (既存)
CREATE TABLE mir_application_license (
    id VARCHAR(36) PRIMARY KEY,
    subject_type VARCHAR(20) NOT NULL,  -- USER / TENANT
    subject_id VARCHAR(36) NOT NULL,    -- userId or tenantId
    application_id VARCHAR(50) NOT NULL,
    tier VARCHAR(20) NOT NULL,          -- FREE, TRIAL, PRO, MAX
    features TEXT,                       -- JSON: 有効機能リスト (将来拡張)
    granted_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,                -- TRIAL/PRO/MAX の期限
    granted_by VARCHAR(36),
    auto_renew BOOLEAN DEFAULT FALSE,    -- 自動更新フラグ (将来)
    billing_cycle VARCHAR(20),           -- MONTHLY, YEARLY (将来)
    INDEX idx_license_subject (subject_type, subject_id, application_id),
    INDEX idx_license_expires (expires_at)
);

-- FeatureFlag (新規)
CREATE TABLE mir_feature_flag (
    id VARCHAR(36) PRIMARY KEY,
    feature_key VARCHAR(100) NOT NULL UNIQUE,
    feature_name VARCHAR(200) NOT NULL,
    description TEXT,
    application_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,        -- STABLE, BETA, ALPHA, PLANNING, DEPRECATED
    in_development BOOLEAN DEFAULT FALSE,
    required_license_tier VARCHAR(20),  -- FREE, TRIAL, PRO, MAX, NULL
    enabled_by_default BOOLEAN DEFAULT TRUE,
    enabled_for_user_ids TEXT,          -- JSON: 特定ユーザーのみ有効化
    enabled_for_tenant_ids TEXT,        -- JSON: 特定テナントのみ有効化
    rollout_percentage INT DEFAULT 100, -- カナリアリリース用 (将来)
    metadata TEXT,                       -- JSON: 拡張設定
    INDEX idx_ff_application (application_id),
    INDEX idx_ff_status (status)
);

-- Subscription (将来拡張)
CREATE TABLE mir_subscription (
    id VARCHAR(36) PRIMARY KEY,
    license_id VARCHAR(36) NOT NULL,    -- ApplicationLicense.id
    external_subscription_id VARCHAR(100), -- Stripe Subscription ID等
    status VARCHAR(20) NOT NULL,        -- ACTIVE, PAST_DUE, CANCELED, TRIALING
    billing_cycle VARCHAR(20),          -- MONTHLY, YEARLY
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (license_id) REFERENCES mir_application_license(id)
);

-- BillingHistory (将来拡張)
CREATE TABLE mir_billing_history (
    id VARCHAR(36) PRIMARY KEY,
    subscription_id VARCHAR(36),
    invoice_id VARCHAR(100),
    amount DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'JPY',
    status VARCHAR(20),                 -- PAID, PENDING, FAILED
    billing_date TIMESTAMP,
    payment_method VARCHAR(50),
    FOREIGN KEY (subscription_id) REFERENCES mir_subscription(id)
);
```

### 3.2 エンティティ関連図

```
User ────────┐
             ├──< ApplicationLicense >───┐
Tenant ──────┘                           │
                                         ├──< Subscription (将来)
FeatureFlag ─────────────────────────────┘
             (requiredLicenseTier で紐付け)

ApplicationLicense ────< BillingHistory (将来)
```

---

## 4. ライセンス判定ロジック

### 4.1 ExecutionContext によるライセンス解決

```java
/**
 * ライセンス判定戦略
 */
public enum LicenseResolveStrategy {
    TENANT_PRIORITY,  // テナントライセンス優先、なければユーザー (推奨)
    USER_PRIORITY,    // ユーザーライセンス優先、なければテナント
    TENANT_ONLY,      // テナントライセンスのみ (B2B 厳格モード)
    USER_ONLY,        // ユーザーライセンスのみ (個人向け)
    EITHER,           // どちらか一方 (現在の実装)
    BOTH_REQUIRED     // 両方必要 (超セキュア機能向け)
}

/**
 * ExecutionContext 拡張メソッド
 */
public boolean hasLicense(String applicationId, LicenseTier tier, LicenseResolveStrategy strategy) {
    switch (strategy) {
        case TENANT_PRIORITY:
            // 1. テナントライセンスをチェック
            Optional<ApplicationLicense> tenantLicense = effectiveLicenses.stream()
                .filter(l -> l.getSubjectType() == SubjectType.TENANT)
                .filter(l -> l.getApplicationId().equals(applicationId))
                .filter(l -> l.getTier().ordinal() >= tier.ordinal())
                .filter(l -> isNotExpired(l))
                .findFirst();
            
            if (tenantLicense.isPresent()) return true;
            
            // 2. ユーザーライセンスをチェック
            return effectiveLicenses.stream()
                .filter(l -> l.getSubjectType() == SubjectType.USER)
                .filter(l -> l.getApplicationId().equals(applicationId))
                .filter(l -> l.getTier().ordinal() >= tier.ordinal())
                .filter(l -> isNotExpired(l))
                .findAny()
                .isPresent();
        
        case TENANT_ONLY:
            // テナントライセンスのみ
            // ... (実装略)
        
        // 他の戦略も同様に実装
    }
}

/**
 * フィーチャーフラグ統合判定
 */
public boolean hasFeature(String featureKey, LicenseResolveStrategy strategy) {
    FeatureFlag feature = featureFlagRepository.findByFeatureKey(featureKey)
        .orElseThrow(() -> new FeatureNotFoundException(featureKey));
    
    // 1. 開発中フラグチェック
    if (feature.getInDevelopment() && !isDevEnvironment()) {
        return false;
    }
    
    // 2. ライセンス要件チェック
    if (feature.getRequiredLicenseTier() != null) {
        if (!hasLicense(feature.getApplicationId(), feature.getRequiredLicenseTier(), strategy)) {
            return false;
        }
    }
    
    // 3. 個別ユーザー・テナント有効化チェック
    if (feature.getEnabledForUserIds() != null) {
        List<String> enabledUsers = parseJson(feature.getEnabledForUserIds());
        if (!enabledUsers.isEmpty() && !enabledUsers.contains(getCurrentUserId())) {
            return false;
        }
    }
    
    // 4. カナリアリリースチェック (将来)
    if (feature.getRolloutPercentage() < 100) {
        // ユーザーIDのハッシュ値で判定
        // return (hash(getCurrentUserId()) % 100) < feature.getRolloutPercentage();
    }
    
    return feature.getEnabledByDefault();
}
```

### 4.2 AOP による自動ライセンスチェック

```java
/**
 * @RequireLicense アノテーション (既存)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLicense {
    String application();
    LicenseTier tier() default LicenseTier.FREE;
    LicenseResolveStrategy strategy() default LicenseResolveStrategy.TENANT_PRIORITY;
}

/**
 * @RequireFeature アノテーション (新規)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireFeature {
    String featureKey();
    LicenseResolveStrategy strategy() default LicenseResolveStrategy.TENANT_PRIORITY;
}

/**
 * LicenseCheckAspect (既存) + FeatureCheckAspect (新規)
 */
@Aspect
@Component
public class AccessControlAspect {
    
    @Before("@annotation(requireLicense)")
    public void checkLicense(JoinPoint joinPoint, RequireLicense requireLicense) {
        if (!executionContext.hasLicense(
                requireLicense.application(), 
                requireLicense.tier(), 
                requireLicense.strategy())) {
            throw new LicenseRequiredException(requireLicense.application(), requireLicense.tier());
        }
    }
    
    @Before("@annotation(requireFeature)")
    public void checkFeature(JoinPoint joinPoint, RequireFeature requireFeature) {
        if (!executionContext.hasFeature(
                requireFeature.featureKey(), 
                requireFeature.strategy())) {
            throw new FeatureNotEnabledException(requireFeature.featureKey());
        }
    }
}
```

---

## 5. ライセンス管理 API

### 5.1 ユーザー向け API

```yaml
# 現在のライセンス情報取得
GET /users/me/licenses
Response:
  licenses: ApplicationLicense[]
  features: FeatureFlag[]
  upgradeRecommendations: UpgradeOption[]

# 利用可能機能一覧
GET /features/available
Response:
  features: FeatureFlag[]
  grouped_by_application: Map<String, FeatureFlag[]>

# アップグレード見積もり
POST /billing/upgrade/estimate
Request:
  targetTier: PRO | MAX
  billingCycle: MONTHLY | YEARLY
Response:
  monthlyPrice: number
  annualPrice: number
  savings: number
  featuresGained: string[]
```

### 5.2 管理者向け API

```yaml
# ライセンス一覧取得
GET /admin/licenses
Query:
  - subjectType: USER | TENANT
  - applicationId: string
  - tier: FREE | TRIAL | PRO | MAX
  - expiringSoon: boolean (30日以内に期限切れ)
  - page, size
Response:
  content: ApplicationLicense[]
  totalElements: number

# ライセンス付与
POST /admin/licenses/grant
Request:
  subjectType: USER | TENANT
  subjectId: UUID
  applicationId: string
  tier: FREE | TRIAL | PRO | MAX
  expiresAt: timestamp (TRIAL/PRO/MAX の場合必須)
  grantReason: string
Response:
  license: ApplicationLicense

# ライセンス取消
DELETE /admin/licenses/{id}
Query:
  - reason: string
  - notifyUser: boolean

# フィーチャーフラグ管理
GET/POST/PUT/DELETE /admin/features/**
(feature-flag-management-plan.md 参照)
```

### 5.3 課金連携 API (将来拡張)

```yaml
# Stripe Webhook
POST /billing/webhooks/stripe
Request: (Stripe Event Object)
Handlers:
  - checkout.session.completed → ライセンス付与
  - invoice.payment_succeeded → 支払い記録
  - invoice.payment_failed → ライセンス一時停止
  - customer.subscription.deleted → ライセンス取消

# サブスクリプション作成
POST /billing/subscriptions
Request:
  applicationId: string
  tier: PRO | MAX
  billingCycle: MONTHLY | YEARLY
  paymentMethodId: string (Stripe Payment Method ID)
Response:
  subscription: Subscription
  clientSecret: string (Stripe Checkout用)

# サブスクリプションキャンセル
DELETE /billing/subscriptions/{id}
Query:
  - cancelAtPeriodEnd: boolean (即時 or 期末)
```

---

## 6. 運用シナリオ

### 6.1 ライセンスライフサイクル

```
┌──────────────┐
│ 新規ユーザー │ → 自動的に FREE ライセンス付与
└──────────────┘         (signup時)
       │
       ▼
┌──────────────┐
│  TRIAL 開始  │ ← 管理者付与 or セルフアップグレード (14日間)
└──────────────┘
       │
       ├─ 期限内に有料プラン契約 → PRO/MAX ライセンス付与
       │
       └─ 期限切れ → 自動的に FREE にダウングレード
                     (GracePeriod: 3日間は PRO 機能継続)

┌──────────────┐
│  PRO / MAX   │ ← サブスクリプション開始
└──────────────┘
       │
       ├─ 自動更新成功 → 継続
       │
       ├─ 支払い失敗 → PAST_DUE (7日間猶予)
       │                  ├─ 再試行成功 → 復活
       │                  └─ 失敗継続 → SUSPENDED → FREE
       │
       └─ ユーザーがキャンセル → 期末まで継続 → FREE
```

### 6.2 TRIAL ライセンスの自動管理

**バッチジョブ実装 (Spring Scheduler):**

```java
@Scheduled(cron = "0 0 2 * * *") // 毎日午前2時実行
public void expireTrialLicenses() {
    Instant now = Instant.now();
    Instant gracePeriodEnd = now.minusSeconds(3 * 24 * 3600); // 3日前
    
    List<ApplicationLicense> expiredTrials = licenseRepository.findExpiredTrials(gracePeriodEnd);
    
    for (ApplicationLicense license : expiredTrials) {
        // 1. FREE ライセンスに自動切り替え
        ApplicationLicense freeLicense = createFreeLicense(license);
        licenseRepository.save(freeLicense);
        
        // 2. TRIAL ライセンスを論理削除
        license.setDeleteFlag(true);
        licenseRepository.save(license);
        
        // 3. ユーザーに通知メール送信
        emailService.sendTrialExpiredNotification(license);
        
        // 4. 監査ログ記録
        auditLog.record("LICENSE_EXPIRED", license, "TRIAL expired, downgraded to FREE");
    }
}
```

### 6.3 テナント単位ライセンス運用

**企業契約 (B2B) パターン:**

```
企業A (Tenant: enterprise-001)
  ├─ Tenant License: ProMarker MAX (全社員が利用可能)
  └─ Users:
      ├─ user-001 (OWNER) → MAX 利用可能
      ├─ user-002 (MEMBER) → MAX 利用可能
      └─ user-003 (GUEST) → FREE のみ (ゲストはテナントライセンスを継承しない)

判定ロジック (TENANT_PRIORITY):
  1. user-001, user-002 → テナントの MAX ライセンス適用
  2. user-003 → ゲストなのでテナントライセンス無効、個人の FREE のみ
```

**フリーランス (個人) パターン:**

```
User: freelance-user-001
  ├─ User License: ProMarker PRO
  └─ Tenants:
      ├─ personal-workspace (自分のテナント) → PRO 継承
      ├─ client-a-workspace (クライアントA, FREE) → PRO 継承 (USER_PRIORITY)
      └─ client-b-workspace (クライアントB, MAX) → MAX 適用 (TENANT優先)

判定ロジック (TENANT_PRIORITY):
  - personal-workspace: ユーザー PRO
  - client-a-workspace: ユーザー PRO (テナント FREE より優先)
  - client-b-workspace: テナント MAX (ユーザー PRO より高い)
```

---

## 7. セキュリティ・コンプライアンス

### 7.1 監査ログ

すべてのライセンス操作を `mir_audit_log` に記録:

```sql
INSERT INTO mir_audit_log (
    user_id, tenant_id, event_type, resource_type, resource_id, 
    metadata, ip_address, user_agent
) VALUES (
    'admin-001', 'default', 'LICENSE_GRANTED', 'ApplicationLicense', 'license-123',
    '{"tier": "PRO", "application": "promarker", "grantedTo": "user-456"}',
    '192.168.1.100', 'Mozilla/5.0...'
);
```

### 7.2 不正利用防止

- **Rate Limiting**: ライセンスティア別に API レート制限
  - FREE: 100 req/hour
  - TRIAL: 500 req/hour
  - PRO: 2000 req/hour
  - MAX: 10000 req/hour
- **IP Whitelist**: MAX ライセンスで企業IPからのアクセスのみ許可 (オプション)
- **Device Limit**: ユーザーライセンスは最大5デバイスまで (RefreshToken で管理)

### 7.3 GDPR 対応

- ライセンス情報のエクスポート (`GET /users/me/data-export`)
- ライセンス履歴の削除 (アカウント削除時、論理削除 → 物理削除)

---

## 8. 将来拡張計画

### Phase 1 (現在): 基本ライセンス管理
- ✅ ApplicationLicense エンティティ
- ✅ ExecutionContext によるライセンス判定
- ✅ @RequireLicense AOP
- 🔄 FeatureFlag システム (本計画書)
- 🔄 管理画面 (ライセンス CRUD)

### Phase 2: 課金連携
- Stripe 統合 (サブスクリプション、支払い)
- Subscription エンティティ実装
- 自動更新・請求書生成
- 支払い失敗時のグレースピリオド処理

### Phase 3: 高度な機能
- A/B テスト (FeatureFlag の rolloutPercentage 活用)
- カナリアリリース (特定ユーザー群への段階的ロールアウト)
- Usage-based Billing (API 呼び出し回数に応じた従量課金)
- エンタープライズカスタムライセンス (特定機能のみの個別契約)

### Phase 4: 分析・最適化
- ライセンス利用状況ダッシュボード (Grafana)
- アップグレード予測 (機械学習)
- チャーン分析 (解約率分析)
- オンボーディング最適化

---

## 9. 参考資料

- **既存実装:** 
  - `ApplicationLicense.java` - [backend/src/main/java/.../entity/ApplicationLicense.java](../../backend/src/main/java/jp/vemi/mirel/foundation/abst/dao/entity/ApplicationLicense.java)
  - `ExecutionContext.java` - [backend/src/main/java/.../context/ExecutionContext.java](../../backend/src/main/java/jp/vemi/mirel/foundation/context/ExecutionContext.java)
  - `@RequireLicense` - [backend/src/main/java/.../license/RequireLicense.java](../../backend/src/main/java/jp/vemi/mirel/foundation/security/license/RequireLicense.java)

- **関連ドキュメント:**
  - [SaaS対応計画.md](../docs/issue/#39/SaaS対応計画.md)
  - [feature-flag-management-plan.md](./feature-flag-management-plan.md)

- **外部参考:**
  - [Stripe Subscription API](https://stripe.com/docs/api/subscriptions)
  - [LaunchDarkly Feature Flags](https://docs.launchdarkly.com/)
  - [Auth0 RBAC Best Practices](https://auth0.com/docs/manage-users/access-control/rbac)

---

**作成者:** GitHub Copilot 🤖  
**レビュー状態:** Draft  
**次回更新予定:** Phase 2 実装開始時 (2025年Q1予定)
