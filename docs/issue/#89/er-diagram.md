# 組織エンティティ ER図

## 1. 現状 (As-is)

```mermaid
erDiagram
    %% ===== 現行モデル =====
    Organization["Organization (mir_organization)"] {
        string organizationId PK "組織ID"
        string tenantId FK "テナントID"
        string name "会社名"
        string code "会社コード"
        string description "説明"
        int fiscalYearStart "会計年度開始月"
        boolean isActive "有効フラグ"
    }

    OrganizationUnit["OrganizationUnit (mir_organization_unit)"] {
        string unitId PK "ユニットID"
        string organizationId FK "組織ID（親Organizationへの参照）"
        string parentUnitId FK "親ユニットID（Nullable）"
        string name "部署名"
        string code "部署コード"
        string unitType "種別（COMPANY, DIVISION, DEPARTMENT...）"
        int level "階層レベル（ROOT=0）"
        int sortOrder "表示順"
        boolean isActive "有効フラグ"
        date effectiveFrom "有効開始日"
        date effectiveTo "有効終了日"
    }

    UserOrganization["UserOrganization (mir_user_organization)"] {
        string id PK "ID"
        string userId FK "ユーザーID"
        string unitId FK "ユニットID"
        string positionType "所属種別（PRIMARY, SECONDARY, TEMPORARY）"
        string jobTitle "役職名"
        int jobGrade "職位等級"
        boolean isManager "管理者フラグ"
        boolean canApprove "承認権限"
        date startDate "開始日"
        date endDate "終了日"
    }

    %% Relationships
    Organization ||--|{ OrganizationUnit : "has"
    OrganizationUnit ||--o{ OrganizationUnit : "parent/child"
    OrganizationUnit ||--|{ UserOrganization : "assigns"
```

### 現状の課題

| 課題                   | 詳細                                                                                            |
| :--------------------- | :---------------------------------------------------------------------------------------------- |
| **構造の二重化**       | `Organization` が「会社（Level 0）」として固定されているため、`OrganizationUnit` と役割が重複。 |
| **冗長なリレーション** | `UnitType.COMPANY` が存在するにも関わらず、別エンティティとして `Organization` が必須。         |
| **設定の分離不足**     | 会社設定（`fiscalYearStart` 等）が `Organization` に埋め込まれており、拡張性が低い。            |

---

## 2. あるべき姿 (To-be)

```mermaid
erDiagram
    %% ===== 新モデル =====
    Organization["Organization (mir_organization)"] {
        string id PK "組織ID"
        string tenantId "テナントID"
        string parentId FK "親組織ID（Rootはnull）"
        string name "組織名（正式名称）"
        string displayName "表示名"
        string code "組織コード"
        string type "種別（COMPANY, DIVISION, DEPARTMENT, TEAM...）"
        string path "階層パス（例: /root/div/dept）"
        int level "階層レベル（0=COMPANY）"
        int sortOrder "表示順"
        boolean isActive "有効フラグ"
        date startDate "開始日"
        date endDate "終了日"
        string periodCode "期間コード（リレーション管理用）"
    }

    CompanySettings["CompanySettings (mir_company_settings)"] {
        string id PK "設定ID"
        string organizationId FK "組織ID（Root Organizationへの参照）"
        string periodCode "期間コード（リレーション管理用）"
        int fiscalYearStart "会計年度開始月"
        string currencyCode "通貨コード"
        string timezone "タイムゾーン"
        string locale "ロケール"
    }

    OrganizationSettings["OrganizationSettings (mir_organization_settings)"] {
        string id PK "設定ID"
        string organizationId FK "組織ID"
        string periodCode "期間コード（リレーション管理用）"
        boolean allowFlexibleSchedule "フレックス許可"
        boolean requireApproval "承認必須"
        int maxMemberCount "最大メンバー数"
        json extendedSettings "拡張設定（JSON）"
    }

    UserOrganization["UserOrganization (mir_user_organization)"] {
        string id PK "ID"
        string userId FK "ユーザーID"
        string organizationId FK "組織ID"
        string positionType "所属種別（PRIMARY, SECONDARY, TEMPORARY）"
        string role "役割（manager, leader, member...）"
        string jobTitle "役職名"
        int jobGrade "職位等級"
        boolean canApprove "承認権限"
        date startDate "開始日"
        date endDate "終了日"
    }

    %% Relationships
    Organization ||--o{ Organization : "parent/child"
    Organization ||--o| CompanySettings : "has (Root only)"
    Organization ||--o| OrganizationSettings : "has"
    Organization ||--|{ UserOrganization : "assigns"
```

---

## 3. 変更点サマリ

| 項目                   | As-is                       | To-be                                    | 備考                                     |
| :--------------------- | :-------------------------- | :--------------------------------------- | :--------------------------------------- |
| **会社エンティティ**   | `Organization` (別テーブル) | `Organization` (Root Node, type=COMPANY) | 統合により1テーブルへ集約                |
| **会社設定**           | `Organization` に埋め込み   | `CompanySettings` テーブルへ分離         | Root Organizationのみが持つ              |
| **組織設定**           | なし                        | `OrganizationSettings` テーブルを新設    | 基本項目＋JSON拡張値                     |
| **表示名**             | なし                        | `displayName` カラム追加                 | 正式名称と表示名を分離                   |
| **期間管理**           | `effectiveFrom/To`          | `startDate`, `endDate`, `periodCode`     | リレーション管理用に `periodCode` を追加 |
| **マネージャーフラグ** | `isManager` (boolean)       | `role` (String)                          | より柔軟な役割定義へ変更                 |
| **パス階層**           | なし                        | `path` カラム追加                        | 検索効率化のため                         |

---

## 4. 補足: UnitType Enum

`Organization.type` には以下の値を想定します（現行の `UnitType` を踏襲）。

| Type         | 説明                     |
| :----------- | :----------------------- |
| `COMPANY`    | 会社（Root）             |
| `DIVISION`   | 本部                     |
| `DEPARTMENT` | 部                       |
| `SECTION`    | 課                       |
| `TEAM`       | チーム                   |
| `PROJECT`    | プロジェクト（期間限定） |
| `VIRTUAL`    | バーチャル組織           |

---

## 5. 次のステップ

To-be ER図の承認後、以下を実施予定:

1. アプリケーションの影響範囲調査
2. 詳細設計（DDL / Entity クラス定義）
3. 実装・テスト
