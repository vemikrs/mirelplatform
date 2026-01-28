# 組織エンティティ リファクタリング 影響調査レポート

## 1. 調査サマリ

| カテゴリ       | ファイル数 | 主な影響                             |
| :------------- | ---------: | :----------------------------------- |
| **Model**      |          8 | エンティティ統合・新設定テーブル追加 |
| **Repository** |          5 | クエリ変更・新リポジトリ追加         |
| **Service**    |          6 | ロジック移行・役割フラグ変更         |
| **DTO**        |          4 | フィールド追加・統合対応             |
| **Controller** |          3 | APIエンドポイント整理                |
| **Frontend**   |          4 | 型定義・API呼び出し更新              |

---

## 2. バックエンド影響範囲

### 2.1 モデル層 (`foundation/organization/model/`)

| ファイル                                                                                                         | 変更内容                                                                                        | 影響度 |
| :--------------------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------- | :----: |
| [Organization.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/model/Organization.java)         | **削除** → `OrganizationUnit` を新 `Organization` にリネーム                                    | 🔴 高  |
| [OrganizationUnit.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/model/OrganizationUnit.java) | **リネーム** → `Organization`。`displayName`, `path`, `startDate`, `endDate`, `periodCode` 追加 | 🔴 高  |
| [UserOrganization.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/model/UserOrganization.java) | `isManager` → `role` (String) へ変更、`unitId` → `organizationId` リネーム                      | 🟡 中  |
| [UnitType.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/model/UnitType.java)                 | 変更なし（`COMPANY` 既存）                                                                      | 🟢 低  |
| **[NEW] CompanySettings.java**                                                                                   | 新規作成（会社設定エンティティ）                                                                | 🟡 中  |
| **[NEW] OrganizationSettings.java**                                                                              | 新規作成（組織設定エンティティ with JSON拡張）                                                  | 🟡 中  |

### 2.2 リポジトリ層 (`foundation/organization/repository/`)

| ファイル                                                                                                                                  | 変更内容                                                                       | 影響度 |
| :---------------------------------------------------------------------------------------------------------------------------------------- | :----------------------------------------------------------------------------- | :----: |
| [OrganizationRepository.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/repository/OrganizationRepository.java)         | **削除** または新 `Organization` 用に再定義                                    | 🔴 高  |
| [OrganizationUnitRepository.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/repository/OrganizationUnitRepository.java) | **リネーム** → `OrganizationRepository`。クエリ修正（`parentId`, `path` 対応） | 🔴 高  |
| [UserOrganizationRepository.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/repository/UserOrganizationRepository.java) | フィールド名変更（`unitId` → `organizationId`）                                | 🟡 中  |
| **[NEW] CompanySettingsRepository.java**                                                                                                  | 新規作成                                                                       | 🟡 中  |
| **[NEW] OrganizationSettingsRepository.java**                                                                                             | 新規作成                                                                       | 🟡 中  |

### 2.3 サービス層 (`foundation/organization/service/`)

| ファイル                                                                                                                             | 変更内容                                                 | 影響度 |
| :----------------------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------- | :----: |
| [OrganizationService.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/service/OrganizationService.java)             | **統合**: ルート組織（type=COMPANY）の作成ロジックへ変更 | 🔴 高  |
| [OrganizationUnitService.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/service/OrganizationUnitService.java)     | **統合**: `OrganizationService` に統合、リネーム         | 🔴 高  |
| [UserOrganizationService.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/service/UserOrganizationService.java)     | `isManager` → `role` 参照変更                            | 🟡 中  |
| [ApprovalRouteResolver.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/service/ApprovalRouteResolver.java)         | `isManager` → `role` フィルタ条件変更                    | 🟡 中  |
| [DelegateResolver.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/service/DelegateResolver.java)                   | `isManager` → `role` フィルタ条件変更                    | 🟡 中  |
| [OrganizationImportService.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/service/OrganizationImportService.java) | インポートロジック全面見直し                             | 🔴 高  |
| **[NEW] CompanySettingsService.java**                                                                                                | 新規作成                                                 | 🟡 中  |
| **[NEW] OrganizationSettingsService.java**                                                                                           | 新規作成                                                 | 🟡 中  |

### 2.4 DTO層 (`foundation/organization/dto/`)

| ファイル                                                                                                             | 変更内容                                                                                     | 影響度 |
| :------------------------------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------- | :----: |
| [OrganizationDto.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/dto/OrganizationDto.java)         | **統合**: 新フィールド追加（`displayName`, `path`, `startDate`, `endDate`, `periodCode` 等） | 🔴 高  |
| [OrganizationUnitDto.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/dto/OrganizationUnitDto.java) | **削除** または `OrganizationDto` に統合                                                     | 🔴 高  |
| [UserOrganizationDto.java](backend/src/main/java/jp/vemi/mirel/foundation/organization/dto/UserOrganizationDto.java) | `isManager` → `role` フィールド変更                                                          | 🟡 中  |
| **[NEW] CompanySettingsDto.java**                                                                                    | 新規作成                                                                                     | 🟡 中  |
| **[NEW] OrganizationSettingsDto.java**                                                                               | 新規作成                                                                                     | 🟡 中  |

