# フィーチャーフラグ管理システム実装計画書

## Overview

mirelplatform に統合的なフィーチャーフラグ管理システムを導入し、ユーザーダッシュボードでライセンス機能の可視化、システム管理画面でのフラグ操作、開発中機能の段階的公開を実現します。

**主要目標:**
1. 全ユーザーのホームダッシュボードで、所有ライセンスと利用可能機能を表示
2. バックエンドに汎用性の高いフィーチャーフラグエンティティと管理API実装
3. システム管理画面で堅牢なフィーチャーフラグCRUD操作UI構築
4. 開発中・開発予定機能も「開発中」フラグとして管理・表示
5. SaaSStatusPage を navigation の「開発中」セクションに暫定追加

---

## Requirements

### 機能要件

#### FR-1: ユーザーダッシュボード（ホーム画面拡張）
- **FR-1.1** ログインユーザーの所有ライセンス一覧を表示（アプリケーション別・ティア別）
- **FR-1.2** 各ライセンスで利用可能な機能を階層表示（Feature Flag紐付け）
- **FR-1.3** 機能ステータスバッジ表示（`STABLE`, `BETA`, `ALPHA`, `PLANNING`, `DEPRECATED`）
- **FR-1.4** アップグレード促進UI（FREE→PRO、PRO→MAX）
- **FR-1.5** 開発中機能のプレビュー表示（フラグが `IN_DEVELOPMENT: true` の機能）

#### FR-2: フィーチャーフラグ管理（バックエンド）
- **FR-2.1** `FeatureFlag` エンティティ実装
  - `id` (UUID), `featureKey` (一意識別子), `featureName`, `description`
  - `applicationId` (promarker, etc.), `status` (STABLE, BETA, ALPHA, PLANNING, DEPRECATED)
  - `inDevelopment` (boolean), `requiredLicenseTier` (FREE/TRIAL/PRO/MAX/null)
  - `enabledByDefault`, `enabledForUserIds`, `enabledForTenantIds` (JSON or relation)
  - `metadata` (JSON: 追加情報・設定値)
- **FR-2.2** CRUD API実装
  - `GET /admin/features` - 一覧取得（ページング・フィルタ）
  - `GET /admin/features/{id}` - 詳細取得
  - `POST /admin/features` - 新規作成
  - `PUT /admin/features/{id}` - 更新
  - `DELETE /admin/features/{id}` - 削除（論理削除）
- **FR-2.3** ユーザー向けAPI
  - `GET /features/available` - 現在のユーザー・テナントで利用可能な機能一覧
- **FR-2.4** ExecutionContext連携
  - `executionContext.hasFeature(featureKey)` メソッド追加
  - ライセンスチェック + フィーチャーフラグチェック統合

#### FR-3: システム管理画面（フロントエンド）
- **FR-3.1** `/admin/features` ルート実装
- **FR-3.2** 機能一覧画面
  - テーブル表示（featureKey, name, application, status, tier, inDevelopment）
  - フィルタリング（アプリケーション、ステータス、開発中のみ）
  - ソート、ページング、検索
- **FR-3.3** 機能詳細・編集ダイアログ
  - フォームバリデーション（featureKey重複チェック）
  - ライセンスティア選択（FREE/TRIAL/PRO/MAX/なし）
  - ステータス選択、開発中フラグ切替
  - メタデータJSON編集（CodeMirror or Monaco Editor）
- **FR-3.4** 新規作成ダイアログ
- **FR-3.5** 削除確認ダイアログ（論理削除実行）

#### FR-4: 初期データ投入
- **FR-4.1** DatabaseUtil 拡張（`initializeFeatureFlagData()`）
- **FR-4.2** ProMarker 機能定義
  - `promarker.basic_generation` (FREE, STABLE)
  - `promarker.multi_file_generation` (PRO, STABLE)
  - `promarker.custom_stencil_upload` (MAX, BETA)
  - `promarker.stencil_editor` (PRO, STABLE)
