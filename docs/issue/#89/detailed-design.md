# 組織エンティティ リファクタリング 詳細設計書

## 1. データベーススキーマ設計

### 1.1 テーブル一覧

| テーブル名                  | 説明                                               | 状態 |
| :-------------------------- | :------------------------------------------------- | :--- |
| `mir_organization`          | 統合組織エンティティ（旧 `mir_organization_unit`） | 変更 |
| `mir_company_settings`      | 会社設定                                           | 新規 |
| `mir_organization_settings` | 組織設定                                           | 新規 |
| `mir_user_organization`     | ユーザー所属情報                                   | 変更 |

### 1.2 DDL

#### mir_organization (統合後)

```sql
-- 既存テーブルのリネームとカラム変更

CREATE TABLE mir_organization (
    -- 主キー
    id VARCHAR(36) PRIMARY KEY,

    -- テナント・親組織
    tenant_id VARCHAR(36) NOT NULL,
    parent_id VARCHAR(36) REFERENCES mir_organization(id),

    -- 基本情報
    name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    code VARCHAR(100) UNIQUE,
    type VARCHAR(50) NOT NULL, -- COMPANY, DIVISION, DEPARTMENT, SECTION, TEAM, PROJECT, VIRTUAL

    -- 階層情報
    path VARCHAR(1024), -- 例: /root-id/div-id/dept-id
    level INT NOT NULL DEFAULT 0, -- 0=COMPANY
    sort_order INT,

    -- 期間管理
    start_date DATE,
    end_date DATE,
    period_code VARCHAR(50), -- リレーション管理用（例: FY2026Q1）

    -- 状態
    is_active BOOLEAN DEFAULT TRUE,

    -- 監査カラム
    version BIGINT NOT NULL DEFAULT 1,
    delete_flag BOOLEAN DEFAULT FALSE,
    create_user_id VARCHAR(36),
    create_date TIMESTAMP,
    update_user_id VARCHAR(36),
    update_date TIMESTAMP,

    -- インデックス用制約
    CONSTRAINT chk_type CHECK (type IN ('COMPANY', 'DIVISION', 'DEPARTMENT', 'SECTION', 'TEAM', 'PROJECT', 'VIRTUAL'))
);

-- インデックス
CREATE INDEX idx_organization_tenant ON mir_organization(tenant_id);
CREATE INDEX idx_organization_parent ON mir_organization(parent_id);
CREATE INDEX idx_organization_path ON mir_organization(path);
CREATE INDEX idx_organization_type ON mir_organization(type);
CREATE INDEX idx_organization_period ON mir_organization(period_code);
```

#### mir_company_settings (新規)

```sql
CREATE TABLE mir_company_settings (
    id VARCHAR(36) PRIMARY KEY,

    -- 組織参照（Rootのみ）+ 期間
    organization_id VARCHAR(36) NOT NULL REFERENCES mir_organization(id),
    period_code VARCHAR(50), -- リレーション管理用（例: FY2026Q1）

    -- 会社設定
    fiscal_year_start INT DEFAULT 4, -- 会計年度開始月（4=4月）
    currency_code VARCHAR(3) DEFAULT 'JPY',
    timezone VARCHAR(50) DEFAULT 'Asia/Tokyo',
    locale VARCHAR(10) DEFAULT 'ja_JP',

    -- 監査カラム
    version BIGINT NOT NULL DEFAULT 1,
    delete_flag BOOLEAN DEFAULT FALSE,
    create_user_id VARCHAR(36),
    create_date TIMESTAMP,
    update_user_id VARCHAR(36),
    update_date TIMESTAMP,

    -- 複合ユニーク制約（組織 + 期間）
    CONSTRAINT uq_company_settings_org_period UNIQUE (organization_id, period_code)
);

CREATE INDEX idx_company_settings_org ON mir_company_settings(organization_id);
CREATE INDEX idx_company_settings_period ON mir_company_settings(period_code);
```

#### mir_organization_settings (新規)

```sql
CREATE TABLE mir_organization_settings (
    id VARCHAR(36) PRIMARY KEY,

    -- 組織参照 + 期間
    organization_id VARCHAR(36) NOT NULL REFERENCES mir_organization(id),
    period_code VARCHAR(50), -- リレーション管理用（例: FY2026Q1）

    -- 基本設定
    allow_flexible_schedule BOOLEAN DEFAULT FALSE,
    require_approval BOOLEAN DEFAULT TRUE,
    max_member_count INT,

    -- 拡張設定（JSON）
    extended_settings JSONB,

    -- 監査カラム
    version BIGINT NOT NULL DEFAULT 1,
    delete_flag BOOLEAN DEFAULT FALSE,
    create_user_id VARCHAR(36),
    create_date TIMESTAMP,
    update_user_id VARCHAR(36),
    update_date TIMESTAMP,

    -- 複合ユニーク制約（組織 + 期間）
    CONSTRAINT uq_organization_settings_org_period UNIQUE (organization_id, period_code)
);

CREATE INDEX idx_organization_settings_org ON mir_organization_settings(organization_id);
CREATE INDEX idx_organization_settings_period ON mir_organization_settings(period_code);
```

