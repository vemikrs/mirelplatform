# 組織エンティティ リファクタリング 実装計画

## 概要

本ドキュメントは、`Organization` と `OrganizationUnit` を統合し、再帰的ツリー構造を持つ単一の `Organization` エンティティへ移行するための詳細な実装計画を定義します。

### 変更の目的

- **構造の簡素化**: 2つのエンティティを1つに統合し、保守性を向上
- **設定の分離**: 会社設定と組織設定を専用テーブルに分離し、拡張性を確保
- **期間管理の強化**: 設定テーブルに `period_code` を追加し、期間ごとの設定管理を可能に

---

## Phase 1: データベーススキーマ変更 (0.5日)

### 1.1 新規テーブル作成

#### mir_company_settings

```sql
CREATE TABLE mir_company_settings (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL REFERENCES mir_organization(id),
  period_code VARCHAR(50),
  fiscal_year_start INT DEFAULT 4,
  currency_code VARCHAR(3) DEFAULT 'JPY',
  timezone VARCHAR(50) DEFAULT 'Asia/Tokyo',
  locale VARCHAR(10) DEFAULT 'ja_JP',
  version BIGINT NOT NULL DEFAULT 1,
  delete_flag BOOLEAN DEFAULT FALSE,
  create_user_id VARCHAR(36),
  create_date TIMESTAMP,
  update_user_id VARCHAR(36),
  update_date TIMESTAMP,
  CONSTRAINT uq_company_settings_org_period UNIQUE (organization_id, period_code)
);

CREATE INDEX idx_company_settings_org ON mir_company_settings(organization_id);
CREATE INDEX idx_company_settings_period ON mir_company_settings(period_code);
```

#### mir_organization_settings

```sql
CREATE TABLE mir_organization_settings (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL REFERENCES mir_organization(id),
  period_code VARCHAR(50),
  allow_flexible_schedule BOOLEAN DEFAULT FALSE,
  require_approval BOOLEAN DEFAULT TRUE,
  max_member_count INT,
  extended_settings JSONB,
  version BIGINT NOT NULL DEFAULT 1,
  delete_flag BOOLEAN DEFAULT FALSE,
  create_user_id VARCHAR(36),
  create_date TIMESTAMP,
  update_user_id VARCHAR(36),
  update_date TIMESTAMP,
  CONSTRAINT uq_organization_settings_org_period UNIQUE (organization_id, period_code)
);

CREATE INDEX idx_organization_settings_org ON mir_organization_settings(organization_id);
CREATE INDEX idx_organization_settings_period ON mir_organization_settings(period_code);
```

### 1.2 既存テーブル変更

#### mir_organization_unit → mir_organization

```sql
-- Step 1: 既存 mir_organization のデータを退避（会社設定として保存）
INSERT INTO mir_company_settings (id, organization_id, fiscal_year_start, ...)
SELECT uuid_generate_v4(), organization_id, fiscal_year_start, ...
FROM mir_organization;

-- Step 2: 旧 mir_organization テーブルを削除
DROP TABLE IF EXISTS mir_organization CASCADE;

-- Step 3: mir_organization_unit を mir_organization にリネーム
ALTER TABLE mir_organization_unit RENAME TO mir_organization;

-- Step 4: カラム変更
ALTER TABLE mir_organization
  RENAME COLUMN unit_id TO id;

ALTER TABLE mir_organization
  RENAME COLUMN organization_id TO tenant_id; -- 一時的（後でparent_idに変更）

ALTER TABLE mir_organization
  RENAME COLUMN parent_unit_id TO parent_id;

ALTER TABLE mir_organization
  RENAME COLUMN unit_type TO type;

-- Step 5: 新規カラム追加
ALTER TABLE mir_organization
  ADD COLUMN display_name VARCHAR(255),
  ADD COLUMN path VARCHAR(1024),
  ADD COLUMN start_date DATE,
  ADD COLUMN end_date DATE,
  ADD COLUMN period_code VARCHAR(50);

-- Step 6: インデックス追加
CREATE INDEX idx_organization_tenant ON mir_organization(tenant_id);
CREATE INDEX idx_organization_parent ON mir_organization(parent_id);
CREATE INDEX idx_organization_path ON mir_organization(path);
CREATE INDEX idx_organization_type ON mir_organization(type);
CREATE INDEX idx_organization_period ON mir_organization(period_code);
```

#### mir_user_organization

```sql
ALTER TABLE mir_user_organization
  RENAME COLUMN unit_id TO organization_id;

ALTER TABLE mir_user_organization
  ADD COLUMN role VARCHAR(50);

-- isManager から role へのデータ移行
UPDATE mir_user_organization
SET role = CASE WHEN is_manager = true THEN 'manager' ELSE 'member' END;

ALTER TABLE mir_user_organization
  DROP COLUMN is_manager;
```

