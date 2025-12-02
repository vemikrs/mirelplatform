# メニュー管理機能設計書

## 1. 概要

フロントエンドのナビゲーションメニューをバックエンドで一元管理し、認可に基づいて動的に配信する機能。
現在の静的JSON（`/mock/navigation.json`）をデータベース管理に移行する。

---

## 2. 鳥瞰図

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                            メニュー管理 アーキテクチャ概要                           │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  【現状】                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                               │   │
│  │   Frontend                           Backend                                  │   │
│  │   ┌─────────────────────┐            (メニュー管理なし)                       │   │
│  │   │ /mock/navigation.json│                                                    │   │
│  │   │ (静的ファイル)       │                                                    │   │
│  │   └──────────┬──────────┘                                                    │   │
│  │              │ fetch                                                          │   │
│  │              ▼                                                                │   │
│  │   ┌─────────────────────┐                                                    │   │
│  │   │ navigation.schema.ts │  ← Zodでパース、型定義                             │   │
│  │   │ loadNavigationConfig │                                                    │   │
│  │   └─────────────────────┘                                                    │   │
│  │                                                                               │   │
│  └──────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                      │
│  【目標】                                                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                               │   │
│  │   Frontend                           Backend                                  │   │
│  │   ┌─────────────────────┐            ┌─────────────────────────────────┐     │   │
│  │   │ loadNavigationConfig │◀──────────│ GET /navigation                  │     │   │
│  │   │ (API呼び出し)        │   JSON    │ MenuController                   │     │   │
│  │   └─────────────────────┘            └───────────────┬─────────────────┘     │   │
│  │                                                      │                        │   │
│  │                                                      ▼                        │   │
│  │                                       ┌─────────────────────────────────┐     │   │
│  │                                       │ MenuService                     │     │   │
│  │                                       │ - 認可フィルタリング             │     │   │
│  │                                       │ - テナント別メニュー             │     │   │
│  │                                       │ - ライセンス制限                 │     │   │
│  │                                       └───────────────┬─────────────────┘     │   │
│  │                                                      │                        │   │
│  │                                                      ▼                        │   │
│  │                                       ┌─────────────────────────────────┐     │   │
│  │                                       │ Database                        │     │   │
│  │                                       │ mir_menu_item                   │     │   │
│  │                                       │ mir_menu_permission             │     │   │
│  │                                       └─────────────────────────────────┘     │   │
│  │                                                                               │   │
│  └──────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                      │
│  【認可に基づくメニュー配信】                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                               │   │
│  │   ユーザー: ADMIN + OWNER                                                     │   │
│  │   ┌─────────────────────────────────────────────────────────────────────┐    │   │
│  │   │ ホーム │ ProMarker │ 管理 ▼         │ 設定         │ ヘルプ        │    │   │
│  │   │        │           │ ├ ユーザー管理 │              │               │    │   │
│  │   │        │           │ ├ テナント管理 │              │               │    │   │
│  │   │        │           │ ├ 組織管理     │              │               │    │   │
│  │   │        │           │ └ フィーチャー │              │               │    │   │
│  │   └─────────────────────────────────────────────────────────────────────┘    │   │
│  │                                                                               │   │
│  │   ユーザー: USER + MEMBER                                                     │   │
│  │   ┌─────────────────────────────────────────────────────────────────────┐    │   │
│  │   │ ホーム │ ProMarker │ 設定           │ ヘルプ                        │    │   │
│  │   │        │           │                │               ← 管理メニューなし │    │   │
│  │   └─────────────────────────────────────────────────────────────────────┘    │   │
│  │                                                                               │   │
│  └──────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. データモデル