- **FR-4.3** 開発中機能定義（navigation.json の `inDevelopment` 反映）
  - `users_tenants_management` (null, PLANNING, inDevelopment=true)
  - `themes_switcher` (null, ALPHA, inDevelopment=true)
  - `menu_nav_management` (null, PLANNING, inDevelopment=true)
  - `context_management` (null, PLANNING, inDevelopment=true)

#### FR-5: Navigation 統合
- **FR-5.1** navigation.json に SaaSStatusPage 追加
  ```json
  "inDevelopment": [
    {
      "id": "saas-status",
      "label": "SaaS実装状況",
      "path": "/saas-status",
      "description": "Phase 1-4 実装進捗確認（開発者向け）",
      "badge": { "label": "開発中", "tone": "warning" }
    },
    // ... 既存の inDevelopment アイテム
  ]
  ```

### 非機能要件

#### NFR-1: パフォーマンス
- フィーチャーフラグ判定はリクエストスコープでキャッシュ（ExecutionContext）
- 管理画面のテーブルは仮想スクロール対応（100件以上の場合）

#### NFR-2: セキュリティ
- `/admin/*` は `@PreAuthorize("hasRole('ADMIN')")` で保護
- FeatureFlag API は監査ログ記録対象

#### NFR-3: 拡張性
- FeatureFlag エンティティは将来の A/B テスト、カナリアリリース対応可能な設計
  - `rollout_percentage`, `target_segments` カラムを Phase 1 で定義（Phase 3 で使用）
  - `license_resolve_strategy` カラムで機能ごとのライセンス判定戦略を制御可能（Phase 2 で使用）
- メタデータJSON で柔軟な拡張（UI表示設定、使用量制限、A/Bテスト変数等）
- スキーママイグレーション不要で新機能を段階的にロールアウト可能（Forward Compatibility）

#### NFR-4: 運用性
- フィーチャーフラグ変更時は AuditLog 自動記録
- 削除は論理削除（`deleteFlag=true`）

---

## Implementation Steps

### Phase 1: データモデル・バックエンド基盤（3-5 steps）

**Step 1.1: FeatureFlag エンティティ実装**
- ファイル: `backend/src/main/java/jp/vemi/mirel/foundation/abst/dao/entity/FeatureFlag.java`
- 内容: JPA エンティティ、インデックス定義（`featureKey UNIQUE`, `applicationId`）
- 列挙型: 
  - `FeatureStatus` (STABLE, BETA, ALPHA, PLANNING, DEPRECATED)
  - `LicenseResolveStrategy` (TENANT_PRIORITY, USER_PRIORITY, TENANT_ONLY, USER_ONLY, EITHER) ※Phase 2で使用
- **将来拡張用カラム（Phase 1で定義、Phase 2+で使用）:**
  - `rolloutPercentage` (Integer, default 100): カナリアリリース用
  - `targetSegments` (String/JSON): セグメント別ロールアウト
  - `disabledForUserIds`, `disabledForTenantIds` (String/JSON): ブラックリスト制御
  - `licenseResolveStrategy` (Enum): テナント/ユーザーライセンス判定優先度
- **注意**: Phase 1ではこれらのカラムは NULL または デフォルト値のまま。ビジネスロジックでは未使用

**Step 1.2: FeatureFlagRepository 実装**
- ファイル: `backend/src/main/java/jp/vemi/mirel/foundation/abst/dao/repository/FeatureFlagRepository.java`
- メソッド:
  - `findByApplicationId(String applicationId)`
  - `findByInDevelopmentTrue()`
  - `findByFeatureKey(String featureKey)`
  - `findEffectiveFeatures(String userId, String tenantId, Instant now)` (カスタムクエリ)

**Step 1.3: FeatureFlagService 実装**
- ファイル: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/service/FeatureFlagService.java`
- メソッド: CRUD 操作、フィルタリング、ページング
- ビジネスロジック: featureKey 重複チェック、ライセンス整合性検証

**Step 1.4: AdminFeatureFlagController 実装**
- ファイル: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/admin/controller/AdminFeatureFlagController.java`
- エンドポイント: `GET/POST/PUT/DELETE /admin/features/**`
- 認可: `@PreAuthorize("hasRole('ADMIN')")`

