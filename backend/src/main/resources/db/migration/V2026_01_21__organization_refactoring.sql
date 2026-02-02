-- =============================================================================
-- Organization Entity Refactoring Migration Script
-- Issue: #89
-- Created: 2026-01-21
-- =============================================================================
-- NOTE: このプロジェクトはHibernate DDL-Autoを使用しているため、
-- このスクリプトは参照用ドキュメントとして、および手動実行用に提供されます。
-- 実際の変更はエンティティクラスの変更によって自動適用されます。
-- =============================================================================

-- ==============================
-- Step 1: 新規テーブル作成
-- ==============================

-- mir_company_settings: 会社レベルの設定
CREATE TABLE IF NOT EXISTS mir_company_settings (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_company_settings_org ON mir_company_settings(organization_id);
CREATE INDEX IF NOT EXISTS idx_company_settings_period ON mir_company_settings(period_code);

-- mir_organization_settings: 組織レベルの設定
CREATE TABLE IF NOT EXISTS mir_organization_settings (
  id VARCHAR(36) PRIMARY KEY,
  organization_id VARCHAR(36) NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_organization_settings_org ON mir_organization_settings(organization_id);
CREATE INDEX IF NOT EXISTS idx_organization_settings_period ON mir_organization_settings(period_code);

-- 外部キー制約（参照用: Hibernate DDL-Autoにより自動適用されるため手動実行は不要）
-- 手動でFKを追加する場合は以下を実行:
-- ALTER TABLE mir_company_settings
--   ADD CONSTRAINT fk_company_settings_org
--   FOREIGN KEY (organization_id) REFERENCES mir_organization(id) ON DELETE CASCADE;
-- ALTER TABLE mir_organization_settings
--   ADD CONSTRAINT fk_organization_settings_org
--   FOREIGN KEY (organization_id) REFERENCES mir_organization(id) ON DELETE CASCADE;

-- ==============================
-- Step 2: mir_organization 統合（mir_organization_unit との統合）
-- ==============================
-- 注意: Hibernateを使用しているため、エンティティの変更によって自動的に適用されます。
-- 以下は参考として実際のDDL変更を記載しています。

-- 新しい mir_organization テーブル定義 (統合版)
-- CREATE TABLE mir_organization (
--   id VARCHAR(36) PRIMARY KEY,
--   tenant_id VARCHAR(36) NOT NULL,
--   parent_id VARCHAR(36),
--   name VARCHAR(255) NOT NULL,
--   display_name VARCHAR(255),
--   code VARCHAR(100) UNIQUE,
--   type VARCHAR(50),
--   path VARCHAR(1024),
--   level INT,
--   sort_order INT,
--   is_active BOOLEAN DEFAULT TRUE,
--   start_date DATE,
--   end_date DATE,
--   period_code VARCHAR(50),
--   version BIGINT NOT NULL DEFAULT 1,
--   delete_flag BOOLEAN DEFAULT FALSE,
--   create_user_id VARCHAR(36),
--   create_date TIMESTAMP,
--   update_user_id VARCHAR(36),
--   update_date TIMESTAMP
-- );

-- CREATE INDEX idx_organization_tenant ON mir_organization(tenant_id);
-- CREATE INDEX idx_organization_parent ON mir_organization(parent_id);
-- CREATE INDEX idx_organization_path ON mir_organization(path);
-- CREATE INDEX idx_organization_type ON mir_organization(type);
-- CREATE INDEX idx_organization_period ON mir_organization(period_code);

-- ==============================
-- Step 3: mir_user_organization 変更
-- ==============================
-- 注意: エンティティの変更により自動適用されます。

-- unit_id -> organization_id へのリネーム
-- ALTER TABLE mir_user_organization RENAME COLUMN unit_id TO organization_id;

-- role カラム追加
-- ALTER TABLE mir_user_organization ADD COLUMN role VARCHAR(50);

-- is_manager から role へのデータ移行
-- UPDATE mir_user_organization SET role = CASE WHEN is_manager = true THEN 'manager' ELSE 'member' END;

-- is_manager カラム削除
-- ALTER TABLE mir_user_organization DROP COLUMN is_manager;
