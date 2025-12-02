# エンタープライズ機能 設計書

## 📋 概要

大企業・高セキュリティ要件顧客向けの高度な機能群。

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                        Enterprise Features Overview                                 │
├────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────────────┐ │
│   │                     Identity & Access Management                             │ │
│   │                                                                              │ │
│   │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │ │
│   │  │   SAML 2.0   │  │   OIDC /     │  │    SCIM      │  │    JIT       │   │ │
│   │  │    SSO       │  │   OAuth2     │  │  Provisioning│  │   Creation   │   │ │
│   │  │              │  │              │  │              │  │              │   │ │
│   │  │ IdP連携      │  │ Google/Azure │  │ 自動同期     │  │ 初回ログイン │   │ │
│   │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │ │
│   └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────────────┐ │
│   │                       Compliance & Governance                                │ │
│   │                                                                              │ │
│   │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │ │
│   │  │    Data      │  │   Audit      │  │   Custom     │  │  Compliance  │   │ │
│   │  │  Residency   │  │    Logs      │  │  Retention   │  │   Reports    │   │ │
│   │  │              │  │              │  │              │  │              │   │ │
│   │  │ 地域データ保存│  │ 全操作記録   │  │ データ保持   │  │ SOC2/ISO対応│   │ │
│   │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │ │
│   └─────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────────────┐ │
│   │                      Dedicated Infrastructure                                │ │
│   │                                                                              │ │
│   │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │ │
│   │  │   Dedicated  │  │   Custom     │  │   White      │  │    VPN/      │   │ │
│   │  │   Instances  │  │    SLA       │  │  Labeling    │  │  IP Restrict │   │ │
│   │  │              │  │              │  │              │  │              │   │ │
│   │  │ 専用環境     │  │ 99.95%+保証  │  │ ブランド変更 │  │ 接続制限    │   │ │
│   │  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘   │ │
│   └─────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 1️⃣ SAML 2.0 / OIDC SSO

### 1.1 認証フロー

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         SAML 2.0 SSO Flow                                   │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────┐        ┌────────────┐         ┌─────────────┐                 │
│  │  User   │        │   Mirel    │         │    IdP      │                 │
│  │ Browser │        │  Platform  │         │ (Okta/Azure)│                 │
│  └────┬────┘        └─────┬──────┘         └──────┬──────┘                 │
│       │                   │                       │                         │
│       │  1. Access        │                       │                         │
│       │─────────────────→ │                       │                         │
│       │                   │                       │                         │
│       │  2. SAML Request  │                       │                         │
│       │ ←─────────────────│                       │                         │
│       │                   │                       │                         │
│       │  3. Redirect to IdP                       │                         │
│       │──────────────────────────────────────────→│                         │
│       │                   │                       │                         │
│       │  4. Authenticate  │                       │                         │
│       │←─────────────────────────────────────────→│                         │
│       │                   │                       │                         │
│       │  5. SAML Response │                       │                         │
│       │←──────────────────────────────────────────│                         │
│       │                   │                       │                         │
│       │  6. POST Response │                       │                         │
│       │─────────────────→ │                       │                         │
│       │                   │                       │                         │
│       │                   │ 7. Validate           │                         │
│       │                   │    & Create Session   │                         │
│       │                   │                       │                         │
│       │  8. JWT Token     │                       │                         │
│       │ ←─────────────────│                       │                         │
│       │                   │                       │                         │
└────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 データモデル

```java
@Entity
@Table(name = "sso_configurations")
public class SsoConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SsoProvider provider; // SAML, OIDC

    private String displayName; // "Company SSO"

    // SAML 設定
    private String entityId;
    private String ssoUrl;
    private String sloUrl; // Single Logout URL
    @Column(columnDefinition = "text")
    private String x509Certificate;

    // OIDC 設定
    private String issuer;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String clientId;
    private String clientSecret;
    private String scopes; // "openid profile email"

    // 共通設定
    @Enumerated(EnumType.STRING)
    private SsoEnforcement enforcement; // OPTIONAL, REQUIRED, DOMAIN_BASED

    @ElementCollection
    @CollectionTable(name = "sso_email_domains")
    private Set<String> emailDomains; // ["company.com", "company.co.jp"]

    private boolean autoProvision = true; // JIT作成
    private String defaultRoleId; // 新規ユーザーのデフォルトロール

    @Column(columnDefinition = "jsonb")
    private String attributeMapping; // SAML属性マッピング

    private boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 1.3 Spring Security設定

```java
@Configuration
@EnableWebSecurity
public class SamlSecurityConfig {

