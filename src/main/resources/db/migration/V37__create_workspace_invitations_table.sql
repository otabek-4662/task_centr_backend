CREATE TABLE IF NOT EXISTS workspace_invitations (
    id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    sender_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id VARCHAR(255) REFERENCES users(id) ON DELETE CASCADE,
    receiver_email VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_workspace_invitations_receiver ON workspace_invitations(receiver_id);
CREATE INDEX IF NOT EXISTS idx_workspace_invitations_workspace ON workspace_invitations(workspace_id);
