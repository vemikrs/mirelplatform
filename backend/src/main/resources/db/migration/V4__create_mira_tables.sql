-- Mira (mirel Assist) テーブル作成マイグレーション
-- Version: V4__create_mira_tables.sql
-- Issue: #50

-- ========================================
-- mir_conversation: 会話セッション
-- ========================================
CREATE TABLE IF NOT EXISTS mir_conversation (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    mode VARCHAR(30) NOT NULL,
    title VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mir_conv_tenant_user ON mir_conversation(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_mir_conv_created ON mir_conversation(created_at);

-- ========================================
-- mir_message: メッセージ
-- ========================================
CREATE TABLE IF NOT EXISTS mir_message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    content TEXT,
    content_type VARCHAR(20) NOT NULL DEFAULT 'PLAIN',
    context_snapshot_id VARCHAR(36),
    used_model VARCHAR(100),
    token_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mir_msg_conversation ON mir_message(conversation_id);
CREATE INDEX IF NOT EXISTS idx_mir_msg_created ON mir_message(created_at);

-- ========================================
-- mir_context_snapshot: コンテキストスナップショット
-- ========================================
CREATE TABLE IF NOT EXISTS mir_context_snapshot (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    app_id VARCHAR(50) NOT NULL,
    screen_id VARCHAR(100) NOT NULL,
    system_role VARCHAR(30),
    app_role VARCHAR(30),
    payload TEXT,  -- JSON形式（H2/PostgreSQL両対応のためTEXT使用）
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mir_ctx_tenant_user ON mir_context_snapshot(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_mir_ctx_app_screen ON mir_context_snapshot(app_id, screen_id);

-- ========================================
-- mir_audit_log: 監査ログ
-- ========================================
CREATE TABLE IF NOT EXISTS mir_audit_log (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    conversation_id VARCHAR(36) NOT NULL,
    message_id VARCHAR(36),
    action VARCHAR(30) NOT NULL,
    mode VARCHAR(30),
    app_id VARCHAR(50),
    screen_id VARCHAR(100),
    prompt_length INTEGER,
    response_length INTEGER,
    prompt_hash VARCHAR(64),
    used_model VARCHAR(100),
    latency_ms INTEGER,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mir_audit_tenant_created ON mir_audit_log(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_mir_audit_user_created ON mir_audit_log(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_mir_audit_conversation ON mir_audit_log(conversation_id);