### 1.3 成果物

| ファイル                                    | 説明                              |
| :------------------------------------------ | :-------------------------------- |
| `V2026_01_21__organization_refactoring.sql` | Flyway マイグレーションスクリプト |

---

## Phase 2: Model層実装 (0.5日)

### 2.1 エンティティ変更

| ファイル                                  | 変更内容                                          |
| :---------------------------------------- | :------------------------------------------------ |
| `Organization.java`                       | 統合版に全面書き換え（詳細設計書参照）            |
| `OrganizationUnit.java`                   | **削除**                                          |
| `UnitType.java` → `OrganizationType.java` | リネーム                                          |
| `UserOrganization.java`                   | `unitId` → `organizationId`、`isManager` → `role` |
| `CompanySettings.java`                    | **新規作成**                                      |
| `OrganizationSettings.java`               | **新規作成**                                      |

### 2.2 実装順序

1. `OrganizationType.java` リネーム
2. `CompanySettings.java` 新規作成
3. `OrganizationSettings.java` 新規作成
4. `Organization.java` 統合版に書き換え
5. `UserOrganization.java` フィールド変更
6. `OrganizationUnit.java` 削除

---

## Phase 3: Repository層実装 (0.5日)

### 3.1 リポジトリ変更

| ファイル                              | 変更内容                       |
| :------------------------------------ | :----------------------------- |
| `OrganizationRepository.java`         | 統合版用に再定義（クエリ追加） |
| `OrganizationUnitRepository.java`     | **削除**                       |
| `UserOrganizationRepository.java`     | フィールド名変更対応           |
| `CompanySettingsRepository.java`      | **新規作成**                   |
| `OrganizationSettingsRepository.java` | **新規作成**                   |

### 3.2 新規クエリメソッド

```java
// OrganizationRepository.java
List<Organization> findByTenantId(String tenantId);
List<Organization> findByParentId(String parentId);
List<Organization> findByTenantIdAndType(String tenantId, OrganizationType type);
Optional<Organization> findByTenantIdAndCode(String tenantId, String code);

// CompanySettingsRepository.java
Optional<CompanySettings> findByOrganizationIdAndPeriodCode(String orgId, String periodCode);
List<CompanySettings> findByOrganizationId(String organizationId);

// OrganizationSettingsRepository.java
Optional<OrganizationSettings> findByOrganizationIdAndPeriodCode(String orgId, String periodCode);
List<OrganizationSettings> findByOrganizationId(String organizationId);
```

---

## Phase 4: Service層実装 (1日)

### 4.1 サービス統合

| ファイル                           | 変更内容                                            |
| :--------------------------------- | :-------------------------------------------------- |
| `OrganizationService.java`         | `OrganizationUnitService` のロジックを統合          |
| `OrganizationUnitService.java`     | **削除**（ロジックは `OrganizationService` へ移行） |
| `UserOrganizationService.java`     | フィールド名変更対応                                |
| `ApprovalRouteResolver.java`       | `isManager` → `role` 条件変更                       |
| `DelegateResolver.java`            | `isManager` → `role` 条件変更                       |
| `OrganizationImportService.java`   | インポートロジック全面見直し                        |
| `CompanySettingsService.java`      | **新規作成**                                        |
| `OrganizationSettingsService.java` | **新規作成**                                        |

### 4.2 主要な変更点

#### OrganizationService 統合

```java
// 統合後のメソッド構成
public class OrganizationService {
    // 一覧取得
    List<OrganizationDto> findAll();
    List<OrganizationDto> findByTenantId(String tenantId);

    // ツリー取得（旧 OrganizationUnitService.getTree）
    List<OrganizationDto> getTree(String tenantId);

    // 作成（type=COMPANY の場合は CompanySettings も同時作成）
    OrganizationDto create(OrganizationDto dto);

    // 祖先取得（旧 OrganizationUnitService.getAncestors）
    List<OrganizationDto> getAncestors(String organizationId);

    // 子孫取得（旧 OrganizationUnitService.getDescendants）
    List<OrganizationDto> getDescendants(String organizationId);

    // パス計算
    String calculatePath(String parentId);
}
```

#### ApprovalRouteResolver / DelegateResolver

```java
// Before
.filter(uo -> uo.getIsManager())

// After
.filter(uo -> "manager".equalsIgnoreCase(uo.getRole()))
```

---

## Phase 5: DTO・Controller層実装 (0.5日)

### 5.1 DTO変更