### 3.1 ER図

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                            メニュー管理 ER図                                         │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                        mir_menu_item (メニュー項目)                         │     │
│  ├────────────────────────────────────────────────────────────────────────────┤     │
│  │ menu_id (PK)             VARCHAR(36)                                        │     │
│  │ tenant_id                VARCHAR(36)   -- null=グローバル                   │     │
│  │ parent_menu_id           VARCHAR(36)   -- null=トップレベル                 │     │
│  │ menu_key                 VARCHAR(100)  -- 一意キー (home, promarker, ...)   │     │
│  │ label                    VARCHAR(100)  -- 表示ラベル                        │     │
│  │ path                     VARCHAR(500)  -- パス or URL                       │     │
│  │ icon                     VARCHAR(50)   -- アイコン名 (Lucide等)             │     │
│  │ description              VARCHAR(500)  -- 説明                              │     │
│  │ menu_type                VARCHAR(20)   -- PRIMARY,SECONDARY,QUICK,ADMIN,...│     │
│  │ external                 BOOLEAN       -- 外部リンクか                      │     │
│  │ badge_label              VARCHAR(50)   -- バッジラベル                      │     │
│  │ badge_tone               VARCHAR(20)   -- info,success,warning,neutral      │     │
│  │ sort_order               INT           -- 表示順                            │     │
│  │ is_enabled               BOOLEAN       -- 有効/無効                         │     │
│  │ is_visible               BOOLEAN       -- 表示/非表示                       │     │
│  │ application_id           VARCHAR(50)   -- 関連アプリID (ライセンス制御用)   │     │
│  │ feature_flag_key         VARCHAR(100)  -- 関連フィーチャーフラグ            │     │
│  │ created_at               TIMESTAMP                                          │     │
│  │ updated_at               TIMESTAMP                                          │     │
│  └────────────────────────────────────┬───────────────────────────────────────┘     │
│                                       │                                              │
│  ┌────────────────────────────────────┼───────────────────────────────────────┐     │
│  │                                    │                                        │     │
│  │                                    ▼                                        │     │
│  │  ┌───────────────────────────────────────────────────────────────────┐     │     │
│  │  │            mir_menu_permission (メニュー権限)                      │     │     │
│  │  ├───────────────────────────────────────────────────────────────────┤     │     │
│  │  │ id (PK)                   VARCHAR(36)                              │     │     │
│  │  │ menu_id (FK)              VARCHAR(36)                              │     │     │
│  │  │ permission_type           VARCHAR(20)   -- SYSTEM_ROLE,TENANT_ROLE │     │     │
│  │  │ permission_value          VARCHAR(50)   -- ADMIN,OWNER,...         │     │     │
│  │  │ is_required               BOOLEAN       -- 必須か(AND/OR条件)      │     │     │
│  │  └───────────────────────────────────────────────────────────────────┘     │     │
│  │                                                                             │     │
│  │  例: メニュー「ユーザー管理」                                               │     │
│  │      permission_type=SYSTEM_ROLE, permission_value=ADMIN, is_required=true  │     │
│  │                                                                             │     │
│  │  例: メニュー「テナント設定」                                               │     │
│  │      permission_type=TENANT_ROLE, permission_value=OWNER, is_required=true  │     │
│  │      permission_type=TENANT_ROLE, permission_value=MANAGER, is_required=false│     │
│  │      → OWNER または MANAGER が見れる                                        │     │
│  │                                                                             │     │
│  └─────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────┐     │
│  │                    mir_menu_global_action (グローバルアクション)            │     │
│  ├────────────────────────────────────────────────────────────────────────────┤     │
│  │ action_id (PK)           VARCHAR(36)                                        │     │
│  │ action_key               VARCHAR(50)   -- theme-toggle, notifications, ... │     │
│  │ action_type              VARCHAR(20)   -- theme, notifications, profile,.. │     │
│  │ path                     VARCHAR(500)  -- オプション                        │     │
│  │ sort_order               INT                                                │     │
│  │ is_enabled               BOOLEAN                                            │     │
│  │ created_at               TIMESTAMP                                          │     │
│  │ updated_at               TIMESTAMP                                          │     │
│  └────────────────────────────────────────────────────────────────────────────┘     │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Entity定義