**Step 1.5: ユーザー向けFeatureController 実装**
- ファイル: `backend/src/main/java/jp/vemi/mirel/foundation/web/api/feature/controller/FeatureController.java`
- エンドポイント: `GET /features/available`
- ロジック: ExecutionContext から userId, tenantId 取得 → 有効フィーチャーフラグ + ライセンス判定

**Step 1.6: ExecutionContext 拡張**
- ファイル: ExecutionContext.java
- 追加メソッド:
  ```java
  public boolean hasFeature(String featureKey);
  public List<FeatureFlag> getAvailableFeatures();
  ```
- フィーチャーフラグキャッシュ実装（リクエストスコープ Map）

**Step 1.7: 初期データ投入**
- ファイル: `backend/src/main/resources/data/feature_flags.csv`
- 形式: CSV（id, feature_key, feature_name, description, application_id, status, in_development, required_license_tier, enabled_by_default, metadata）
- DatabaseUtil: CSV読込ロジック実装（`initializeFeatureFlagData()`）
- 内容: ProMarker 4機能 + 開発中機能 4件のデータ投入

### Phase 2: システム管理画面（3-4 steps）

**Step 2.1: Feature管理ルート追加**
- ファイル: router.config.tsx
- ルート: `/admin/features` → `AdminFeaturesPage`（ProtectedRoute + ADMIN role check）

**Step 2.2: AdminFeaturesPage 実装**
- ファイル: `apps/frontend-v3/src/features/admin/pages/AdminFeaturesPage.tsx`
- コンポーネント:
  - FeatureFlagTable（TanStack Table使用、ページング・ソート・フィルタ）
  - CreateFeatureButton → CreateFeatureDialog
  - EditFeatureDialog（featureId 受取）
  - DeleteConfirmDialog

**Step 2.3: FeatureFlagForm 実装**
- ファイル: `apps/frontend-v3/src/features/admin/components/FeatureFlagForm.tsx`
- フォーム項目: featureKey, featureName, description, applicationId, status, requiredLicenseTier, inDevelopment, metadata（JSON）
- バリデーション: Zod schema使用、featureKey重複API呼び出し

**Step 2.4: Feature API Client 実装**
- ファイル: `apps/frontend-v3/src/lib/api/features.ts`
- 関数:
  ```typescript
  getFeatures(params: FeatureFilterParams): Promise<FeaturePage>
  getFeature(id: string): Promise<FeatureFlag>
  createFeature(data: CreateFeatureRequest): Promise<FeatureFlag>
  updateFeature(id: string, data: UpdateFeatureRequest): Promise<FeatureFlag>
  deleteFeature(id: string): Promise<void>
  getAvailableFeatures(): Promise<FeatureFlag[]>
  ```

### Phase 3: ユーザーダッシュボード拡張（2-3 steps）

**Step 3.1: HomePage 拡張（ライセンス情報セクション追加）**
- ファイル: HomePage.tsx
- 新規セクション:
  - 「あなたのライセンス」カード（LicenseCard コンポーネント）
  - 各ライセンスの利用可能機能一覧（FeatureList コンポーネント）
  - アップグレード促進ボタン（FREE/PRO の場合のみ表示）

**Step 3.2: LicenseCard コンポーネント実装**
- ファイル: `apps/frontend-v3/src/features/home/components/LicenseCard.tsx`
- 表示内容:
  - アプリケーション名、ライセンスティア、有効期限
  - 利用可能機能数（`getAvailableFeatures()` API使用）
  - ステータスバッジ（STABLE=緑、BETA=黄、ALPHA=オレンジ、PLANNING=グレー）

**Step 3.3: FeatureList コンポーネント実装**
- ファイル: `apps/frontend-v3/src/features/home/components/FeatureList.tsx`
- 階層表示:
  - アプリケーション > 機能一覧
  - 各機能: 名前、説明、ステータスバッジ、「開発中」タグ
  - 利用不可機能はグレーアウト + アップグレード誘導