### 2.5 コントローラ層 (`foundation/web/api/organization/`)

| ファイル                                                                                                                               | 変更内容                                            | 影響度 |
| :------------------------------------------------------------------------------------------------------------------------------------- | :-------------------------------------------------- | :----: |
| [OrganizationController.java](backend/src/main/java/jp/vemi/mirel/foundation/web/api/organization/OrganizationController.java)         | **統合**: 新 `Organization` 対応                    | 🔴 高  |
| [OrganizationUnitController.java](backend/src/main/java/jp/vemi/mirel/foundation/web/api/organization/OrganizationUnitController.java) | **統合**: `OrganizationController` に統合または削除 | 🔴 高  |
| [UserOrganizationController.java](backend/src/main/java/jp/vemi/mirel/foundation/web/api/organization/UserOrganizationController.java) | フィールド名変更対応                                | 🟡 中  |
| **[NEW] CompanySettingsController.java**                                                                                               | 新規作成                                            | 🟢 低  |
| **[NEW] OrganizationSettingsController.java**                                                                                          | 新規作成                                            | 🟢 低  |

---

## 3. フロントエンド影響範囲 (`apps/frontend-v3/`)

| ファイル                                                                                                          | 変更内容                                                         | 影響度 |
| :---------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------- | :----: |
| [types.ts](apps/frontend-v3/src/features/organization/types.ts)                                                   | `Organization` / `OrganizationUnit` 型統合、`isManager` → `role` | 🔴 高  |
| [api.ts](apps/frontend-v3/src/features/organization/api.ts)                                                       | API関数統合（エンドポイント変更対応）                            | 🔴 高  |
| [OrganizationManagementPage.tsx](apps/frontend-v3/src/features/organization/pages/OrganizationManagementPage.tsx) | 型・API呼び出し変更                                              | 🟡 中  |
| [router.config.tsx](apps/frontend-v3/src/app/router.config.tsx)                                                   | ルーティング確認（通常変更不要）                                 | 🟢 低  |

---

## 4. データベース変更 (DDL)

### 4.1 削除対象

```sql
DROP TABLE IF EXISTS mir_organization;
```

### 4.2 変更対象

```sql
-- mir_organization_unit → mir_organization へリネーム
ALTER TABLE mir_organization_unit RENAME TO mir_organization;

-- カラム追加・変更
ALTER TABLE mir_organization
  RENAME COLUMN organization_id TO parent_id,
  ADD COLUMN display_name VARCHAR(255),
  ADD COLUMN path VARCHAR(1024),
  ADD COLUMN start_date DATE,
  ADD COLUMN end_date DATE,
  ADD COLUMN period_code VARCHAR(50);
```

### 4.3 新規作成

```sql
-- 会社設定テーブル
CREATE TABLE mir_company_settings (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL REFERENCES mir_organization(id),
  period_code VARCHAR(50), -- リレーション管理用（例: FY2026Q1）
  fiscal_year_start INT,
  currency_code VARCHAR(3),
  timezone VARCHAR(50),
  locale VARCHAR(10),
  version BIGINT NOT NULL DEFAULT 1,
  delete_flag BOOLEAN DEFAULT FALSE,
  create_user_id VARCHAR(36),
  create_date TIMESTAMP,
  update_user_id VARCHAR(36),
  update_date TIMESTAMP,
  -- 複合ユニーク制約（組織 + 期間）
  CONSTRAINT uq_company_settings_org_period UNIQUE (organization_id, period_code)
);

-- 組織設定テーブル
CREATE TABLE mir_organization_settings (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL REFERENCES mir_organization(id),
  period_code VARCHAR(50), -- リレーション管理用（例: FY2026Q1）
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
  -- 複合ユニーク制約（組織 + 期間）
  CONSTRAINT uq_organization_settings_org_period UNIQUE (organization_id, period_code)
);

-- UserOrganization変更
ALTER TABLE mir_user_organization
  RENAME COLUMN unit_id TO organization_id,
  DROP COLUMN is_manager,
  ADD COLUMN role VARCHAR(50);
```

---

## 5. 移行戦略

1. **フェーズ1: スキーマ変更**
   - 新テーブル作成（`mir_company_settings`, `mir_organization_settings`）
   - カラム追加（`display_name`, `path`, `start_date`, `end_date`, `periodCode`）

2. **フェーズ2: バックエンド実装**
   - エンティティ統合・リネーム
   - サービス層統合
   - API整備

3. **フェーズ3: フロントエンド実装**
   - 型定義更新
   - API呼び出し更新
   - UI調整

4. **フェーズ4: 検証**
   - 単体テスト
   - E2Eテスト

---

## 6. 次のステップ

To-be ER図と本影響調査レポートの承認後:

1. 詳細設計（DDL / Entity クラス定義）の作成
2. 実装計画（Implementation Plan）の策定
3. フェーズごとの実装・テスト