    @Bean
    public SecurityFilterChain samlFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher("/saml/**")
            .saml2Login(saml -> saml
                .relyingPartyRegistrationRepository(relyingPartyRegistrationRepository())
                .authenticationSuccessHandler(samlSuccessHandler())
            )
            .saml2Logout(Customizer.withDefaults())
            .build();
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        // テナントごとのSSO設定を動的にロード
        return new DynamicRelyingPartyRegistrationRepository(ssoConfigurationRepository);
    }

    @Bean
    public AuthenticationSuccessHandler samlSuccessHandler() {
        return (request, response, authentication) -> {
            Saml2AuthenticatedPrincipal principal = 
                (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
            
            // 属性からユーザー情報取得
            String email = principal.getFirstAttribute("email");
            String name = principal.getFirstAttribute("displayName");
            
            // JIT Provisioning
            User user = userService.findOrCreateSsoUser(email, name, principal);
            
            // JWT発行
            String jwt = jwtService.generateToken(user);
            
            // フロントエンドにリダイレクト
            response.sendRedirect("/auth/sso/callback?token=" + jwt);
        };
    }
}
```

---

## 2️⃣ SCIM 2.0 プロビジョニング

### 2.1 SCIM エンドポイント

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      SCIM 2.0 Provisioning                                  │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌───────────────┐                          ┌────────────────────┐         │
│  │     IdP       │                          │    Mirel SCIM      │         │
│  │ (Okta/Azure)  │                          │     Endpoint       │         │
│  └───────┬───────┘                          └──────────┬─────────┘         │
│          │                                              │                   │
│          │  POST /scim/v2/Users                        │                   │
│          │  ─────────────────────────────────────────→ │                   │
│          │  (ユーザー作成)                              │                   │
│          │                                              │                   │
│          │  PUT /scim/v2/Users/{id}                    │                   │
│          │  ─────────────────────────────────────────→ │                   │
│          │  (ユーザー更新)                              │                   │
│          │                                              │                   │
│          │  PATCH /scim/v2/Users/{id}                  │                   │
│          │  ─────────────────────────────────────────→ │                   │
│          │  (属性更新)                                  │                   │
│          │                                              │                   │
│          │  DELETE /scim/v2/Users/{id}                 │                   │
│          │  ─────────────────────────────────────────→ │                   │
│          │  (ユーザー削除/無効化)                        │                   │
│          │                                              │                   │
│          │  POST /scim/v2/Groups                       │                   │
│          │  ─────────────────────────────────────────→ │                   │
│          │  (グループ作成)                              │                   │
│          │                                              │                   │
└────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 SCIM コントローラー

```java
@RestController
@RequestMapping("/scim/v2")
public class ScimController {

    @PostMapping("/Users")
    public ResponseEntity<ScimUser> createUser(
        @RequestBody ScimUser user,
        @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        User created = scimService.createUser(tenantId, user);
        return ResponseEntity.created(URI.create("/scim/v2/Users/" + created.getId()))
            .body(ScimUserMapper.toScim(created));
    }

    @PutMapping("/Users/{id}")
    public ResponseEntity<ScimUser> replaceUser(
        @PathVariable String id,
        @RequestBody ScimUser user,
        @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        User updated = scimService.replaceUser(tenantId, id, user);
        return ResponseEntity.ok(ScimUserMapper.toScim(updated));
    }

    @PatchMapping("/Users/{id}")
    public ResponseEntity<ScimUser> patchUser(
        @PathVariable String id,
        @RequestBody ScimPatchRequest patch,
        @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        User updated = scimService.patchUser(tenantId, id, patch);
        return ResponseEntity.ok(ScimUserMapper.toScim(updated));
    }