### Phase 4: Navigation統合・UI調整（2-3 steps）

**Step 4.1: navigation.json 更新**
- ファイル: navigation.json
- 変更内容:
  - `inDevelopment` 配列の先頭に `saas-status` 追加
  - 既存の開発中機能の `badge.tone` を統一（`warning` or `info`）

**Step 4.2: SaaSStatusPage の UI/UX改善（optional）**
- ファイル: SaaSStatusPage.tsx
- 改善内容:
  - Liquid Glass デザイン適用（HomePage と統一）
  - Phase 4 の未完了項目を最新情報に更新
  - 「このページは開発者向けです」の注意書き追加

**Step 4.3: RootLayout のメニュー表示調整**
- ファイル: RootLayout.tsx
- 変更内容:
  - `inDevelopment` セクションのバッジ表示強化
  - 開発中機能へのリンク時に警告ツールチップ表示（optional）

### Phase 5: テスト・検証・ドキュメント（2-3 steps）

**Step 5.1: バックエンド単体テスト**
- ファイル: `backend/src/test/java/jp/vemi/mirel/foundation/web/api/admin/service/FeatureFlagServiceTest.java`
- テストケース:
  - CRUD操作正常系
  - featureKey重複エラー
  - ライセンスティア別フィルタリング
  - 開発中フラグフィルタリング

**Step 5.2: E2Eテスト（Admin機能）**
- ファイル: `packages/e2e/tests/specs/admin/feature-flag-management.spec.ts`
- シナリオ:
  - 管理者ログイン → `/admin/features` アクセス
  - 新規フィーチャーフラグ作成
  - 編集・削除
  - 一般ユーザーでアクセス拒否確認

**Step 5.3: E2Eテスト（ユーザーダッシュボード）**
- ファイル: `packages/e2e/tests/specs/saas/dashboard-license-features.spec.ts`
- シナリオ:
  - 異なるライセンスティアでログイン（FREE, PRO, MAX）
  - ダッシュボードの利用可能機能表示確認
  - 開発中機能の表示確認

**Step 5.4: ドキュメント作成**
- ファイル: `docs/issue/#<issue番号>/feature-flag-implementation-report.md`
- 内容:
  - 実装概要
  - データモデル図
  - API仕様書
  - 管理画面操作手順
  - 初期データ一覧

---

## Validation & Testing

### 主要テストケース

#### TC-1: フィーチャーフラグCRUD（管理者）
- 前提: ADMIN ロールでログイン
- 手順:
  1. `/admin/features` で一覧表示
  2. 「新規作成」クリック → フォーム入力 → 保存
  3. 一覧で新規作成した機能を検索
  4. 「編集」クリック → 内容変更 → 保存
  5. 「削除」クリック → 確認 → 削除実行
- 期待結果: すべての操作が成功、AuditLog記録確認

#### TC-2: ライセンス別機能表示（ユーザー）
- 前提: FREE, PRO, MAX の3ユーザーを用意
- 手順:
  1. 各ユーザーでログイン → ホーム画面
  2. 「あなたのライセンス」セクション確認
  3. 利用可能機能一覧確認
- 期待結果:
  - FREE: `promarker.basic_generation` のみ表示
  - PRO: `basic_generation`, `multi_file_generation`, `stencil_editor` 表示
  - MAX: すべて表示（`custom_stencil_upload` 含む）

#### TC-3: 開発中機能表示
- 前提: 任意のユーザーでログイン
- 手順:
  1. ホーム画面で「開発中の機能」セクション確認
  2. navigation メニューの「開発中」セクション確認
  3. `/saas-status` リンククリック
- 期待結果:
  - 開発中機能が「開発中」バッジ付きで表示
  - SaaSStatusPage が表示される

#### TC-4: ExecutionContext連携
- 前提: ProMarker の `/mapi/apps/mste/api/generate` に `@RequireFeature("promarker.basic_generation")` 付与
- 手順:
  1. FREE ライセンスユーザーでコード生成API呼び出し
  2. ライセンスなしユーザーでコード生成API呼び出し