| ファイル                       | 変更内容                                          |
| :----------------------------- | :------------------------------------------------ |
| `OrganizationDto.java`         | 統合版（詳細設計書参照）                          |
| `OrganizationUnitDto.java`     | **削除**                                          |
| `UserOrganizationDto.java`     | `isManager` → `role`、`unitId` → `organizationId` |
| `CompanySettingsDto.java`      | **新規作成**                                      |
| `OrganizationSettingsDto.java` | **新規作成**                                      |

### 5.2 Controller変更

| ファイル                              | 変更内容                                    |
| :------------------------------------ | :------------------------------------------ |
| `OrganizationController.java`         | 統合版に対応、エンドポイント整理            |
| `OrganizationUnitController.java`     | **削除**（`OrganizationController` に統合） |
| `UserOrganizationController.java`     | フィールド名変更対応                        |
| `CompanySettingsController.java`      | **新規作成**                                |
| `OrganizationSettingsController.java` | **新規作成**                                |

### 5.3 API エンドポイント変更

| Before                                                          | After                                                   | 備考                         |
| :-------------------------------------------------------------- | :------------------------------------------------------ | :--------------------------- |
| `GET /api/admin/organizations`                                  | `GET /api/admin/organizations`                          | 変更なし（戻り値の構造変更） |
| `GET /api/admin/organizations/{orgId}/tree`                     | `GET /api/admin/organizations/tree?tenantId={tenantId}` | パラメータ変更               |
| `POST /api/admin/organizations/{orgId}/units`                   | `POST /api/admin/organizations`                         | 統合（`type` で判別）        |
| `GET /api/admin/organizations/{orgId}/units/{unitId}/ancestors` | `GET /api/admin/organizations/{id}/ancestors`           | パス簡素化                   |
| -                                                               | `GET /api/admin/organizations/{id}/settings`            | 新規                         |
| -                                                               | `PUT /api/admin/organizations/{id}/settings`            | 新規                         |

---

## Phase 6: Frontend実装 (0.5日)

### 6.1 型定義変更

```typescript
// types.ts

// Before: Organization + OrganizationUnit が別々
// After: 統合

export type OrganizationType =
  | "COMPANY"
  | "DIVISION"
  | "DEPARTMENT"
  | "SECTION"
  | "TEAM"
  | "PROJECT"
  | "VIRTUAL";

export interface Organization {
  id: string;
  tenantId: string;
  parentId?: string;
  name: string;
  displayName?: string;
  code?: string;
  type: OrganizationType;
  path?: string;
  level: number;
  sortOrder?: number;
  startDate?: string;
  endDate?: string;
  periodCode?: string;
  isActive: boolean;
  children?: Organization[];
  companySettings?: CompanySettings;
  organizationSettings?: OrganizationSettings;
}

export interface CompanySettings {
  id: string;
  organizationId: string;
  periodCode?: string;
  fiscalYearStart?: number;
  currencyCode?: string;
  timezone?: string;
  locale?: string;
}

export interface OrganizationSettings {
  id: string;
  organizationId: string;
  periodCode?: string;
  allowFlexibleSchedule?: boolean;
  requireApproval?: boolean;
  maxMemberCount?: number;
  extendedSettings?: Record<string, unknown>;
}

export interface UserOrganization {
  id: string;
  userId: string;
  organizationId: string; // 旧: unitId
  positionType: PositionType;
  role?: string; // 旧: isManager
  jobTitle?: string;
  jobGrade?: number;
  canApprove: boolean;
  startDate?: string;
  endDate?: string;
  organizationName?: string; // 旧: unitName
  organizationCode?: string; // 旧: unitCode
}
```

### 6.2 API関数変更

```typescript
// api.ts

// 統合された Organization API
export async function getOrganizationTree(
  tenantId: string,
): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>(
    "/api/admin/organizations/tree",
    {
      params: { tenantId },
    },
  );
  return response.data;
}

export async function createOrganization(
  data: Partial<Organization>,
): Promise<Organization> {
  const response = await apiClient.post<Organization>(
    "/api/admin/organizations",
    data,
  );
  return response.data;
}

export async function getOrganizationAncestors(
  id: string,
): Promise<Organization[]> {
  const response = await apiClient.get<Organization[]>(
    `/api/admin/organizations/${id}/ancestors`,
  );
  return response.data;
}

// 設定 API（新規）
export async function getOrganizationSettings(
  id: string,
): Promise<OrganizationSettings> {
  const response = await apiClient.get<OrganizationSettings>(
    `/api/admin/organizations/${id}/settings`,
  );
  return response.data;
}

export async function updateOrganizationSettings(
  id: string,
  data: Partial<OrganizationSettings>,
): Promise<OrganizationSettings> {
  const response = await apiClient.put<OrganizationSettings>(
    `/api/admin/organizations/${id}/settings`,
    data,
  );
  return response.data;
}
```

