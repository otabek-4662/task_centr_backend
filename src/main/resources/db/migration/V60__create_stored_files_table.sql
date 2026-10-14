-- V60: Persistent database file storage table for ephemeral container environments (e.g. Render Free)
CREATE TABLE IF NOT EXISTS stored_files (
    storage_path VARCHAR(512) PRIMARY KEY,
    content_type VARCHAR(128) NOT NULL DEFAULT 'application/octet-stream',
    file_size BIGINT NOT NULL DEFAULT 0,
    file_data BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stored_files_created_at ON stored_files (created_at DESC);