- 期待結果:
  - FREE: 成功
  - ライセンスなし: 403 Forbidden

### 受け入れ基準

- [ ] FeatureFlag エンティティがDB初期化時に正しく投入される
- [ ] `/admin/features` で管理者がCRUD操作可能
- [ ] `/features/available` が現在のユーザー・テナント・ライセンスで利用可能な機能を返す
- [ ] ホーム画面で所有ライセンスと利用可能機能が表示される
- [ ] 開発中機能が「開発中」バッジ付きで navigation とダッシュボードに表示される
- [ ] `SaaSStatusPage` が `inDevelopment` セクションからアクセス可能
- [ ] E2Eテストが全て成功
- [ ] 実装報告書が `docs/issue/#<issue番号>/` に保存される

---

## Dependencies

### 内部モジュール依存
- `ExecutionContext` (既存)
- `ApplicationLicense` エンティティ (既存)
- `authStore` (Zustand, 既存)
- `@mirel/ui` (既存)
- TanStack Query, TanStack Table (既存)

### 外部サービス依存
- なし（内部完結）

### 前提条件
- ApplicationLicense エンティティと初期データが正常動作
- ExecutionContext が全リクエストで正しく初期化される
- Spring AOP が有効（既存の `@RequireLicense` で検証済み）

---

## Risks

### 技術リスク

**R-1: フィーチャーフラグとライセンス管理の二重化**
- 影響度: 中
- 軽減策: ExecutionContext で両者を統合判定する `hasFeatureWithLicense(featureKey)` メソッド実装
- 備考: 将来的にライセンスティアをフィーチャーフラグに統合する可能性あり

**R-2: 初期データ投入の複雑化**
- 影響度: 低
- 軽減策: `initializeFeatureFlagData()` を独立したメソッドとし、既存の `initializeSaasTestData()` から呼び出す
- 備考: テストデータとプロダクションデータの分離を考慮

**R-3: メタデータJSON の型安全性**
- 影響度: 低
- 軽減策: TypeScript 側で `FeatureFlagMetadata` インターフェース定義、Zod でバリデーション
- 備考: 将来的に JSON Schema 検証を導入

### 運用リスク

**R-4: フィーチャーフラグ設定ミスによる機能停止**
- 影響度: 高
- 軽減策:
  - 管理画面に「プレビュー」機能（変更前後の diff 表示）
  - 重要フィーチャーフラグ削除時に確認ダイアログ2段階
  - AuditLog による変更履歴追跡

**R-5: 開発中機能の誤公開**
- 影響度: 中
- 軽減策:
  - `inDevelopment=true` の機能は navigation でバッジ表示必須
  - プロダクションビルド時に環境変数で開発中機能を非表示にする仕組み（optional）

---

## Design Decisions

### D-1: フィーチャーフラグとライセンスの関係性
`requiredLicenseTier` を FeatureFlag エンティティのカラムとして実装。シンプルで高速なクエリを実現し、初期段階の複雑性を回避。将来的に複雑な条件が必要になった場合は `FeatureLicense` 中間テーブルへの移行を検討。

### D-2: SaaSStatusPage の扱い
navigation の `inDevelopment` セクションに追加し、実装進捗を開発チーム・ステークホルダーと共有。Phase 4 完全完了後に削除を検討。

### D-3: フィーチャーフラグの段階的ロールアウト
基本的なフィーチャーフラグのみ実装し、MVP を早期リリース。A/B テスト・カナリアリリース機能は将来の拡張として、メタデータ JSON で対応可能な設計を維持。

### D-4: ライセンスティア体系
FREE, TRIAL, PRO, MAX の4段階を採用。TRIAL は期間限定の評価版として位置づけ、PRO 相当の機能を提供。具体的な TRIAL 機能実装は別スコープで対応。

### D-5: 初期データ投入方式
CSV ファイル (`backend/src/main/resources/data/feature_flags.csv`) による一括投入を採用。データの可読性・メンテナンス性を向上し、環境別の初期データ管理を容易にする。

