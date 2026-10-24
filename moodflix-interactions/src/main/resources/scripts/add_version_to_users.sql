-- Migration: Add version column to users for Optimistic Locking
-- Rollback consideration: Column has DEFAULT 0, fully backward compatible with Gateway R2DBC.
ALTER TABLE users
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0 NOT NULL;