ALTER TABLE workspace_invitations
    ADD COLUMN IF NOT EXISTS type VARCHAR(50) NOT NULL DEFAULT 'EMAIL',
    ADD COLUMN IF NOT EXISTS token_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS max_uses INTEGER,
    ADD COLUMN IF NOT EXISTS use_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS email_delivery_status VARCHAR(50) NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD COLUMN IF NOT EXISTS email_delivery_error VARCHAR(500);

UPDATE workspace_invitations
SET token_hash = md5(id || '_legacy_token')
WHERE token_hash IS NULL;

ALTER TABLE workspace_invitations
    ALTER COLUMN token_hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_workspace_invitations_token_hash ON workspace_invitations(token_hash);
CREATE INDEX IF NOT EXISTS idx_workspace_invitations_type ON workspace_invitations(type);