```java
@Entity
@Table(name = "mir_menu_item")
public class MenuItem {
    @Id
    private String menuId;
    
    private String tenantId;         // null = グローバルメニュー
    
    private String parentMenuId;     // null = トップレベル
    
    @Column(nullable = false)
    private String menuKey;          // 一意キー
    
    @Column(nullable = false)
    private String label;
    
    private String path;
    
    private String icon;             // Lucide icon name
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    private MenuType menuType;
    
    @Column(name = "external")
    private Boolean external = false;
    
    private String badgeLabel;
    
    @Enumerated(EnumType.STRING)
    private BadgeTone badgeTone;
    
    private Integer sortOrder;
    
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    @Column(name = "is_visible")
    private Boolean isVisible = true;
    
    private String applicationId;    // ライセンス制御用
    
    private String featureFlagKey;   // フィーチャーフラグ制御用
    
    private Instant createdAt;
    private Instant updatedAt;
    
    @OneToMany(mappedBy = "menuId", fetch = FetchType.LAZY)
    private List<MenuPermission> permissions;
    
    @Transient
    private List<MenuItem> children;
}

public enum MenuType {
    PRIMARY,        // メインナビゲーション
    SECONDARY,      // セカンダリ（外部リンク等）
    QUICK_LINKS,    // クイックリンク
    IN_DEVELOPMENT, // 開発中機能
    ADMIN,          // 管理メニュー
    USER            // ユーザーメニュー（プロフィール等）
}

public enum BadgeTone {
    info, success, warning, neutral
}

@Entity
@Table(name = "mir_menu_permission")
public class MenuPermission {
    @Id
    private String id;
    
    private String menuId;
    
    @Enumerated(EnumType.STRING)
    private PermissionType permissionType;
    
    private String permissionValue;
    
    @Column(name = "is_required")
    private Boolean isRequired = false;
}

public enum PermissionType {
    SYSTEM_ROLE,    // ADMIN, USER, SYSTEM_ADMIN
    TENANT_ROLE,    // OWNER, MANAGER, MEMBER, GUEST
    LICENSE,        // promarker:PRO, promarker:MAX
    FEATURE_FLAG    // 特定フィーチャーが有効
}
```

---

## 4. メニュー配信ロジック

### 4.1 MenuService

```java
@Service
public class MenuService {
    
    @Autowired
    private MenuItemRepository menuItemRepo;
    
    @Autowired
    private ExecutionContext executionContext;
    
    @Autowired
    private FeatureFlagService featureFlagService;
    
    @Autowired
    private LicenseService licenseService;
    
    /**
     * 現在ユーザー向けのメニュー設定を取得
     */
    public NavigationConfig getNavigationForCurrentUser() {
        String userId = executionContext.getCurrentUserId();
        String tenantId = executionContext.getCurrentTenantId();
        Set<String> systemRoles = executionContext.getSystemRoles();
        String tenantRole = executionContext.getTenantRole();
        
        // 全メニュー取得（グローバル + テナント固有）
        List<MenuItem> allMenus = menuItemRepo.findByTenantIdOrGlobal(tenantId);
        
        // 権限フィルタリング
        List<MenuItem> filteredMenus = allMenus.stream()
            .filter(menu -> isAccessible(menu, systemRoles, tenantRole, tenantId))
            .toList();
        
        // ツリー構造に変換
        return buildNavigationConfig(filteredMenus);
    }
    
    private boolean isAccessible(MenuItem menu, Set<String> systemRoles, 
                                  String tenantRole, String tenantId) {
        // 1. 有効/表示チェック
        if (!menu.getIsEnabled() || !menu.getIsVisible()) {
            return false;
        }
        
        // 2. フィーチャーフラグチェック
        if (menu.getFeatureFlagKey() != null) {
            if (!featureFlagService.isEnabled(menu.getFeatureFlagKey(), tenantId)) {
                return false;
            }
        }
        
        // 3. ライセンスチェック
        if (menu.getApplicationId() != null) {
            if (!licenseService.hasValidLicense(tenantId, menu.getApplicationId())) {
                return false;
            }
        }
        
        // 4. 権限チェック
        List<MenuPermission> permissions = menu.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            return true; // 権限設定なし = 誰でもアクセス可
        }
        
        // 必須権限（is_required=true）はすべて満たす必要がある
        // オプション権限（is_required=false）は1つでも満たせばOK
        List<MenuPermission> required = permissions.stream()
            .filter(MenuPermission::getIsRequired)
            .toList();
        List<MenuPermission> optional = permissions.stream()
            .filter(p -> !p.getIsRequired())
            .toList();
        
        boolean allRequiredMet = required.stream()
            .allMatch(p -> checkPermission(p, systemRoles, tenantRole));
        
        boolean anyOptionalMet = optional.isEmpty() || optional.stream()
            .anyMatch(p -> checkPermission(p, systemRoles, tenantRole));
        
        return allRequiredMet && anyOptionalMet;
    }
    
    private boolean checkPermission(MenuPermission perm, Set<String> systemRoles, 
                                     String tenantRole) {
        switch (perm.getPermissionType()) {
            case SYSTEM_ROLE:
                return systemRoles.contains(perm.getPermissionValue());
            case TENANT_ROLE:
                return perm.getPermissionValue().equals(tenantRole) ||
                       isHigherTenantRole(tenantRole, perm.getPermissionValue());
            default:
                return true;
        }
    }
    
    private NavigationConfig buildNavigationConfig(List<MenuItem> menus) {
        // MenuType別にグループ化
        Map<MenuType, List<MenuItem>> grouped = menus.stream()
            .collect(Collectors.groupingBy(MenuItem::getMenuType));
        
        return NavigationConfig.builder()
            .brand(getBrandConfig())
            .primary(buildTree(grouped.getOrDefault(MenuType.PRIMARY, List.of())))
            .secondary(buildTree(grouped.getOrDefault(MenuType.SECONDARY, List.of())))
            .quickLinks(buildTree(grouped.getOrDefault(MenuType.QUICK_LINKS, List.of())))
            .inDevelopment(buildTree(grouped.getOrDefault(MenuType.IN_DEVELOPMENT, List.of())))
            .globalActions(getGlobalActions())
            .build();
    }
}
```

