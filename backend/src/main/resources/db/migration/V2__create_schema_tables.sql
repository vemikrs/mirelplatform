-- 更新日時自動更新用トリガー関数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- レコードテーブル
CREATE TABLE sch_record (
    id VARCHAR(36) PRIMARY KEY,
    schema VARCHAR(255),
    record_data JSONB,
    text VARCHAR(255),
    tenant_id VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_sch_record_schema ON sch_record(schema);
CREATE INDEX idx_sch_record_updated ON sch_record(updated_at DESC);
CREATE INDEX idx_sch_record_tenant ON sch_record(tenant_id);
-- JSONB GINインデックス（全文検索用）
CREATE INDEX idx_sch_record_data_gin ON sch_record USING GIN (record_data);

-- モデル定義テーブル
CREATE TABLE sch_dic_model (
    model_id VARCHAR(255) NOT NULL,
    field_id VARCHAR(255) NOT NULL,
    is_key BOOLEAN DEFAULT FALSE,
    field_name VARCHAR(255),
    widget_type VARCHAR(50),
    data_type VARCHAR(50),
    description TEXT,
    sort BIGINT,
    is_header BOOLEAN DEFAULT FALSE,
    display_width BIGINT,
    format VARCHAR(255),
    constraint_id VARCHAR(255),
    max_digits DECIMAL,
    is_required BOOLEAN DEFAULT FALSE,
    default_value TEXT,
    relation_code_group VARCHAR(255),
    function TEXT,
    regex_pattern VARCHAR(500),
    min_length INTEGER,
    max_length INTEGER,
    min_value DECIMAL,
    max_value DECIMAL,
    tenant_id VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (model_id, field_id)
);

CREATE INDEX idx_sch_dic_model_model ON sch_dic_model(model_id);
CREATE INDEX idx_sch_dic_model_tenant ON sch_dic_model(tenant_id);

-- モデルヘッダーテーブル
CREATE TABLE sch_dic_model_header (
    model_id VARCHAR(255) PRIMARY KEY,
    model_name VARCHAR(255),
    description TEXT,
    is_hidden BOOLEAN DEFAULT FALSE,
    tenant_id VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NOT NULL
);

CREATE INDEX idx_sch_dic_model_header_tenant ON sch_dic_model_header(tenant_id);

-- コードマスタテーブル
CREATE TABLE sch_dic_code (
    group_id VARCHAR(255) NOT NULL,
    code VARCHAR(255) NOT NULL,
    group_text VARCHAR(255),
    text VARCHAR(255),
    sort BIGINT,
    delete_flag BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (group_id, code)
);

CREATE INDEX idx_sch_dic_code_group ON sch_dic_code(group_id);
CREATE INDEX idx_sch_dic_code_active ON sch_dic_code(group_id) WHERE delete_flag = FALSE;
CREATE INDEX idx_sch_dic_code_tenant ON sch_dic_code(tenant_id);

-- 各テーブルへのトリガー適用
CREATE TRIGGER update_sch_record_updated_at
    BEFORE UPDATE ON sch_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sch_dic_model_updated_at
    BEFORE UPDATE ON sch_dic_model
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sch_dic_model_header_updated_at
    BEFORE UPDATE ON sch_dic_model_header
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sch_dic_code_updated_at
    BEFORE UPDATE ON sch_dic_code
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