### D-6: 将来拡張用カラムの事前実装（Forward Compatibility）
Phase 1 実装時に、Phase 2+ で必要となるカラムをエンティティ・DBスキーマに予め定義する。これにより、将来のマイグレーションコストを削減し、ダウンタイムなしでの機能拡張を可能にする。

**Phase 1で定義するが未使用のカラム:**
- `rollout_percentage`: カナリアリリース用（Phase 3）
- `license_resolve_strategy`: テナント/ユーザーライセンス判定戦略（Phase 2）
- `disabled_for_user_ids`, `disabled_for_tenant_ids`: ブラックリスト制御（Phase 2）
- `target_segments`: セグメント別ロールアウト（Phase 3）

**実装方針:**
- エンティティフィールドは `@Column(nullable = true)` で定義
- CSV初期データにはデフォルト値を設定（`rollout_percentage=100`, `license_resolve_strategy=TENANT_PRIORITY`, 配列系は `[]`）
- Phase 1のビジネスロジックでは **これらのカラムを参照しない** (Phase 2以降で段階的に実装)
- 管理画面UIでは非表示、またはツールチップで「Phase 2以降で利用可能」と表示

**メリット:**
- ALTER TABLE によるスキーマ変更が不要（本番環境での移行リスク軽減）
- 新機能追加時にエンティティクラスの変更が最小限
- 既存データへのバックフィル不要

---

## 補足: データモデル詳細

### FeatureFlag エンティティスキーマ

```sql
CREATE TABLE mir_feature_flag (
    id VARCHAR(36) PRIMARY KEY,
    feature_key VARCHAR(100) NOT NULL UNIQUE,
    feature_name VARCHAR(200) NOT NULL,
    description TEXT,
    application_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL, -- STABLE, BETA, ALPHA, PLANNING, DEPRECATED
    in_development BOOLEAN DEFAULT FALSE,
    required_license_tier VARCHAR(20), -- FREE, TRIAL, PRO, MAX, NULL
    
    -- 基本有効化制御
    enabled_by_default BOOLEAN DEFAULT TRUE,
    enabled_for_user_ids TEXT, -- JSON array: 特定ユーザーのみ有効化
    enabled_for_tenant_ids TEXT, -- JSON array: 特定テナントのみ有効化
    disabled_for_user_ids TEXT, -- JSON array: 特定ユーザーは無効化 (Phase 2+)
    disabled_for_tenant_ids TEXT, -- JSON array: 特定テナントは無効化 (Phase 2+)
    
    -- カナリアリリース用 (Phase 3+)
    rollout_percentage INT DEFAULT 100, -- 0-100: 段階的ロールアウト比率
    target_segments TEXT, -- JSON array: ターゲットセグメント (beta_users, early_adopters等)
    
    -- ライセンス判定戦略 (Phase 2+)
    license_resolve_strategy VARCHAR(20) DEFAULT 'TENANT_PRIORITY', -- TENANT_PRIORITY, USER_PRIORITY, TENANT_ONLY, USER_ONLY, EITHER
    
    -- 拡張メタデータ
    metadata TEXT, -- JSON: A/Bテスト設定、使用量制限、UI表示設定等
    
    -- 標準カラム
    version BIGINT NOT NULL DEFAULT 1,
    delete_flag BOOLEAN DEFAULT FALSE,
    create_user_id VARCHAR(36),
    create_date TIMESTAMP,
    update_user_id VARCHAR(36),
    update_date TIMESTAMP,
    
    -- インデックス
    INDEX idx_ff_application (application_id),
    INDEX idx_ff_status (status),
    INDEX idx_ff_in_development (in_development),
    INDEX idx_ff_rollout (rollout_percentage) -- カナリアリリース用
);
```

### 初期データ（CSV形式）

ファイル: `backend/src/main/resources/data/feature_flags.csv`