---

## 5. API設計

### 5.1 メニュー取得API（ユーザー向け）

```yaml
# 現在ユーザー向けナビゲーション取得
GET /navigation
Headers:
  Authorization: Bearer <token>
Response:
  200:
    brand:
      name: "mirelplatform"
      shortName: "mirel"
      tagline: "業務アプリケーション基盤"
    primary:
      - id: "home"
        label: "ホーム"
        path: "/home"
        icon: "home"
      - id: "promarker"
        label: "ProMarker"
        path: "/promarker"
        icon: "code"
        description: "コード生成支援"
      - id: "admin"
        label: "管理"
        path: "/admin"
        icon: "settings"
        children:
          - id: "admin-users"
            label: "ユーザー管理"
            path: "/admin/users"
          - id: "admin-tenants"
            label: "テナント管理"
            path: "/admin/tenants"
    secondary:
      - id: "docs"
        label: "ドキュメント"
        path: "https://docs.mirel.dev"
        external: true
        badge:
          label: "外部"
          tone: "neutral"
    quickLinks: [...]
    inDevelopment: [...]
    globalActions:
      - id: "theme-toggle"
        type: "theme"
      - id: "notifications"
        type: "notifications"
      - id: "profile"
        type: "profile"
```

### 5.2 メニュー管理API（Admin向け）

```yaml
# メニュー一覧取得
GET /admin/menus
Query:
  menuType: "PRIMARY"
  tenantId: null  # グローバルのみ
Response:
  200:
    - menuId: "menu-001"
      menuKey: "home"
      label: "ホーム"
      path: "/home"
      menuType: "PRIMARY"
      sortOrder: 10
      isEnabled: true
      permissions: []

# メニュー作成
POST /admin/menus
Request:
  menuKey: "new-feature"
  label: "新機能"
  path: "/new-feature"
  icon: "sparkles"
  menuType: "PRIMARY"
  parentMenuId: null
  sortOrder: 50
  permissions:
    - permissionType: "SYSTEM_ROLE"
      permissionValue: "ADMIN"
      isRequired: true

# メニュー更新
PUT /admin/menus/{menuId}
Request:
  label: "新機能（更新）"
  isEnabled: true
  permissions:
    - permissionType: "SYSTEM_ROLE"
      permissionValue: "ADMIN"
      isRequired: false  # ADMINは任意に変更

# メニュー削除
DELETE /admin/menus/{menuId}

# メニュー順序変更
PUT /admin/menus/reorder
Request:
  menuType: "PRIMARY"
  order:
    - menuId: "menu-001"
      sortOrder: 10
    - menuId: "menu-002"
      sortOrder: 20

# テナント固有メニュー設定
POST /admin/tenants/{tenantId}/menus
Request:
  menuKey: "tenant-custom"
  label: "カスタムメニュー"
  path: "/custom"
  menuType: "QUICK_LINKS"
```

---

## 6. 初期データマイグレーション

### 6.1 既存navigation.jsonからの移行