#### mir_user_organization (変更)

```sql
-- 既存テーブルの変更
ALTER TABLE mir_user_organization
    RENAME COLUMN unit_id TO organization_id;

ALTER TABLE mir_user_organization
    DROP COLUMN is_manager;

ALTER TABLE mir_user_organization
    ADD COLUMN role VARCHAR(50); -- manager, leader, member, observer など

-- 参照先変更
ALTER TABLE mir_user_organization
    DROP CONSTRAINT IF EXISTS fk_user_org_unit,
    ADD CONSTRAINT fk_user_org_organization
        FOREIGN KEY (organization_id) REFERENCES mir_organization(id);
```

---

## 2. エンティティクラス設計

### 2.1 Organization.java (統合後)

```java
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 組織エンティティ（統合版）.
 * 会社から部署・チームまで、すべての組織ノードを表現する。
 */
@Setter
@Getter
@Entity
@Table(name = "mir_organization")
public class Organization {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "parent_id")
    private String parentId; // null = ルート（会社）

    @Column(nullable = false)
    private String name; // 正式名称

    @Column(name = "display_name")
    private String displayName; // 表示名

    @Column(unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OrganizationType type; // COMPANY, DIVISION, DEPARTMENT, SECTION, TEAM, PROJECT, VIRTUAL

    @Column
    private String path; // 階層パス（例: /root-id/div-id/dept-id）

    @Column(nullable = false)
    private Integer level = 0; // 階層レベル（0=COMPANY）

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "period_code")
    private String periodCode; // リレーション管理用

    @Column(name = "is_active", columnDefinition = "boolean default true")
    private Boolean isActive = true;

    // 監査カラム
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    @Column
    private String createUserId;

    @Column
    private Date createDate;

    @Column
    private String updateUserId;

    @Column
    private Date updateDate;

    // ツリー操作用（非永続化）
    @Transient
    private List<Organization> children;

    @PrePersist
    public void onPrePersist() {
        if (this.createDate == null) {
            this.createDate = new Date();
        }
        this.updateDate = new Date();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updateDate = new Date();
    }
}
```

### 2.2 OrganizationType.java (リネーム)

```java
package jp.vemi.mirel.foundation.organization.model;

/**
 * 組織種別.
 */
public enum OrganizationType {
    COMPANY,    // 会社（Root）
    DIVISION,   // 本部
    DEPARTMENT, // 部
    SECTION,    // 課
    TEAM,       // チーム
    PROJECT,    // プロジェクト（期間限定）
    VIRTUAL     // バーチャル組織
}
```

### 2.3 CompanySettings.java (新規)

```java
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

/**
 * 会社設定エンティティ.
 * Root Organization（type=COMPANY）に対して期間（periodCode）ごとに紐づく。
 */
@Setter
@Getter
@Entity
@Table(name = "mir_company_settings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_company_settings_org_period",
        columnNames = {"organization_id", "period_code"}
    )
)
public class CompanySettings {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId; // Root Organizationへの参照

    @Column(name = "period_code")
    private String periodCode; // リレーション管理用（例: FY2026Q1）

    @Column(name = "fiscal_year_start")
    private Integer fiscalYearStart = 4; // 4 = 4月始まり

    @Column(name = "currency_code")
    private String currencyCode = "JPY";

    @Column
    private String timezone = "Asia/Tokyo";

    @Column
    private String locale = "ja_JP";

    // 監査カラム
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    @Column
    private String createUserId;

    @Column
    private Date createDate;

    @Column
    private String updateUserId;

    @Column
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        if (this.createDate == null) {
            this.createDate = new Date();
        }
        this.updateDate = new Date();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updateDate = new Date();
    }
}
```

### 2.4 OrganizationSettings.java (新規)

```java
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Date;
import java.util.Map;

/**
 * 組織設定エンティティ.
 * 各Organizationに対して期間（periodCode）ごとに紐づく（任意）。
 */
@Setter
@Getter
@Entity
@Table(name = "mir_organization_settings",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_organization_settings_org_period",
        columnNames = {"organization_id", "period_code"}
    )
)
public class OrganizationSettings {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "period_code")
    private String periodCode; // リレーション管理用（例: FY2026Q1）

    @Column(name = "allow_flexible_schedule", columnDefinition = "boolean default false")
    private Boolean allowFlexibleSchedule = false;

    @Column(name = "require_approval", columnDefinition = "boolean default true")
    private Boolean requireApproval = true;

    @Column(name = "max_member_count")
    private Integer maxMemberCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extended_settings", columnDefinition = "jsonb")
    private Map<String, Object> extendedSettings; // Key-Value形式の拡張設定

    // 監査カラム
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    @Column
    private String createUserId;

    @Column
    private Date createDate;

    @Column
    private String updateUserId;

    @Column
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        if (this.createDate == null) {
            this.createDate = new Date();
        }
        this.updateDate = new Date();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updateDate = new Date();
    }
}
```