    @DeleteMapping("/Users/{id}")
    public ResponseEntity<Void> deleteUser(
        @PathVariable String id,
        @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        scimService.deleteUser(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/Users")
    public ResponseEntity<ScimListResponse<ScimUser>> listUsers(
        @RequestParam(required = false) String filter,
        @RequestParam(defaultValue = "1") int startIndex,
        @RequestParam(defaultValue = "100") int count,
        @RequestHeader("X-Tenant-Id") String tenantId
    ) {
        Page<User> users = scimService.listUsers(tenantId, filter, startIndex, count);
        return ResponseEntity.ok(ScimUserMapper.toListResponse(users));
    }
}
```

---

## 3️⃣ データレジデンシー

### 3.1 地域別データセンター

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      Data Residency Architecture                            │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │  Global Load Balancer                                                │  │
│  │  (GeoIP Routing)                                                     │  │
│  └──────────────────────────────┬──────────────────────────────────────┘  │
│                                 │                                          │
│         ┌───────────────────────┼───────────────────────┐                 │
│         ▼                       ▼                       ▼                 │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐         │
│  │  Japan (Tokyo)  │   │  EU (Frankfurt) │   │  US (Virginia)  │         │
│  │                 │   │                 │   │                 │         │
│  │ ┌─────────────┐ │   │ ┌─────────────┐ │   │ ┌─────────────┐ │         │
│  │ │   App       │ │   │ │   App       │ │   │ │   App       │ │         │
│  │ │   Servers   │ │   │ │   Servers   │ │   │ │   Servers   │ │         │
│  │ └─────────────┘ │   │ └─────────────┘ │   │ └─────────────┘ │         │
│  │ ┌─────────────┐ │   │ ┌─────────────┐ │   │ ┌─────────────┐ │         │
│  │ │   Database  │ │   │ │   Database  │ │   │ │   Database  │ │         │
│  │ │   (Primary) │ │   │ │   (Primary) │ │   │ │   (Primary) │ │         │
│  │ └─────────────┘ │   │ └─────────────┘ │   │ └─────────────┘ │         │
│  │ ┌─────────────┐ │   │ ┌─────────────┐ │   │ ┌─────────────┐ │         │
│  │ │   Storage   │ │   │ │   Storage   │ │   │ │   Storage   │ │         │
│  │ └─────────────┘ │   │ └─────────────┘ │   │ └─────────────┘ │         │
│  └─────────────────┘   └─────────────────┘   └─────────────────┘         │
│         │                       │                       │                 │
│         └───────────────────────┼───────────────────────┘                 │
│                                 ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │  Metadata Sync Only (No PII)                                        │  │
│  │  ・テナント設定同期                                                   │  │
│  │  ・認証トークン検証（JWK）                                            │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 テナント設定

```java
@Entity
@Table(name = "tenant_data_residency")
public class TenantDataResidency {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataRegion region; // JAPAN, EU, US, APAC

    @Enumerated(EnumType.STRING)
    private DataResidencyStatus status; // PENDING, ACTIVE, MIGRATING

    // 移行情報
    private String sourceRegion;
    private LocalDateTime migrationStartedAt;
    private LocalDateTime migrationCompletedAt;

    // 法的要件
    private boolean gdprApplicable;
    private boolean ccpaApplicable;
    private boolean appiApplicable; // 日本の個人情報保護法

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 4️⃣ ホワイトラベリング

### 4.1 カスタマイズ項目

| カテゴリ | カスタマイズ項目 | 説明 |
|----------|------------------|------|
| ブランド | ロゴ（ヘッダー、ファビコン） | 企業ロゴに置き換え |
| | アプリ名 | 「Mirel Platform」→「Company Portal」 |
| | カラースキーム | プライマリ/アクセントカラー |
| ドメイン | カスタムドメイン | app.company.com |
| | メール送信元 | noreply@company.com |
| 表示 | フッターテキスト | 著作権表示変更 |
| | ヘルプリンク | 自社ドキュメントへ |

### 4.2 データモデル

```java
@Entity
@Table(name = "white_label_configs")
public class WhiteLabelConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String tenantId;

    // ブランド
    private String appName;
    private String logoUrl;
    private String faviconUrl;

    // カラー
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;

    // ドメイン
    private String customDomain;
    private boolean customDomainVerified;
    private String sslCertificateId;

    // メール
    private String emailFromName;
    private String emailFromAddress;
    private String emailDomain;
    private boolean emailDomainVerified;

    // UI
    private String footerText;
    private String helpUrl;
    private String privacyPolicyUrl;
    private String termsOfServiceUrl;

    @Column(columnDefinition = "jsonb")
    private String customCss; // 追加CSS

    private boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 4.3 フロントエンド適用

```typescript
// hooks/useWhiteLabel.ts
function useWhiteLabel() {
  const { data: config } = useQuery({
    queryKey: ['whiteLabel'],
    queryFn: () => api.get('/mapi/white-label/config').then(r => r.data),
    staleTime: Infinity,
  });

  useEffect(() => {
    if (!config) return;

    // CSS変数を動的に設定
    const root = document.documentElement;
    root.style.setProperty('--color-primary', config.primaryColor);
    root.style.setProperty('--color-secondary', config.secondaryColor);
    root.style.setProperty('--color-accent', config.accentColor);

    // ファビコン変更
    const favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]');
    if (favicon && config.faviconUrl) {
      favicon.href = config.faviconUrl;
    }

    // ドキュメントタイトル
    if (config.appName) {
      document.title = config.appName;
    }
  }, [config]);

  return config;
}
```

---

## 5️⃣ IP制限・VPN連携

### 5.1 アクセス制限設定

```java
@Entity
@Table(name = "tenant_access_policies")
public class TenantAccessPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String tenantId;

    // IP制限
    @ElementCollection
    @CollectionTable(name = "allowed_ip_ranges")
    private Set<String> allowedIpRanges; // CIDR形式 "192.168.1.0/24"

    private boolean ipRestrictionEnabled = false;

    // 時間制限
    private boolean timeRestrictionEnabled = false;
    private String allowedTimeZone;
    private String allowedStartTime; // "09:00"
    private String allowedEndTime; // "18:00"
    private Set<DayOfWeek> allowedDays;

    // デバイス制限
    private boolean deviceRestrictionEnabled = false;
    @ElementCollection
    @CollectionTable(name = "allowed_device_types")
    private Set<String> allowedDeviceTypes; // "desktop", "mobile", "tablet"

    // VPN要件
    private boolean vpnRequired = false;
    @ElementCollection
    @CollectionTable(name = "trusted_vpn_ranges")
    private Set<String> trustedVpnRanges;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 5.2 アクセス検証フィルター

```java
@Component
public class AccessPolicyFilter extends OncePerRequestFilter {

    @Autowired
    private TenantAccessPolicyService policyService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        String tenantId = extractTenantId(request);
        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        TenantAccessPolicy policy = policyService.getPolicy(tenantId);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        
        // IP制限チェック
        if (policy.isIpRestrictionEnabled()) {
            if (!isIpAllowed(clientIp, policy.getAllowedIpRanges())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                    "Access denied: IP address not allowed");
                return;
            }
        }

        // VPN要件チェック
        if (policy.isVpnRequired()) {
            if (!isFromTrustedVpn(clientIp, policy.getTrustedVpnRanges())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                    "Access denied: VPN connection required");
                return;
            }
        }

        // 時間制限チェック
        if (policy.isTimeRestrictionEnabled()) {
            if (!isWithinAllowedTime(policy)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                    "Access denied: Outside allowed hours");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isIpAllowed(String clientIp, Set<String> allowedRanges) {
        InetAddress address = InetAddress.getByName(clientIp);
        return allowedRanges.stream()
            .anyMatch(range -> new SubnetUtils(range).getInfo().isInRange(clientIp));
    }
}
```

---

## 📊 工数見積

| 機能 | フロントエンド | バックエンド | インフラ | 合計 |
|------|----------------|--------------|----------|------|
| SAML 2.0 SSO | 3人日 | 10人日 | - | 13人日 |
| OIDC SSO | 2人日 | 5人日 | - | 7人日 |
| SCIM Provisioning | 2人日 | 10人日 | - | 12人日 |
| データレジデンシー | - | 5人日 | 20人日 | 25人日 |
| ホワイトラベリング | 10人日 | 5人日 | 3人日 | 18人日 |
| IP制限/VPN連携 | 3人日 | 5人日 | 2人日 | 10人日 |
| カスタムSLA | 2人日 | 5人日 | 5人日 | 12人日 |
| **合計** | **22人日** | **45人日** | **30人日** | **97人日** |

---

## ⚠️ 実装優先度

```
Phase 1 (Enterprise Tier Launch)
  ├── SAML 2.0 SSO ★★★
  ├── IP制限 ★★★
  └── 監査ログ強化 ★★★

Phase 2 (高セキュリティ顧客対応)
  ├── SCIM Provisioning ★★☆
  ├── ホワイトラベリング（基本） ★★☆
  └── OIDC SSO ★★☆

Phase 3 (グローバル展開)
  ├── データレジデンシー ★☆☆
  ├── VPN連携 ★☆☆
  └── ホワイトラベリング（完全） ★☆☆
```

---

*[MUST機能一覧に戻る](../MUST/00_overview.md) | [WANT機能概要に戻る](./00_overview.md)*
