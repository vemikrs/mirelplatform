-- Migration: Add username column to mir_system_user
-- Date: 2025-11-23
-- Issue: #40

-- Step 1: Add username column as nullable
ALTER TABLE mir_system_user ADD COLUMN IF NOT EXISTS username VARCHAR(100);

-- Step 2: Update existing records with username derived from email
UPDATE mir_system_user 
SET username = SUBSTRING(email, 1, POSITION('@' IN email) - 1)
WHERE username IS NULL;

-- Step 3: Make username NOT NULL and UNIQUE
ALTER TABLE mir_system_user ALTER COLUMN username SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_username ON mir_system_user(username);

-- Add OAuth2 columns if not exists
ALTER TABLE mir_system_user ADD COLUMN IF NOT EXISTS oauth2_provider VARCHAR(50);
ALTER TABLE mir_system_user ADD COLUMN IF NOT EXISTS oauth2_provider_id VARCHAR(255);
ALTER TABLE mir_system_user ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