```java
@Component
public class MenuDataMigration {
    
    @Autowired
    private MenuItemRepository menuRepo;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    /**
     * 既存のnavigation.jsonをDBに移行
     */
    @PostConstruct
    public void migrate() {
        if (menuRepo.count() > 0) {
            log.info("Menu data already exists, skipping migration");
            return;
        }
        
        try {
            Resource resource = resourceLoader.getResource(
                "classpath:migration/navigation.json");
            NavigationConfig config = objectMapper.readValue(
                resource.getInputStream(), NavigationConfig.class);
            
            // Primary menus
            int sortOrder = 10;
            for (NavigationLink link : config.getPrimary()) {
                createMenuItem(link, MenuType.PRIMARY, null, sortOrder);
                sortOrder += 10;
            }
            
            // Secondary, QuickLinks, InDevelopment も同様に...
            
            log.info("Menu data migration completed");
        } catch (Exception e) {
            log.error("Failed to migrate menu data", e);
        }
    }
    
    private void createMenuItem(NavigationLink link, MenuType type, 
                                 String parentId, int sortOrder) {
        MenuItem menu = new MenuItem();
        menu.setMenuId(UUID.randomUUID().toString());
        menu.setMenuKey(link.getId());
        menu.setLabel(link.getLabel());
        menu.setPath(link.getPath());
        menu.setIcon(link.getIcon());
        menu.setDescription(link.getDescription());
        menu.setMenuType(type);
        menu.setParentMenuId(parentId);
        menu.setExternal(link.getExternal());
        menu.setSortOrder(sortOrder);
        menu.setIsEnabled(true);
        menu.setIsVisible(true);
        
        if (link.getBadge() != null) {
            menu.setBadgeLabel(link.getBadge().getLabel());
            menu.setBadgeTone(BadgeTone.valueOf(link.getBadge().getTone()));
        }
        
        // 権限設定
        if (link.getPermissions() != null) {
            for (String perm : link.getPermissions()) {
                MenuPermission mp = new MenuPermission();
                mp.setId(UUID.randomUUID().toString());
                mp.setMenuId(menu.getMenuId());
                mp.setPermissionType(PermissionType.SYSTEM_ROLE);
                mp.setPermissionValue(perm);
                mp.setIsRequired(true);
                menuPermissionRepo.save(mp);
            }
        }
        
        menuRepo.save(menu);
        
        // 子メニュー
        if (link.getChildren() != null) {
            int childOrder = 10;
            for (NavigationLink child : link.getChildren()) {
                createMenuItem(child, type, menu.getMenuId(), childOrder);
                childOrder += 10;
            }
        }
    }
}
```

---

## 7. フロントエンド変更

### 7.1 navigation.schema.ts の変更

```typescript
// apps/frontend-v3/src/app/navigation.schema.ts

// 既存のスキーマ定義は維持

export async function loadNavigationConfig(): Promise<NavigationConfig> {
  // 認証状態を確認
  const token = getAccessToken();
  
  if (token) {
    // 認証済み: APIからメニュー取得
    try {
      const response = await fetch('/mapi/navigation', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      });
      
      if (response.ok) {
        const data = await response.json();
        return navigationConfigSchema.parse(data);
      }
    } catch (error) {
      console.warn('Failed to load navigation from API, falling back to mock', error);
    }
  }
  
  // 未認証 or API失敗: モックから取得（フォールバック）
  const response = await fetch('/mock/navigation.json');
  if (!response.ok) {
    throw new Error('Failed to load navigation configuration');
  }
  const data = await response.json();
  return navigationConfigSchema.parse(data);
}
```