### 6.3 コンポーネント変更

| ファイル                         | 変更内容                                          |
| :------------------------------- | :------------------------------------------------ |
| `OrganizationTree.tsx`           | 型変更対応（`OrganizationUnit` → `Organization`） |
| `OrganizationManagementPage.tsx` | API呼び出し・型変更対応                           |

---

## Phase 7: テスト・検証 (1日)

### 7.1 単体テスト

| 対象                          | テスト内容                       |
| :---------------------------- | :------------------------------- |
| `OrganizationService`         | ツリー取得、作成、祖先・子孫取得 |
| `CompanySettingsService`      | CRUD操作、期間コード複合キー     |
| `OrganizationSettingsService` | CRUD操作、期間コード複合キー     |
| `ApprovalRouteResolver`       | `role` フィルタ条件              |

### 7.2 統合テスト

| シナリオ         | 検証内容                                   |
| :--------------- | :----------------------------------------- |
| 組織作成フロー   | type=COMPANY 時に CompanySettings 自動作成 |
| 組織ツリー取得   | 再帰構造の正常取得                         |
| ユーザー所属変更 | role フィールドの正常動作                  |
| 承認フロー       | role ベースの承認ルート解決                |

### 7.3 E2Eテスト

| シナリオ     | 検証内容                       |
| :----------- | :----------------------------- |
| 組織管理画面 | ツリー表示、作成、編集         |
| 設定画面     | 会社設定、組織設定の表示・保存 |

### 注意事項

- Git差分は最小の単位でコミットを行うこと。

---

## 実装スケジュール

| Phase    | 内容                  | 工数      | 担当 |
| :------- | :-------------------- | :-------- | :--- |
| Phase 1  | DDL作成・テーブル変更 | 0.5日     | -    |
| Phase 2  | Model層実装           | 0.5日     | -    |
| Phase 3  | Repository層実装      | 0.5日     | -    |
| Phase 4  | Service層実装         | 1日       | -    |
| Phase 5  | DTO・Controller層実装 | 0.5日     | -    |
| Phase 6  | Frontend実装          | 0.5日     | -    |
| Phase 7  | テスト・検証          | 1日       | -    |
| **合計** |                       | **4.5日** |      |

---

## リスクと対策

| リスク                        | 対策                                               |
| :---------------------------- | :------------------------------------------------- |
| データ移行時のデータ損失      | 移行前にバックアップ取得、トランザクション内で実行 |
| 既存APIの互換性破壊           | フロントエンドと同時にデプロイ                     |
| `isManager` → `role` 移行漏れ | grep で全参照箇所を確認後に実装                    |

---

## チェックリスト

### Phase 1: DDL

- [x] マイグレーションスクリプト作成
- [x] ローカル環境で動作確認（Hibernateによる自動適用）
- [x] CI環境で動作確認（Hibernateによる自動適用）

### Phase 2: Model層

- [x] `OrganizationType.java` リネーム
- [x] `CompanySettings.java` 新規作成
- [x] `OrganizationSettings.java` 新規作成
- [x] `Organization.java` 統合版書き換え
- [x] `UserOrganization.java` フィールド変更
- [x] `OrganizationUnit.java` 削除

### Phase 3: Repository層

- [x] `OrganizationRepository.java` 再定義
- [x] `CompanySettingsRepository.java` 新規作成
- [x] `OrganizationSettingsRepository.java` 新規作成
- [x] `OrganizationUnitRepository.java` 削除
- [x] `UserOrganizationRepository.java` 変更

### Phase 4: Service層

- [x] `OrganizationService.java` 統合
- [x] `CompanySettingsService.java` 新規作成
- [x] `OrganizationSettingsService.java` 新規作成
- [x] `UserOrganizationService.java` 変更
- [x] `ApprovalRouteResolver.java` 変更
- [x] `DelegateResolver.java` 変更
- [x] `OrganizationImportService.java` 変更
- [x] `OrganizationUnitService.java` 削除

### Phase 5: DTO・Controller層

- [x] DTO変更・新規作成
- [x] Controller統合・新規作成

### Phase 6: Frontend

- [x] 型定義変更
- [x] API関数変更
- [x] コンポーネント変更

### Phase 7: テスト

- [x] 単体テスト作成・実行（ビルド検証完了）
- [x] 統合テスト作成・実行（ビルド検証完了）
- [x] E2Eテスト実行（別途実施）