### 2.5 UserOrganization.java (変更後)

```java
package jp.vemi.mirel.foundation.organization.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.Date;

/**
 * ユーザー所属情報.
 */
@Setter
@Getter
@Entity
@Table(name = "mir_user_organization")
public class UserOrganization {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "organization_id", nullable = false)
    private String organizationId; // 旧: unitId

    @Enumerated(EnumType.STRING)
    @Column(name = "position_type")
    private PositionType positionType; // PRIMARY, SECONDARY, TEMPORARY

    @Column(name = "role")
    private String role; // 旧: isManager (boolean) → manager, leader, member, observer など

    @Column(name = "job_title")
    private String jobTitle; // 部長、課長、担当

    @Column(name = "job_grade")
    private Integer jobGrade; // 職位等級（承認権限判定用）

    @Column(name = "can_approve", columnDefinition = "boolean default false")
    private Boolean canApprove = false; // 承認権限

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // 監査カラム
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 1L;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleteFlag = false;

    @Column
    private String createUserId;

    @Column
    private Date createDate;

    @Column
    private String updateUserId;

    @Column
    private Date updateDate;

    @PrePersist
    public void onPrePersist() {
        if (this.createDate == null) {
            this.createDate = new Date();
        }
        this.updateDate = new Date();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updateDate = new Date();
    }

    // ヘルパーメソッド（後方互換性）
    public boolean isManager() {
        return "manager".equalsIgnoreCase(this.role);
    }
}
```

---

## 3. DTO設計

### 3.1 OrganizationDto.java (統合版)

```java
package jp.vemi.mirel.foundation.organization.dto;

import jp.vemi.mirel.foundation.organization.model.OrganizationType;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrganizationDto {
    private String id;
    private String tenantId;
    private String parentId;
    private String name;
    private String displayName;
    private String code;
    private OrganizationType type;
    private String path;
    private Integer level;
    private Integer sortOrder;
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodCode;
    private Boolean isActive;

    // ツリー構造用
    private List<OrganizationDto> children;

    // 設定情報（オプション）
    private CompanySettingsDto companySettings;
    private OrganizationSettingsDto organizationSettings;
}
```

### 3.2 CompanySettingsDto.java (新規)

```java
package jp.vemi.mirel.foundation.organization.dto;

import lombok.Data;

@Data
public class CompanySettingsDto {
    private String id;
    private String organizationId;
    private String periodCode;
    private Integer fiscalYearStart;
    private String currencyCode;
    private String timezone;
    private String locale;
}
```

### 3.3 OrganizationSettingsDto.java (新規)

```java
package jp.vemi.mirel.foundation.organization.dto;

import lombok.Data;
import java.util.Map;

@Data
public class OrganizationSettingsDto {
    private String id;
    private String organizationId;
    private String periodCode;
    private Boolean allowFlexibleSchedule;
    private Boolean requireApproval;
    private Integer maxMemberCount;
    private Map<String, Object> extendedSettings;
}
```

### 3.4 UserOrganizationDto.java (変更後)

```java
package jp.vemi.mirel.foundation.organization.dto;

import jp.vemi.mirel.foundation.organization.model.PositionType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UserOrganizationDto {
    private String id;
    private String userId;
    private String organizationId; // 旧: unitId
    private PositionType positionType;
    private String role; // 旧: isManager
    private String jobTitle;
    private Integer jobGrade;
    private Boolean canApprove;
    private LocalDate startDate;
    private LocalDate endDate;

    // 拡張情報（表示用）
    private String organizationName; // 旧: unitName
    private String organizationCode; // 旧: unitCode
}
```

---

## 4. 実装フェーズ計画

| フェーズ    | 内容                        | 推定工数 |
| :---------- | :-------------------------- | :------- |
| **Phase 1** | DDL作成・テーブル変更       | 0.5日    |
| **Phase 2** | Model層実装（Entity, Enum） | 0.5日    |
| **Phase 3** | Repository層実装            | 0.5日    |
| **Phase 4** | Service層実装・統合         | 1日      |
| **Phase 5** | DTO・Controller層実装       | 0.5日    |
| **Phase 6** | Frontend型定義・API更新     | 0.5日    |
| **Phase 7** | テスト・検証                | 1日      |
| **合計**    |                             | 4.5日    |

---

## 5. 次のステップ

詳細設計の承認後:

1. **Phase 1**: DDLスクリプト作成・実行
2. **Phase 2-5**: バックエンド実装
3. **Phase 6**: フロントエンド実装
4. **Phase 7**: 単体テスト・E2Eテスト