```csv
id,feature_key,feature_name,description,application_id,status,in_development,required_license_tier,enabled_by_default,rollout_percentage,license_resolve_strategy,enabled_for_user_ids,enabled_for_tenant_ids,disabled_for_user_ids,disabled_for_tenant_ids,target_segments,metadata
ff-pm-001,promarker.basic_generation,基本コード生成,単一ファイルのコード生成機能,promarker,STABLE,false,FREE,true,100,TENANT_PRIORITY,,,,,,[]
ff-pm-002,promarker.multi_file_generation,複数ファイル生成,複数ファイルの一括コード生成,promarker,STABLE,false,PRO,true,100,TENANT_PRIORITY,,,,,,[]
ff-pm-003,promarker.stencil_editor,ステンシルエディタ,カスタムテンプレート編集・バージョン管理,promarker,STABLE,false,PRO,true,100,TENANT_PRIORITY,,,,,,[]
ff-pm-004,promarker.custom_stencil_upload,カスタムステンシルアップロード,独自ステンシルのアップロード機能,promarker,BETA,false,MAX,true,100,TENANT_PRIORITY,,,,,,[]
ff-sys-001,users_tenants_management,ユーザ & テナント管理,認証・権限・テナントスコープ統合管理,mirelplatform,PLANNING,true,,true,100,TENANT_PRIORITY,,,,,,[]
ff-sys-002,themes_switcher,テーマスイッチャ,ブランド毎のテーマ・配色切替,mirelplatform,ALPHA,true,,true,100,TENANT_PRIORITY,,,,,,[]
ff-sys-003,menu_nav_management,メニュー/ナビ集中管理,統一メニュー管理システム,mirelplatform,PLANNING,true,,true,100,TENANT_PRIORITY,,,,,,[]
ff-sys-004,context_management,コンテキスト管理,利用者・テナント・業務文脈の状態共有,mirelplatform,PLANNING,true,,true,100,TENANT_PRIORITY,,,,,,[]
```

**CSVカラム仕様:**

*Phase 1 で使用するカラム:*
- `id`: UUID（手動生成、`ff-{app}-{seq}` 形式推奨）
- `feature_key`: 一意識別子（ドット区切り）
- `feature_name`: 日本語表示名
- `description`: 機能説明
- `application_id`: アプリケーションID（promarker, mirelplatform, etc.）
- `status`: STABLE, BETA, ALPHA, PLANNING, DEPRECATED
- `in_development`: true/false
- `required_license_tier`: FREE, TRIAL, PRO, MAX, 空文字（null相当）
- `enabled_by_default`: true/false
- `metadata`: JSON文字列（空の場合は `[]`）

*Phase 2+ で使用する拡張カラム（Phase 1ではデフォルト値のまま）:*
- `rollout_percentage`: 0-100（デフォルト: 100）カナリアリリース用ロールアウト比率
- `license_resolve_strategy`: TENANT_PRIORITY, USER_PRIORITY, TENANT_ONLY, USER_ONLY, EITHER（デフォルト: TENANT_PRIORITY）
- `enabled_for_user_ids`: JSON配列文字列（例: `["user-001","user-002"]`）特定ユーザーのみ有効化
- `enabled_for_tenant_ids`: JSON配列文字列（例: `["tenant-001"]`）特定テナントのみ有効化
- `disabled_for_user_ids`: JSON配列文字列。ブラックリスト制御用（Phase 2+）
- `disabled_for_tenant_ids`: JSON配列文字列。ブラックリスト制御用（Phase 2+）
- `target_segments`: JSON配列文字列（例: `["beta_users","early_adopters"]`）セグメント別ロールアウト（Phase 3+）

**Phase 1 実装時の注意:**
- 拡張カラムはエンティティとDBスキーマに定義するが、ビジネスロジックでは使用しない
- CSV読み込み時にデフォルト値を設定（`rollout_percentage=100`, `license_resolve_strategy=TENANT_PRIORITY`, 他は空配列`[]`）
- 管理画面UIでは拡張カラムを非表示、または「将来拡張」として表示のみ

---

**計画書作成日:** 2025年11月28日  
**対象Issue:** #40  
**実装優先度:** 高  
**推定工数:** 12-16 人日（バックエンド 5-7日、フロントエンド 5-7日、テスト 2日）

Powered by Copilot 🤖