### 7.2 メニュー管理UI

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│  メニュー管理                                                    [+ メニュー追加]   │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│  タブ: [PRIMARY] [SECONDARY] [QUICK_LINKS] [IN_DEVELOPMENT] [ADMIN]                  │
│                                                                                      │
│  ┌──────────────────────────────────────────────────────────────────────────────┐   │
│  │  ドラッグで順序変更可能                                                       │   │
│  ├──────────────────────────────────────────────────────────────────────────────┤   │
│  │  ≡ 🏠 ホーム                          /home               ✅有効  [編集][削除]│   │
│  │  ≡ 📝 ProMarker                       /promarker          ✅有効  [編集][削除]│   │
│  │  ≡ ⚙️ 管理                            /admin              ✅有効  [編集][削除]│   │
│  │    └─ 👥 ユーザー管理                 /admin/users        ✅有効  [編集][削除]│   │
│  │    └─ 🏢 テナント管理                 /admin/tenants      ✅有効  [編集][削除]│   │
│  │    └─ 🏛️ 組織管理                    /admin/orgs         ✅有効  [編集][削除]│   │
│  │    └─ 🚀 フィーチャーフラグ           /admin/features     ✅有効  [編集][削除]│   │
│  │  ≡ 📊 UIカタログ                      /catalog            ✅有効  [編集][削除]│   │
│  └──────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                      │
│  ── 編集: 管理 ──                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────────┐   │
│  │  キー: [admin        ]  ラベル: [管理          ]                              │   │
│  │  パス: [/admin       ]  アイコン: [settings ▼]                                │   │
│  │  説明: [管理者向けメニュー                               ]                     │   │
│  │                                                                               │   │
│  │  権限設定:                                                                    │   │
│  │  ┌────────────────────────────────────────────────────────────────────────┐  │   │
│  │  │ + システムロール: ADMIN    必須: ☑                             [削除] │  │   │
│  │  │ + テナントロール: OWNER    必須: ☐                             [削除] │  │   │
│  │  │ + テナントロール: MANAGER  必須: ☐                             [削除] │  │   │
│  │  │ [+ 権限追加]                                                           │  │   │
│  │  └────────────────────────────────────────────────────────────────────────┘  │   │
│  │                                                                               │   │
│  │  [保存] [キャンセル]                                                          │   │
│  └──────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. キャッシュ戦略

```java
@Service
public class MenuCacheService {
    
    @Autowired
    private RedisTemplate<String, NavigationConfig> redisTemplate;
    
    private static final String CACHE_PREFIX = "mirel:menu:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    /**
     * ユーザー別メニューキャッシュキー
     * キー: mirel:menu:user:{userId}:tenant:{tenantId}
     */
    public String getCacheKey(String userId, String tenantId) {
        return CACHE_PREFIX + "user:" + userId + ":tenant:" + tenantId;
    }
    
    public NavigationConfig getFromCache(String userId, String tenantId) {
        String key = getCacheKey(userId, tenantId);
        return redisTemplate.opsForValue().get(key);
    }
    
    public void setToCache(String userId, String tenantId, NavigationConfig config) {
        String key = getCacheKey(userId, tenantId);
        redisTemplate.opsForValue().set(key, config, CACHE_TTL);
    }
    
    /**
     * メニュー更新時にキャッシュ無効化
     */
    public void invalidateAll() {
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
    
    /**
     * 特定テナントのキャッシュ無効化
     */
    public void invalidateTenant(String tenantId) {
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*:tenant:" + tenantId);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

---

## 9. 実装タスク

```
【Phase 1: Entity・Repository (2日)】
□ MenuItem Entity
□ MenuPermission Entity
□ MenuGlobalAction Entity
□ Repository実装

【Phase 2: MenuService (2日)】
□ メニュー取得ロジック
□ 権限フィルタリング
□ ツリー構築

【Phase 3: API (2日)】
□ MenuController (ユーザー向け)
□ AdminMenuController (管理向け)
□ 認可設定

【Phase 4: データマイグレーション (1日)】
□ navigation.jsonからの移行
□ 初期データ投入

【Phase 5: フロントエンド変更 (2日)】
□ loadNavigationConfig API対応
□ フォールバック実装

【Phase 6: 管理UI (3日)】
□ メニュー一覧（ドラッグ並び替え）
□ メニュー編集フォーム
□ 権限設定UI

【Phase 7: キャッシュ (1日)】
□ Redis キャッシュ実装
□ 無効化処理
```

---

## 10. 工数見積もり

| 機能 | 見積もり | 備考 |
|------|----------|------|
| Entity・Repository | 2日 | |
| MenuService | 2日 | フィルタリング含む |
| API | 2日 | |
| データマイグレーション | 1日 | 既存JSONから |
| フロントエンド変更 | 2日 | API対応 |
| 管理UI | 3日 | ドラッグ並び替え |
| キャッシュ | 1日 | Redis |
| **合計** | **13日** | |

---

**作成日**: 2025年11月28日  
**作成者**: GitHub Copilot 🤖


